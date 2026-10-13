package piscicola;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.security.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import static piscicola.Model.*;

public final class Store implements AutoCloseable {
    final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    final Path file;
    final FileChannel channel;
    final FileLock lock;
    public Database db;
    public Store(Path directory) throws Exception {
        Files.createDirectories(directory); file=directory.resolve("piscicola.json");
        channel=FileChannel.open(directory.resolve("app.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock acquired;
        try { acquired=channel.tryLock(); } catch (OverlappingFileLockException e) { acquired=null; }
        if(acquired==null) { channel.close(); throw new IOException("La aplicación ya está abierta para estos datos."); }
        lock=acquired;
        try {
            if(Files.exists(file)) {
                db=gson.fromJson(Files.readString(file), Database.class);
                if(db==null || db.version!=1 || db.users==null || db.ponds==null || db.lots==null || db.feedings==null || db.notices==null || db.audit==null)
                    throw new IOException("El JSON no tiene un formato válido. Conserve el archivo y revise la copia .bak.");
            } else { db=demo(); save(); }
        } catch(Exception e) { close(); throw e; }
    }
    public void transaction(Runnable change) {
        String before=gson.toJson(db);
        try { change.run(); save(); } catch(Exception e) { db=gson.fromJson(before, Database.class); throw new IllegalArgumentException("No se guardaron los cambios: " + e.getMessage(),e); }
    }
    private void save() throws IOException {
        Path temp=file.resolveSibling("piscicola.json.tmp");
        byte[] bytes=gson.toJson(db).getBytes(StandardCharsets.UTF_8);
        try(FileChannel out=FileChannel.open(temp, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            java.nio.ByteBuffer buffer=java.nio.ByteBuffer.wrap(bytes); while(buffer.hasRemaining()) out.write(buffer); out.force(true);
        }
        if(Files.exists(file)) Files.copy(file,file.resolveSibling("piscicola.json.bak"),StandardCopyOption.REPLACE_EXISTING);
        // Do not silently fall back to a non-atomic overwrite.
        Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
    }
    public User login(String id, char[] password, String role) {
        try { return db.users.stream().filter(u->u.active && u.id.equals(id) && u.role.equals(role)
                && MessageDigest.isEqual(Base64.getDecoder().decode(u.hash),derive(password,u.salt))).findFirst()
                .orElseThrow(()->new IllegalArgumentException("Usuario, contraseña o cargo incorrectos; verifique que la cuenta esté activa."));
        } finally { Arrays.fill(password,'\0'); }
    }
    private static byte[] derive(char[] password,String salt) {
        PBEKeySpec spec=new PBEKeySpec(password,Base64.getDecoder().decode(salt),120000,256);
        try { return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded(); }
        catch(Exception e) { throw new IllegalStateException(e); } finally { spec.clearPassword(); }
    }
    static User user(String id,String name,String role,String station,String password) {
        User u=new User(); u.id=id; u.name=name; u.role=role; u.station=station;
        byte[] salt=new byte[16]; new SecureRandom().nextBytes(salt); u.salt=Base64.getEncoder().encodeToString(salt);
        u.hash=Base64.getEncoder().encodeToString(derive(password.toCharArray(),u.salt)); return u;
    }
    private static Database demo() {
        Database d=new Database();
        d.users.add(user("piscicultor","Ana Martínez",OWNER,"Todas","Piscicola123!"));
        d.users.add(user("encargado","Carlos Pérez",KEEPER,"Cumaral","Estanque123!"));
        d.users.add(user("encargado2","Laura Gómez",KEEPER,"Cubarral","Estanque123!"));
        for(int i=1;i<=3;i++) {
            Pond p=new Pond(); p.id="E00"+i; p.name="Estanque "+i; p.station=i==3?"Cubarral":"Cumaral";
            p.keeperId=i==3?"encargado2":"encargado"; p.species=i==2?"Cachama":"Tilapia";
            p.size=1200; p.system="Aireador"; p.purpose="Venta"; p.state=i==2?"En descanso":"En producción"; d.ponds.add(p);
            if(i!=2) { Lot l=new Lot(); l.id="L00"+i; l.pondId=p.id; l.species=p.species; l.date=java.time.LocalDate.now().minusMonths(2).toString(); l.quantity=2000; d.lots.add(l); }
        }
        return d;
    }
    @Override public void close() throws IOException { if(lock.isValid()) lock.release(); channel.close(); }
}

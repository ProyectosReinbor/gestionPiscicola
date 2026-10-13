package piscicola;

import java.nio.file.*;
import java.time.*;
import static piscicola.Model.*;

public class ServiceTest {
    static int checks;
    static void check(boolean value,String label) { if(!value)throw new AssertionError(label);checks++; }
    static void denied(Runnable action) { try {action.run();throw new AssertionError("Se esperaba rechazo");}catch(IllegalArgumentException expected){checks++;} }
    public static void main(String[] args) throws Exception {
        Path dir=Files.createTempDirectory(Path.of("build"),"test-data-");
        try(Store store=new Store(dir)) {
            denied(()->store.login("encargado","Estanque123!".toCharArray(),OWNER));
            Service owner=new Service(store,store.login("piscicultor","Piscicola123!".toCharArray(),OWNER));
            Service keeper=new Service(store,store.login("encargado","Estanque123!".toCharArray(),KEEPER));
            check(owner.ponds().size()==3,"visibilidad total");check(keeper.ponds().size()==2,"asignación");
            denied(()->keeper.pond("E003")); denied(()->keeper.cancel(keeper.lots().getFirst(),"prueba"));
            denied(()->owner.feed("E001",LocalDate.now().toString(),2,"Concentrado"));
            denied(()->keeper.feed("E003",LocalDate.now().toString(),2,"Concentrado"));
            denied(()->keeper.feed("E001",LocalDate.now().toString(),Double.NaN,"Concentrado"));
            keeper.feed("E001",LocalDate.now().toString(),12.5,"Concentrado");
            keeper.feed("E001",LocalDate.now().toString(),2.5,"Concentrado");
            check(owner.total(owner.active("E001"))==15,"acumulación");
            keeper.notice("E001","Boqueo","Peces en superficie",null);
            check(owner.notices().size()==1,"notificación local");
            owner.resolve(owner.notices().getFirst(),true);
            check(owner.notices().getFirst().status.equals("Aprobado / atendido"),"atención");
            keeper.notice("E001","Daño / inutilización","Aireador dañado","Dañado");
            Notice report=owner.notices().getLast(); denied(()->owner.resolve(report,true));
            // A rejected transaction reloads objects: resolve by persisted ID.
            owner.cancel(owner.active("E001"),"Mortalidad total");
            check(owner.pond("E001").state.equals("En descanso"),"liberación");
            owner.resolve(report,true);check(owner.pond("E001").state.equals("Dañado"),"aprobación daño");
            Lot l=new Lot();l.pondId="E002";l.species="Cachama";l.date=LocalDate.now().toString();l.quantity=500;
            owner.saveLot(l,true,"Carga administrativa inicial");check(owner.pond("E002").state.equals("En producción"),"lote inicial");
            denied(()->owner.saveLot(newLot(),true,"duplicado"));
            int old=store.db.audit.size(); denied(()->store.transaction(()-> {store.db.audit.clear();throw new IllegalArgumentException("simular error");}));
            check(store.db.audit.size()==old,"rollback");
            try {new Store(dir);throw new AssertionError("bloqueo esperado");}catch(java.io.IOException expected){checks++;}
        }
        try(Store reopened=new Store(dir)) {
            Service owner=new Service(reopened,reopened.login("piscicultor","Piscicola123!".toCharArray(),OWNER));
            check(owner.feedings().size()==2,"persistencia al reiniciar");check(owner.notices().size()==2,"persistencia alertas");
            reopened.db.users.stream().filter(u->u.id.equals("encargado")).findFirst().orElseThrow().active=false;
            denied(()->reopened.login("encargado","Estanque123!".toCharArray(),KEEPER));
        }
        Files.writeString(dir.resolve("piscicola.json"),"{invalid");
        try {new Store(dir);throw new AssertionError("JSON corrupto debe rechazarse");}catch(Exception expected){check(Files.readString(dir.resolve("piscicola.json")).equals("{invalid"),"conservar corrupción");}
        System.out.println("OK: "+checks+" comprobaciones. Datos aislados: "+dir);
    }
    static Lot newLot(){Lot l=new Lot();l.pondId="E002";l.species="Cachama";l.date=LocalDate.now().toString();l.quantity=2;return l;}
}

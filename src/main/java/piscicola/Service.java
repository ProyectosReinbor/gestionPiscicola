package piscicola;

import static piscicola.Model.*;
import java.time.*;
import java.util.*;

public final class Service {
    final Store store; final String userId;
    public Service(Store store,User user) { this.store=store; this.userId=user.id; }
    public User user() { return store.db.users.stream().filter(u->u.id.equals(userId)&&u.active).findFirst().orElseThrow(()->new IllegalArgumentException("Cuenta desactivada.")); }
    public boolean owner() { return user().role.equals(OWNER); }
    void requireOwner() { if(!owner()) throw new IllegalArgumentException("Esta operación requiere el cargo Piscicultor."); }
    void requireKeeper() { if(!user().role.equals(KEEPER)) throw new IllegalArgumentException("Esta operación requiere el cargo Encargado de la estación."); }
    public List<Pond> ponds() { return store.db.ponds.stream().filter(p->owner() || (p.station.equals(user().station)&&p.keeperId.equals(userId)&&!p.state.equals("En prueba"))).toList(); }
    public Pond pond(String id) { return ponds().stream().filter(p->p.id.equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Estanque fuera de su asignación.")); }
    public List<Lot> lots() { return store.db.lots.stream().filter(l->ponds().stream().anyMatch(p->p.id.equals(l.pondId))).toList(); }
    public List<Feeding> feedings() { return store.db.feedings.stream().filter(f->ponds().stream().anyMatch(p->p.id.equals(f.pondId))).toList(); }
    public List<Notice> notices() { return store.db.notices.stream().filter(n->ponds().stream().anyMatch(p->p.id.equals(n.pondId))).toList(); }
    public String keeper(Pond p) { return store.db.users.stream().filter(u->u.id.equals(p.keeperId)).map(u->u.name).findFirst().orElse("Sin encargado"); }
    public double total(Lot l) { return feedings().stream().filter(f->f.lotId.equals(l.id)).mapToDouble(f->f.kg).sum(); }
    public Lot active(String pondId) { return lots().stream().filter(l->l.pondId.equals(pondId)&&l.state.equals("Activo")).findFirst().orElseThrow(()->new IllegalArgumentException("El estanque no tiene un lote activo.")); }
    void audit(String message) { store.db.audit.add(new Audit(userId,message)); }
    static void text(String s,String name) { if(s==null || s.isBlank()) throw new IllegalArgumentException("Complete "+name+"."); }
    public void savePond(Pond p,boolean create) {
        requireOwner(); text(p.name,"nombre"); text(p.station,"estación"); text(p.system,"sistema"); text(p.species,"especie");
        if(!Double.isFinite(p.size)||p.size<=0) throw new IllegalArgumentException("El tamaño debe ser mayor que cero.");
        if(store.db.users.stream().noneMatch(u->u.active&&u.role.equals(KEEPER)&&u.id.equals(p.keeperId)&&u.station.equals(p.station))) throw new IllegalArgumentException("El encargado debe estar activo y pertenecer a la estación.");
        if(!create) {
            pond(p.id);
            if(store.db.lots.stream().anyMatch(l->l.pondId.equals(p.id)&&l.state.equals("Activo"))) {
                if(!p.state.equals("En producción")) throw new IllegalArgumentException("Cancele el lote activo antes de cambiar el estado del estanque.");
                Pond old=pond(p.id); if(!old.station.equals(p.station)||!old.species.equals(p.species)) throw new IllegalArgumentException("No cambie estación o especie mientras exista un lote activo.");
            } else if(p.state.equals("En producción")) throw new IllegalArgumentException("En producción requiere un lote activo.");
        } else if(p.state.equals("En producción")) throw new IllegalArgumentException("Registre primero el estanque disponible y después su lote inicial.");
        store.transaction(()-> { if(create) { p.id=UUID.randomUUID().toString(); store.db.ponds.add(p); } else { store.db.ponds.removeIf(x->x.id.equals(p.id)); store.db.ponds.add(p); } audit("Estanque guardado: "+p.id); });
    }
    public void saveLot(Lot l,boolean create,String reason) {
        requireOwner(); Pond p=pond(l.pondId); text(l.species,"especie"); text(reason,"motivo del registro o corrección");
        if(l.quantity<=0 || l.months<5 || l.months>6) throw new IllegalArgumentException("Cantidad positiva y proyección de 5 o 6 meses requeridas.");
        LocalDate date=LocalDate.parse(l.date); if(date.isAfter(LocalDate.now())) throw new IllegalArgumentException("La fecha no puede ser futura.");
        if(create && (!List.of("Inactivo","En descanso","Activo").contains(p.state)||store.db.lots.stream().anyMatch(x->x.pondId.equals(p.id)&&x.state.equals("Activo")))) throw new IllegalArgumentException("El estanque no está disponible.");
        if(!create) { Lot old=lots().stream().filter(x->x.id.equals(l.id)&&x.state.equals("Activo")).findFirst().orElseThrow(()->new IllegalArgumentException("Solo se corrigen lotes activos.")); if(!old.pondId.equals(l.pondId)) throw new IllegalArgumentException("No se puede trasladar el lote.");
            if(feedings().stream().anyMatch(f->f.lotId.equals(l.id)&&LocalDate.parse(f.date).isBefore(date))) throw new IllegalArgumentException("La fecha de siembra no puede ser posterior a una alimentación registrada."); }
        store.transaction(()-> { l.reason=reason; if(create) { l.id=UUID.randomUUID().toString(); store.db.lots.add(l); } else { store.db.lots.removeIf(x->x.id.equals(l.id)); store.db.lots.add(l); } p.state="En producción"; p.species=l.species; audit("Lote "+l.id+": "+reason); });
    }
    public void cancel(Lot l,String reason) { requireOwner(); text(reason,"causa de cancelación"); Lot current=lots().stream().filter(x->x.id.equals(l.id)&&x.state.equals("Activo")).findFirst().orElseThrow(()->new IllegalArgumentException("Seleccione un lote activo.")); Pond p=pond(current.pondId);
        store.transaction(()-> { current.state="Cancelado"; current.reason=reason; p.state="En descanso"; audit("Cancelación "+current.id+": "+reason); }); }
    public void feed(String pondId,String date,double kg,String food) {
        requireKeeper(); Pond p=pond(pondId); Lot l=active(p.id); text(food,"alimento"); LocalDate day=LocalDate.parse(date);
        if(day.isAfter(LocalDate.now())||day.isBefore(LocalDate.parse(l.date))) throw new IllegalArgumentException("La fecha debe estar entre la siembra y hoy.");
        if(!Double.isFinite(kg)||kg<=0) throw new IllegalArgumentException("Cantidad de alimento positiva requerida.");
        store.transaction(()-> { Feeding f=new Feeding(); f.id=UUID.randomUUID().toString(); f.pondId=p.id; f.lotId=l.id; f.date=date; f.kg=kg; f.food=food; f.userId=userId; store.db.feedings.add(f); audit("Alimentación: "+f.id); });
    }
    public void notice(String pondId,String type,String detail,String state) {
        requireKeeper(); Pond p=pond(pondId); text(detail,"descripción");
        store.transaction(()-> { Notice n=new Notice(); n.id=UUID.randomUUID().toString(); n.pondId=p.id; n.type=type; n.detail=detail; n.date=LocalDateTime.now().toString(); n.userId=userId; n.requestedState=state; store.db.notices.add(n); audit("Alerta/reporte: "+n.id); });
    }
    public void resolve(Notice notice,boolean approve) {
        requireOwner(); Notice n=notices().stream().filter(x->x.id.equals(notice.id)&&x.status.equals("Pendiente")).findFirst().orElseThrow(()->new IllegalArgumentException("Seleccione un reporte pendiente.")); Pond p=pond(n.pondId);
        if(approve && n.requestedState!=null && store.db.lots.stream().anyMatch(l->l.pondId.equals(p.id)&&l.state.equals("Activo"))) throw new IllegalArgumentException("Cancele el lote antes de aprobar la inutilización del estanque.");
        store.transaction(()-> { n.status=approve?"Aprobado / atendido":"Rechazado"; if(approve&&n.requestedState!=null) p.state=n.requestedState; audit("Reporte "+n.id+": "+n.status); });
    }
}

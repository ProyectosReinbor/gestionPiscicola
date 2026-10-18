package piscicola;

import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.*;
import javafx.collections.*;
import javafx.beans.property.SimpleStringProperty;
import java.nio.file.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import static piscicola.Model.*;

public class App extends Application {
    Store store; Service service; Stage stage; BorderPane shell; VBox content;
    public static void main(String[] args) { launch(args); }
    @Override public void start(Stage stage) {
        this.stage=stage;
        try { store=new Store(Path.of(System.getProperty("piscicola.data","data"))); }
        catch(Exception e) { message("No se pudieron abrir los datos",e.getMessage()); Platform.exit(); return; }
        stage.setTitle("Gestión de una piscícola"); stage.setMinWidth(960); stage.setMinHeight(680); login(); stage.show();
        if(getParameters().getRaw().contains("--smoke")) { Platform.runLater(()-> { snapshot("login"); service=new Service(store,store.login("piscicultor","Piscicola123!".toCharArray(),OWNER)); home(); ponds(); lots(); data(); snapshot("piscicultor"); service=new Service(store,store.login("encargado","Estanque123!".toCharArray(),KEEPER)); home(); ponds(); lots(); feeding(); snapshot("alimentacion"); alerts(); data(); snapshot("encargado"); System.out.println("UI_SMOKE_OK"); Platform.exit(); }); }
    }
    @Override public void stop() throws Exception { if(store!=null) store.close(); }
    void snapshot(String name) {
        try {
            Parent root=stage.getScene().getRoot(); root.applyCss(); root.layout();
            javafx.scene.image.WritableImage fx=root.snapshot(null,null);
            java.awt.image.BufferedImage image=new java.awt.image.BufferedImage((int)fx.getWidth(),(int)fx.getHeight(),java.awt.image.BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++)image.setRGB(x,y,fx.getPixelReader().getArgb(x,y));
            Files.createDirectories(Path.of("build/screenshots")); javax.imageio.ImageIO.write(image,"png",Path.of("build/screenshots",name+".png").toFile());
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    void scene(Parent root) { Scene scene=new Scene(root,1150,760); scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm()); stage.setScene(scene); }
    Label label(String text,String style) { Label l=new Label(text); l.getStyleClass().add(style); l.setWrapText(true); return l; }
    Button button(String text,Runnable action) { Button b=new Button(text); b.setOnAction(e->run(action)); return b; }
    void run(Runnable action) { try { action.run(); } catch(Exception e) { message("Revise la información",e.getMessage()); } }
    void message(String title,String text) { Alert a=new Alert(Alert.AlertType.INFORMATION); a.setTitle(title); a.setHeaderText(title); a.setContentText(text); a.showAndWait(); }
    void login() {
        service=null;
        VBox brand=new VBox(24,label("PISCÍCOLA / VERSIÓN 01","eyebrow"),label("Gestión de una piscícola","hero"),label("El estado de tus estanques,\nen un solo lugar.","subtitle"),label("Estanques · Lotes · Alimentación · Alertas","subtitle")); brand.getStyleClass().add("brand"); brand.setPrefWidth(550); brand.setAlignment(Pos.CENTER_LEFT);
        TextField user=new TextField(); user.setPromptText("Usuario"); PasswordField password=new PasswordField(); password.setPromptText("Contraseña"); ComboBox<String> role=combo(OWNER,KEEPER);
        Button enter=button("Entrar",()-> { service=new Service(store,store.login(user.getText().trim(),password.getText().toCharArray(),role.getValue())); password.clear(); home(); }); enter.setDefaultButton(true); enter.setMaxWidth(Double.MAX_VALUE);
        VBox form=new VBox(15,label("Bienvenido","title"),label("Ingresa con tu cuenta y selecciona tu cargo.","muted"),new Label("Usuario"),user,new Label("Contraseña"),password,new Label("Cargo"),role,enter,label("Datos locales · Disponible sin internet","muted")); form.setPadding(new Insets(55)); form.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(form,Priority.ALWAYS);
        scene(new HBox(brand,form));
    }
    void home() {
        shell=new BorderPane();
        VBox menu=new VBox(14,label("PISCÍCOLA","eyebrow"),label(service.user().name,"side-name"),label(service.user().role+"\n"+service.user().station,"side-info")); menu.setPrefWidth(245); menu.getStyleClass().add("sidebar");
        menu.getChildren().addAll(button("Data / Resumen",this::data),button("Gestionar estanques",this::ponds),button("Gestionar lotes",this::lots));
        if(!service.owner()) menu.getChildren().addAll(button("Gestionar alimentación",this::feeding),button("Registrar alerta de boqueo",this::alerts));
        else menu.getChildren().add(button("Alertas y aprobaciones",this::alerts));
        Region spacer=new Region(); VBox.setVgrow(spacer,Priority.ALWAYS); menu.getChildren().addAll(spacer,label("V1 · Almacenamiento JSON","side-info"),button("Cerrar sesión",this::login)); shell.setLeft(menu); scene(shell); data();
        if(service.owner()&&service.notices().stream().anyMatch(n->n.status.equals("Pendiente"))) message("Alertas pendientes","Hay alertas o reportes pendientes. Consulte Alertas y aprobaciones.");
    }
    void page(String title,String description) {
        content=new VBox(18); content.setPadding(new Insets(30)); content.getChildren().addAll(label(title,"title"),label(description,"muted")); ScrollPane sc=new ScrollPane(content); sc.setFitToWidth(true); shell.setCenter(sc);
    }
    <T> TableView<T> table(List<T> rows,String[] names,List<Function<T,String>> values) {
        TableView<T> t=new TableView<>(FXCollections.observableArrayList(rows));
        double suma=0;
        for(int i=0;i<names.length;i++) { Function<T,String> value=values.get(i); TableColumn<T,String> c=new TableColumn<>(names[i]); c.setCellValueFactory(x->new SimpleStringProperty(value.apply(x.getValue()))); c.setPrefWidth(ancho(names[i])); suma+=c.getPrefWidth(); t.getColumns().add(c); }
        final double anchoColumnas=suma+20;
        t.widthProperty().addListener((obs,antes,ahora)->t.setColumnResizePolicy(anchoColumnas<=ahora.doubleValue()?TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN:TableView.UNCONSTRAINED_RESIZE_POLICY));
        t.setPrefHeight(300); t.setPlaceholder(new Label("No hay registros disponibles.")); return t;
    }
        double ancho(String titulo) {
        return switch (titulo) {
            case "Fecha" -> 170;
            case "Descripción" -> 280;
            case "Estado" -> 170;
            default -> Math.max(120, titulo.length() * 9 + 40);
        };
    }

    String fecha(String texto) {
        if (texto == null) return "";
        String limpio = texto.replace('T', ' ');
        return limpio.substring(0, Math.min(16, limpio.length()));
    }
    String pondName(String id) { return service.pond(id).name; }
    void data() {
        page("Data / Resumen","Información guardada en JSON · "+(service.owner()?"Todas las estaciones":"Solo sus estanques asignados"));
        long active=service.lots().stream().filter(l->l.state.equals("Activo")).count(); long pending=service.notices().stream().filter(n->n.status.equals("Pendiente")).count();
        HBox cards=new HBox(16,card("Estanques",String.valueOf(service.ponds().size())),card("Lotes activos",String.valueOf(active)),card("Alimento registrado",String.format(Locale.ROOT,"%.2f kg",service.feedings().stream().mapToDouble(f->f.kg).sum())),card("Alertas / reportes",String.valueOf(pending))); content.getChildren().add(cards);
        content.getChildren().addAll(label("Estado de los estanques","section"),table(service.ponds(),new String[]{"Estanque","Estación","Encargado","Especie","Estado"},List.of(p->p.name,p->p.station,p->service.keeper(p),p->p.species,p->p.state)),label("Lotes y consumo acumulado","section"),lotTable(),label("Alertas y reportes registrados","section"),noticeTable(),label("Archivo de datos: "+store.file.toAbsolutePath(),"muted"));
    }
    VBox card(String title,String value) { VBox box=new VBox(10,label(title,"muted"),label(value,"metric")); box.getStyleClass().add("card"); HBox.setHgrow(box,Priority.ALWAYS); box.setMaxWidth(Double.MAX_VALUE); return box; }
    void ponds() {
        page("Gestionar estanques",service.owner()?"Registre estanques, asigne encargados y mantenga su información actualizada.":"Consulte sus estanques y reporte daños para aprobación del piscicultor.");
        TableView<Pond> t=table(service.ponds(),new String[]{"Estanque","Estación","Encargado","Tamaño m²","Sistema","Estado"},List.of(p->p.name,p->p.station,p->service.keeper(p),p->String.valueOf(p.size),p->p.system,p->p.state)); content.getChildren().add(t);
        if(service.owner()) content.getChildren().add(new HBox(12,button("Nuevo estanque",()->pondForm(null)),button("Editar seleccionado",()->pondForm(selected(t)))));
        else content.getChildren().add(button("Reportar daño / inutilización",()-> { Pond p=selected(t); noticeForm(p,true); }));
    }
    <T> T selected(TableView<T> t) { T row=t.getSelectionModel().getSelectedItem(); if(row==null) throw new IllegalArgumentException("Seleccione una fila de la tabla."); return row; }
    ComboBox<String> combo(String... options) { ComboBox<String> c=new ComboBox<>(FXCollections.observableArrayList(options)); if(options.length>0)c.getSelectionModel().selectFirst(); c.setMaxWidth(Double.MAX_VALUE); return c; }
    TextField field(String value) { return new TextField(value==null?"":value); }
    GridPane grid() { GridPane g=new GridPane(); g.setHgap(18); g.setVgap(14); g.setPadding(new Insets(15)); return g; }
    void row(GridPane g,int index,String name,Node node) { g.add(new Label(name),0,index); g.add(node,1,index); if(node instanceof Region r) r.setPrefWidth(330); }
    void dialog(String title,GridPane form,Runnable save) {
        Dialog<Void> d=new Dialog<>(); d.initOwner(stage); d.setTitle(title); d.getDialogPane().setContent(form); ButtonType ok=new ButtonType("Guardar",ButtonBar.ButtonData.OK_DONE); d.getDialogPane().getButtonTypes().addAll(ok,ButtonType.CANCEL); d.getDialogPane().getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        d.getDialogPane().lookupButton(ok).addEventFilter(javafx.event.ActionEvent.ACTION,e-> { try { save.run(); } catch(Exception ex) { e.consume(); message("Revise la información",ex.getMessage()); } }); d.showAndWait();
    }
    double number(TextField f) { try { return Double.parseDouble(f.getText().trim().replace(',','.')); } catch(Exception e) { throw new IllegalArgumentException("Ingrese una cantidad numérica válida."); } }
    int integer(TextField f) { try { return Integer.parseInt(f.getText().trim()); } catch(Exception e) { throw new IllegalArgumentException("Ingrese una cantidad entera válida."); } }
    String date(DatePicker p) { if(p.getValue()==null) throw new IllegalArgumentException("Seleccione una fecha válida."); return p.getValue().toString(); }
    void pondForm(Pond old) {
        GridPane g=grid(); TextField name=field(old==null?"":old.name),station=field(old==null?"Cumaral":old.station),size=field(old==null?"":String.valueOf(old.size)),species=field(old==null?"Tilapia":old.species),system=field(old==null?"Aireador":old.system);
        ComboBox<String> keeper=new ComboBox<>(FXCollections.observableArrayList(store.db.users.stream().filter(u->u.active&&u.role.equals(KEEPER)).map(u->u.id).toList())); keeper.setValue(old==null?"encargado":old.keeperId);
        ComboBox<String> state=combo("Inactivo","En descanso","Activo","En producción","En prueba","Dañado","Inutilizable"),purpose=combo("Venta","Reproducción","Prueba"); if(old!=null) {state.setValue(old.state);purpose.setValue(old.purpose);}
        row(g,0,"Nombre",name);row(g,1,"Estación / finca",station);row(g,2,"Usuario encargado",keeper);row(g,3,"Tamaño (m²)",size);row(g,4,"Especie",species);row(g,5,"Sistema de cultivo / oxigenación",system);row(g,6,"Estado",state);row(g,7,"Propósito",purpose);
        dialog(old==null?"Nuevo estanque":"Editar estanque",g,()-> { Pond p=new Pond(); p.id=old==null?null:old.id; p.name=name.getText().trim(); p.station=station.getText().trim(); p.keeperId=keeper.getValue(); p.size=number(size); p.species=species.getText().trim(); p.system=system.getText().trim(); p.state=state.getValue(); p.purpose=purpose.getValue(); service.savePond(p,old==null); ponds(); });
    }
    TableView<Lot> lotTable() { return table(service.lots(),new String[]{"Lote","Estanque / finca","Especie","Siembra","Días","Peces","Cosecha estimada","Alimento kg","Estado"},List.of(l->l.id.substring(0,Math.min(8,l.id.length())),l->service.pond(l.pondId).toString(),l->l.species,l->l.date,l->String.valueOf(ChronoUnit.DAYS.between(LocalDate.parse(l.date),LocalDate.now())),l->String.valueOf(l.quantity),l->LocalDate.parse(l.date).plusMonths(l.months).toString(),l->String.format(Locale.ROOT,"%.2f",service.total(l)),l->l.state)); }
    void lots() {
        page("Gestionar lotes",service.owner()?"Corrija datos con una razón o cancele por mortalidad total. La proyección es configurable entre 5 y 6 meses.":"Consulte los lotes de sus estanques asignados."); TableView<Lot> t=lotTable(); content.getChildren().add(t);
        if(service.owner()) content.getChildren().add(new HBox(10,button("Registrar lote inicial",()->lotForm(null)),button("Corregir lote",()->lotForm(selected(t))),button("Cancelar por mortalidad",()-> { Lot l=selected(t); TextInputDialog d=new TextInputDialog(); d.setTitle("Cancelar lote"); d.setHeaderText("Indique la causa de mortalidad total"); d.showAndWait().ifPresent(reason->run(()-> { service.cancel(l,reason); lots(); })); })));
    }
    void lotForm(Lot old) {
        if(old!=null&&!old.state.equals("Activo")) throw new IllegalArgumentException("Solo puede corregir lotes activos.");
        GridPane g=grid(); ComboBox<Pond> p=new ComboBox<>(FXCollections.observableArrayList(service.ponds())); if(old!=null) {p.setValue(service.pond(old.pondId)); p.setDisable(true);} else p.getSelectionModel().selectFirst();
        TextField species=field(old==null?"Tilapia":old.species),quantity=field(old==null?"":String.valueOf(old.quantity)),reason=field(""); DatePicker date=new DatePicker(old==null?LocalDate.now():LocalDate.parse(old.date)); ComboBox<String> months=combo("5","6"); months.setValue(old==null?"6":String.valueOf(old.months));
        row(g,0,"Estanque",p); row(g,1,"Especie",species);row(g,2,"Cantidad de peces",quantity);row(g,3,"Fecha de siembra",date);row(g,4,"Meses para cosecha",months);row(g,5,"Motivo administrativo",reason);
        dialog("Registro administrativo de lote",g,()-> { if(p.getValue()==null)throw new IllegalArgumentException("Seleccione un estanque."); Lot l=new Lot();l.id=old==null?null:old.id;l.pondId=p.getValue().id;l.species=species.getText().trim();l.quantity=integer(quantity);l.date=date(date);l.months=Integer.parseInt(months.getValue());service.saveLot(l,old==null,reason.getText().trim()); lots(); });
    }
    void feeding() {
        page("Gestionar alimentación","Registre cada suministro diario en kilogramos. Se acumulará en el lote activo.");
        content.getChildren().addAll(button("Registrar alimentación",()-> { GridPane g=grid(); ComboBox<Pond> p=new ComboBox<>(FXCollections.observableArrayList(service.ponds().stream().filter(x->service.lots().stream().anyMatch(l->l.pondId.equals(x.id)&&l.state.equals("Activo"))).toList())); p.getSelectionModel().selectFirst(); DatePicker day=new DatePicker(LocalDate.now()); TextField kg=field(""),food=field(""); row(g,0,"Estanque",p);row(g,1,"Fecha",day);row(g,2,"Cantidad (kg)",kg);row(g,3,"Alimento / concentrado",food); dialog("Registrar alimentación",g,()-> {if(p.getValue()==null)throw new IllegalArgumentException("No hay estanques con lote activo.");service.feed(p.getValue().id,date(day),number(kg),food.getText().trim());feeding();}); }),table(service.feedings(),new String[]{"Fecha","Estanque","Alimento","Cantidad kg","Registrado por"},List.of(f->f.date,f->pondName(f.pondId),f->f.food,f->String.valueOf(f.kg),f->f.userId)));
    }
    TableView<Notice> noticeTable() { return table(service.notices(),new String[]{"Fecha","Estanque / finca","Tipo","Descripción","Estado"},List.of(n->fecha(n.date),n->service.pond(n.pondId).toString(),n->n.type,n->n.detail,n->n.status)); }
    void alerts() {
        page(service.owner()?"Alertas y aprobaciones":"Registrar alerta de boqueo","Las alertas quedan visibles al piscicultor en esta aplicación. Los daños requieren su aprobación."); TableView<Notice> t=noticeTable();content.getChildren().add(t);
        if(service.owner())content.getChildren().add(new HBox(12,button("Atender / aprobar",()-> {service.resolve(selected(t),true);alerts();}),button("Rechazar reporte",()-> { service.resolve(selected(t),false);alerts();})));
        else content.getChildren().add(button("Nueva alerta",()->noticeForm(null,false)));
    }
    void noticeForm(Pond fixed,boolean damage) {
        GridPane g=grid(); ComboBox<Pond> p=new ComboBox<>(FXCollections.observableArrayList(service.ponds())); if(fixed!=null) {p.setValue(fixed);p.setDisable(true);}else p.getSelectionModel().selectFirst(); ComboBox<String> type=damage?combo("Daño / inutilización"):combo("Boqueo","Bajo oxígeno"); TextArea detail=new TextArea();detail.setPrefRowCount(3); ComboBox<String> state=combo("Dañado","Inutilizable"); row(g,0,"Estanque afectado",p);row(g,1,"Tipo",type);row(g,2,"Descripción",detail);if(damage)row(g,3,"Estado solicitado",state);
        dialog(damage?"Reportar daño":"Registrar alerta",g,()-> {if(p.getValue()==null)throw new IllegalArgumentException("No tiene estanques asignados.");service.notice(p.getValue().id,type.getValue(),detail.getText().trim(),damage?state.getValue():null);if(damage)ponds();else alerts();});
    }
}

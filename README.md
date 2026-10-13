# Gestión de una piscícola — versión 1

Aplicación de escritorio para Windows, escrita en Java con JavaFX. Basada en los PDF de requerimientos y resumen incluidos en este directorio. Implementa RF-01, RF-02, RF-05 y RF-06. En el documento RF-06 se llama **alerta por boqueo**, interpretado como el requisito «bloqueo» del mensaje inicial.

## Ejecutar

Haga doble clic en **Iniciar.bat**. Las bibliotecas JavaFX 25 y Gson 2.13.2 están en `lib`; no se necesita Maven ni internet para ejecutar. Requiere **JDK 23 o superior, Windows x64**. El lanzador utiliza `JAVA_HOME` o el JDK 27 instalado en `C:\Program Files\Java\jdk-27`. No utiliza el Java 8 predeterminado del equipo. JavaFX 25 requiere JDK 23 o posterior: https://openjfx.io/highlights/25/.

También puede abrir `dist\GestionPiscicola\GestionPiscicola.exe` si se ha generado el paquete portátil, que incluye su propio Java.

## Cuentas de demostración

| Usuario | Contraseña | Cargo | Estación |
|---|---|---|---|
| piscicultor | Piscicola123! | Piscicultor | Todas |
| encargado | Estanque123! | Encargado de la estación | Cumaral |
| encargado2 | Estanque123! | Encargado de la estación | Cubarral |

La selección del cargo no otorga permisos: debe coincidir con la cuenta autenticada. Las contraseñas se almacenan mediante PBKDF2-HMAC-SHA256 con sal individual. Son cuentas de ejemplo; no hay gestión de empleados en esta versión.

## Funciones y permisos

| Función | Piscicultor | Encargado |
|---|---|---|
| Data | Todas las estaciones, consumo y alertas | Solo estanques asignados |
| Estanques | Crear, editar, desactivar, asignar encargado | Consultar y reportar daño |
| Lotes | Consultar, registrar carga inicial, corregir con motivo, cancelar por mortalidad | Consultar |
| Alimentación | Consulta de consumo en Data | Registrar suministro en kg e historial |
| Alertas por boqueo / bajo oxígeno | Ver y atender | Registrar |
| Daños | Aprobar o rechazar | Solicitar cambio de estado |

El menú de alimentación y el registro de alertas no aparecen para el piscicultor. Los permisos también se verifican en la capa de servicio. Los estanques en prueba no son visibles al encargado. Para aprobar la inutilización de un estanque con un lote activo, primero debe cancelarse el lote con una causa.

RF-03 (siembras) está fuera de alcance: el registro de lote inicial es una carga administrativa para permitir probar la versión y mantener lotes sin ese módulo. La fecha estimada se calcula con un parámetro de 5 o 6 meses; no constituye una recomendación productiva por especie.

## JSON y respaldo

`data/piscicola.json` contiene usuarios, estanques, lotes, alimentaciones, alertas/reportes y auditoría. El ejecutable portátil guarda su archivo independiente en `dist/GestionPiscicola/app/data/piscicola.json`. Se crea con ejemplos solo cuando no existe. Data muestra la información persistida y respeta el cargo. Para trasladar el programa portátil, copie toda la carpeta `GestionPiscicola`, incluyendo `app` y `runtime`.

Cada operación guarda una instantánea completa UTF-8, fuerza su escritura y reemplaza el JSON de forma atómica. `piscicola.json.bak` conserva la versión anterior. Solo se permite una instancia sobre el mismo directorio. Si el JSON es ilegible, la aplicación informa el error y no lo reemplaza. Para recuperar: cierre la aplicación, conserve el archivo afectado y copie el `.bak` como `piscicola.json` después de revisarlo. No edite los archivos mientras la aplicación esté abierta.

Los datos son locales al directorio de ejecución. No hay sincronización entre equipos ni envío de SMS. Las alertas se guardan de inmediato, aparecen en Data y provocan un aviso al iniciar sesión el piscicultor. Una estación aislada no envía una notificación remota; esa integración y el resto de los requisitos del PDF quedan para versiones posteriores.

## Compilar y comprobar

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/test.ps1
```

Código: `src/main/java/piscicola`. Estilos: `src/main/resources/style.css`. Las pruebas utilizan un directorio independiente en `build`, sin modificar los datos reales. El paquete portátil se genera con `scripts/package.ps1` usando `jpackage` del JDK.

## Resumen en cuatro renglones

La aplicación centraliza la información de estanques y lotes de las estaciones piscícolas.  
El piscicultor administra estanques y lotes, y revisa alertas y solicitudes de cambios de estado.  
El encargado registra alimentación y alertas por boqueo de sus estanques asignados.  
La información se conserva en JSON y se consulta en Data, sin necesitar conexión a internet.

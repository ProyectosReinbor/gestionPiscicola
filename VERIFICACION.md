# Verificación de la versión 1

- Compilación con JDK 27 y JavaFX 25: correcta; código compatible con JDK 23 o posterior.
- 23 comprobaciones de la capa de servicio: correctas. Incluyen autenticación por cargo, rechazo de accesos a otros estanques, rechazo de alimentación para el piscicultor, acumulación por lote, alertas, cancelación, aprobación de daño, persistencia al reabrir, reversión de cambios fallidos, bloqueo entre instancias y conservación de JSON corrupto.
- Prueba de construcción de todas las pantallas para ambos cargos: `UI_SMOKE_OK`.
- Login y Data de ambos cargos revisados mediante capturas en `build/screenshots`.
- Paquete portátil Windows x64 creado con jpackage. Su ejecutable terminó la prueba de pantallas con código de salida 0 y `UI_SMOKE_OK`, usando el runtime incluido.

Las pruebas de servicio usan datos aislados. La prueba de las pantallas crea los ejemplos si no existen, sin registrar operaciones productivas.

No se ha probado una interrupción física de energía, un envío SMS ni sincronización entre equipos. Las dos últimas funciones están fuera de esta versión.

# Vocal / Autonomia sin limites

APK Android local-first para sostener la base diaria del usuario: dashboard,
checklist por capas, abstinencias configurables, modo riesgo y progreso basico.

No tiene backend, login, analytics, nube, comunidad ni notificaciones en el MVP.

## Estado del proyecto

El proyecto ya tiene una direccion documental inicializada. Empieza por:

- `AGENTS.md`
- `docs/README.md`
- `docs/estado-actual-mvp.md`
- `docs/frontend-design.md`
- `docs/tono-comunicacion.md`

La direccion visual vigente es organica/editorial: calma, estructura, carbon
calido, carton/beige y coral mate. Cualquier idea anterior tipo cyberpunk,
terminal o neon queda reemplazada.

## Prototipos visuales

- Guia visual e iconografia: `docs/prototipo/index.html`
- Dashboard mobile: `docs/prototipo/dashboard.html`

Estos HTML son la referencia actual para llevar el frontend a Jetpack Compose.

## Compilar

En PowerShell, desde `D:\APK-Personal`:

```powershell
powershell -ExecutionPolicy Bypass -File .\build-apk.ps1
```

La APK debug queda en:

```text
app\build\outputs\apk\debug\app-debug.apk
```

Tambien puedes compilar directo si ya tienes `JAVA_HOME` y Android SDK:

```powershell
.\gradlew.bat assembleDebug
```

## Instalar en GrapheneOS / Pixel 7 Pro

Con depuracion USB habilitada y el telefono autorizado:

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

La app usa `minSdk 26`, `targetSdk 36` y no solicita permisos especiales.

## Donde modificar

- UI Compose actual: `app/src/main/java/dev/panopt/autonomia/MainActivity.kt`
- Modelo de dominio: `app/src/main/java/dev/panopt/autonomia/Models.kt`
- Tablas Room: `app/src/main/java/dev/panopt/autonomia/data/Entities.kt`
- Consultas Room: `app/src/main/java/dev/panopt/autonomia/data/AutonomiaDao.kt`
- Seed de capas, actividades y rachas: `app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt`
- Nombre visible de la app: `app/src/main/res/values/strings.xml`
- Icono actual de espiral: `app/src/main/res/drawable/ic_spiral.xml`

## Persistencia

La app usa Room en una base local llamada `autonomia.db`.

Principio:

- Room guarda hechos.
- El dominio calcula inferencias.
- Compose presenta el resultado.

Documentacion:

- `docs/definicion-tablas-room-v1.md`
- `docs/nucleo-dominio-autonomia.md`
- `docs/especificacion-actividades-sobriedad-v1.md`

## Proximas decisiones

- Actividades predeterminadas por capa.
- Tipos de actividad y campos necesarios para metricas.
- Calculo inicial de progreso y estado del dia.
- Tono final de mensajes.
- Portar el dashboard HTML a Compose.

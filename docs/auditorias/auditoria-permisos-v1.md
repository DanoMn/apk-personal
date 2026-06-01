# Auditoría de permisos — 2026-05-31
Versión app: `0.3.0` (versionCode 3) · targetSdk: 36 · minSdk: 26

> Alcance: **solo auditoría y documentación.** No se modificó código ni el
> Manifest. La evidencia sale del Manifest fuente, del Manifest mergeado
> (`app/build/intermediates/merged_manifest/.../AndroidManifest.xml`), del
> historial de git y del código de las features.

## Resumen ejecutivo

- El permiso candidato al bloqueo de GrapheneOS es **`PACKAGE_USAGE_STATS`**
  ("Acceso a datos de uso" / *Usage access*). El mensaje "puede poner en riesgo
  tu información financiera y personal" es **textualmente** el warning que
  GrapheneOS (y AOSP) muestran sobre el toggle de *Usage access*. No es Graphene
  siendo paranoico: cualquier app que pida ese acceso ve ese cartel.
- **Confirmado por git:** `PACKAGE_USAGE_STATS` lo introdujo el commit
  `71e1af6 feat(telemetry): add reusable device-activity capture infrastructure`.
  Antes de la feature device-telemetría el permiso no existía. La hipótesis de
  trabajo es correcta.
- **Las dependencias NO inyectan permisos peligrosos.** El árbol es AndroidX puro
  (Compose, Room, WorkManager). No hay Firebase, Play Services ni SDK de
  analytics. Los únicos permisos transitivos (de WorkManager) son todos
  `PROTECTION_NORMAL` y ninguno dispara el bloqueo.
- **Hallazgo adicional fuera de la hipótesis:** existe un segundo "special app
  access" igual de sensible — un **Device Admin** (`BIND_DEVICE_ADMIN` con
  política `force-lock`) usado por la feature Sueño para `lockNow()`. No causa
  *este* bloqueo, pero es el permiso más invasivo de toda la app y merece una
  decisión de producto.
- **Recomendación principal:** el acceso a UsageStats no es la única vía para
  inferir sueño. Los eventos de pantalla encendida/apagada se pueden capturar con
  un `BroadcastReceiver` runtime de `ACTION_SCREEN_ON/OFF` **sin ningún permiso**.
  Evaluar migrar la fuente de telemetría a esa vía y dejar `PACKAGE_USAGE_STATS`
  como opt-in avanzado (o eliminarlo). Ver § Opciones de remediación.

## Permisos base de la app (core)

Declarados explícitamente en `app/src/main/AndroidManifest.xml`:

| Permiso | Nivel de protección | Origen (propio / lib) | Propósito |
|---------|---------------------|-----------------------|-----------|
| `android.permission.INTERNET` | `normal` | Propio | Declarado en el Manifest. **No se observó uso de red en el código** (sin Retrofit/OkHttp/HttpURLConnection); candidato a eliminar en una limpieza futura. |
| `android.permission.PACKAGE_USAGE_STATS` | `signature\|privileged\|development\|appop` (**special app access** — *Usage access*) | Propio (feature device-telemetría) | Leer `UsageStatsManager.queryEvents()` para inferir actividad del dispositivo (pantalla on/off) y derivar ventanas de sueño. **Candidato #1 al bloqueo.** |

Permisos auto-generados por el toolchain (AGP), visibles solo en el Manifest mergeado:

| Permiso | Nivel | Origen | Propósito |
|---------|-------|--------|-----------|
| `dev.panopt.autonomia.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | `signature` (auto-definido) | AGP / `androidx.core` | Protección interna para receivers dinámicos no exportados (Android 13+/`T`). Inofensivo; no visible para el usuario. |

## Permisos feature: device-telemetría

Directorio: `app/src/main/java/dev/panopt/autonomia/platform/telemetry/`

| Permiso | Nivel de protección | Archivo donde se usa | Propósito específico |
|---------|---------------------|----------------------|----------------------|
| `PACKAGE_USAGE_STATS` | special app access (*Usage access*) | `UsageStatsTelemetrySource.kt` (`queryEvents`), `TelemetryPermission.kt` (chequeo vía `AppOpsManager.OPSTR_GET_USAGE_STATS` + intent a `Settings.ACTION_USAGE_ACCESS_SETTINGS`) | Capturar eventos de uso del dispositivo. El `TelemetryEventMapper` los traduce a `DeviceActivityEvent`; el `DeviceTelemetryDrainWorker` (WorkManager) los drena periódicamente. La feature Sueño consume estos eventos para interpretar noches. |

Notas de implementación (bien hechas, vale registrarlo):
- El permiso se trata como **special permission, no runtime**: `TelemetryPermission`
  expone `GRANTED/MISSING` y, sin el permiso, la captura es **no-op** (no crashea).
- `TelemetryPermission.kt` ya contempla el caso GrapheneOS/`adb install`: ofrece
  un intent a *App info* para desbloquear **Restricted Settings** (overflow ⋮ →
  "Allow restricted settings") antes de que el toggle de Usage access deje de
  estar griseado. Ver `meta/handoffs/handoff-sleep-followups.md` (follow-up 1).

## Permisos feature: Sueño

Directorios: `app/src/main/java/dev/panopt/autonomia/sleep/`,
`app/src/main/java/dev/panopt/autonomia/ui/sleep/`, wiring en `MainActivity.kt`.

| Permiso | Nivel de protección | Archivo donde se usa | Propósito específico |
|---------|---------------------|----------------------|----------------------|
| `PACKAGE_USAGE_STATS` (indirecto) | special app access | vía device-telemetría | Inferir/puntuar noches a partir de eventos de pantalla. Es el consumidor de la telemetría de arriba. |
| `android.permission.BIND_DEVICE_ADMIN` (**Device Admin** — special app access) | `signature` | Receiver `sleep.SleepDeviceAdminReceiver` (Manifest) + política `force-lock` en `res/xml/sleep_device_admin.xml`; activado y usado en `MainActivity.kt` (`DevicePolicyManager.isAdminActive` / `ACTION_ADD_DEVICE_ADMIN` / `lockNow()`) | Bloquear la pantalla del teléfono (`lockNow()`) al iniciar una sesión de sueño. **No** se declara como `<uses-permission>`: es el patrón correcto de Device Admin (permiso de binding sobre el receiver + activación del admin por el usuario en Settings). |

Permisos transitivos de **WorkManager** (`androidx.work`, usado por
`DeviceTelemetryDrainWorker` y el cierre diario) — solo en el Manifest mergeado:

| Permiso | Nivel | Origen | Propósito |
|---------|-------|--------|-----------|
| `android.permission.WAKE_LOCK` | `normal` | `androidx.work` | Mantener el CPU despierto durante un job. |
| `android.permission.ACCESS_NETWORK_STATE` | `normal` | `androidx.work` | Constraints de red de WorkManager. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | `normal` | `androidx.work` | Reprogramar jobs tras reinicio. |
| `android.permission.FOREGROUND_SERVICE` | `normal` | `androidx.work` | Jobs expedited / foreground de WorkManager. |

Ninguno de estos cuatro es peligroso ni dispara el bloqueo: son `normal`, se
conceden en instalación y no aparecen como toggles de riesgo.

## Análisis del bloqueo GrapheneOS

### Permiso(s) candidatos

**Candidato único y confirmado: `PACKAGE_USAGE_STATS` (Usage access).**

El mensaje exacto del dispositivo ("acceso a este permiso puede poner en riesgo
tu información financiera y personal") es el texto estándar del toggle de *Usage
access* en Settings → Apps → Acceso especial. Ese acceso permite leer qué apps
usás y cuándo — de ahí la advertencia. GrapheneOS no lo bloquea de forma
arbitraria: lo presenta como acceso especial con un warning fuerte, y en apps
instaladas por fuente no confiable (`adb`) lo deja **griseado** hasta habilitar
*Restricted settings*.

Descarte de los otros sospechosos de la lista de prioridad:
- `READ_PHONE_STATE`, `ACTIVITY_RECOGNITION`, `ACCESS_BACKGROUND_LOCATION`,
  `MANAGE_NETWORK_POLICY`, `CHANGE_NETWORK_STATE`: **no declarados** en el Manifest
  fuente ni en el mergeado. No existen en esta app.
- Permisos de vendor / Google Play Services: **no aplican** — no hay GMS en el
  árbol de dependencias.

### Evidencia

- Declaración: `app/src/main/AndroidManifest.xml:6-8`
  ```xml
  <uses-permission
      android:name="android.permission.PACKAGE_USAGE_STATS"
      tools:ignore="ProtectedPermissions" />
  ```
- Introducción (git): `git log -S "PACKAGE_USAGE_STATS"` → único commit
  `71e1af6 feat(telemetry): add reusable device-activity capture infrastructure`.
- Uso real: `UsageStatsTelemetrySource.kt:18-23` (`usageStatsManager.queryEvents(from, to)`).
- Chequeo de concesión: `TelemetryPermission.kt:22-39` (`AppOpsManager.OPSTR_GET_USAGE_STATS`).
- Confirmación de que **no** hay otros permisos peligrosos: Manifest mergeado
  (`app/build/intermediates/merged_manifest/debug/.../AndroidManifest.xml:11-20`)
  lista solo `INTERNET`, `PACKAGE_USAGE_STATS` y los cuatro `normal` de WorkManager.

### Opciones de remediación

Ordenadas de menos a más invasivas para el usuario:

1. **Reemplazar la fuente por `ACTION_SCREEN_ON` / `ACTION_SCREEN_OFF` (recomendada).**
   Los eventos de pantalla — que es lo que la telemetría realmente necesita para
   inferir sueño — se pueden capturar con un `BroadcastReceiver` **registrado en
   runtime** (no se puede declarar en Manifest desde Android 8) escuchando
   `Intent.ACTION_SCREEN_ON/OFF`. **No requiere ningún permiso.** Costo: necesita
   un proceso vivo (foreground service liviano o muestreo periódico), porque el
   receiver runtime muere con el proceso. Esto elimina el bloqueo de raíz y baja
   muchísimo el perfil de privacidad.
   - Tradeoff: UsageStats da histórico (podés consultar hacia atrás aunque la app
     estuviera muerta); el receiver de pantalla solo capta en vivo. Para "inferir
     noches" en una app que de todos modos corre WorkManager, el receiver suele
     alcanzar.

2. **Dejar `PACKAGE_USAGE_STATS` como opt-in avanzado explícito.** Mantener la
   telemetría apagada por defecto y ofrecer el acceso solo a usuarios que lo
   activen conscientemente, con copy honesto sobre qué se lee y por qué. La
   arquitectura ya lo soporta (`TelemetryPermission` = `MISSING` → no-op), así que
   el cambio es de UX/onboarding, no de motor.

3. **Eliminar el permiso** si la inferencia de sueño puede vivir solo con la
   sesión manual de sueño (el usuario marca inicio/fin) + el `lockNow()` ya
   existente. Es la opción más limpia para privacy-first si el valor incremental
   de la telemetría automática no justifica el acceso especial.

> Las tres son compatibles con la arquitectura actual porque la telemetría ya
> está aislada detrás de `TelemetryCaptureSource` (`UsageStatsTelemetrySource` es
> solo un adaptador intercambiable). Migrar la fuente no toca el dominio ni el
> scoring.

## Permisos a revisar para releases futuros

- **`BIND_DEVICE_ADMIN` / Device Admin (`force-lock`) — prioridad alta.** Aunque
  no causa el bloqueo actual, Device Admin es **el acceso más invasivo de la app**
  y GrapheneOS/AOSP lo marcan con advertencias fuertes; además complica la
  desinstalación (hay que desactivar el admin antes). Para una sola acción
  (`lockNow()` al dormir) es desproporcionado. Alternativa menos invasiva a
  evaluar: en muchos casos el objetivo "bajar el estímulo de pantalla al dormir"
  se cubre con un overlay/Do-Not-Disturb o simplemente dejando que el usuario
  bloquee el teléfono — sin pedir Device Admin. Revisar si la feature justifica
  el costo de privacidad y de fricción de desinstalación.
- **`INTERNET` — prioridad baja.** Declarado pero **sin uso de red detectado** en
  el código. En una app local-first, un permiso sin uso es ruido y contradice el
  posicionamiento privacy-first. Candidato a remover (o documentar para qué se
  reserva).
- **`FOREGROUND_SERVICE` (de WorkManager) — vigilar.** Hoy es `normal`, pero si en
  Android 14+ se empieza a usar un foreground service propio (p. ej. para la
  opción 1 de remediación), habrá que declarar el subtipo
  `FOREGROUND_SERVICE_*` correcto y su justificación en Play. No es un problema
  ahora; es un recordatorio para cuando se toque la fuente de telemetría.

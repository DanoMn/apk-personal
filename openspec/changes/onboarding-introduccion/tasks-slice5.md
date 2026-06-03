# Tasks: onboarding-introduccion · slice 5 (Notificaciones)

## Review Workload Forecast

| Campo | Valor |
|-------|-------|
| Líneas estimadas | ~415–460 (dominio+tests ~125, canales+notifier+permiso ~95, scheduler+worker ~80, repo+DailyClosureWorker ~45, MainActivity wiring ~50, manifest+strings ~20) |
| 400-line budget risk | High |
| Chained PRs recommended | No — commits a main por responsabilidad (mismo patrón que slices 3 y 4) |
| Estrategia de partición | 3 commits de código + 1 commit docs SDD |
| Delivery strategy | exception-ok (dueño autorizó commits work-unit a main sin PRs encadenados) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: High

### Mapeo commits (work-unit commits a main)

| Commit | Contenido | Líneas est. | Compila solo |
|--------|-----------|-------------|-------------|
| **Commit 5a** — dominio + datos + tests | `SleepNotificationPolicy.kt` (new) · `SleepNotificationPolicyTest.kt` (new) · `WindDownSchedulePolicy.kt` (new, opcional) · `WindDownSchedulePolicyTest.kt` (new, si aplica) · `AutonomiaRepository.kt` — `maybeFireSleepDataAlert` + 2 prefs · `DailyClosureWorker.kt` — hook final al cierre · stub no-op temporal de `SleepNotifier` si 5b no va en el mismo commit | ~190–210 | Sí (notifier como stub; la plataforma real entra en 5b) |
| **Commit 5b** — infra plataforma | `SleepNotificationChannels.kt` (new) · `SleepNotifier.kt` (new, reemplaza stub) · `PostNotificationsPermission.kt` (new) · `WindDownNotificationScheduler.kt` (new) · `WindDownNotificationWorker.kt` (new) · `AndroidManifest.xml` · `res/values/strings.xml` · `MainActivity.onCreate` — `ensureCreated` | ~170–185 | Sí (depende de 5a; el scheduler llama repo que ya existe) |
| **Commit 5c** — permiso UI | `MainActivity.kt` — launcher `POST_NOTIFICATIONS` perezoso + evaluación Notif A al entrar al Dashboard + gate Notif B en siguiente apertura | ~60–75 | Sí (depende de 5b) |
| **Commit 5d** — docs SDD | `tasks-slice5.md` (este archivo) | — | N/A |

> Orden obligatorio: 5a → 5b → 5c → 5d. Cada commit debe pasar `assembleDebug` + `testDebugUnitTest` antes de committear.

---

## Diseño (decisiones Slice 5, referencia rápida)

- **S5-D1** — `SleepNotificationPolicy` dominio puro (sin Android): `shouldScheduleWindDown(consent, targetSleepAt)`, `shouldFireDataAlert(confidences, threshold=3)`. El acceso a Room lo hace el repo; la policy solo recibe la lista ya lista.
- **S5-D2** — Notif A: `WindDownNotificationScheduler` + `WindDownNotificationWorker` (patrón `DailyClosureWorkScheduler`). `UNIQUE = "wind_down_reminder"`, política `REPLACE`. El worker re-verifica la condición antes de postear.
- **S5-D3** — Notif B: hook `maybeFireSleepDataAlert(today)` al final de `DailyClosureWorker.doWork()`. Lee `getSleepNightsInRange`, arma la lista de confianzas, llama la policy, postea si aplica. Anti-spam: pref `sleep_data_alert_last_fired_date` o `notificationId` fijo (decisión del apply).
- **S5-D4** — Dos canales (`sleep_wind_down`, `sleep_data_alert`), `IMPORTANCE_DEFAULT`. `SleepNotificationChannels.ensureCreated(context)` en `MainActivity.onCreate` + defensivamente en cada worker antes de `notify`.
- **S5-D5** — Permiso perezoso: `rememberLauncherForActivityResult(RequestPermission())` en `MainActivity`. API < 33 = concedido. Nunca en onboarding. Pref `post_notifications_requested` evita re-pedir tras denegación.
- **S5-D6** — Denegación no bloqueante: `isGranted` gatea toda llamada a `notify`; cero impacto en scoring.
- **S5-D7** — Copy canónico: Notif A titulo "Se acerca tu hora de descanso"; Notif B titulo "Faltan datos de sueño". Español neutro. Recursos en `strings.xml`.
- **S5-D8** — Prefs mínimas: `post_notifications_requested` (Boolean, obligatoria) + `sleep_data_alert_last_fired_date` (String?, opcional anti-spam). Sin Room nuevo, sin entidad nueva.

---

## Phase 1: Dominio puro — `SleepNotificationPolicy` (TDD, RED → GREEN) · Commit 5a

### [x] 1.1 — `SleepNotificationPolicy.kt` esqueleto compilable (stub en RED)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/domain/notifications/SleepNotificationPolicy.kt` (New)
**Dependencias:** ninguna
**Acción:** crear `object SleepNotificationPolicy` con la constante `NIGHTS_WITHOUT_DATA_THRESHOLD = 3`, firma de `shouldScheduleWindDown` devolviendo `TODO()`, y firma de `shouldFireDataAlert` devolviendo `TODO()`. Sin imports de Android. La función `isValidTime` privada puede incluirse ya (usa `java.time.LocalTime.parse`, JVM puro).

---

### [x] 1.2 — `SleepNotificationPolicyTest.kt` (RED — todos fallan)

**Archivo:** `app/src/test/java/dev/panopt/autonomia/domain/notifications/SleepNotificationPolicyTest.kt` (New)
**Dependencias:** 1.1
**Test runner:** `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.notifications.SleepNotificationPolicyTest'`

Casos obligatorios — `shouldFireDataAlert`:

| # | Nombre del test | Verificación |
|---|-----------------|--------------|
| a | `threeNoData_firesAlert` | `shouldFireDataAlert([NoData, NoData, NoData])` ⇒ `true` |
| b | `twoNoData_noAlert_historyShort` | `shouldFireDataAlert([NoData, NoData])` ⇒ `false` (history < threshold) |
| c | `firstNightHasData_noAlert` | `shouldFireDataAlert([High, NoData, NoData])` ⇒ `false` |
| d | `middleNightHasData_noAlert` | `shouldFireDataAlert([NoData, High, NoData])` ⇒ `false` |
| e | `nullNightsAreNoData_fires` | `shouldFireDataAlert([null, null, null])` ⇒ `true` |
| f | `fourNoData_defaultThreshold3_fires` | `shouldFireDataAlert([NoData, NoData, NoData, NoData])` ⇒ `true` (toma solo 3) |
| g | `threshold1_oneNoData_fires` | `shouldFireDataAlert([NoData], threshold=1)` ⇒ `true` |
| h | `threshold0_defenseGuard` | `shouldFireDataAlert([NoData, NoData, NoData], threshold=0)` ⇒ `false` |

Casos obligatorios — `shouldScheduleWindDown`:

| # | Nombre del test | Verificación |
|---|-----------------|--------------|
| i | `consentTrue_validTime_schedules` | `shouldScheduleWindDown(true, "23:30")` ⇒ `true` |
| j | `consentFalse_doesNotSchedule` | `shouldScheduleWindDown(false, "23:30")` ⇒ `false` |
| k | `consentNull_doesNotSchedule` | `shouldScheduleWindDown(null, "23:30")` ⇒ `false` |
| l | `consentTrue_nullTime_doesNotSchedule` | `shouldScheduleWindDown(true, null)` ⇒ `false` |
| m | `consentTrue_invalidTime_nocrash` | `shouldScheduleWindDown(true, "99:99")` ⇒ `false` (sin crash) |

Caso constante calibrable:

| # | Nombre del test | Verificación |
|---|-----------------|--------------|
| n | `thresholdConstantIs3` | `NIGHTS_WITHOUT_DATA_THRESHOLD == 3` |

---

### [x] 1.3 — Implementar `SleepNotificationPolicy` (GREEN)

**Archivo:** `domain/notifications/SleepNotificationPolicy.kt` (Modified)
**Dependencias:** 1.2
**Acción:** reemplazar los `TODO()` con la implementación real (ver S5-D1 del design). `shouldFireDataAlert`: `take(threshold)`, `size < threshold → false`, `all { null o NoData } → true`. `shouldScheduleWindDown`: `consent == true && isValidTime(targetSleepAt)`. `isValidTime`: `runCatching { LocalTime.parse(value) }.getOrNull() != null`.
**Verificar GREEN:** `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.notifications.SleepNotificationPolicyTest'`

---

### [x] 1.4 — `WindDownSchedulePolicy.kt` + tests (opcional — si el initialDelay se encapsula puro)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/domain/closure/WindDownSchedulePolicy.kt` (New, opcional)
**Dependencias:** 1.3
**Acción:** si el cálculo de `initialDelay(now: LocalTime, targetSleepAt: String): Duration` se puede testear JVM puro, extraerlo aquí (gemelo de `DailyClosureSchedulePolicy`). Tests en `WindDownSchedulePolicyTest.kt`: `targetSleepAt` en el futuro del mismo día → delay correcto; hora ya pasada → delay hasta mañana; cruce de medianoche (`23:30` evaluado a las `02:00`) → delay ~21.5h. Si la técnica existente en `DailyClosureWorkScheduler` ya está probada y no se puede aislar limpiamente, documentar la omisión y validar en runtime.
**Test runner:** `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.closure.WindDownSchedulePolicyTest'`

---

## Phase 2: Datos + hook de cierre · Commit 5a (cont.)

### [x] 2.1 — `AutonomiaRepository.kt`: prefs mínimas (S5-D8)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt` (Modified)
**Dependencias:** ninguna (paralelo con Phase 1)
**Acción:** agregar con el patrón canónico `MutableStateFlow(prefs.getX(...)) / asStateFlow() / suspend setX()`:
- `post_notifications_requested` (Boolean, default `false`): `isPostNotificationsRequestedFlow()` + `setPostNotificationsRequested(Boolean)`.
- `sleep_data_alert_last_fired_date` (String?, default `null`): `getSleepDataAlertLastFiredDate()` (getter simple, no Flow) + `setSleepDataAlertLastFiredDate(String?)`.
Sin Room, sin migración.

---

### [x] 2.2 — `AutonomiaRepository.kt`: `maybeFireSleepDataAlert` (S5-D3)

**Archivo:** `AutonomiaRepository.kt` (Modified)
**Dependencias:** 1.3, 2.1
**Acción:** `suspend fun maybeFireSleepDataAlert(today: LocalDate, zoneId: ZoneId)` que:
1. Calcula `from = today.minusDays((NIGHTS_WITHOUT_DATA_THRESHOLD - 1).toLong())`, `to = today`.
2. Llama `dao.getSleepNightsInRange(from.toString(), to.toString())` (existente).
3. Construye la lista de `SleepConfidence?` de tamaño `NIGHTS_WITHOUT_DATA_THRESHOLD` (más reciente primero), mapeando filas ausentes → `null` y `confidenceLevel` → `SleepConfidence.valueOf`.
4. Llama `SleepNotificationPolicy.shouldFireDataAlert(confidences)`.
5. Si `true`: verifica dedup con `getSleepDataAlertLastFiredDate() == today.toString()` (si iguales, no postea). Si no hay dup: llama `SleepNotifier.postDataAlert(context)` (stub temporal en 5a → implementación real en 5b) + `setSleepDataAlertLastFiredDate(today.toString())`. Guarda el `context` del Worker como parámetro o lo recibe de `applicationContext` inyectado.
6. NO pide permiso aquí (background). Si falta el permiso, simplemente no postea.

---

### [x] 2.3 — `DailyClosureWorker.kt`: hook Notif B al final de `doWork()` (S5-D3)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/data/worker/DailyClosureWorker.kt` (Modified)
**Dependencias:** 2.2
**Acción:** al final del bloque `runCatching` de `doWork()`, DESPUÉS de `materializeSleepNight` y del snapshot, agregar:
```kotlin
SleepNotificationChannels.ensureCreated(applicationContext)  // defensivo
repository.maybeFireSleepDataAlert(today = today, zoneId = zoneId)
```
Nota: `SleepNotificationChannels` es stub no-op en 5a (se implementa en 5b); el llamado defensivo compilará aunque no haga nada aún.

---

### [x] 2.4 — Build verde del Commit 5a

**Dependencias:** 1.4 (o skip), 2.3
**Comando:**
```powershell
powershell.exe -Command "\$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat assembleDebug --no-daemon"
```
**Suite de tests en verde:**
```powershell
powershell.exe -Command "\$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat testDebugUnitTest --no-daemon"
```
Alcance mínimo: `SleepNotificationPolicyTest` (14 casos) + `WindDownSchedulePolicyTest` (si se creó) + suite sin regresiones.

---

## Phase 3: Infra plataforma · Commit 5b

### [x] 3.1 — `SleepNotificationChannels.kt` (S5-D4)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/platform/notifications/SleepNotificationChannels.kt` (New)
**Dependencias:** strings.xml (3.5, puede hacerse en paralelo)
**Req cubiertos:** Notification Channel Registration (ambos escenarios de la spec).
**Acción:** `object SleepNotificationChannels { fun ensureCreated(context: Context) }`. Solo en `Build.VERSION.SDK_INT >= Build.VERSION_CODES.O`. Crear dos canales:
- `channelId = "sleep_wind_down"`, `IMPORTANCE_DEFAULT`, nombre desde `R.string.notif_channel_wind_down_name`, descripción desde `R.string.notif_channel_wind_down_desc`.
- `channelId = "sleep_data_alert"`, `IMPORTANCE_DEFAULT`, nombre desde `R.string.notif_channel_data_alert_name`, descripción desde `R.string.notif_channel_data_alert_desc`.
Reemplaza el stub no-op de 5a.

---

### [x] 3.2 — `SleepNotifier.kt` (S5-D7)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/platform/notifications/SleepNotifier.kt` (New)
**Dependencias:** 3.1
**Req cubiertos:** tono canónico Notif A y Notif B; prohibición de copy culpabilizador; prohibición de "registrar"/"anotar".
**Acción:** `object SleepNotifier` con:
- `fun postWindDown(context: Context)`: `NotificationCompat.Builder(context, "sleep_wind_down")` con título `R.string.notif_wind_down_title` ("Se acerca tu hora de descanso"), texto `R.string.notif_wind_down_text` ("Es un buen momento para empezar a bajar el ritmo."), `smallIcon` (icono existente del proyecto). Posteado con `NotificationManagerCompat.from(context).notify(NOTIF_ID_WIND_DOWN, n)`.
- `fun postDataAlert(context: Context)`: canal `"sleep_data_alert"`, título `R.string.notif_data_alert_title` ("Faltan datos de sueño"), texto `R.string.notif_data_alert_text` ("No detectamos datos de sueño en los últimos días. Puedes revisar el permiso de uso en la configuración."), `ContentIntent` que abre la app (opcional: navegar a config sueño). `notify(NOTIF_ID_DATA_ALERT, n)`.
- Constantes de ID enteras (`NOTIF_ID_WIND_DOWN = 1001`, `NOTIF_ID_DATA_ALERT = 1002`).
Reemplaza el stub de 5a.

---

### [x] 3.3 — `PostNotificationsPermission.kt` (S5-D5)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/platform/notifications/PostNotificationsPermission.kt` (New)
**Dependencias:** ninguna
**Req cubiertos:** API < 33 concedido sin pedir; permiso runtime en API ≥ 33.
**Acción:**
```kotlin
object PostNotificationsPermission {
    fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
```
Sin lógica adicional. La solicitud vive en `MainActivity` (5c).

---

### [x] 3.4 — `WindDownNotificationScheduler.kt` (S5-D2)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/data/worker/WindDownNotificationScheduler.kt` (New)
**Dependencias:** 3.3, `WindDownSchedulePolicy` (1.4 o inline)
**Req cubiertos:** Notif A programada cuando `shouldScheduleWindDown == true`; cancelada cuando `false`/`null`.
**Acción:** `object WindDownNotificationScheduler` con:
- `fun schedule(context: Context, targetSleepAt: String, zoneId: ZoneId)`: calcula `initialDelay` (delta hasta la próxima ocurrencia de `targetSleepAt` — reusa `WindDownSchedulePolicy` o técnica análoga al `DailyClosureWorkScheduler`). Encola `PeriodicWorkRequest<WindDownNotificationWorker>` con `repeatInterval = 1.days`, `initialDelay`, constraints vacíos. `WorkManager.getInstance(context).enqueueUniquePeriodicWork("wind_down_reminder", ExistingPeriodicWorkPolicy.REPLACE, request)`.
- `fun cancel(context: Context)`: `WorkManager.getInstance(context).cancelUniqueWork("wind_down_reminder")`.

---

### [x] 3.5 — `WindDownNotificationWorker.kt` (S5-D2)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/data/worker/WindDownNotificationWorker.kt` (New)
**Dependencias:** 3.2, 3.3
**Req cubiertos:** re-verificación de condición antes de postear; no pide permiso en background.
**Acción:** `class WindDownNotificationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params)`. En `doWork()`:
1. `SleepNotificationChannels.ensureCreated(applicationContext)` (defensivo).
2. Leer `sleep_wind_down_consent` + `targetSleepAt` desde `repository` (o prefs directas si repository no es inyectable en Worker — seguir el patrón de `DailyClosureWorker`).
3. Llamar `SleepNotificationPolicy.shouldScheduleWindDown(consent, targetSleepAt)`.
4. Si `true` y `PostNotificationsPermission.isGranted(applicationContext)` → `SleepNotifier.postWindDown(applicationContext)`.
5. Si permiso falta → no postear, no pedir, retornar `Result.success()`.
6. `return Result.success()` en todo caso (no hay retry con sentido para un recordatorio).

---

### [x] 3.6 — `AndroidManifest.xml`: declarar `POST_NOTIFICATIONS` (S5-D5)

**Archivo:** `app/src/main/AndroidManifest.xml` (Modified)
**Dependencias:** ninguna
**Acción:** agregar dentro de `<manifest>`:
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```
No rompe builds en API < 33 (el sistema lo ignora). Verificar que el bloque `<uses-permission>` esté antes de `<application>`.

---

### [x] 3.7 — `res/values/strings.xml`: copy canónico Notif A/B y nombres de canales (S5-D7)

**Archivo:** `app/src/main/res/values/strings.xml` (Modified)
**Dependencias:** ninguna (paralelo con 3.1)
**Acción:** agregar strings:
```xml
<string name="notif_channel_wind_down_name">Recordatorio de descanso</string>
<string name="notif_channel_wind_down_desc">Aviso cuando se acerca tu hora de dormir</string>
<string name="notif_channel_data_alert_name">Datos de sueño</string>
<string name="notif_channel_data_alert_desc">Avisos sobre datos de sueño incompletos</string>
<string name="notif_wind_down_title">Se acerca tu hora de descanso</string>
<string name="notif_wind_down_text">Es un buen momento para empezar a bajar el ritmo.</string>
<string name="notif_data_alert_title">Faltan datos de sueño</string>
<string name="notif_data_alert_text">No detectamos datos de sueño en los últimos días. Puedes revisar el permiso de uso en la configuración.</string>
```
Verificar: ningún string contiene "deberías", "fallaste", "registrar", "anotar", "olvidaste", signos de exclamación de alarma.

---

### [x] 3.8 — `MainActivity.onCreate`: `SleepNotificationChannels.ensureCreated` (S5-D4)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/MainActivity.kt` (Modified)
**Dependencias:** 3.1
**Acción:** en `onCreate`, antes o junto a `DailyClosureWorkScheduler.schedule(...)` (línea ~45):
```kotlin
SleepNotificationChannels.ensureCreated(applicationContext)
```
Idempotente; no requiere condición.

---

### [x] 3.9 — Build verde del Commit 5b

**Dependencias:** 3.8 (último del bloque)
**Comandos:**
```powershell
powershell.exe -Command "\$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat assembleDebug --no-daemon"
```
```powershell
powershell.exe -Command "\$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; .\gradlew.bat testDebugUnitTest --no-daemon"
```
Sin regresiones. Lint del manifest sin error por `POST_NOTIFICATIONS`.

---

## Phase 4: Permiso UI + wiring Notif A · Commit 5c

### [x] 4.1 — `MainActivity.kt`: launcher `POST_NOTIFICATIONS` perezoso (S5-D5)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/MainActivity.kt` (Modified)
**Dependencias:** 3.3, 3.6
**Req cubiertos:** escenarios "permiso concedido → notificación se muestra"; "permiso denegado → no crashea ni degrada"; "API < 33 → no se pide"; "no se pide durante el onboarding".
**Acción:** dentro del `setContent { }` de `MainActivity`, agregar (mismo patrón que `adminLauncher` ya presente):
```kotlin
val postNotificationsLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { /* granted: Boolean — no-op; denegación no bloquea */
    // Podría guardarse el flag post_notifications_requested aquí si no se hace antes
}
```
La solicitud se dispara en 4.2, no aquí.

---

### [x] 4.2 — `MainActivity.kt`: wiring Notif A al entrar al Dashboard + gatillo de permiso (S5-D5)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/MainActivity.kt` (Modified)
**Dependencias:** 4.1, 3.4, 3.3, 2.1 (pref `post_notifications_requested`)
**Req cubiertos:** Notif A programada al completar onboarding con `windDownConsent = true`; no programada con `false`/`null`; no programada sin `targetSleepAt`; permiso perezoso no se pide en onboarding; no se reintenta tras denegación.
**Acción:** en el punto de entrada al Dashboard (transición de `AppScreen.Onboarding → AppScreen.Dashboard` tras `complete()`, y también en cada `onCreate` como garantía idempotente), ejecutar:
```kotlin
val consent = repository.sleepWindDownConsentFlow().value        // Boolean? del slice 3
val targetSleepAt = /* leer de SleepConfigEntity / currentSleepConfig() */
if (SleepNotificationPolicy.shouldScheduleWindDown(consent, targetSleepAt)) {
    WindDownNotificationScheduler.schedule(applicationContext, targetSleepAt!!, zoneId)
    // Gatillo perezoso de permiso (API >= 33 y no ya solicitado antes):
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !PostNotificationsPermission.isGranted(applicationContext) &&
        !repository.isPostNotificationsRequested()
    ) {
        repository.setPostNotificationsRequested(true)
        postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
} else {
    WindDownNotificationScheduler.cancel(applicationContext)
}
```
El gatillo de Notif B para solicitar el permiso en la siguiente apertura se maneja automáticamente aquí: si el Worker marcó una racha NoData y el permiso falta, la próxima vez que `MainActivity` evalúe el estado de sueño y detecte que hay `N` noches NoData activas (leyendo el mismo `getSleepNightsInRange`), puede lanzar el permiso aquí también. Alternativa más simple: basar el gatillo solo en `shouldScheduleWindDown` (Notif A) y aceptar que Notif B espera al próximo `DailyClosureWorker` tras conceder el permiso. Decisión del apply según complejidad real.

---

### [x] 4.3 — Build + lintDebug + tests del Commit 5c en verde

**Dependencias:** 4.2
**Comandos (en orden):**
1. `assembleDebug` — sin errores.
2. `lintDebug` — sin errores nuevos.
3. `testDebugUnitTest --no-daemon` — suite completa sin regresiones.

---

## Phase 5: Verificación estática global · Commit 5c (cont.)

### [x] 5.1 — Suite completa de tests en verde

**Dependencias:** 4.3
**Alcance mínimo:** `SleepNotificationPolicyTest` (14 casos), `WindDownSchedulePolicyTest` (si se creó), sin regresiones en `OnboardingFlowTest`, `OnboardingSleepRuleTest`, `OnboardingIntentionRuleTest`, ni en scoring/abstinence. Verificar que no hay `@Ignore` sin justificación.

---

### [x] 5.2 — Lint sin errores nuevos sobre la infra de notificaciones

**Dependencias:** 5.1
**Verificar:** no hay warning de canal sin registrar (`NotificationChannelCompat`), no hay uso de `startForeground` donde no aplica, no hay import de `android.permission.POST_NOTIFICATIONS` hardcodeado (debe ir vía `Manifest.permission`).

---

## Phase 6: Verificación runtime (emulador)

### [ ] 6.1 — Checklist runtime (verificacion-por-capas.md, capas 1-4)

**Dependencias:** 5.2
**Prerequisito:** install limpio (`adb uninstall dev.panopt.autonomia`; `adb install app-debug.apk`).

| # | Verificación | Pasa si |
|---|--------------|---------|
| a | App inicia → canales `sleep_wind_down` y `sleep_data_alert` visibles en Ajustes del sistema (canal notifs de la app) | Canales creados idempotentemente |
| b | Abrir la app una segunda vez → canales siguen sin duplicarse | Idempotencia confirmada |
| c | Completar onboarding con `windDownConsent = true` y `targetSleepAt` válido → en API 33+: aparece diálogo `POST_NOTIFICATIONS` | Gatillo perezoso correcto |
| d | Denegar el permiso → Dashboard carga normalmente, sin error, sin crash | Denegación no bloqueante |
| e | Abrir la app de nuevo → diálogo de permiso NO vuelve a aparecer | `post_notifications_requested` evita re-pedido |
| f | Conceder el permiso → Notif A aparece cerca de `targetSleepAt` (ajustar a 1-2 min en el futuro para el test manual) | Scheduling correcto |
| g | Completar onboarding con `windDownConsent = false` → ninguna Notif A programada (verificar WorkManager en adb shell) | No se programa con consent false |
| h | Bajar `NIGHTS_WITHOUT_DATA_THRESHOLD` a 1 temporalmente → esperar/simular cierre nocturno → Notif B aparece | Notif B funciona end-to-end |
| i | Restaurar threshold a 3 | Limpieza de test manual |
| j | En emulador API < 33 → completar onboarding → no aparece diálogo de `POST_NOTIFICATIONS` | API guard correcto |
| k | Logcat sin `"channel not registered"`, sin crash, sin `IllegalArgumentException` en `notify` | Estabilidad de canales |

---

## Dependencias entre tareas (grafo)

```
1.1 → 1.2 → 1.3
                \
1.4 (paralelo)   +→ 2.2 → 2.3 → 2.4 [Commit 5a verde]
                /                        |
2.1 ────────────                         ↓
                             3.1 → 3.8
                             3.2 → 3.5
                             3.3 → 3.4
                             3.3 → 4.1
                             3.6 (paralelo)
                             3.7 (paralelo con 3.1)
                             todos → 3.9 [Commit 5b verde]
                                          |
                                          ↓
                                   4.1 → 4.2 → 4.3 → 5.1 → 5.2 → 6.1
```

| Bloque | Puede correr en paralelo con |
|--------|------------------------------|
| Phase 1 TDD (1.1–1.3) | Phase 1.4 (`WindDownSchedulePolicy`) — sin dependencia cruzada |
| Phase 2.1 (prefs) | Phase 1 — independiente |
| Phase 3 (3.6 manifest, 3.7 strings) | Sí, entre sí y con 3.1/3.2/3.3 |
| Phase 3.1–3.5 (plataforma) | Requieren Commit 5a verificado |
| Phase 4 (permiso UI) | Requiere Commit 5b verificado |
| Phase 5 static | Requiere 4.3; los 3 pasos pueden solaparse |
| Phase 6 runtime | Requiere Phase 5 completa |

---

## Trazabilidad spec → tareas

| Requirement (spec) | Tareas |
|--------------------|--------|
| Notification Channel Registration | 3.1, 3.7, 3.8, 2.3 (ensureCreated defensivo) |
| Notification A — Wind-Down Reminder | 1.1–1.3 (shouldScheduleWindDown), 3.4 (Scheduler), 3.5 (Worker), 4.2 (wiring en Dashboard) |
| Notification B — Sleep Data Alert | 1.1–1.3 (shouldFireDataAlert), 2.2 (maybeFireSleepDataAlert), 2.3 (DailyClosureWorker hook) |
| POST_NOTIFICATIONS Lazy Permission | 3.3 (PostNotificationsPermission), 3.6 (manifest), 4.1 (launcher), 4.2 (gatillo perezoso, no en onboarding) |
| Sin registro nocturno manual | 3.7 (copy Notif B verificado), 3.2 (SleepNotifier) |
| Persistencia sin Room nuevo | 2.1 (prefs mínimas), 2.2 (deriva de sleep_nights existente) |
| Tono: invita, no ordena, sin culpa | 3.7 (strings), 3.2 (SleepNotifier) |
| API < 33: permiso no se pide | 3.3, 4.2 (guard `SDK_INT < TIRAMISU`) |
| Denegación no bloqueante | 3.5 (Worker no retries), 4.2 (cero impacto scoring), 3.2 (notify no lanza) |
| Constante calibrable N=3 | 1.1 (`NIGHTS_WITHOUT_DATA_THRESHOLD`), 1.2 (test umbral configurable) |

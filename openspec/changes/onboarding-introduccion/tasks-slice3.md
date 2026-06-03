# Tasks: onboarding-introduccion · slice 3 (onboarding-sleep)

## Review Workload Forecast

| Campo | Valor |
|-------|-------|
| Líneas estimadas | ~390–430 (dominio+tests ~80, extracción helpers ~90, UI OnboardingSleepStep ~180, prefs ~30, wiring ~40) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Decisión de partición | Ver "Plan B — partición del diff" al final. |

> La extracción de `AutoModeCard`/`PermissionStep` y `TimeField`/`DurationRow` desde
> `SleepConfigScreen.kt` es refactor mecánico sin cambio de comportamiento, pero infla
> el diff con líneas movidas. El Plan A mantiene todo en un PR si el conteo real
> queda ≤ 400; el Plan B corta la extracción en dos commits para que el revisor pueda
> ignorar el "move-only" más fácilmente.

---

## Diseño (decisiones Slice 3)

- **S3-D1** — `OnboardingSleepStep` dedicado que reusa dominio + piezas válidas
  extraídas (`AutoModeCard`, `PermissionStep`, y opcionalmente `TimeField`/`DurationRow`).
- **S3-D2** — `OnboardingSleepRule` (dominio puro): wrapper de `SleepPolicy` que fija el
  contrato del onboarding sin duplicar fórmulas. Núcleo testeable JVM (Strict TDD).
- **S3-D3** — 3 prefs nuevas (`sleep_usage_stats_requested`, `sleep_usage_stats_skipped`,
  `sleep_wind_down_consent`) en `AutonomiaRepository`. Ventana elegida → `saveSleepConfig`
  (tabla `sleep_config` existente). Sin Room nuevo.
- **S3-D4** — Flujo de permiso UsageStats reusa `TelemetryPermission` +
  `toggleSleepAutoMode`. Re-chequeo en ON_RESUME con `DisposableEffect` +
  `LifecycleEventObserver`.
- **S3-D5** — `OnboardingSleepStep` recibe estado + callbacks; `canAdvance` calculado con
  `OnboardingSleepRule` (no inline en Compose). Callbacks cableados en `MainActivity`
  contra `dashboardViewModel`/`repository`, igual que el patrón de anclas.

---

## Phase 1: Dominio puro (TDD — tests PRIMERO)

### [x] 1.1 — `OnboardingSleepRule.kt` (esqueleto en RED)

**Archivo:** `domain/onboarding/OnboardingSleepRule.kt`
**Dependencias previas:** ninguna (slice 1 y 2 ya implementados).
**Acción:** crear `object OnboardingSleepRule` con firmas de `canAdvance` y
`derivedWindowMinutes` que compilan pero devuelven stubs (`TODO()`/`false`/`null`).

> Propósito: que los tests del paso 1.2 compilen en RED antes de implementar.

---

### [x] 1.2 — `OnboardingSleepRuleTest.kt` (RED)

**Archivo:** `app/src/test/java/dev/panopt/autonomia/domain/onboarding/OnboardingSleepRuleTest.kt`
**Dependencias:** 1.1
**Test runner:** `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.onboarding.OnboardingSleepRuleTest'`

Casos que DEBEN estar en RED antes del paso 1.3:

| # | Nombre del test | Verificación |
|---|-----------------|--------------|
| a | `canAdvance_validDefault_8h_returnsTrue` | `"23:30" → "07:30"` (8h) ⇒ `true` |
| b | `canAdvance_below5h_returnsFalse` | `"23:30" → "02:00"` (2,5h) ⇒ `false` |
| c | `canAdvance_exactly300min_returnsTrue` | `"23:30" → "04:30"` (300 min exactos) ⇒ `true` |
| d | `derivedWindowMinutes_crossMidnight_480min` | `"23:30" → "07:30"` ⇒ `480` |
| e | `derivedWindowMinutes_unparseable_returnsNull` | `"" → "07:30"` ⇒ `null` |
| f | `canAdvance_adjustedValid_6h_returnsTrue` | `"23:30" → "05:30"` (6h) ⇒ `true` (escenario "ajustar pickers") |

Req cubiertos: Active Sleep Window Choice (escenarios a, b, c, d, f).

---

### [x] 1.3 — Implementación de `OnboardingSleepRule` (GREEN)

**Archivo:** `domain/onboarding/OnboardingSleepRule.kt`
**Dependencias:** 1.2
**Acción:** implementar `canAdvance` y `derivedWindowMinutes` delegando en
`SleepPolicy.validatePlannedWindow` y `SleepPolicy.minutesBetween`. Sin lógica
duplicada de minutos.

```kotlin
object OnboardingSleepRule {
    val minWindowMinutes: Int = SleepPolicy.MIN_SLEEP_WINDOW_MINUTES

    fun canAdvance(plannedSleepAt: String, plannedWakeAt: String): Boolean =
        SleepPolicy.validatePlannedWindow(plannedSleepAt, plannedWakeAt) is SleepWindowValidation.Valid

    fun derivedWindowMinutes(plannedSleepAt: String, plannedWakeAt: String): Int? =
        SleepPolicy.minutesBetween(plannedSleepAt, plannedWakeAt)
}
```

Verificar GREEN: `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.onboarding.OnboardingSleepRuleTest'`

---

## Phase 2: Persistencia en prefs (AutonomiaRepository)

### [x] 2.1 — 3 prefs nuevas en `AutonomiaRepository`

**Archivo:** `app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt`
**Dependencias:** ninguna (paralelo con Phase 1 si se prefiere, pero se recomienda después de 1.3 para mantener el PR ordenado).
**Req cubiertos:** Telemetry Permission Offer, Wind-Down Consent Capture, "consentimiento sobrevive al cierre".

Agregar con el patrón `MutableStateFlow + asStateFlow() + suspend setter`:

```kotlin
// sleep_usage_stats_requested
private val _sleepUsageStatsRequested = MutableStateFlow(
    prefs.getBoolean("sleep_usage_stats_requested", false))
fun sleepUsageStatsRequestedFlow(): StateFlow<Boolean> = _sleepUsageStatsRequested.asStateFlow()
suspend fun setSleepUsageStatsRequested(requested: Boolean) {
    prefs.edit { putBoolean("sleep_usage_stats_requested", requested) }
    _sleepUsageStatsRequested.value = requested
}

// sleep_usage_stats_skipped
private val _sleepUsageStatsSkipped = MutableStateFlow(
    prefs.getBoolean("sleep_usage_stats_skipped", false))
fun sleepUsageStatsSkippedFlow(): StateFlow<Boolean> = _sleepUsageStatsSkipped.asStateFlow()
suspend fun setSleepUsageStatsSkipped(skipped: Boolean) {
    prefs.edit { putBoolean("sleep_usage_stats_skipped", skipped) }
    _sleepUsageStatsSkipped.value = skipped
}

// sleep_wind_down_consent
private val _sleepWindDownConsent = MutableStateFlow<Boolean?>(
    if (prefs.contains("sleep_wind_down_consent"))
        prefs.getBoolean("sleep_wind_down_consent", false) else null)
fun sleepWindDownConsentFlow(): StateFlow<Boolean?> = _sleepWindDownConsent.asStateFlow()
suspend fun setSleepWindDownConsent(consent: Boolean) {
    prefs.edit { putBoolean("sleep_wind_down_consent", consent) }
    _sleepWindDownConsent.value = consent
}
```

> Nota: `windDownConsent` es `Boolean?` (null = no respondió aún). Los otros dos son
> `Boolean` (false = aún no interactuó). Consistente con el contrato de las specs.

---

## Phase 3: Extracción de helpers de `SleepConfigScreen`

> Esta phase es refactor mecánico puro — sin cambio de comportamiento. `SleepConfigScreen`
> debe seguir funcionando exactamente igual después. Constituye el "commit move-only" del
> Plan B de presupuesto.

### [x] 3.1 — Extraer `AutoModeCard` + `PermissionStep` → `SleepAutoModeCard.kt`

**Archivos:**
- Nuevo: `app/src/main/java/dev/panopt/autonomia/ui/sleep/SleepAutoModeCard.kt`
- Modificado: `ui/sleep/SleepConfigScreen.kt`

**Dependencias:** Phase 2 completada (o en paralelo, no hay dependencia real).

**Acción:**
1. Mover las funciones `private fun AutoModeCard(...)` (línea ~206) y
   `private fun PermissionStep(...)` (línea ~300) a `SleepAutoModeCard.kt` como
   `internal fun`.
2. Actualizar `SleepConfigScreen.kt`: reemplazar la definición por import del nuevo archivo.
3. Verificar build: `assembleDebug` sin errores.

> `AutoModeCard`/`PermissionStep` son las piezas no triviales (orquestan el flujo de
> permiso). SIEMPRE se extraen (no están en el Plan B de "duplicar"). Plan B solo aplica a
> `TimeField`/`DurationRow`.

---

### [x] 3.2 — Extraer `TimeField` + `DurationRow` + `filterTimeInput` → `SleepWindowFields.kt` (opcional según budget)

**Archivos:**
- Nuevo: `app/src/main/java/dev/panopt/autonomia/ui/sleep/SleepWindowFields.kt`
- Modificado: `ui/sleep/SleepConfigScreen.kt`

**Dependencias:** 3.1

**Acción:**
1. Mover `private fun TimeField(...)` (línea ~449), `private fun DurationRow(...)` (línea ~487)
   y `private fun String.filterTimeInput()` (línea ~597) a `SleepWindowFields.kt` como
   `internal`.
2. Actualizar `SleepConfigScreen.kt`.
3. Verificar build.

> **DECISIÓN de budget en apply:** si al contar líneas totales del slice el diff ya
> supera 380, duplicar estos helpers dentro de `OnboardingSleepStep` (~50 líneas triviales)
> en vez de extraerlos. Saltar al paso 4.1 directamente. Documentar la decisión en el
> commit.

---

## Phase 4: UI — `OnboardingSleepStep`

### [x] 4.1 — Crear `OnboardingSleepStep.kt` (estructura y tiempo real)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/onboarding/OnboardingSleepStep.kt`
**Dependencias:** 1.3, 3.1 (y 3.2 si se completó; si no, duplicar `TimeField`/`DurationRow` inline).
**Req cubiertos:** Active Sleep Window Choice, Copy canónico y tono, Sin modelo de datos nuevo.

Estructura del Composable:

```kotlin
@Composable
fun OnboardingSleepStep(
    sleepAt: String,           // sembrado de DashboardSleepState.targetSleepAt ?: "23:30"
    wakeAt: String,            // sembrado de DashboardSleepState.targetWakeAt  ?: "07:30"
    usageStatsRequested: Boolean,
    usageStatsSkipped: Boolean,
    windDownConsent: Boolean?,
    onSleepAtChange: (String) -> Unit,
    onWakeAtChange: (String) -> Unit,
    onActivateTelemetry: (onPermissionRequired: () -> Unit) -> Unit,
    onSkipTelemetry: () -> Unit,
    onWindDownConsent: (Boolean) -> Unit,
    onContinue: (sleepAt: String, wakeAt: String) -> Unit,
    onBack: () -> Unit,
)
```

Partes obligatorias dentro del Composable:

1. **Estado local:** `var showPermissionPrompt by remember { mutableStateOf(false) }`.
2. **`canAdvance`:** `OnboardingSleepRule.canAdvance(sleepAt, wakeAt)` (dominio puro, no inline).
3. **`derivedMinutes`:** `OnboardingSleepRule.derivedWindowMinutes(sleepAt, wakeAt)`.
4. **Encabezado:** título `"El descanso primero"` (tipografía serif), subtítulo con
   "ventana" (no "número de horas").
5. **Pickers:** dos `TimeField` (o duplicados inline) sembrados de `sleepAt`/`wakeAt`
   + `DurationRow` con la duración derivada (o `"—"` si `derivedMinutes == null`).
6. **Mensaje de ventana inválida:** visible cuando `!canAdvance` con texto neutral
   (ej. `"La ventana mínima es de 5 horas"`). SIN "error", "incorrecto", "fallaste",
   SIN signos de alarma.
7. **Oferta de telemetría:** `AutoModeCard`/`PermissionStep` importados desde
   `SleepAutoModeCard.kt`; "Activar" llama `onActivateTelemetry { showPermissionPrompt = true }`;
   "Más tarde" llama `onSkipTelemetry()` sin diálogo del sistema.
8. **`DisposableEffect` ON_RESUME:** re-evalúa `TelemetryPermission.isGranted(context)`;
   si pasó a `GRANTED`, oculta el prompt (`showPermissionPrompt = false`) y completa el
   toggle de automode via callback. Validar firma con Context7 antes de fijar.
9. **Consentimiento wind-down:** pregunta `"¿Quieres que te avise cuando se acerque tu
   hora de descanso?"` con botones `"Sí"` / `"No"` que llaman `onWindDownConsent(true/false)`.
   Estado inicial: neutral si `windDownConsent == null`.
10. **Botón "Continuar":** `enabled = canAdvance`, llama `onContinue(sleepAt, wakeAt)`.
11. **Botón "Atrás":** siempre habilitado, llama `onBack()`.

> Verificar que `rememberLauncherForActivityResult` y `LocalLifecycleOwner` estén
> disponibles (depende de `androidx.lifecycle:lifecycle-runtime-compose`, ya en classpath
> vía `collectAsStateWithLifecycle`). Consultar Context7 para firmas vigentes.

---

## Phase 5: Wiring en `OnboardingScreen` y `MainActivity`

### [x] 5.1 — `OnboardingScreen.kt`: reemplazar placeholder de Sleep

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/onboarding/OnboardingScreen.kt`
**Dependencias:** 4.1

**Acción:**
1. Agregar parámetros al Composable `OnboardingScreen` necesarios para el Bloque Sueño
   (espejo del patrón de anclas: `DashboardSleepState`, callbacks de telemetría/wind-down/continuar).
2. Agregar rama antes del `else`:
   ```kotlin
   OnboardingStep.Sleep -> OnboardingSleepStep(
       sleepAt = sleepState.targetSleepAt ?: "23:30",
       wakeAt = sleepState.targetWakeAt ?: "07:30",
       usageStatsRequested = sleepState.usageStatsRequested,
       usageStatsSkipped = sleepState.usageStatsSkipped,
       windDownConsent = sleepState.windDownConsent,
       onSleepAtChange = onSleepAtChange,
       onWakeAtChange = onWakeAtChange,
       onActivateTelemetry = onActivateTelemetry,
       onSkipTelemetry = onSkipTelemetry,
       onWindDownConsent = onWindDownConsent,
       onContinue = onContinue,
       onBack = onBack,
   )
   ```
3. El `else` queda para `Intention` y `Sobriety` (placeholders intactos).
4. `placeholderTitle` puede mantenerse (su caso `Sleep` queda inalcanzable; no hace daño).

---

### [x] 5.2 — `MainActivity.kt`: cablear callbacks del Bloque Sueño

**Archivo:** `app/src/main/java/dev/panopt/autonomia/MainActivity.kt`
**Dependencias:** 2.1, 5.1
**Req cubiertos:** Telemetry Permission Offer (escenarios Activar / Más tarde / Permiso denegado), Wind-Down Consent Capture (escenarios Sí / No / sobrevive al cierre), Active Sleep Window Choice (escenario "Ajustar pickers actualiza derivación").

Callbacks a cablear en la llamada a `OnboardingScreen(...)`:

```kotlin
// Telemetría
onActivateTelemetry = { onPermissionRequired ->
    dashboardViewModel.toggleSleepAutoMode(true) { onPermissionRequired() }
    scope.launch { repository.setSleepUsageStatsRequested(true) }
},
onSkipTelemetry = {
    scope.launch { repository.setSleepUsageStatsSkipped(true) }
},

// Wind-down consent
onWindDownConsent = { consent ->
    scope.launch { repository.setSleepWindDownConsent(consent) }
},

// Continuar: persiste ventana y avanza
onContinue = { sleepAt, wakeAt ->
    scope.launch {
        repository.saveSleepConfig(
            targetSleepAt = sleepAt,
            targetWakeAt = wakeAt,
            preserveDigitalWindDown = true,  // no tocar digitalWindDownMinutes
        )
        onboardingViewModel.advance()
    }
},

// Datos de estado del sueño (de dashboardViewModel o repository flows)
sleepState = dashboardState.sleep,
```

> Confirmar firma exacta de `saveSleepConfig` en `AutonomiaRepository.kt` (línea ~649)
> antes de escribir el callback.

---

## Phase 6: Verificación estática y de build

### [x] 6.1 — `assembleDebug` verde

**Dependencias:** 5.2
**Comando:**
```powershell
powershell.exe -Command "\$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat assembleDebug --no-daemon"
```
**Falla si:** errores de tipo, imports faltantes, firmas incompatibles.

---

### 6.2 — `lintDebug` sin errores nuevos

**Dependencias:** 6.1
**Comando:** reemplazar `assembleDebug` por `lintDebug` en el comando anterior.
**Falla si:** warnings de accesibilidad en Compose no resueltos, deprecated sin sustituto.

---

### [x] 6.3 — `testDebugUnitTest` suite completa en verde

**Dependencias:** 6.1
**Comando:**
```powershell
powershell.exe -Command "\$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat testDebugUnitTest --no-daemon"
```
**Alcance mínimo:** `OnboardingSleepRuleTest` (6 casos). Suite de onboarding existente
(`OnboardingFlowTest`, `OnboardingAnchorsRuleTest`) debe seguir en verde.

---

## Phase 7: Verificación runtime (emulador)

### 7.1 — Checklist runtime (verificacion-por-capas.md, capas 1–4)

**Dependencias:** 6.1, 6.2, 6.3
**Prerequisito:** install limpio (`adb uninstall dev.panopt.autonomia`; `adb install app-debug.apk`).

| # | Verificación | Pasa si |
|---|--------------|---------|
| a | Abrir app → Bloque Sueño aparece al avanzar desde Bloque Anclas | Navegación correcta |
| b | Pickers parten en 23:30 / 07:30 con duración "8 horas" | Siembra correcta |
| c | Cambiar despertar a 02:00 → "Continuar" deshabilitado + mensaje neutral | Gate + tono |
| d | Corregir a 05:30 → "Continuar" habilitado | Re-habilitación |
| e | Tocar "Más tarde" en telemetría → no abre Settings, avance no bloqueado | Skippable |
| f | Tocar "Sí" al wind-down → avanzar → matar app → reabrir → volver al Sleep → "Sí" activo | Persistencia |
| g | Tocar "Continuar" con ventana válida → avanza al siguiente bloque | Flujo completo |
| h | Logcat sin crashes durante todo el flujo | Estabilidad |

> Verificación del permiso UsageStats real (escenario "Activar") se hace en dispositivo
> físico o emulador con UsageStats disponible. Si no está disponible en el entorno de
> verificación, registrar como "verificado en el dispositivo del dueño" y documentar.

---

## Dependencias entre tareas (grafo)

```
1.1 → 1.2 → 1.3
                \
2.1              +→ 4.1 → 5.1 → 5.2 → 6.1 → 6.2 → 6.3 → 7.1
                /
3.1 → 3.2 ----+
```

| Bloque | Puede correr en paralelo con |
|--------|------------------------------|
| Phase 1 (TDD dominio) | Phase 2 (prefs) — sin dependencia cruzada |
| Phase 3 (extracción) | Phase 1 + Phase 2 — refactor mecánico independiente |
| Phase 4 (UI) | Requiere 1.3 + 3.1 completados |
| Phase 5 (wiring) | Requiere 4.1 + 2.1 completados |
| Phase 6 (static) | Requiere 5.2 completado; 6.1/6.2/6.3 se pueden correr en cualquier orden |
| Phase 7 (runtime) | Requiere Phase 6 completa |

---

## Trazabilidad spec → tareas

| Requirement (spec) | Tareas que lo satisfacen |
|--------------------|--------------------------|
| Active Sleep Window Choice (gate 2) | 1.1, 1.2, 1.3, 4.1 (pickers + gate), 5.2 (saveSleepConfig) |
| Telemetry Permission Offer (skippable) | 2.1 (prefs), 3.1 (AutoModeCard extracción), 4.1 (DisposableEffect + ON_RESUME), 5.2 (callbacks) |
| Wind-Down Consent Capture | 2.1 (sleep_wind_down_consent), 4.1 (sección UI), 5.2 (callback) |
| Copy canónico y tono | 4.1 (encabezado literal + mensaje neutral) |
| Sin modelo de datos nuevo | 2.1 (prefs), 5.2 (saveSleepConfig existente), Phase 3 (sin Room) |

---

## Plan B — partición del diff (si budget > 400 líneas)

Si el conteo real de líneas al aplicar Phase 3 completa supera el presupuesto:

1. **Commit 1 — "move-only"**: Phase 3 completa (extracción de helpers de
   `SleepConfigScreen`). Sin lógica nueva. Fácil de revisar como "no-op de comportamiento".
2. **Commit 2 — lógica nueva**: Phases 1, 2, 4, 5, 6, 7.

Si incluso `TimeField`/`DurationRow` inflan demasiado el Commit 2:

- **Commit 2a**: Phase 3.1 (solo `AutoModeCard`/`PermissionStep`) + Phase 1 + Phase 2.
- **Commit 2b**: Phase 4 + Phase 5 + Phase 6 + Phase 7, duplicando `TimeField`/`DurationRow`
  inline en `OnboardingSleepStep` (~50 líneas triviales) en vez de importarlas.

Decisión final: el ejecutor (sdd-apply) cuenta las líneas ANTES de escribir Phase 4 y elige.

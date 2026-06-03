# Design: onboarding-gate (onboarding-introduccion · slice 1)

Spec: `specs/onboarding-gate/spec.md` · Insumo: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md`

## 1. Contexto y hallazgo clave

El proyecto NO usa librería de navegación: `MainActivity` mantiene
`var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }` y un
`when (currentScreen)`. El estado liviano (tema, automode) vive en prefs vía
`AutonomiaRepository` con el patrón `MutableStateFlow(prefs.getX(...))` + `asStateFlow()`
+ `suspend set...()` que persiste con `prefs.edit { }`.

**Hallazgo:** ya existe plomería huérfana para el flag de completitud:
`isInitialConfigurationCompleteFlow()` / `setInitialConfigurationComplete()` (key
`initial_configuration_complete`), **sin consumidores**. Se **reutiliza** como flag de
"onboarding completado" en vez de crear uno nuevo.

## 2. Decisiones de diseño

### D1 — Reusar el flag de completitud existente
`isInitialConfigurationCompleteFlow()` / `setInitialConfigurationComplete()` pasan a ser
el flag de "onboarding completado". Sin clave nueva, sin churn. (La DB/prefs de dev son
descartables; reusar la key no arrastra legacy problemático.)

### D2 — Reanudación: nueva clave `onboarding_current_step` (String)
Se agrega al repositorio, con el mismo patrón:
```
private val _onboardingCurrentStep = MutableStateFlow(prefs.getString("onboarding_current_step", null))
fun onboardingCurrentStepFlow(): StateFlow<String?> = _onboardingCurrentStep.asStateFlow()
suspend fun setOnboardingCurrentStep(stepName: String) { prefs.edit { putString("onboarding_current_step", stepName) }; _onboardingCurrentStep.value = stepName }
```
Se persiste por **nombre** (no por índice/ordinal): reordenar o quitar bloques en un
update NO corrompe la reanudación por desplazamiento de índice.

### D3 — Modelo de dominio puro (testeable, TDD)
En `domain/onboarding/` (Kotlin puro, sin Android):
```kotlin
enum class OnboardingStep { Welcome, Intention, Anchors, Sleep, Sobriety, Closing }  // orden = secuencia

data class OnboardingState(val completed: Boolean, val currentStep: OnboardingStep)

object OnboardingFlow {
    val firstStep = OnboardingStep.Welcome
    fun resolve(completed: Boolean, persistedStepName: String?): OnboardingState  // nombre inválido/null → Welcome
    fun next(step: OnboardingStep): OnboardingStep        // último → se mantiene (Closing dispara complete)
    fun previous(step: OnboardingStep): OnboardingStep    // primero → se mantiene
}
```
`resolve` encapsula el **clamp de paso inválido** (Requirement "Invalid Persisted Step").
Estas funciones son el núcleo testeable del slice.

> Nota: `Sobriety` (Bloque 3) es condicional a la intención (slice 4). En slice 1 forma
> parte de la secuencia como placeholder; la lógica condicional de salto entra con slice 4.

### D4 — Gate en MainActivity (sin romper local-first)
- Nuevo valor `Onboarding` en el `enum AppScreen` (privado de `MainActivity`).
- Un `OnboardingViewModel` expone `onboardingState: StateFlow<OnboardingState>` (combina
  los dos flows del repo vía `OnboardingFlow.resolve`) y `advance() / back() / complete()`.
- La pantalla inicial se **siembra** del valor síncrono del StateFlow (el
  `MutableStateFlow` se inicializa con la lectura síncrona de prefs → **sin flicker**):
  ```
  val onboardingState by onboardingViewModel.onboardingState.collectAsStateWithLifecycle()
  var currentScreen by remember {
      mutableStateOf(if (onboardingState.completed) AppScreen.Dashboard else AppScreen.Onboarding)
  }
  ```
- `complete()` → `setInitialConfigurationComplete(true)` y `currentScreen = AppScreen.Dashboard`.

### D5 — `OnboardingScreen` (esqueleto)
En `ui/onboarding/OnboardingScreen.kt`: recibe `state.currentStep` + callbacks
`onAdvance` / `onBack` / `onComplete`, y renderiza el Composable del bloque actual.
- **Bloque 0 (Welcome)** y **Bloque 4 (Closing)**: implementados con su copy canónico.
- **Bloques 0.5/1/2/3**: placeholders navegables (un Composable mínimo con un contrato de
  "paso" estable) que llenan los slices 2-4.
- `onAdvance` en Welcome → `setOnboardingCurrentStep(next)`; en Closing → `onComplete`.

### D6 — Navegación manual, NO Navigation Compose (consistencia > novedad)
Se mantiene el patrón manual `when (currentScreen)` y SharedPreferences existentes. NO se
introduce Navigation Compose ni DataStore en este slice, aunque sean "más modernos":
el principio de escribir código consistente con el que rodea pesa más que la novedad, y
evita arrastrar una dependencia/migración fuera de alcance. (Si se quisiera migrar a
DataStore/Nav-Compose, es un refactor transversal aparte.)

## 3. Componentes nuevos / modificados

| Componente | Tipo | Rol |
|------------|------|-----|
| `domain/onboarding/OnboardingStep.kt` | New | Enum ordenado de bloques |
| `domain/onboarding/OnboardingState.kt` | New | `data class` + `OnboardingFlow` (puro) |
| `AutonomiaRepository.kt` | Modified | Reusar flag completitud (D1) + clave `onboarding_current_step` (D2) |
| `ui/onboarding/OnboardingViewModel.kt` | New | Expone `OnboardingState` + advance/back/complete |
| `ui/onboarding/OnboardingScreen.kt` | New | Esqueleto + Bloque 0 y 4 |
| `MainActivity.kt` | Modified | `AppScreen.Onboarding` + gate (D4) |
| `domain/onboarding/OnboardingFlowTest.kt` | New | TDD: resolve/next/previous/clamp |

## 4. Estrategia de testing

- **TDD (JVM puro):** `OnboardingFlow.resolve` (incluye nombre inválido → Welcome),
  `next`/`previous` (bordes), y la derivación de pantalla inicial (`completed → Dashboard`,
  `!completed → Onboarding`). Se escriben ANTES del wiring. Strict TDD activo.
- **Runtime (`verificacion-por-capas.md`, porque es UI):** install limpio abre en
  Bloque 0; matar a mitad y reabrir reanuda; completar → Dashboard y persiste tras
  relanzar; sin crash; logcat limpio; Lint sin Error; build verde.

## 5. Mapa spec → diseño (trazabilidad)

| Requirement (spec) | Resuelto por |
|--------------------|--------------|
| First-Run Routing | D4 (gate) + D3 (`resolve`) |
| Persisted State (prefs, sin Room) | D1 + D2 |
| Resume on Reopen | D2 (paso por nombre) + D4 |
| Completion Sets Flag | D4 (`complete` → setter existente) |
| Invalid Persisted Step → Safe Restart | D3 (`resolve` clampa a Welcome) |
| Block Navigation Skeleton | D5 |
| Tono y nombres canónicos | D5 (copy del doc de captura) |

## 6. Riesgos de implementación

- **Primer frame del gate:** mitigado — el `StateFlow` tiene valor inicial síncrono
  (lectura de prefs en el constructor), así que `collectAsStateWithLifecycle` da el valor
  correcto en la primera composición (sin parpadeo Dashboard→Onboarding).
- **`AppScreen` es privado de `MainActivity`:** el dominio NO referencia `AppScreen`; la
  decisión pura trabaja sobre `OnboardingState`/`Boolean` y `MainActivity` mapea a
  `AppScreen`. Se preserva el encapsulamiento.

---

# Slice 3 — Bloque Sueño

Spec: `specs/onboarding-sleep/spec.md` (5 requirements, 13 escenarios) · Insumo
conceptual: `meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §2.7, §4
(Bloque 2), §6, §7.

> Estado de partida: slices 1 (gate + estado + esqueleto) y 2 (anclas) implementados y
> verificados en emulador. El Bloque Sueño REEMPLAZA el placeholder actual del
> `OnboardingStep.Sleep` en `OnboardingScreen` (hoy cae en la rama `else ->
> OnboardingBlock(...)`). PR encadenado, presupuesto ~400 líneas.

## S3.1 — Contexto y precedente que se respeta

El precedente del slice 2 (decisión del orquestador, NO re-abrir) es la guía:
`AnchorConfigScreen` NO se reusó porque arrastraba targets que el onboarding no quiere;
se hizo un `OnboardingAnchorsStep` dedicado que reusa el **dominio** y los **datos**
(`DashboardActivityOptionState`, `OnboardingAnchorsRule`, callbacks del
`DashboardViewModel`). Mismo criterio para sueño:

- `SleepConfigScreen` arrastra UI fuera de alcance del onboarding: `SleepLockStatusCard`
  (bloqueo de pantalla), `WindDownChips` de `digitalWindDownMinutes` (diferido D3, fuera
  de alcance explícito por la spec), y un botón "Guardar" terminal. El onboarding NO
  quiere nada de eso.
- Por lo tanto: se crea un **`OnboardingSleepStep`** dedicado que reusa el **dominio**
  (`SleepPolicy.validatePlannedWindow` / `minutesBetween` / `formatDuration`) y las
  **piezas válidas** ya escritas (la `AutoModeCard` y `PermissionStep` del flujo de
  telemetría, extraídas para compartirse). NO se reusa la pantalla completa.

**Hallazgo clave (reuso del flujo de permiso ya resuelto):** el permiso UsageStats YA
está resuelto en el repo y no hay que reinventarlo. `TelemetryPermission` (en
`platform/telemetry/`) expone `isGranted(context)`, `state(context)`, `settingsIntent()`
y `appDetailsSettingsIntent(context)`. `DashboardViewModel.toggleSleepAutoMode(enabled,
onPermissionRequired)` ya orquesta: si falta el permiso devuelve `PermissionRequired` y
dispara el callback; si está, registra el worker y persiste `sleep_auto_mode_enabled`.
El Bloque Sueño REUSA exactamente ese camino — no agrega un segundo mecanismo de
permiso.

## S3.2 — Decisiones de diseño (Slice 3)

### S3-D1 — `OnboardingSleepStep` dedicado, reusando dominio + piezas válidas

Nuevo Composable `ui/onboarding/OnboardingSleepStep.kt`. Estructura (copy v3, §4 "El
descanso primero"):

1. **Encabezado + intro literaria** (serif para título, sans para cuerpo; tono Cuidador
   Lúcido). Título literal: `"El descanso primero"`; subtítulo que menciona "ventana",
   no "número de horas" (escenario "Texto literal del encabezado").
2. **Pickers de ventana**: dos `TimeField` (Dormir / Despertar) sembrados en `23:30` /
   `07:30` + una fila de duración derivada (`SleepPolicy.formatDuration`). Se REUSAN los
   `TimeField` + `filterTimeInput()` + `DurationRow` ya escritos en
   `SleepConfigScreen.kt`. Para no duplicar, se **extraen** esos tres helpers privados a
   un archivo compartido `ui/sleep/SleepWindowFields.kt` (internal) y ambos
   (`SleepConfigScreen` y `OnboardingSleepStep`) los consumen.
3. **Oferta de telemetría salteable**: la `AutoModeCard` + `PermissionStep` ya escritas,
   **extraídas** de `SleepConfigScreen.kt` a `ui/sleep/SleepAutoModeCard.kt` (internal)
   para reuso (hoy son `private` dentro de `SleepConfigScreen`). Botonera "Activar" /
   "Más tarde". "Más tarde" NO lanza diálogo del sistema.
4. **Consentimiento wind-down**: pregunta "¿Quieres que te avise cuando se acerque tu
   hora de descanso?" con botones "Sí" / "No" (selección persistida, sin scheduling —
   eso es slice 5).
5. **Botón "Continuar"**: habilitado SOLO cuando la ventana es válida
   (`SleepWindowValidation.Valid`), espejando el patrón `enabled = canAdvance` del
   `OnboardingAnchorsStep`. Mensaje neutral de bloqueo cuando es inválida (sin "error",
   "fallaste", ni signos de alarma — escenario de tono).

> Nota de alcance: extraer los helpers compartidos (TimeField/DurationRow/AutoModeCard)
> es refactor mecánico sin cambio de comportamiento, y es lo que mantiene el slice
> dentro del presupuesto (no se reescribe esa UI, se mueve). Si la extracción amenaza el
> budget de 400 líneas, el fallback aceptable es duplicar `TimeField`/`DurationRow`
> (son ~50 líneas triviales) y extraer SOLO `AutoModeCard`/`PermissionStep` (que es la
> pieza no trivial). Decisión final del apply según el conteo real.

### S3-D2 — Regla pura del gate de ventana: `OnboardingSleepRule` (dominio, TDD)

Análogo a `OnboardingAnchorsRule`: un objeto de dominio puro en
`domain/onboarding/OnboardingSleepRule.kt` que envuelve la compuerta del motor sin
duplicar la fórmula. NO reimplementa el cálculo de minutos: delega en `SleepPolicy`.

```kotlin
object OnboardingSleepRule {
    /** Espejo del umbral del motor (no diverge). */
    val minWindowMinutes: Int = SleepPolicy.MIN_SLEEP_WINDOW_MINUTES

    /** true cuando la ventana planificada cumple la 2.ª compuerta del motor. */
    fun canAdvance(plannedSleepAt: String, plannedWakeAt: String): Boolean =
        SleepPolicy.validatePlannedWindow(plannedSleepAt, plannedWakeAt) is SleepWindowValidation.Valid

    /** Duración derivada para mostrar (null si la ventana es inválida/imparseable). */
    fun derivedWindowMinutes(plannedSleepAt: String, plannedWakeAt: String): Int? =
        SleepPolicy.minutesBetween(plannedSleepAt, plannedWakeAt)
}
```

Este es el **núcleo testeable JVM** del slice (Strict TDD): los escenarios "≥5h
habilita", "<5h bloquea", "exactamente 300 min válido", "derivación en tiempo real" se
cubren acá ANTES del wiring. (La validación cruda ya está testeada en `SleepPolicy`; el
wrapper sólo fija el contrato del onboarding y la derivación, igual que
`OnboardingAnchorsRule` envuelve `ScoringConstants`.)

### S3-D3 — Persistencia en prefs (sin Room) + escritura de la ventana elegida

El slice NO introduce entidad ni migración Room. Tres piezas de estado nuevas, con el
mismo patrón `MutableStateFlow(prefs.getX(...))` + `asStateFlow()` + `suspend set...()`:

| Pref key (snake_case) | Tipo | Significado |
|-----------------------|------|-------------|
| `sleep_usage_stats_requested` | Boolean | El usuario tocó "Activar" (intentó conceder el permiso). |
| `sleep_usage_stats_skipped` | Boolean | El usuario tocó "Más tarde" (saltó la oferta). |
| `sleep_wind_down_consent` | Boolean | Respuesta Sí/No al recordatorio (consentimiento informado). |

Notas de modelado:

- **`requested` vs `skipped` son flags independientes, no un enum**: el escenario
  "Permiso denegado por el sistema" exige distinguir "intentó activar, permiso no
  concedido" de "saltó". Tras volver de Settings, `requested=true` + permiso aún
  `MISSING` ⇒ ese estado exacto ("intentó, no concedido"). El consentimiento de
  activar/saltar persiste; el permiso real lo lee `TelemetryPermission.isGranted` en
  vivo (no se cachea el grant en prefs, evita desincronización si el usuario lo revoca
  en Settings).
- **La ventana elegida (`targetSleepAt` + `targetWakeAt`) NO va a prefs**: se persiste
  con el camino existente `repository.saveSleepConfig(...)` (Room `SleepConfigEntity`,
  ya escrito), preservando `digitalWindDownMinutes` actual. Esto NO es "modelo nuevo":
  reusa el upsert existente. La spec "Sin modelo de datos nuevo" prohíbe entidad/
  migración NUEVA, no escribir en la tabla `sleep_config` ya existente. Escribir la
  ventana al avanzar materializa la "elección activa" (Requirement gate 2: el default
  heredado NO debe usarse en silencio como elección).
- **Retroceso voluntario (decisión del orquestador #2):** los valores se MANTIENEN. Como
  la ventana vive en `SleepConfigEntity` y los consentimientos en prefs, reanudar o
  volver al bloque los relee tal cual (sin reset). El `remember(sleep.targetSleepAt)`
  del step se siembra del estado ya persistido.

### S3-D4 — Flujo del permiso UsageStats y retorno desde Settings (Compose lifecycle)

Este es el punto técnico delicado. `PACKAGE_USAGE_STATS` NO es un permiso runtime
(`requestPermissions` no aplica): se concede en una pantalla de Ajustes del sistema
(`Settings.ACTION_USAGE_ACCESS_SETTINGS`). El usuario SALE de la app y VUELVE; hay que
**re-evaluar el permiso al recuperar el foco**.

Patrón decidido (consistente con lo ya hecho en el repo + lifecycle canónico de
Compose):

- **Lanzamiento**: reusar el camino existente. "Activar" → `toggleSleepAutoMode(true,
  onPermissionRequired)`; si falta el permiso, `onPermissionRequired` muestra el
  `PermissionStep` (escape de Restricted Settings + botón a Usage access). El botón
  navega con `startActivity(TelemetryPermission.settingsIntent())` (ya cableado en
  `MainActivity`). Para abrir Settings se PREFIERE
  `rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult())`
  sobre `startActivity` directo, porque el contrato de Activity Result da un punto de
  retorno limpio; PERO como Usage access no retorna un `resultCode` útil (el usuario
  navega libremente), el re-chequeo NO puede depender del callback del launcher.
- **Re-chequeo en ON_RESUME (mecanismo principal)**: al volver del settings, se
  re-evalúa `TelemetryPermission.isGranted(context)` observando el ciclo de vida. Patrón
  Compose canónico:

  ```kotlin
  val lifecycleOwner = LocalLifecycleOwner.current
  DisposableEffect(lifecycleOwner) {
      val observer = LifecycleEventObserver { _, event ->
          if (event == Lifecycle.Event.ON_RESUME) {
              // re-leer permiso y reflejar estado (oculta el prompt si ya se concedió)
              onResumeRecheck()
          }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
  }
  ```

  El `onResumeRecheck` lee `TelemetryPermission.isGranted` y, si pasó a `GRANTED`,
  completa el toggle de automode (registra el worker) y oculta el prompt; si sigue
  `MISSING`, deja el estado "intentó, no concedido" sin error culpabilizador (escenario
  "Permiso denegado"). NINGÚN camino bloquea "Continuar": la oferta es 100% salteable.

- **NOTA de proceso (Context7):** el prompt exige consultar el MCP **Context7** para
  fijar este patrón (lifecycle / permission flows / `rememberLauncherForActivityResult`
  / `LocalLifecycleOwner`). En esta fase de diseño Context7 NO estuvo disponible como
  herramienta en el contexto del ejecutor (solo Read/Edit/Grep/Glob/Engram). El patrón
  documentado arriba se ancla en (a) el código ya probado del repo
  (`TelemetryPermission` + `toggleSleepAutoMode`, verificado en emulador en el feature de
  sueño) y (b) el patrón estándar `LifecycleEventObserver` + `DisposableEffect` +
  `LocalLifecycleOwner` de `androidx.lifecycle.compose`. **Acción para el apply:** ANTES
  de fijar el `DisposableEffect`/`ON_RESUME`, validar con Context7 (a) que
  `androidx.lifecycle:lifecycle-runtime-compose` ya esté en el classpath (lo está, se usa
  `collectAsStateWithLifecycle`), y (b) la firma vigente de `LifecycleEventObserver` /
  `LocalLifecycleOwner`. Si Context7 sugiere `rememberLauncherForActivityResult` con un
  contrato más limpio para el retorno, preferirlo SIN romper el invariante "salteable, no
  bloqueante".

### S3-D5 — Estado del step y orquestación (Compose solo renderiza)

`OnboardingSleepStep` recibe estado + callbacks; NO contiene lógica de negocio:

- Estado de UI local (`remember`): `targetSleepAt`, `targetWakeAt` (sembrados del
  `DashboardSleepState` ya persistido), `windDownConsent: Boolean?`, `showPermissionPrompt`.
- `canAdvance` se computa con `OnboardingSleepRule.canAdvance(targetSleepAt,
  targetWakeAt)` (dominio puro), NO con lógica inline en el Composable.
- Callbacks hacia arriba (resueltos por `MainActivity` + `DashboardViewModel` +
  `OnboardingViewModel`):
  - `onActivateTelemetry: (onPermissionRequired: () -> Unit) -> Unit` → reusa
    `dashboardViewModel.toggleSleepAutoMode(true, ...)` + persiste
    `setSleepUsageStatsRequested(true)`.
  - `onSkipTelemetry: () -> Unit` → persiste `setSleepUsageStatsSkipped(true)`.
  - `onWindDownConsent: (Boolean) -> Unit` → persiste `setSleepWindDownConsent(...)`.
  - `onContinue: (sleepAt, wakeAt) -> Unit` → persiste la ventana vía
    `saveSleepConfig(...)` y avanza (`onboardingViewModel.advance()`). Se llama SOLO si
    `canAdvance` (botón deshabilitado de otro modo).
  - `onBack` → `onboardingViewModel.back()` (los valores se mantienen, S3-D3).

`OnboardingViewModel` NO necesita lógica nueva más allá de exponer los setters de prefs
si se decide centralizar ahí; alternativa más simple y consistente con slice 2: los
callbacks de sueño se cablean en `MainActivity` directamente contra
`dashboardViewModel`/`repository`, igual que `onAddAnchor`/`onCreateAnchor` hoy. Se
prefiere esta segunda vía (menos superficie, igual patrón).

## S3.3 — Componentes nuevos / modificados (Slice 3)

| Componente | Tipo | Rol |
|------------|------|-----|
| `domain/onboarding/OnboardingSleepRule.kt` | New | Gate de ventana (wrapper puro de `SleepPolicy`) + derivación de duración |
| `domain/onboarding/OnboardingSleepRuleTest.kt` | New | TDD: ≥5h / <5h / =300 / derivación / inválida |
| `ui/onboarding/OnboardingSleepStep.kt` | New | Bloque Sueño dedicado (reemplaza el placeholder de `OnboardingStep.Sleep`) |
| `ui/sleep/SleepAutoModeCard.kt` | New (extracción) | `AutoModeCard` + `PermissionStep` movidos desde `SleepConfigScreen` para reuso |
| `ui/sleep/SleepWindowFields.kt` | New (extracción) | `TimeField` + `DurationRow` + `filterTimeInput` compartidos (opcional según budget, S3-D1) |
| `ui/sleep/SleepConfigScreen.kt` | Modified | Consume los helpers extraídos (sin cambio de comportamiento) |
| `ui/onboarding/OnboardingScreen.kt` | Modified | Rama `OnboardingStep.Sleep -> OnboardingSleepStep(...)` reemplaza el placeholder |
| `AutonomiaRepository.kt` | Modified | 3 prefs nuevas (S3-D3) con su patrón flow/setter |
| `MainActivity.kt` | Modified | Wiring del Bloque Sueño (callbacks telemetría/wind-down/continuar) en la rama `AppScreen.Onboarding` |

## S3.4 — Unidades de dominio puro testeables (Strict TDD, tests primero)

1. `OnboardingSleepRule.canAdvance` — `23:30→07:30` (8h) ⇒ true; `23:30→02:00` (2,5h) ⇒
   false; `23:30→04:30` (exactamente 300 min) ⇒ true (escenario "exacta de 5h").
2. `OnboardingSleepRule.derivedWindowMinutes` — derivación correcta y cruce de medianoche
   (`23:30→07:30` = 480); entrada imparseable ⇒ null (alimenta el "—" de la UI).
3. (Cobertura existente, no se re-testea aquí) `SleepPolicy.validatePlannedWindow` /
   `minutesBetween` ya están probados; el wrapper sólo fija el contrato del onboarding.

> Lo que NO es dominio puro (queda para capa runtime / verificación por capas): el
> `DisposableEffect`/`ON_RESUME`, el lanzamiento de Settings, y la persistencia en prefs.
> Esas se validan en emulador (capas 1-4 de `verificacion-por-capas.md`).

## S3.5 — Invariante "sin Room" (explícito)

El slice NO agrega entidad ni migración Room. La ventana usa la tabla `sleep_config`
EXISTENTE vía `saveSleepConfig` (upsert ya escrito); los consentimientos van a prefs. Si
durante el apply apareciera una necesidad real de Room (no la hay según este diseño), es
una **decisión para el dueño**, NO una asunción del ejecutor (regla de proyecto).

## S3.6 — Cómo el bloque reemplaza el placeholder

Hoy `OnboardingScreen` enruta `OnboardingStep.Sleep` a la rama `else ->
OnboardingBlock(...)` (placeholder genérico) y `placeholderTitle` ya devuelve "El
descanso primero" para `Sleep`. El cambio: agregar una rama explícita
`OnboardingStep.Sleep -> OnboardingSleepStep(...)` ANTES del `else`, pasando el
`DashboardSleepState` (de `dashboardState.sleep`) y los callbacks. El `else` queda solo
para `Intention` y `Sobriety` (placeholders de slice 4). El andamiaje de navegación
(`advance`/`back`/persistencia de `onboarding_current_step`) NO se toca: ya funciona.

## S3.7 — Mapa spec → diseño (trazabilidad Slice 3)

| Requirement (spec) | Resuelto por |
|--------------------|--------------|
| Active Sleep Window Choice (gate 2) | S3-D2 (`OnboardingSleepRule`) + S3-D3 (escribe ventana vía `saveSleepConfig`) + S3-D5 (botón `enabled`) |
| Telemetry Permission Offer (skippable) | S3-D4 (flujo permiso + ON_RESUME) + S3-D3 (`requested`/`skipped` prefs) |
| Wind-Down Consent Capture | S3-D3 (`sleep_wind_down_consent`) + S3-D5 (`onWindDownConsent`) |
| Copy canónico y tono | S3-D1 (copy v3 §4, serif/sans, mensaje neutral) |
| Sin modelo de datos nuevo | S3-D3 (prefs + `sleep_config` existente) + S3.5 (invariante) |

## S3.8 — Riesgos de implementación (Slice 3)

- **Budget de ~400 líneas:** la extracción de helpers (S3-D1) es lo que mantiene el slice
  acotado. Si la extracción + el step nuevo + tests superan el presupuesto, el fallback
  es duplicar los `TimeField`/`DurationRow` triviales (S3-D1 nota). Riesgo medio,
  mitigado con plan B.
- **Re-chequeo del permiso al volver de Settings:** si el `ON_RESUME` no re-evalúa, el
  prompt podría quedar pegado tras conceder el permiso. Mitigación: `DisposableEffect` +
  `LifecycleEventObserver` (S3-D4); validar firmas vigentes con Context7 en el apply.
- **Context7 no consultado en diseño:** el patrón lifecycle quedó documentado pero NO
  verificado contra Context7 en esta fase (herramienta no disponible en el contexto del
  ejecutor). Se traslada como acción obligatoria al apply (S3-D4). Riesgo bajo: el patrón
  ya está probado en el repo.
- **Escribir la ventana al avanzar:** si el usuario retrocede tras escribir y vuelve a
  avanzar, se re-escribe `sleep_config` (idempotente, upsert). Sin efecto adverso.

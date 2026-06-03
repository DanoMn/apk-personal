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

---

# Slice 4 — Bloque Intención + Bloque Sobriedad

Specs: `specs/onboarding-intention/spec.md` (4 requirements), `specs/onboarding-sobriety/spec.md`
(4 requirements), `specs/onboarding-gate-slice4-delta/spec.md` (MODIFIED `Block Navigation
Skeleton` + ADDED `Intention-Aware State`) · Insumo conceptual:
`meta/instructions/2026-06-02-onboarding-introduccion-diseno.md` §2.2, §2.4, §2.5, §4
(Bloques 0.5 y 3), §7.

> Estado de partida: slices 1 (gate + estado + esqueleto), 2 (anclas) y 3 (sueño)
> implementados, verificados en emulador y commiteados. SLICE 4 = **Bloque 0.5 Intención**
> (hoy cae en la rama `else -> OnboardingBlock(...)` con `placeholderTitle` "¿Qué te trae
> aquí?") + **Bloque 3 Sobriedad** (hoy también `else`, título "Cuidar algo que te
> cuesta"). Ambos placeholders se REEMPLAZAN. PR encadenado, presupuesto ~400 líneas.

## S4.1 — Contexto y hallazgo clave (qué ya existe, qué cambia)

El **corazón del slice no es UI sino dominio puro**: hoy `OnboardingFlow.next/previous`
recorren `OnboardingStep.entries` en orden LINEAL sin ramificar (verificado en
`OnboardingState.kt` líneas 36-46). La spec de gate (delta) exige que la secuencia dependa
de la **intención persistida**: la ruta estándar SALTA `Sobriety`; la ruta sobriedad lo
incluye. Eso obliga a cambiar la firma de `next/previous` y a modelar la intención.

Hallazgos que evitan reinventar:

- **El repo YA persiste tracks de abstinencia sin tocar Room nuevo.**
  `AutonomiaRepository.createCustomAbstinenceTrack(name)` (líneas 445-462) reusa
  `AbstinencePolicy.createCustomDraft(name)` (retorna `null` si el nombre normalizado es
  blanco) e inserta en la tabla EXISTENTE `AbstinenceTrackEntity` con `dao.upsertAbstinenceTrack`,
  aplicando los defaults del draft (`severity=Moderate`, `contributionRole=Protective`,
  `importanceTier=High`, `active=true`). El `DashboardViewModel` ya lo expone como
  `createCustomAbstinenceTrack` (cableado en `MainActivity` línea 275 para el dashboard).
  **El Bloque Sobriedad reusa exactamente este camino — cero Room nuevo, cero migración.**
- **El patrón de prefs sin Room está canonizado** (slices 1-3): `MutableStateFlow(prefs.getX(...))`
  + `asStateFlow()` + `suspend set...()` con `prefs.edit { }`. La intención usa este mismo patrón.
- **El precedente de los slices 2-3 (decisión del orquestador, NO re-abrir):** NO se reusa
  la pantalla de config completa cuando arrastra UI fuera de alcance; se hace un step
  dedicado que reusa **dominio + datos**. `SobrietyConfigScreen` trae gestión de tracks,
  presets, registro diario y borrado — todo fuera del alcance del onboarding (que solo
  ofrece crear UN track custom por nombre). Por lo tanto: **`OnboardingSobrietyStep`
  dedicado** que reusa `AbstinencePolicy` + el writer del repo, NO la pantalla completa.

## S4.2 — Decisiones de diseño (Slice 4)

### S4-D1 — La intención se modela como campo de `OnboardingState` (no como overloads)

Decisión de modelado (la instrucción dejó la firma a criterio del ejecutor; se elige la
opción más limpia y la documenta). Se introduce un enum de dominio puro y se agrega como
campo del estado:

```kotlin
// domain/onboarding/OnboardingIntention.kt (New)
enum class OnboardingIntention { STANDARD, PROTECTION }

// domain/onboarding/OnboardingState.kt (Modified)
data class OnboardingState(
    val completed: Boolean,
    val currentStep: OnboardingStep,
    val intention: OnboardingIntention = OnboardingIntention.STANDARD,  // default seguro
)
```

Por qué campo y no overloads sueltos de `next(step, intention)`:

- `next/previous` leen la ruta DESDE el estado (`state.intention`), de modo que el
  ViewModel y los tests trabajan con un único valor de verdad. Evita propagar el parámetro
  `intention` por toda la cadena de llamadas y mantiene la firma de navegación estable.
- **Ausencia de intención = `STANDARD`** (delta spec, ADDED "Intention-Aware State"): el
  default del campo y el fallback de `resolve` garantizan que evaluar el flujo ANTES de que
  el usuario pase por el Bloque 0.5 nunca crashea y nunca muestra `Sobriety` por accidente.

> **Firma elegida (NOTA de impacto en tests, S4.4):** `next`/`previous` reciben la
> intención. Para no romper a ciegas el contrato y mantener testabilidad pura, se exponen
> como `fun next(step, intention)` / `fun previous(step, intention)`. El ViewModel pasa
> `onboardingState.value.intention`. Esto **cambia el contrato de slice 1** → los tests
> `OnboardingFlowTest` que llaman `next(step)` / `previous(step)` sin intención DEBEN
> migrarse (detalle en S4.4). No se dejan overloads de compatibilidad: arrastrar una firma
> sin-intención invita a olvidar la ramificación. La migración de tests es parte del slice.

### S4-D2 — `resolve` aprende a leer la intención persistida

`OnboardingFlow.resolve` gana un tercer parámetro para hidratar el campo `intention` desde
prefs (string nominal → enum, con fallback seguro a `STANDARD` ante null/valor inválido,
espejando el clamp de paso inválido ya existente):

```kotlin
fun resolve(
    completed: Boolean,
    persistedStepName: String?,
    persistedIntention: String?,   // "STANDARD" | "PROTECTION" | null/inválido → STANDARD
): OnboardingState
```

La conversión usa `runCatching { OnboardingIntention.valueOf(it) }.getOrNull() ?: STANDARD`,
idéntico patrón al `OnboardingStep.valueOf` actual. Esto resuelve los escenarios de
reanudación ("Reanudación con intención persistida respeta la ruta") sin lógica en Compose.

### S4-D3 — Tabla de transiciones ramificadas (`next`/`previous`)

La ramificación solo aplica de `Sleep` en adelante (delta spec). El resto de la secuencia
es lineal. Tabla canónica de transiciones que `OnboardingFlow` debe implementar:

| Paso actual | `next` STANDARD | `next` PROTECTION | `previous` STANDARD | `previous` PROTECTION |
|-------------|-----------------|-------------------|---------------------|-----------------------|
| Welcome     | Intention       | Intention         | Welcome (clamp)     | Welcome (clamp)       |
| Intention   | Anchors         | Anchors           | Welcome             | Welcome               |
| Anchors     | Sleep           | Sleep             | Intention           | Intention             |
| **Sleep**   | **Closing**     | **Sobriety**      | Anchors             | Anchors               |
| **Sobriety**| Closing¹        | Closing           | Sleep¹              | **Sleep**             |
| **Closing** | Closing (clamp) | Closing (clamp)   | **Sleep**           | **Sobriety**          |

¹ En ruta `STANDARD`, `Sobriety` no debería ser alcanzable por navegación normal (se
saltea). Si por reanudación con paso persistido `Sobriety` + intención `STANDARD` se evalúa,
`next(Sobriety, STANDARD)=Closing` y `previous(Sobriety, STANDARD)=Sleep` lo tratan como un
paso "de paso" coherente (no crashea). Caso de borde defensivo, no de flujo feliz.

Implementación recomendada (dominio puro, sin reflexión frágil sobre ordinales): un `when`
explícito sobre `(step, intention)` para los pasos ramificados (`Sleep`, `Sobriety`,
`Closing`) y el recorrido lineal de `entries` para el resto. El `when` explícito es más
legible y testeable que filtrar dinámicamente la lista de entries.

### S4-D4 — Persistencia de la intención en prefs (sin Room)

Nueva clave de pref con el patrón canónico, en `AutonomiaRepository`:

| Pref key (snake_case) | Tipo | Valores | Significado |
|-----------------------|------|---------|-------------|
| `onboarding_intention` | String? | `"STANDARD"` \| `"PROTECTION"` \| `null` | Ruta elegida en Bloque 0.5. `null` = aún no elegida (se trata como STANDARD en `resolve`). |

```kotlin
private val _onboardingIntention = MutableStateFlow(prefs.getString("onboarding_intention", null))
fun onboardingIntentionFlow(): StateFlow<String?> = _onboardingIntention.asStateFlow()
suspend fun setOnboardingIntention(value: String) {
    prefs.edit { putString("onboarding_intention", value) }
    _onboardingIntention.value = value
}
```

Se persiste por **nombre del enum** (no ordinal), mismo criterio que `onboarding_current_step`.
Decisión cerrada #1 incorporada: la intención SOLO se usa para ramificar; teñir tono/ofertas
queda documentado como FUTURO (ver S4.7 "Fuera de alcance").

### S4-D5 — `OnboardingViewModel`: ramifica leyendo el estado, persiste la intención

`OnboardingViewModel` combina ahora TRES flows (agrega `onboardingIntentionFlow`) y pasa
la intención a `resolve`. `advance`/`back` leen la ruta del estado:

```kotlin
val onboardingState: StateFlow<OnboardingState> =
    combine(
        repository.isInitialConfigurationCompleteFlow(),
        repository.onboardingCurrentStepFlow(),
        repository.onboardingIntentionFlow(),
    ) { completed, stepName, intentionName ->
        OnboardingFlow.resolve(completed, stepName, intentionName)
    }.stateIn(/* initialValue = resolve(.value, .value, .value) — síncrono, sin flicker */)

fun advance() {
    val s = onboardingState.value
    val next = OnboardingFlow.next(s.currentStep, s.intention)
    viewModelScope.launch { repository.setOnboardingCurrentStep(next.name) }
}
fun back() {
    val s = onboardingState.value
    val previous = OnboardingFlow.previous(s.currentStep, s.intention)
    viewModelScope.launch { repository.setOnboardingCurrentStep(previous.name) }
}

/** Nuevo: persiste la elección del Bloque 0.5 ANTES de avanzar. */
fun selectIntention(intention: OnboardingIntention) {
    viewModelScope.launch { repository.setOnboardingIntention(intention.name) }
}
```

`combine` con 3 fuentes mantiene el patrón existente; el `initialValue` síncrono evita
flicker igual que en slice 1. El ViewModel NO contiene reglas de ramificación: solo lee
`s.intention` y delega en `OnboardingFlow` (dominio puro).

### S4-D6 — `OnboardingIntentionStep` (Bloque 0.5) — Composable dedicado

Nuevo `ui/onboarding/OnboardingIntentionStep.kt`. Reemplaza la rama `else` para
`OnboardingStep.Intention`. Estructura (copy v3 §4 "Bloque 0.5", español neutro, estilo
oscuro orgánico / coral mate / serif en título):

1. **Encabezado**: "¿Qué te trae aquí?" (serif) + aviso "No hay respuesta correcta. Podrás
   cambiarla cuando quieras." (sans, `textMuted`).
2. **Dos opciones seleccionables** (tarjetas planas, sin bordes duros): "Quiero ordenar mi
   día a día" (→ `STANDARD`) y "Quiero cuidarme de algo que me cuesta" (→ `PROTECTION`).
   La opción tocada queda con feedback visual (resalte coral); el estado de selección es UI
   local (`remember { mutableStateOf<OnboardingIntention?>(...) }`), sembrado de
   `state.intention` si ya había elección persistida (soporta el escenario "cambiada al
   volver atrás").
3. **Botón "Continuar"**: `enabled = selección != null`, espejando `OnboardingPrimaryButton(enabled=...)`
   ya usado en sleep/anchors. Sin selección → deshabilitado, SIN mensaje culpabilizador
   (escenario "Sin selección, avance bloqueado").
4. **"Volver"** opcional (mismo patrón que `OnboardingBlock`).

Callback hacia arriba: `onSelectAndContinue: (OnboardingIntention) -> Unit` que, en
`MainActivity`, persiste la intención (`onboardingViewModel.selectIntention(it)`) y LUEGO
avanza (`onboardingViewModel.advance()`). Orden importa: persistir la intención ANTES de
`advance`, para que `next(Sleep, intention)` —cuando se llegue— ya lea la ruta correcta.

> **Regla de avance (dominio puro testeable):** la habilitación del botón es una función
> pura `OnboardingIntentionRule.canAdvance(selection: OnboardingIntention?): Boolean =
> selection != null`, análoga a `OnboardingAnchorsRule`/`OnboardingSleepRule`. Mantiene la
> regla fuera del Composable y le da un test JVM directo (criterio de aceptación de la spec
> de intención). Si se considera demasiado trivial para un objeto propio, se admite
> testearla como parte de `OnboardingFlowTest`; decisión final del apply, pero la regla NO
> vive inline en Compose.

### S4-D7 — `OnboardingSobrietyStep` (Bloque 3) — Composable dedicado + creación de track

Nuevo `ui/onboarding/OnboardingSobrietyStep.kt`. Reemplaza la rama `else` para
`OnboardingStep.Sobriety`. Aparece SOLO en ruta `PROTECTION` (la ramificación de S4-D3
garantiza que en `STANDARD` nunca se navega a él). Estructura (copy v3 §4 "Bloque 3"):

1. **Encabezado**: "Cuidar algo que te cuesta" (serif) + el cuerpo literario v3 ("A veces
   hay un hábito oscuro… solo el ejercicio de tu libertad…") + el mensaje de tono canónico
   **literal**: "Una recaída no es un fracaso. Es una señal, no una condena." (escenario de
   tono lo exige textual).
2. **Pregunta**: "¿Quieres llevar el registro de algo que estás cuidando?" con dos acciones:
   **"Sí, agregar"** y **"Ahora no"**. Ninguna bloquea el avance (ambas llevan a `Closing`).
3. **"Sí, agregar"** revela un **formulario mínimo** (un solo `TextField` para nombrar lo
   que se está cuidando) + botón confirmar. Estado UI local: `showForm: Boolean`,
   `trackName: String`. Al confirmar:
   - Validación vía `AbstinencePolicy.createCustomDraft(name)`: si retorna `null` (nombre
     en blanco/solo espacios), NO avanza ni crea nada; mensaje neutral opcional (sin
     "fallaste"/diagnóstico — escenario "nombre en blanco rechazado sin culpa").
   - Si es válido: `onCreateTrack(name)` → repo crea el track + `advance()`.
4. **"Ahora no"**: `onSkip()` → `advance()` directo, sin crear nada.
5. **"Volver"** opcional.

Callbacks hacia arriba (cableados en `MainActivity`, igual patrón que slice 3):

- `onCreateTrackAndContinue: (name: String) -> Unit` →
  `scope.launch { repository.createCustomAbstinenceTrack(name); onboardingViewModel.advance() }`.
  **Reusa el writer existente** (S4.1): inserta en `AbstinenceTrackEntity`, cero Room nuevo.
  Alternativa equivalente: `dashboardViewModel.createCustomAbstinenceTrack(name)` (ya
  expuesto) + `advance()`. Se prefiere ir directo al `repository` dentro del `scope.launch`
  para encadenar con `advance()` de forma secuencial (el track existe antes de salir del
  bloque), igual que `onSleepContinue` encadena `saveSleepConfig` + `advance`.
- `onSkipSobriety: () -> Unit` → `onboardingViewModel.advance()`.

> **Validación de nombre = dominio puro YA testeado.** `AbstinencePolicy.createCustomDraft`
> ya existe y se testea como núcleo TDD del slice (nombre vacío → `null`; nombre válido →
> draft con defaults `Moderate`/`Protective`/`High`). No se reimplementa validación en
> Compose: el Composable solo llama y reacciona a `null` vs no-`null`.

### S4-D8 — Track huérfano se MANTIENE (decisión cerrada #2)

Si el usuario crea un track en ruta `PROTECTION`, vuelve atrás al Bloque 0.5 y cambia a
`STANDARD`, el track **NO se destruye**. No se agrega lógica de borrado: regla de no
destruir datos del usuario; la Sobriedad sigue accesible desde el Dashboard tras completar
el onboarding. El cambio de intención solo afecta la **secuencia de navegación** (re-evalúa
la ruta), nunca los datos ya creados. Esto es consistente con S3-D3 (los valores se
mantienen al retroceder).

## S4.3 — Componentes nuevos / modificados (Slice 4)

| Componente | Tipo | Rol |
|------------|------|-----|
| `domain/onboarding/OnboardingIntention.kt` | New | Enum `STANDARD`/`PROTECTION` (dominio puro) |
| `domain/onboarding/OnboardingState.kt` | Modified | Campo `intention` + `resolve(.., persistedIntention)` + `next/previous` ramificados (S4-D1/D2/D3) |
| `domain/onboarding/OnboardingIntentionRule.kt` | New (opcional) | `canAdvance(selection): Boolean` puro (S4-D6); puede plegarse en `OnboardingFlow` |
| `domain/onboarding/OnboardingFlowTest.kt` | Modified | Migrar firmas `next/previous` + casos de ramificación (S4.4) |
| `AutonomiaRepository.kt` | Modified | Pref `onboarding_intention` (flow/setter, S4-D4). Reusa `createCustomAbstinenceTrack` existente (sin cambio) |
| `ui/onboarding/OnboardingViewModel.kt` | Modified | `combine` 3 flows + `selectIntention` + `advance/back` leen `state.intention` (S4-D5) |
| `ui/onboarding/OnboardingIntentionStep.kt` | New | Bloque 0.5 dedicado (reemplaza placeholder) |
| `ui/onboarding/OnboardingSobrietyStep.kt` | New | Bloque 3 dedicado (reemplaza placeholder) + creación de track |
| `ui/onboarding/OnboardingScreen.kt` | Modified | Ramas explícitas `Intention -> ...` y `Sobriety -> ...` antes del `else`; nuevos parámetros/callbacks |
| `MainActivity.kt` | Modified | Wiring: `selectIntention`+`advance` (Bloque 0.5); `createCustomAbstinenceTrack`+`advance` / skip (Bloque 3) |

## S4.4 — Unidades de dominio puro testeables (Strict TDD, tests primero)

**El corazón TDD del slice es la ramificación de `OnboardingFlow`.** Tests escritos ANTES
del wiring (Strict TDD activo). Casos:

1. **Ramificación `next` (delta spec, los 4 escenarios canónicos):**
   - `next(Sleep, STANDARD)` ⇒ `Closing` (Sobriety se saltea).
   - `next(Sleep, PROTECTION)` ⇒ `Sobriety`.
   - `next(Sobriety, PROTECTION)` ⇒ `Closing`.
   - (borde) `next(Sobriety, STANDARD)` ⇒ `Closing` (defensivo, no crashea).
2. **Ramificación `previous`:**
   - `previous(Closing, STANDARD)` ⇒ `Sleep` (no `Sobriety`).
   - `previous(Closing, PROTECTION)` ⇒ `Sobriety`.
   - `previous(Sobriety, PROTECTION)` ⇒ `Sleep`.
3. **Intención ausente no crashea:** `next(Welcome, intention=STANDARD)` (y cualquier paso
   < Sleep) ⇒ paso siguiente lineal sin excepción (ADDED "Intention-Aware State"). Como el
   campo tiene default `STANDARD`, evaluar con intención por defecto cubre el escenario.
4. **`resolve` con intención:** `resolve(false, "Sleep", "PROTECTION").intention == PROTECTION`;
   `resolve(false, null, null).intention == STANDARD`; `resolve(false, null, "basura").intention == STANDARD`
   (fallback seguro).
5. **Regla de avance de intención:** `OnboardingIntentionRule.canAdvance(null) == false`;
   `canAdvance(STANDARD) == true`; `canAdvance(PROTECTION) == true`.
6. **Validación del track (cobertura existente, se referencia):** `AbstinencePolicy.createCustomDraft("")`
   ⇒ `null`; `createCustomDraft("  ")` ⇒ `null`; `createCustomDraft("Alcohol")` ⇒ draft con
   `severity=Moderate`, `contributionRole=Protective`, `importanceTier=High`. Si ya hay test
   de `AbstinencePolicy`, no se duplica; si no, se agrega (es núcleo TDD del Bloque 3).

### Migración de `OnboardingFlowTest` (impacto del cambio de contrato — explícito)

El cambio de firma de S4-D1 ROMPE los tests de slice 1 que llaman `next(step)` /
`previous(step)` sin intención. Tests a migrar (de `OnboardingFlowTest.kt`):

| Test (slice 1) | Acción de migración |
|----------------|---------------------|
| `next avanza al bloque siguiente` | Agregar `intention` → `next(Welcome, STANDARD)` (sigue ⇒ `Intention`). |
| `next en el ultimo bloque se mantiene` | `next(Closing, STANDARD)` ⇒ `Closing`. |
| `next recorre la secuencia completa hasta Closing` | **Reescribir**: con `STANDARD` la secuencia es Welcome→…→Sleep→**Closing** (sin Sobriety, 5 pasos). Agregar un caso gemelo con `PROTECTION` que SÍ incluye Sobriety (6 pasos). El assert `entries.toList()` ya NO es válido como secuencia: ahora hay dos secuencias según ruta. |
| `previous retrocede al bloque anterior` | `previous(Sleep, STANDARD)` ⇒ `Anchors` (sin cambio de valor, solo firma). |
| `previous en el primer bloque se mantiene` | `previous(Welcome, STANDARD)` ⇒ `Welcome`. |
| `resolve …` (4 tests) | Agregar el tercer argumento `persistedIntention` (puede ser `null` → STANDARD; los asserts de `currentStep`/`completed` no cambian). |
| `shouldStartOnboarding …` (2 tests) | Sin cambio (no tocan `next/previous`); si construyen `OnboardingState`, el nuevo campo tiene default → siguen compilando. |

Esta migración es **parte del slice** y entra en el conteo de líneas. Es el costo
inevitable de ramificar el contrato; documentarlo evita que el apply lo descubra tarde.

> Lo que NO es dominio puro (queda para verificación por capas, capas 1-4): la persistencia
> en prefs, el feedback visual de selección, el formulario de track y la inserción real en
> `AbstinenceTrackEntity`. Se validan en emulador.

## S4.5 — Invariante "sin Room" (explícito)

El slice NO agrega entidad ni migración Room. La **intención** va a `SharedPreferences`
(`onboarding_intention`). El **track de sobriedad** reusa el writer existente
`createCustomAbstinenceTrack` → `dao.upsertAbstinenceTrack` sobre la tabla EXISTENTE
`AbstinenceTrackEntity` (insert, NO migración). La spec "Sin modelo de datos nuevo" prohíbe
entidad/migración NUEVA, no escribir en una tabla existente. Si durante el apply apareciera
una necesidad real de Room (no la hay según este diseño), es **decisión del dueño**, NO una
asunción del ejecutor (regla de proyecto: si creés que hace falta Room nuevo, FRENÁ y
reportá).

## S4.6 — Cómo los bloques reemplazan sus placeholders

Hoy `OnboardingScreen` enruta tanto `Intention` como `Sobriety` a la rama genérica `else ->
OnboardingBlock(...)`, con `placeholderTitle` devolviendo "¿Qué te trae aquí?" y "Cuidar
algo que te cuesta" respectivamente. El cambio:

- Agregar **antes del `else`** dos ramas explícitas:
  `OnboardingStep.Intention -> OnboardingIntentionStep(...)` y
  `OnboardingStep.Sobriety -> OnboardingSobrietyStep(...)`.
- Tras esto, **el `else` queda sin pasos reales** (Welcome/Closing/Anchors/Sleep/Intention/
  Sobriety ya tienen rama propia). Se puede dejar el `else` como salvaguarda defensiva
  (futuros pasos) o eliminarlo; se recomienda dejarlo mínimo. `placeholderTitle` queda sin
  consumidores → se elimina (limpieza).
- El andamiaje de navegación (`advance`/`back`/persistencia de `onboarding_current_step`)
  NO se reescribe: ya funciona; solo `advance/back` ahora leen `state.intention` (S4-D5).
- Nuevos parámetros de `OnboardingScreen` (siguiendo el patrón de slice 3): `intention`
  actual (para sembrar la selección), `onSelectIntention: (OnboardingIntention) -> Unit`,
  `onCreateSobrietyTrack: (name: String) -> Unit`, `onSkipSobriety: () -> Unit`. Todos con
  default no-op para no romper call-sites/previews.

## S4.7 — Fuera de alcance (documentado como futuro)

- **Teñir tono/ofertas según la intención** (mensajes del dashboard, sugerencias): la
  intención se persiste pero SOLO ramifica el onboarding (decisión #1). Adaptar el dashboard
  según la ruta es trabajo FUTURO, fuera de este slice.
- **Presets de tracks** (`presetTrackIds`): el onboarding solo crea track custom por nombre;
  los presets viven en el Dashboard.
- **Configuración fina del track** (severidad, tier, rol): toma defaults del draft; se
  ajusta después desde el Dashboard.
- **Borrado del track huérfano** al cambiar de ruta: NO se hace (decisión #2, S4-D8).
- **Cambiar la intención tras completar el onboarding:** fuera de v1.
- **Modo riesgo** (`RiskEventEntity`): no se ofrece ni menciona.

## S4.8 — Notas Context7 (validar en apply)

La ramificación del flujo (Kotlin puro) NO requiere Context7. Si durante el apply surge una
duda de mejores prácticas al cablear la creación del track o el `TextField` del formulario:

- **Validar con Context7 en apply:** el `TextField`/formulario de nombre del Bloque 3 es
  Compose estándar (Material3 `OutlinedTextField` o equivalente del design system del repo);
  si se introduce un patrón Compose nuevo (p. ej. manejo de foco/IME del campo), validar la
  firma vigente con Context7 antes de fijarlo. La inserción del track NO toca un DAO nuevo
  (reusa `createCustomAbstinenceTrack` suspend ya escrito), así que no hay duda Room nueva.

## S4.9 — Mapa spec → diseño (trazabilidad Slice 4)

| Requirement (spec) | Resuelto por |
|--------------------|--------------|
| (intention) Presentación de las dos rutas | S4-D6 (`OnboardingIntentionStep`, copy v3, feedback visual, gate de avance) |
| (intention) Persistencia de la intención en prefs | S4-D4 (`onboarding_intention`) + S4-D5 (`selectIntention`) |
| (intention) Tono y nombres canónicos | S4-D6 (copy neutro, sin clínico; "Sobriedad" canónico) |
| (intention/sobriety) Sin modelo de datos nuevo | S4-D4 (prefs) + S4.5 (writer existente) |
| (sobriety) Oferta opcional de track | S4-D7 (`OnboardingSobrietyStep`, ambas acciones avanzan, validación `createCustomDraft`) |
| (sobriety) Exclusividad de ruta | S4-D3 (tabla de transiciones: `next(Sleep,STANDARD)=Closing`) |
| (sobriety) Tono sin culpa | S4-D7 (mensaje canónico literal) |
| (gate delta) Block Navigation Skeleton ramificado | S4-D1/D2/D3 (`OnboardingFlow` ramificado) + S4-D5 (ViewModel pasa intención) |
| (gate delta) Intention-Aware State | S4-D1 (campo + default STANDARD) + S4-D2 (`resolve` fallback) |

## S4.10 — Riesgos de implementación (Slice 4)

- **Budget de ~400 líneas — riesgo medio-alto.** El slice agrupa DOS bloques + cambio de
  contrato de dominio + migración de tests. Conteo estimado: `OnboardingIntention.kt` (~5),
  cambios en `OnboardingState.kt` (~40, ramificación + resolve), `OnboardingIntentionRule.kt`
  (~10), `OnboardingIntentionStep.kt` (~90), `OnboardingSobrietyStep.kt` (~120, incluye
  formulario), pref en repo (~8), ViewModel (~20), `OnboardingScreen.kt` (~25),
  `MainActivity.kt` (~25), migración + nuevos tests (~80). Total grueso **~420-440 líneas** →
  **roza/excede el budget.**

  **Split propuesto si excede (orden de corte, move-only vs lógica):**
  1. **PR 4a (lógica/dominio — el corazón TDD):** `OnboardingIntention.kt` + ramificación de
     `OnboardingState.kt` + `OnboardingIntentionRule.kt` + **migración completa de
     `OnboardingFlowTest`** + pref `onboarding_intention` en repo + cambios de
     `OnboardingViewModel` (`selectIntention`, `advance/back` ramificados). Esto es el núcleo
     verificable por tests JVM, autónomo (la UI sigue cayendo en placeholders pero el flujo
     ya ramifica). ~180-200 líneas.
  2. **PR 4b (UI Intención):** `OnboardingIntentionStep.kt` + rama en `OnboardingScreen` +
     wiring de `selectIntention` en `MainActivity`. ~140 líneas.
  3. **PR 4c (UI Sobriedad):** `OnboardingSobrietyStep.kt` + rama en `OnboardingScreen` +
     wiring de creación de track en `MainActivity`. ~150 líneas.

  El corte 4a/4b/4c es por **responsabilidad** (dominio vs UI-intención vs UI-sobriedad),
  no move-only: cada PR es funcionalmente coherente y testeable. Recomendación: intentar el
  slice completo primero; si el conteo real supera ~400, cortar 4a (dominio+tests) como PR
  encadenado y dejar 4b+4c juntos o separados según margen. Decisión final del apply con
  conteo real.

- **Migración de tests olvidada → build roto.** El cambio de firma de `next/previous` ROMPE
  la compilación de `OnboardingFlowTest` (slice 1) si no se migra. Mitigación: la migración
  está listada exhaustivamente en S4.4 y va en el MISMO PR que el cambio de firma (PR 4a en
  el split). No se permite dejar overloads de compatibilidad (S4-D1).

- **Orden persistir-intención-antes-de-avanzar.** Si el wiring del Bloque 0.5 llama
  `advance()` antes de `selectIntention()`, la pref de intención podría no estar escrita
  cuando se evalúe `next(Sleep, ...)`. Mitigación: el callback `onSelectIntention` persiste
  PRIMERO y avanza después (S4-D6); como el avance al Bloque 1/2 no ramifica (solo Sleep en
  adelante), hay margen, pero el orden correcto evita carreras sutiles en reanudación.

- **Default `STANDARD` oculta intención no elegida.** Si el usuario llega a Sleep sin pasar
  conscientemente por el Bloque 0.5 (no debería, es secuencial), la ruta cae a `STANDARD` y
  no vería Sobriety. Es el comportamiento deseado por la spec (ausencia = STANDARD), pero se
  documenta como decisión explícita, no como bug.

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

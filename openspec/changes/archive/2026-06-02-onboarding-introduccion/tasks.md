# Tasks: onboarding-introduccion · slice 1 (onboarding-gate)

## Review Workload Forecast

| Campo | Valor |
|-------|-------|
| Líneas cambiadas estimadas | 350–400 (test ~110, dominio ~60, UI ~150, repo+MainActivity ~40) |
| 400-line budget risk | Med (cerca del techo) |
| Chained PRs recommended | No (este slice = 1 PR; el CHANGE global sí es chained entre slices) |
| Suggested split | PR único para slice 1 |
| Delivery strategy | ask-on-risk |
| Chain strategy | chained-across-slices (slices 2-5 son PRs posteriores) |

Decision needed before apply: No (slice 1 dentro del budget)
Chained PRs recommended: No (intra-slice)
400-line budget risk: Med — si `OnboardingScreen` crece, mover placeholders de bloques a su slice.

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Dominio puro (enum + state + flow) + tests | PR único | Núcleo TDD del slice |
| 2 | Wiring: repo + ViewModel + OnboardingScreen + gate MainActivity | (mismo PR) | Verificado por capas runtime |

---

## Phase 1: Foundation (tipos del dominio, para compilar tests)

- [x] 1.1 Crear `app/src/main/java/dev/panopt/autonomia/domain/onboarding/OnboardingStep.kt`: `enum class OnboardingStep { Welcome, Intention, Anchors, Sleep, Sobriety, Closing }` (el orden de declaración ES la secuencia). Vinculado a design §D3.
- [x] 1.2 Crear `domain/onboarding/OnboardingState.kt`: `data class OnboardingState(val completed: Boolean, val currentStep: OnboardingStep)` + `object OnboardingFlow { val firstStep = OnboardingStep.Welcome; fun resolve(completed: Boolean, persistedStepName: String?): OnboardingState; fun next(step): OnboardingStep; fun previous(step): OnboardingStep }` con cuerpos stub (TODO / `NotImplementedError`) para que los tests compilen y fallen RED. Vinculado a design §D3.
- [x] 1.3 En `AutonomiaRepository.kt`: agregar la clave de reanudación siguiendo el patrón existente — `_onboardingCurrentStep = MutableStateFlow(prefs.getString("onboarding_current_step", null))`, `fun onboardingCurrentStepFlow(): StateFlow<String?>`, `suspend fun setOnboardingCurrentStep(stepName: String)` (con `prefs.edit { putString(...) }`). NO crear flag de completitud nuevo: se reusa `isInitialConfigurationCompleteFlow()` / `setInitialConfigurationComplete()` existentes. Vinculado a design §D1, §D2.

---

## Phase 2: Tests (RED — TDD strict, antes de implementar Phase 3)

- [x] 2.1 Crear `app/src/test/java/dev/panopt/autonomia/domain/onboarding/OnboardingFlowTest.kt`. Casos de `resolve` (spec §Persisted State, §Invalid Step): `resolve(false, null) → OnboardingState(false, Welcome)`; `resolve(false, "Sleep") → currentStep Sleep`; `resolve(false, "Inexistente") → currentStep Welcome` (clamp); `resolve(true, "Sleep") → completed true` (el step persiste pero el gate ruteará a Dashboard).
- [x] 2.2 En `OnboardingFlowTest.kt`, casos de `next` (spec §Block Navigation): `next(Welcome) → Intention`; secuencia completa hasta `next(Closing) → Closing` (último se mantiene). Casos de `previous`: `previous(Sleep) → Anchors`; `previous(Welcome) → Welcome` (primero se mantiene).
- [x] 2.3 En `OnboardingFlowTest.kt`, derivación de pantalla inicial (spec §First-Run Routing): una función/regla pura `completed == true → ruta Dashboard`, `completed == false → ruta Onboarding`. (Trabaja sobre `OnboardingState`/`Boolean`, NO sobre `AppScreen` — design §6 riesgos.)

---

## Phase 3: Implementación dominio (GREEN)

- [x] 3.1 Implementar `OnboardingFlow.resolve(...)`: parsear `persistedStepName` con `runCatching { OnboardingStep.valueOf(it) }.getOrNull() ?: firstStep`; `completed=false` y nombre nulo/ inválido → `currentStep = Welcome`. Vinculado a spec §Invalid Persisted Step, §Persisted State.
- [x] 3.2 Implementar `next`/`previous` sobre `OnboardingStep.entries` con clamp en los bordes (último→último, primero→primero). Vinculado a spec §Block Navigation Skeleton.
- [x] 3.3 Correr `OnboardingFlowTest` y dejar Phase 2 en verde. ✅ 11/11 verdes (rojo→verde confirmado). Test runner: `powershell.exe -Command "$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat testDebugUnitTest --tests 'dev.panopt.autonomia.domain.onboarding.*' --no-daemon"`.

---

## Phase 4: Wiring UI + gate (verificado por capas runtime, no TDD)

- [x] 4.1 Crear `ui/onboarding/OnboardingViewModel.kt`: combina `isInitialConfigurationCompleteFlow()` + `onboardingCurrentStepFlow()` vía `OnboardingFlow.resolve` y expone `onboardingState: StateFlow<OnboardingState>`. Métodos: `advance()` (persiste `OnboardingFlow.next` con `setOnboardingCurrentStep`), `back()` (`previous`), `complete()` (`setInitialConfigurationComplete(true)`). Vinculado a design §D4.
- [x] 4.2 Crear `ui/onboarding/OnboardingScreen.kt`: recibe `currentStep` + `onAdvance`/`onBack`/`onComplete`; `when (currentStep)` renderiza el bloque. **Bloque 0 (Welcome)** y **Bloque 4 (Closing)** con el copy canónico del doc de captura (nombre "Autonomía sin límites", tono respetado). Bloques `Intention/Anchors/Sleep/Sobriety` = placeholders navegables mínimos. `onAdvance` en Closing → `onComplete`. Vinculado a design §D5, spec §Block Navigation, §Tono.
- [x] 4.3 En `MainActivity.kt`: agregar `Onboarding` al `enum AppScreen`; instanciar/obtener `OnboardingViewModel`; sembrar `currentScreen` inicial desde el valor síncrono de `onboardingState` (`if (completed) Dashboard else Onboarding`); agregar la rama `AppScreen.Onboarding -> OnboardingScreen(...)` en el `when`; `onComplete` → `currentScreen = AppScreen.Dashboard`. Vinculado a design §D4, spec §First-Run Routing, §Completion.

---

## Phase 5: Verificación por capas (obligatoria — es UI)

- [x] 5.1 Build: `assembleDebug` verde (ver comando en `CLAUDE.md`). Lint sin Error. ✅ assembleDebug + lintDebug + testDebugUnitTest (suite completa) en verde, sin regresiones.
- [ ] 5.2 ⏳ PENDIENTE (requiere emulador; no había adb disponible en la sesión) Runtime en emulador (`scripts/dev/dev.sh`): **install limpio** → la app abre en Bloque 0 (Bienvenida), NO en Dashboard. Vinculado a spec §First-Run Routing.
- [ ] 5.3 Runtime: avanzar hasta un bloque intermedio, **matar la app**, reabrir → reanuda en ese bloque. Vinculado a spec §Resume on Reopen.
- [ ] 5.4 Runtime: completar el onboarding (llegar al cierre → "Entrar") → navega a Dashboard; **relanzar** → abre en Dashboard (no repite). Vinculado a spec §Completion Sets Flag.
- [ ] 5.5 Logcat sin errores/crashes durante el flujo. Confirmar que no se introdujo migración Room (spec §Persisted State).

---

## Phase 6: Cierre del slice

- [x] 6.1 Marcar el avance en `openspec` (apply-progress) y dejar nota en `meta/pendientes.md` de que slice 1 (gate) quedó listo y siguen slices 2-5 (anclas, sueño, sobriedad+intención, notificaciones).

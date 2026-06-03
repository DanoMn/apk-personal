# Tasks: onboarding-introduccion · slice 4 (Intención + Sobriedad)

## Review Workload Forecast

| Campo | Valor |
|-------|-------|
| Líneas estimadas | ~420–440 (dominio+tests ~130, pref+ViewModel ~30, UI Intención ~90, UI Sobriedad ~120, wiring+OnboardingScreen ~50) |
| 400-line budget risk | High |
| Chained PRs recommended | No — entregas como commits a main por responsabilidad (mismo patrón que slice 3) |
| Estrategia de partición | 3 commits de código + 1 commit docs SDD |
| Delivery strategy | exception-ok (dueño autorizó commits work-unit a main sin PRs encadenados) |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: High

### Mapeo commits (work-unit commits a main)

| Commit | Contenido | Líneas est. | Compila solo |
|--------|-----------|-------------|-------------|
| **Commit A** — dominio + tests | `OnboardingIntention.kt` (new) · `OnboardingState.kt` (campo + resolve + next/previous ramificados) · `OnboardingIntentionRule.kt` (new) · `OnboardingFlowTest.kt` (migración + casos nuevos) · `OnboardingIntentionRuleTest.kt` (new) · pref `onboarding_intention` en `AutonomiaRepository.kt` · `OnboardingViewModel.kt` (combine 3 flows + `selectIntention` + advance/back leen intención) | ~180–200 | Sí (UI sigue en placeholders) |
| **Commit B** — UI Intención | `OnboardingIntentionStep.kt` (new) · `OnboardingScreen.kt` (rama Intention) · `MainActivity.kt` (wiring selectIntention + advance) | ~140 | Sí |
| **Commit C** — UI Sobriedad | `OnboardingSobrietyStep.kt` (new) · `OnboardingScreen.kt` (rama Sobriety + limpieza de `placeholderTitle`) · `MainActivity.kt` (wiring createTrack + skip) | ~120–140 | Sí |
| **Commit D** — docs SDD | `tasks-slice4.md` (este archivo) | — | N/A |

---

## Diseño (decisiones Slice 4)

- **S4-D1** — `intention: OnboardingIntention = STANDARD` como campo de `OnboardingState`; `next/previous` ahora son `fun next(step, intention)` / `fun previous(step, intention)`. Sin overloads de compatibilidad — forzar migración completa.
- **S4-D2** — `resolve(completed, persistedStepName, persistedIntention)`: fallback `STANDARD` ante null/valor inválido (igual que el clamp de paso).
- **S4-D3** — Tabla canónica: `next(Sleep, STANDARD)=Closing`; `next(Sleep, PROTECTION)=Sobriety`; `next(Sobriety, PROTECTION)=Closing`; `previous(Closing, STANDARD)=Sleep`; `previous(Closing, PROTECTION)=Sobriety`. Implementación con `when (step, intention)` explícito para ramas ramificadas.
- **S4-D4** — Pref `onboarding_intention` (String?, patrón canónico de prefs del repo) en `AutonomiaRepository`.
- **S4-D5** — `OnboardingViewModel.combine` agrega tercer flow (`onboardingIntentionFlow`). `advance`/`back` leen `state.intention`. Nuevo `selectIntention(OnboardingIntention)`.
- **S4-D6** — `OnboardingIntentionStep` (Bloque 0.5): dos tarjetas planas, encabezado "¿Qué te trae aquí?", avance bloqueado sin selección, regla pura en `OnboardingIntentionRule`.
- **S4-D7** — `OnboardingSobrietyStep` (Bloque 3): "Sí, agregar" revela formulario mínimo; validación vía `AbstinencePolicy.createCustomDraft`; "Ahora no" avanza sin crear; reusa `createCustomAbstinenceTrack` existente.
- **S4-D8** — Track huérfano se mantiene si el usuario cambia de ruta; no hay borrado.

---

## Phase 1: Dominio puro — cambio de contrato + tests (TDD, RED → GREEN) · Commit A

### [x] 1.1 — `OnboardingIntention.kt` (esqueleto — compila)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/domain/onboarding/OnboardingIntention.kt` (New)  
**Dependencias:** ninguna  
**Acción:** `enum class OnboardingIntention { STANDARD, PROTECTION }` — dominio puro, sin Android.

---

### [x] 1.2 — `OnboardingIntentionRule.kt` (esqueleto en RED)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/domain/onboarding/OnboardingIntentionRule.kt` (New)  
**Dependencias:** 1.1  
**Acción:** `object OnboardingIntentionRule { fun canAdvance(selection: OnboardingIntention?): Boolean = TODO() }` — stub que compila, falla en test.

---

### [x] 1.3 — `OnboardingIntentionRuleTest.kt` (RED)

**Archivo:** `app/src/test/java/dev/panopt/autonomia/domain/onboarding/OnboardingIntentionRuleTest.kt` (New)  
**Dependencias:** 1.2  
**Test runner:** `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.onboarding.OnboardingIntentionRuleTest'`

| # | Nombre del test | Verificación |
|---|-----------------|--------------|
| a | `canAdvance_null_returnsFalse` | `canAdvance(null) == false` |
| b | `canAdvance_STANDARD_returnsTrue` | `canAdvance(STANDARD) == true` |
| c | `canAdvance_PROTECTION_returnsTrue` | `canAdvance(PROTECTION) == true` |

---

### [x] 1.4 — Cambiar firma de `OnboardingState` + `OnboardingFlow` (contrato nuevo)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/domain/onboarding/OnboardingState.kt` (Modified)  
**Dependencias:** 1.1  
**Acción:**
1. Agregar campo `val intention: OnboardingIntention = OnboardingIntention.STANDARD` a `OnboardingState`.
2. Cambiar `resolve` a `fun resolve(completed, persistedStepName, persistedIntention: String?)`: convierte la intención con `runCatching { OnboardingIntention.valueOf(it) }.getOrNull() ?: STANDARD`.
3. Cambiar firma: `fun next(step: OnboardingStep, intention: OnboardingIntention): OnboardingStep` — implementar con `when` explícito (Sleep y Closing ramificados según tabla S4-D3; resto lineal por entries).
4. Cambiar firma: `fun previous(step: OnboardingStep, intention: OnboardingIntention): OnboardingStep` — mismo approach.

> **IMPORTANTE:** este paso ROMPE la compilación de `OnboardingFlowTest` — la migración del paso siguiente debe ir en el mismo commit.

---

### [x] 1.5 — Migrar `OnboardingFlowTest.kt` + agregar casos de ramificación (RED → GREEN juntos)

**Archivo:** `app/src/test/java/dev/panopt/autonomia/domain/onboarding/OnboardingFlowTest.kt` (Modified)  
**Dependencias:** 1.4  
**Test runner:** `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.onboarding.OnboardingFlowTest'`

Migraciones obligatorias de tests existentes (todos agregan parámetro `intention`):

| Test slice 1 | Acción |
|--------------|--------|
| `next avanza al bloque siguiente` | `next(Welcome, STANDARD)` ⇒ `Intention` |
| `next en el ultimo bloque se mantiene` | `next(Closing, STANDARD)` ⇒ `Closing` |
| `next recorre la secuencia completa hasta Closing` | Reescribir: secuencia STANDARD = Welcome→Intention→Anchors→Sleep→**Closing** (5 pasos, sin Sobriety). Agregar caso gemelo PROTECTION: Welcome→…→Sleep→**Sobriety**→Closing (6 pasos). Eliminar assert sobre `entries.toList()`. |
| `previous retrocede al bloque anterior` | `previous(Sleep, STANDARD)` ⇒ `Anchors` |
| `previous en el primer bloque se mantiene` | `previous(Welcome, STANDARD)` ⇒ `Welcome` |
| `resolve sin paso persistido …` | Agregar `persistedIntention = null` → assertion `intention == STANDARD` |
| `resolve con paso valido …` | Agregar `persistedIntention = null` → assertion sin cambio de valor |
| `resolve con paso invalido …` | Agregar `persistedIntention = null` |
| `resolve preserva completed true …` | Agregar `persistedIntention = null` |
| `shouldStartOnboarding …` (2 tests) | Sin cambio de lógica; si construyen `OnboardingState`, el campo tiene default → siguen compilando |

Casos NUEVOS de ramificación (escritos en RED antes de 1.4, pasan a GREEN tras 1.4):

| # | Nombre del test | Verificación |
|---|-----------------|--------------|
| a | `next_Sleep_STANDARD_returnsClosing` | `next(Sleep, STANDARD) == Closing` |
| b | `next_Sleep_PROTECTION_returnsSobriety` | `next(Sleep, PROTECTION) == Sobriety` |
| c | `next_Sobriety_PROTECTION_returnsClosing` | `next(Sobriety, PROTECTION) == Closing` |
| d | `next_Sobriety_STANDARD_returnsClosing_defensivo` | `next(Sobriety, STANDARD) == Closing` (borde) |
| e | `previous_Closing_STANDARD_returnsSleep` | `previous(Closing, STANDARD) == Sleep` |
| f | `previous_Closing_PROTECTION_returnsSobriety` | `previous(Closing, PROTECTION) == Sobriety` |
| g | `previous_Sobriety_PROTECTION_returnsSleep` | `previous(Sobriety, PROTECTION) == Sleep` |
| h | `resolve_intentionPROTECTION_hidrata` | `resolve(false, "Sleep", "PROTECTION").intention == PROTECTION` |
| i | `resolve_intentionNull_defaultsSTANDARD` | `resolve(false, null, null).intention == STANDARD` |
| j | `resolve_intentionInvalida_defaultsSTANDARD` | `resolve(false, null, "basura").intention == STANDARD` |

---

### [x] 1.6 — Implementar `OnboardingIntentionRule.canAdvance` (GREEN)

**Archivo:** `domain/onboarding/OnboardingIntentionRule.kt` (Modified)  
**Dependencias:** 1.5  
**Acción:** `fun canAdvance(selection: OnboardingIntention?): Boolean = selection != null`  
**Verificar GREEN:** `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.onboarding.OnboardingIntentionRuleTest'`

---

### [x] 1.7 — Verificar `AbstinencePolicy.createCustomDraft` (cobertura existente o nueva)

**Archivos:** `domain/abstinence/AbstinencePolicy.kt` + tests existentes  
**Dependencias:** ninguna (paralelo con 1.1–1.6 si se prefiere)  
**Acción:** verificar si existe test para `createCustomDraft("")` → `null`, `createCustomDraft("  ")` → `null`, `createCustomDraft("Alcohol")` → draft con `severity=Moderate, contributionRole=Protective, importanceTier=High`. Si no existen, agregar en `AbstinencePolicyTest.kt` (o clase equivalente).  
**Test runner:** `gradlew testDebugUnitTest --tests 'dev.panopt.autonomia.domain.abstinence.*'`

---

## Phase 2: Persistencia + ViewModel · Commit A (cont.)

### [x] 2.1 — Pref `onboarding_intention` en `AutonomiaRepository.kt`

**Archivo:** `app/src/main/java/dev/panopt/autonomia/AutonomiaRepository.kt` (Modified)  
**Dependencias:** 1.1  
**Acción:** agregar con el patrón canónico:

```kotlin
private val _onboardingIntention = MutableStateFlow(prefs.getString("onboarding_intention", null))
fun onboardingIntentionFlow(): StateFlow<String?> = _onboardingIntention.asStateFlow()
suspend fun setOnboardingIntention(value: String) {
    prefs.edit { putString("onboarding_intention", value) }
    _onboardingIntention.value = value
}
```

No se agrega Room ni migración. `createCustomAbstinenceTrack` ya existe — sin cambio.

---

### [x] 2.2 — `OnboardingViewModel.kt`: combine 3 flows + `selectIntention` + firmas ramificadas

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/onboarding/OnboardingViewModel.kt` (Modified)  
**Dependencias:** 1.4, 2.1  
**Acción:**
1. Agregar `onboardingIntentionFlow()` al `combine` (tercer argumento). Actualizar `resolve` call con tercer parámetro.
2. Agregar `fun selectIntention(intention: OnboardingIntention)` que llama `setOnboardingIntention(intention.name)`.
3. Actualizar `advance()`: `OnboardingFlow.next(s.currentStep, s.intention)`.
4. Actualizar `back()`: `OnboardingFlow.previous(s.currentStep, s.intention)`.
5. `initialValue` del `stateIn` debe leer `.value` de los tres flows síncronamente → sin flicker.

---

### [x] 2.3 — Build verde del Commit A

**Dependencias:** 1.6, 2.2  
**Comando:**

```powershell
powershell.exe -Command "\$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat assembleDebug --no-daemon"
```

**Falla si:** errores de tipo en calls a `next/previous` sin intención en código de producción (MainActivity, OnboardingScreen, etc.). Corregir firmas de todos los call-sites antes de marcar como verde.

---

### [x] 2.4 — Suite completa de tests del Commit A en verde

**Dependencias:** 2.3  
**Comando:** `gradlew testDebugUnitTest --no-daemon`  
**Alcance mínimo:** `OnboardingFlowTest` (todos migrados + 10 nuevos), `OnboardingIntentionRuleTest` (3 casos), `AbstinencePolicy` (3 casos si se agregaron). Suite completa sin regresiones.

---

## Phase 3: UI Intención — `OnboardingIntentionStep` · Commit B

### [x] 3.1 — Crear `OnboardingIntentionStep.kt`

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/onboarding/OnboardingIntentionStep.kt` (New)  
**Dependencias:** 1.1, 1.6  
**Req cubiertos:** Presentación de las dos rutas (todos los escenarios), Tono y nombres canónicos.

Firma:
```kotlin
@Composable
fun OnboardingIntentionStep(
    currentIntention: OnboardingIntention?,   // sembrado de state.intention (null si nunca eligió)
    onSelectAndContinue: (OnboardingIntention) -> Unit,
    onBack: () -> Unit,
)
```

Partes obligatorias:
1. **Estado local:** `var selected by remember(currentIntention) { mutableStateOf(currentIntention) }` — sembrado si ya hay elección persistida.
2. **Encabezado** (serif): `"¿Qué te trae aquí?"` + aviso sans: `"No hay respuesta correcta. Podrás cambiarla cuando quieras."` (copy canónico v3).
3. **Dos tarjetas planas seleccionables**: "Quiero ordenar mi día a día" (`→ STANDARD`) y "Quiero cuidarme de algo que me cuesta" (`→ PROTECTION`). La seleccionada con resalte coral; sin bordes duros ni sombras pesadas.
4. **`canAdvance`:** `OnboardingIntentionRule.canAdvance(selected)` (dominio puro, no inline).
5. **Botón "Continuar":** `enabled = canAdvance`. Al tocar: `onSelectAndContinue(selected!!)`.
6. **Botón "Volver":** siempre habilitado, llama `onBack()`.
7. **Sin mensaje culpabilizador** cuando `!canAdvance` (escenario "Sin selección, avance bloqueado" — el botón simplemente queda deshabilitado).
8. **Copy sin lenguaje clínico:** verificar que el texto visible no contenga "adicción", "recuperación", "error", "falló".

---

### [x] 3.2 — `OnboardingScreen.kt`: agregar parámetros + rama Intention

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/onboarding/OnboardingScreen.kt` (Modified)  
**Dependencias:** 3.1  
**Acción:**
1. Agregar parámetros: `intention: OnboardingIntention?`, `onSelectIntention: (OnboardingIntention) -> Unit = {}`.
2. Agregar rama antes del `else`:
   ```kotlin
   OnboardingStep.Intention -> OnboardingIntentionStep(
       currentIntention = intention,
       onSelectAndContinue = onSelectIntention,
       onBack = onBack,
   )
   ```
3. El `else` sigue cubriendo `Sobriety` (placeholder por ahora — se reemplaza en Commit C).

---

### [x] 3.3 — `MainActivity.kt`: cablear `selectIntention` + `advance` (Bloque 0.5)

**Archivo:** `app/src/main/java/dev/panopt/autonomia/MainActivity.kt` (Modified)  
**Dependencias:** 2.2, 3.2  
**Req cubiertos:** Persistencia de la intención en prefs, escenario "Intención cambiada al volver atrás".

```kotlin
onSelectIntention = { chosen ->
    onboardingViewModel.selectIntention(chosen)  // persiste PRIMERO
    onboardingViewModel.advance()                // avanza DESPUÉS
},
```

> Orden importa: la intención debe estar persistida antes de que el usuario llegue a Sleep y `advance()` evalúe `next(Sleep, intention)`.

---

### [x] 3.4 — Build + tests del Commit B en verde

**Dependencias:** 3.3  
**Comandos:**
1. `assembleDebug` sin errores.
2. `testDebugUnitTest --no-daemon` — sin regresiones.

---

## Phase 4: UI Sobriedad — `OnboardingSobrietyStep` · Commit C

### [x] 4.1 — Crear `OnboardingSobrietyStep.kt`

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/onboarding/OnboardingSobrietyStep.kt` (New)  
**Dependencias:** 1.4 (compilación), 2.1 (createCustomAbstinenceTrack existente)  
**Req cubiertos:** Oferta opcional de track, Exclusividad de ruta (garantizada por S4-D3 en dominio), Tono sin culpa.

Firma:
```kotlin
@Composable
fun OnboardingSobrietyStep(
    onCreateTrackAndContinue: (name: String) -> Unit,
    onSkipSobriety: () -> Unit,
    onBack: () -> Unit,
)
```

Partes obligatorias:
1. **Estado local:** `var showForm by remember { mutableStateOf(false) }` + `var trackName by remember { mutableStateOf("") }`.
2. **Encabezado** (serif): `"Cuidar algo que te cuesta"`.
3. **Cuerpo literario** (copy v3 §4 Bloque 3, español neutro): texto canónico + mensaje **literal**: `"Una recaída no es un fracaso. Es una señal, no una condena."` (la spec lo exige textual).
4. **Pregunta**: `"¿Quieres llevar el registro de algo que estás cuidando?"` con dos acciones:
   - `"Sí, agregar"` → `showForm = true`.
   - `"Ahora no"` → `onSkipSobriety()`.
5. **Formulario mínimo** (visible cuando `showForm == true`): un `OutlinedTextField` para `trackName` + botón confirmar. Al confirmar: si `trackName.isBlank()` (equivale a `createCustomDraft` retornando `null`) → NO avanza, mensaje neutral opcional (sin "falló"). Si no blank → `onCreateTrackAndContinue(trackName)`.
6. **Nombre canónico "Sobriedad"** (AGENTS.md) cuando se referencie la feature visualmente. No usar "abstinencia" ni nombre técnico.
7. **Sin mensaje culpabilizador** — copy neutral conforme a `docs/producto/tono-comunicacion.md`.

---

### [x] 4.2 — `OnboardingScreen.kt`: agregar parámetros + rama Sobriety + limpieza

**Archivo:** `app/src/main/java/dev/panopt/autonomia/ui/onboarding/OnboardingScreen.kt` (Modified)  
**Dependencias:** 4.1  
**Acción:**
1. Agregar parámetros: `onCreateSobrietyTrack: (name: String) -> Unit = {}`, `onSkipSobriety: () -> Unit = {}`.
2. Agregar rama antes del `else`:
   ```kotlin
   OnboardingStep.Sobriety -> OnboardingSobrietyStep(
       onCreateTrackAndContinue = onCreateSobrietyTrack,
       onSkipSobriety = onSkipSobriety,
       onBack = onBack,
   )
   ```
3. Tras esta rama, el `else` queda sin pasos reales (Welcome/Closing/Anchors/Sleep/Intention/Sobriety ya tienen rama propia). Dejar el `else` como salvaguarda defensiva vacía o con un `error("Paso sin composable: $currentStep")` para debugging.
4. **Eliminar `placeholderTitle`** (ya sin consumidores tras este commit). Si está referenciada en algún lugar, eliminar esa referencia también.

---

### [x] 4.3 — `MainActivity.kt`: cablear track de Sobriedad + skip

**Archivo:** `app/src/main/java/dev/panopt/autonomia/MainActivity.kt` (Modified)  
**Dependencias:** 2.1, 4.2  
**Req cubiertos:** "Sí, agregar" crea track y avanza; "Ahora no" avanza sin crear.

```kotlin
onCreateSobrietyTrack = { name ->
    scope.launch {
        repository.createCustomAbstinenceTrack(name)  // reusa writer existente
        onboardingViewModel.advance()
    }
},
onSkipSobriety = {
    onboardingViewModel.advance()
},
```

---

## Phase 5: Verificación estática y de build · Commit C (cont.)

### [x] 5.1 — `assembleDebug` verde

**Dependencias:** 4.3  
**Comando:**
```powershell
powershell.exe -Command "\$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat assembleDebug --no-daemon"
```

---

### [x] 5.2 — `lintDebug` sin errores nuevos

**Dependencias:** 5.1  
**Comando:** reemplazar `assembleDebug` por `lintDebug`.

---

### [x] 5.3 — `testDebugUnitTest` suite completa en verde

**Dependencias:** 5.1  
**Comando:** `gradlew testDebugUnitTest --no-daemon`  
**Alcance mínimo:** `OnboardingFlowTest` (migrados + nuevos), `OnboardingIntentionRuleTest` (3), `AbstinencePolicy` (3 si se agregaron). Sin regresiones en `OnboardingSleepRuleTest` ni `OnboardingAnchorsRuleTest`.

---

## Phase 6: Verificación runtime (emulador)

### [ ] 6.1 — Checklist runtime (verificacion-por-capas.md, capas 1–4)

**Dependencias:** 5.3  
**Prerequisito:** install limpio (`adb uninstall dev.panopt.autonomia`; `adb install app-debug.apk`).

| # | Verificación | Pasa si |
|---|--------------|---------|
| a | Bloque Intención reemplaza el placeholder (ya no cae en `else`) | El composable real aparece |
| b | Sin selección → "Continuar" deshabilitado, sin mensaje culpabilizador | Gate correcto |
| c | Tocar "Quiero ordenar mi día a día" → feedback visual (resalte coral), "Continuar" habilitado | UX correcta |
| d | Avanzar con STANDARD → Sleep aparece y luego salta a Closing (Sobriety no aparece) | Ruta STANDARD: 5 pasos |
| e | Volver al Bloque Intención, cambiar a PROTECTION → avanzar: Sobriety aparece entre Sleep y Closing | Ruta PROTECTION: 6 pasos |
| f | "Ahora no" en Sobriedad → avanza a Closing sin crear track | No crear fila |
| g | "Sí, agregar" + nombre válido "Tabaco" → track persiste; al completar onboarding, track visible en Dashboard | Persistencia correcta |
| h | "Sí, agregar" + nombre en blanco → formulario no avanza, mensaje neutral | Validación sin culpa |
| i | Cerrar app con PROTECTION persistida → reabrir → ruta PROTECTION se mantiene (Sobriety sigue en la secuencia) | Reanudación correcta |
| j | Logcat sin crashes durante todo el flujo | Estabilidad |

---

## Dependencias entre tareas (grafo)

```
1.1 → 1.2 → 1.3
1.1 → 1.4 → 1.5 → 1.6  (migración test + GREEN)
                         \
1.7 (paralelo)            +→ 2.2 → [Commit A verde]
                         /            |
2.1 ──────────────────────            ↓
                              3.1 → 3.2 → 3.3 → [Commit B verde]
                                                      |
                                                      ↓
                                         4.1 → 4.2 → 4.3 → 5.1 → 5.2 → 5.3 → 6.1
```

| Bloque | Puede correr en paralelo con |
|--------|------------------------------|
| Phase 1 TDD dominio (1.1–1.6) | Phase 1.7 (AbstinencePolicy) — sin dependencia cruzada |
| Phase 2 prefs+ViewModel (2.1, 2.2) | Requiere 1.1 (para 2.1) y 1.4 (para 2.2) |
| Phase 3 UI Intención | Requiere Commit A verificado (2.4) |
| Phase 4 UI Sobriedad | Requiere Commit B verificado (3.4) |
| Phase 5 static | Requiere 4.3; 5.1/5.2/5.3 corren en cualquier orden |
| Phase 6 runtime | Requiere Phase 5 completa |

---

## Trazabilidad spec → tareas

| Requirement (spec) | Tareas |
|--------------------|--------|
| (intention) Presentación de las dos rutas | 3.1, 3.2, 3.3 |
| (intention) Persistencia de la intención en prefs | 2.1, 2.2, 3.3 |
| (intention) Tono y nombres canónicos | 3.1 (copy, sin clínico) |
| (intention/sobriety) Sin modelo de datos nuevo | 2.1 (prefs), 4.1 (writer existente), S4.5 |
| (sobriety) Oferta opcional de track | 4.1, 4.3 |
| (sobriety) Exclusividad de ruta — Sobriety solo en PROTECTION | 1.4 (next/previous ramificados), 1.5 (tests) |
| (sobriety) Tono sin culpa (copy canónico literal) | 4.1 |
| (gate delta) Block Navigation Skeleton ramificado | 1.4, 1.5, 2.2 |
| (gate delta) Intention-Aware State (default STANDARD) | 1.4 (campo + fallback), 1.5 (resolve tests) |
| Migración `OnboardingFlowTest` (contrato roto sin ella) | 1.5 |

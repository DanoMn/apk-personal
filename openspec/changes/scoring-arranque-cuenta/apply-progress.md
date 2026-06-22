# Apply Progress — `scoring-arranque-cuenta`

> Estado de implementación por lote. Actualizado al cerrar cada lote.

## Lote 1 — Núcleo: `rFromRatios` con `windowDays` — ✅ COMPLETO

Strict TDD (RED → GREEN → REFACTOR). Entregable aislado y aditivo puro:
el motor maduro queda byte-idéntico (nadie pasa `windowDays ≠ 7` todavía).

| Task | Estado | Nota |
|------|--------|------|
| 1.1 (TEST-RED) regresión default ≡ 7 | [x] | RED por firma de 3 args inexistente |
| 1.2 (GREEN) `windowDays=7` + `f_eff` en `sd`/`wt` | [x] | `n=coerceIn(1,7)`, `fEff=min(f,n)` |
| 1.3 suite `AnchorScoringPolicyTest` verde sin tocar | [x] | cero regresión |
| 1.4 ventana parcial reparte sobre N | [x] | f=3/N=4→1.0; f=2/N=4 superhábit>base |
| 1.5 guard f≥N (sin div/0, wt≤1, sin NaN) | [x] | f=5/N=2, f=7/N=1, vacío, transversal 2..7×1..6 |
| 1.6 clamp `windowDays` a [1,7] | [x] | 9≡7, 0≡1, -5≡1 |
| 1.7 doc vivo modelo-matemático §1.3.1 | [x] | parámetro `windowDays` documentado |

### Cambios de producción
- `app/src/main/java/dev/panopt/autonomia/domain/scoring/AnchorScoringPolicy.kt`
  - Firma: `fun rFromRatios(f: Int, dayRatios: List<Double>, windowDays: Int = 7): Double`.
  - `val n = windowDays.coerceIn(1, 7)`; `val fEff = min(f, n)`.
  - `sd = if (fEff < n) v / (n - fEff).toDouble() else 0.0` (reemplaza `7` literal).
  - `wt = (fEff.toDouble() / n.toDouble()).pow(kappa)` (reemplaza `7` literal).
  - `phi`, `cut`, `st` SIN tocar (mantienen `f` crudo). Llamador `r(...)` y call sites intactos.
  - ~9 líneas netas de producción + KDoc.

### Tests nuevos
- `app/src/test/java/dev/panopt/autonomia/domain/scoring/AnchorScoringWindowDaysTest.kt`
  (10 tests; el nombre de archivo sigue task 1.1, no `AnchorScoringPolicyTest`).

### Doc vivo
- `docs/scoring/modelo-matematico-nucleo-v1.md` — nueva §1.3.1 "Ventana de N días (`windowDays`)".

### Verificación (output real)
- `testDebugUnitTest --tests '...AnchorScoringWindowDaysTest'` → **BUILD SUCCESSFUL** (test nuevo verde).
- `testDebugUnitTest --tests '...domain.scoring.*'` → **BUILD SUCCESSFUL** (suite scoring completa, cero regresión).
- `assembleDebug` → **BUILD SUCCESSFUL** (compila).

### Commit
- `feat(scoring): generalize rFromRatios to N-day window (windowDays)` en branch `feat/scoring-motor-nucleo-v1`.

## Lote 2 — Dominio de arranque — ✅ COMPLETO

Strict TDD (RED → GREEN). Seams aditivos + 3 objects nuevos de dominio puro. Entregable
aislado: el dominio de arranque existe y está testeado, pero NADIE lo consume todavía
(sin wiring de UI). El dashboard se comporta como hoy.

| Task | Estado | Nota |
|------|--------|------|
| 2.1 (GREEN-aditivo) `BuildScoreInputUseCase.includeGraceAnchors=false` | [x] | guard `!includeGraceAnchors &&` aditivo; default neutro |
| 2.2 (GREEN-refactor neutro) seam `ScoreEngine.calculateProjection` | [x] | extraído `calculateInternal(input, windowDays)`; `calculate`→`GRACE_DAYS.toInt()` |
| 2.3 (TEST-RED→GREEN) `StartupDetectionRule.isStartup` | [x] | 6 tests: vacío→arranque, 1 real→no, <3 capas→no (gate), all-NoData→arranque, state real→no, archived/inactive no cuentan |
| 2.4 (TEST-RED→GREEN) `StartupProjectionUseCase` | [x] | gracia aporta a la proyección; real sigue NoData; no muta maduro; <gate→null |
| 2.5 (TEST-RED→GREEN) `StartupCounterPolicy.counter` | [x] | d=1→×1/7, d=4→×4/7, d=7→×7/7, clamp [1,7], estado=0→points(0) card activa |
| 2.6 (TEST obligatorio) no-salto día 7→8 | [x] | `counter(día7,×7/7).counterPoints == matureReport(día8).visibleScore` (assertEquals exacto, mismos hechos) |
| 2.7 (TEST) invariante persistencia | [x] | en arranque `ScoreReport` real = NoData, visibleScore null (writer persiste 0 como hoy) |

### Cambios de producción
- `domain/scoring/BuildScoreInputUseCase.kt`
  - Firma: `operator fun invoke(source, includeGraceAnchors: Boolean = false)`.
  - Guard del `filterNot`: `!includeGraceAnchors && ...` como primer término. Default `false` = camino maduro byte-idéntico.
- `domain/scoring/ScoreEngine.kt`
  - Extraído `private fun calculateInternal(input, windowDays: Int): ScoreReport` con el pipeline previo.
  - `fun calculate(input) = calculateInternal(input, AnchorGraceRule.GRACE_DAYS.toInt())` (firma pública intacta, byte-idéntica).
  - `internal fun calculateProjection(input, windowDays) = calculateInternal(input, windowDays)`.
  - Única propagación: `rFromRatios(window.f, ratios)` → `rFromRatios(window.f, ratios, windowDays)`.
  - Import nuevo: `AnchorGraceRule` (fuente única del 7, sin constante nueva).
- `domain/scoring/StartupDetectionRule.kt` (NUEVO, object puro) — `isStartup(report, activities, layers, weeklyHistory, today, minLayersGate=MIN_ACTIVE_LAYERS_WITH_ANCHOR)`. Cuenta capas activas con ancla SIN filtrar gracia; gate manda.
- `domain/scoring/StartupProjectionUseCase.kt` (NUEVO, object) + `data class StartupProjection(estado, windowDays)`. Corre `BuildScoreInputUseCase(source, includeGraceAnchors=true)` + `calculateProjection(input, windowDays)`; NoData→null.
- `domain/scoring/StartupCounterPolicy.kt` (NUEVO, object puro) + `data class StartupCounter(counterPoints, daysLived, daysRemaining, windowProgress)`. `counter = points(estado × d/7)`; `d` clampado [1,7]; divisor `AnchorGraceRule.GRACE_DAYS`.

### Tests nuevos
- `StartupDetectionRuleTest.kt` (6 tests)
- `StartupCounterPolicyTest.kt` (7 tests)
- `StartupProjectionUseCaseTest.kt` (5 tests, incluye convergencia 2.6 y persistencia 2.7)

### Verificación (output real)
- `testDebugUnitTest --tests '...domain.scoring.*'` → **BUILD SUCCESSFUL in 25s** (suite scoring completa verde, cero regresión incl. ScoreEngineTest/AnchorScoringPolicyTest).
- `assembleDebug` → **BUILD SUCCESSFUL in 14s** (APK compila).

### Decisiones de implementación (vs design/spec)
- **Contador sobre ESTADO, no sobre puntos** (task 2.5 gana a la literalidad de la spec §"contador = round(projectedScore × d/7)" con Int): `counter = points(estado × d/7)`. Razón: el motor maduro mapea ESTADO→puntos vía `PointsMappingPolicy`; atenuar el ESTADO antes de mapear es lo que hace converger el día 7 (×7/7) con el día 8 maduro EXACTAMENTE (test 2.6 `assertEquals` sin tolerancia). Atenuar los puntos ya mapeados rompería esa igualdad.
- **windowDays se propaga SOLO por el ramo `dayRatios != null`** (con versiones). El ramo legacy `r(f,t,mins)` mantiene default 7 (design §3.3 lo acepta; la atenuación d/7 corrige). Los tests de proyección usan `targetVersions` para forzar `dayRatios != null`.
- **Convergencia 2.6 con tolerancia 0:** mismos 7 días de hechos; día 7 anclas en gracia (proyección no filtra) windowDays=7 ×7/7; día 8 anclas con `createdAt` hace 7 días (fuera de gracia) maduro. `counterPoints == visibleScore` exacto.
- **`GRACE_DAYS.toInt()` como fuente del 7** en `calculate` (no constante nueva): coherente con el default `windowDays=7` de `rFromRatios` y con `AnchorGraceRule` como única autoridad temporal del arranque.

## Lote 3 — Proyección + UI (+ 2 fixes de dominio del Lote 2) — ✅ COMPLETO

Strict TDD (RED → GREEN → REFACTOR). Dos correcciones de dominio (FIX A + FIX B) más el wiring de
proyección y la UI. Build de Compose verde. Entregable: la cuenta nueva ve la barra de arranque
`0 → score real` en vez del blackout "Sin datos".

### FIX A (CRÍTICO) — atenuación sobre PUNTOS, no sobre estado

| Qué | Detalle |
|-----|---------|
| Problema | `StartupCounterPolicy` atenuaba el ESTADO antes de mapear (`points(estado × d/7)`). Con piso 650 de `PointsMappingPolicy`, el contador quedaba SIEMPRE ≥650 — nunca recorría la zona muerta 0–650. |
| Fix | `counter = round(points(estado) × d/7)`: se atenúan los PUNTOS ya mapeados. El contador arranca cerca de 0 y sube hacia el real. Ejemplos del dueño verificados: score 900 → d1=129, d4=514, d7=900. |
| Convergencia | Se mantiene: en d=7 `×7/7=1` → `counter = points(estado)` == puntos maduros día 8 (mismos hechos). Test 2.6 sigue comparando PUNTOS (`matureReport.visibleScore == counter.counterPoints`), igualdad exacta. |
| Tests | `StartupCounterPolicyTest` reescrito a semántica de puntos (`expected(estado,d) = round(points(estado)×d/7)`); nuevos `counterLivesInDeadZoneBelowFloorOnEarlyDays` y `ownerExamplesNineHundredPointScore` (estado por bisección → points==900). |

### FIX B — el ramo legacy respeta `windowDays`

| Qué | Detalle |
|-----|---------|
| Problema | El ramo `r(f, t, mins)` (cuenta nueva SIN target-versions → `dayRatios == null`) ignoraba `windowDays` → proyectaba con ventana 7, castigando días no vividos. |
| Fix | `AnchorScoringPolicy.r` gana `windowDays: Int = 7` y lo propaga a `rFromRatios`; `ScoreEngine.calculateInternal` lo pasa por el ramo `else`. Default 7 = byte-idéntico. |
| Tests | `newAccountWithoutVersionsRespectsWindowDaysAndIsNotPunished` (cuenta nueva sin versiones, día 2 → ventana justa ≥ ventana 7) y `matureWindowDaysSevenIsByteIdenticalForLegacyBranch` (cero regresión). Helper `sourceWithGraceAnchorsNoVersions`. |

### Lote 3 propiamente (UI + proyección)

| Task | Estado | Nota |
|------|--------|------|
| 3.1 `StartupCardState` + `DashboardState.startup` | [x] | data class de presentación (counterLabel/counterPoints/windowProgress/daysRemaining/daysRemainingLabel/headline/body); campo nullable |
| 3.2 `DashboardProjection` computa `startup` | [x] | extraído `scoreInputSource` a val (reuso); `StartupDetectionRule → StartupProjectionUseCase → StartupCounterPolicy → toStartupCardState`; helper `startupDaysLived` (createdAt más viejo, +1, clamp [1,7]) |
| 3.3 `StartupStatusCard` (Compose hermano) | [x] | `animateIntAsState`(número) + `animateFloatAsState`(arco d/7); reusa `ScoreOrbit`; color cálido `mix(colorCoral,0.35f,colorCardboard)`; sin lógica de negocio; StatusCard intacto |
| 3.4 Dashboard elige card | [x] | `DashboardScreen` L114: `if (state.startup != null) StartupStatusCard else StatusCard` |
| 3.5 Docs vivos | [x] | `modelo-matematico-nucleo-v1.md` §7.1 (atenuación sobre puntos) + §1.3.1 (ambos ramos respetan windowDays); `modelo-scoring-oficial-v1.md` §12.1 (barra de arranque); `frontend-design.md` (card de arranque) |

### Cambios de producción
- `domain/scoring/StartupCounterPolicy.kt` — atenúa PUNTOS: `round(points(estado) × d/7)` (FIX A).
- `domain/scoring/AnchorScoringPolicy.kt` — `r(f, t, mins, windowDays=7)` propaga a `rFromRatios` (FIX B).
- `domain/scoring/ScoreEngine.kt` — ramo legacy `else` propaga `windowDays` (FIX B).
- `domain/dashboard/DashboardState.kt` — `StartupCardState` + `val startup: StartupCardState? = null`.
- `domain/dashboard/DashboardProjection.kt` — `scoreInputSource` extraído a val; cómputo de `startup`;
  helpers `startupDaysLived` y `toStartupCardState` (copy compasivo).
- `ui/dashboard/components/StartupStatusCard.kt` (NUEVO) — card hermano animado.
- `ui/dashboard/DashboardScreen.kt` — branch `if (state.startup != null)`.

### Tests nuevos/modificados
- `StartupCounterPolicyTest.kt` (reescrito a semántica de puntos, 9 tests).
- `StartupProjectionUseCaseTest.kt` (+2 tests FIX B).
- `DashboardProjectionStartupTest.kt` (NUEVO, 3 tests: arranque→startup!=null & NoData; madura→null; <3 capas→null+NoData).

### Verificación (output real)
- `testDebugUnitTest --tests '...domain.*'` → **BUILD SUCCESSFUL** (367 tests, cero regresión incl. ScoreEngineTest/AnchorScoringPolicyTest/DashboardProjectionTest).
- `assembleDebug` → **BUILD SUCCESSFUL in 27s** (build de Compose verde, sin warnings).

### Decisiones de implementación
- **FIX A gana a la decisión previa del Lote 2** (que atenuaba el estado). El learning anterior
  ("contador sobre ESTADO") queda SUPERSEDIDO: la intención del dueño es que el contador viva en la
  zona muerta 0–650. La convergencia se preserva igual porque en d=7 `×7/7=1` no atenúa.
- **`startupDaysLived` con `+1`**: el día de creación es el día 1. Anclas creadas hace N días →
  daysLived = N+1. Los tests de proyección de dashboard alinean su fixture a esa convención.
- **Compose sin test JVM**: el `StartupStatusCard` se valida en build + capa runtime
  (`verificacion-por-capas.md`); la lógica (número, días, copy) ya está testeada en dominio.

### Commits (branch `feat/scoring-motor-nucleo-v1`, sin atribución IA)
- 3a (fixes dominio A+B): `fix(scoring): attenuate startup counter on points and honor windowDays in legacy anchor branch`.
- 3b (UI + proyección + docs): `feat(scoring): startup account card wiring and UI (DashboardState.startup, StartupStatusCard)`.

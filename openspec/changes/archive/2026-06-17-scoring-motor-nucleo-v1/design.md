# Design: scoring-motor-nucleo-v1 — portar el modelo de scoring cerrado

## Technical Approach

Traducción casi literal del blueprint Python (`docs/scoring/modelo-matematico-nucleo-v1.md`
§ Implementación de referencia) a Kotlin de dominio puro JVM. El pipeline se reescribe
bottom-up por nivel (1→6), con un **adapter** que reconstruye las formas de entrada desde
`daily_activity_logs`, y un **mapeo a puntos** (NIVEL 7) que vive en la proyección, NO en el
motor. Cero gates/caps/worst-term duros: todo emerge de peso × valor. Strict TDD: cada nivel
porta primero sus asserts de `verificacion_modelo_oficial.py`. No se toca Room (Camino A).

## Architecture Decisions

| Tema | Opciones | Decisión | Rationale |
|---|---|---|---|
| Precisión numérica | Float interno · Double interno | **Double** en todo el cálculo; `estado: Float` solo en la frontera de `ScoreReport` | Python usa doubles; asserts comparan a ~1e-9. Float (24 bits ≈ 7 díg.) no alcanza para 27 asserts. Tests JUnit en `Double` con `assertEquals(exp, act, 1e-9)`. |
| Invariante "ancla = Minutes" | enforce en motor · enforce al asignar surface | **Helper de dominio `ActivityPolicy.requireAnchorUnit`** + validación en los 2 puntos de creación de surface Anchor; motor/adapter asumen el invariante defensivamente | Hechos → dominio: la regla es de configuración, no de cálculo. El adapter se simplifica a Minutes-only; el motor degrada a 0 si llega algo ilegal (no crashea). |
| Forma de las entradas del modelo | DTOs ricos · value classes mínimas | **data classes mínimas** (`AnchorWindow`, `LayerFacts`, `OptInSignal`) en `ScoringFactsAdapter` | El motor recibe forma final; mínima superficie = menos acoplamiento con Room. |
| Ubicación del mapeo de puntos | motor · proyección | **Proyección** (`DashboardProjection`) | NIVEL 7 es presentación; el motor emite ESTADO crudo. Coherente con local-first. |
| Campos viejos de `ScoreReport` | borrar · preservar mapeados | **Preservar** `weeklyBaseScore/weeklyScore/state/visibleScore/worstLayerId/stability*` mapeados desde ESTADO | El seam de persistencia (`WeeklyScoreSnapshotDraft`) los consume; romperlos rompe el writer. |
| `SCORING_VERSION` | mantener · bumpear | **Bumpear** (p.ej. `v2`) | El significado de `weeklyBaseScore`/`visibleScore` cambia (rango 650–1100); snapshots viejos no son comparables. Evita mezclar convenciones en historia. |

## Data Flow

    daily_activity_logs (hechos)
      → WeeklyScoringContextBuilder (ventana 7d, dedup activityId:date)   [modificado]
      → ScoringFactsAdapter  →  AnchorWindow(F,T,mins) · LayerFacts(supportDays, nTasksToday)
                                · OptInSignal(M) por sueño/sobriedad        [NUEVO]
      → ScoreEngine (orquesta)                                             [reescrito]
          → AnchorPolicy.R(F,T,mins)            NIVEL 1
          → LayerValuePolicy(base_eff, extra)   NIVEL 2  (soportes blend + tasks joint)
          → LayerWeightPolicy.votes(n)          NIVEL 3
          → OptInPolicy.shadowTerm(M, Σpesos)   NIVEL 4
          → StateAggregationPolicy → ESTADO     NIVEL 5
          → BaseStatePolicy.band(estado)        NIVEL 6
      → ScoreReport(estado, state, weeklyBaseScore≈estado, visibleScore, …)
      → DashboardProjection.PointsMapping(estado) → [650,1100]  NIVEL 7   [proyección]
      → WeeklyScoreSnapshotWriter (seam intacto)

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/scoring/ScoringConstants.kt` | Rewrite | 17 params §0.1. Borrar `WORST_LAYER_*`, `UNBREAKABLE_*`, etc. |
| `domain/scoring/AnchorScoringPolicy.kt` | Rewrite | NIVEL 1 `R(F,T,mins):Double`; gate `base.pow(P)` (P de constantes) |
| `domain/scoring/LayerValuePolicy.kt` | Rewrite | NIVEL 2 dos canales + blend soportes (`WS`) + tasks joint (`TAU`, gate `base_eff^P`) |
| `domain/scoring/LayerWeightPolicy.kt` | New/Rewrite | NIVEL 3 `votes(n)=(1−r^n)/(1−r)`, `RHO`, `W0` |
| `domain/scoring/OptInPolicy.kt` | New | NIVEL 4 `M_sobr=Π(1−A)^d`, `shadow=BETA·Σpesos·(1−M)` |
| `domain/scoring/StateAggregationPolicy.kt` | New | NIVEL 5 bolsa-global → ESTADO |
| `domain/scoring/BaseStatePolicy.kt` | Rewrite | NIVEL 6 `band(estado)` pura, cortes `0.40/0.62/0.85/1.10` |
| `domain/scoring/ScoringFactsAdapter.kt` | New | hechos → `AnchorWindow/LayerFacts/OptInSignal` |
| `domain/scoring/WeeklyScoringContextBuilder.kt` | Modify | exponer mins[7]/supportDays/nTasksToday/relapseDays |
| `domain/scoring/ScoreModels.kt` | Modify | `ScoreReport.estado: Float`; adapter DTOs |
| `domain/scoring/ScoreEngine.kt` | Rewrite | orquesta adapter → niveles 1–6 |
| `domain/scoring/WeeklyScorePolicy.kt`, `VisibleScorePolicy.kt` | Delete | worst-layer y `700+base·300` eliminados |
| `domain/scoring/SpecialLayerScoringPolicy.kt` | Delete | ρ/W0 viven en la agregación |
| `domain/scoring/StabilityScoringPolicy.kt` | Inert | no se invoca en la banda |
| `domain/scoring/SobrietyScoringPolicy.kt` | Rewrite | señal `M_sobr` (días de recaída) |
| `domain/activity/ActivityPolicy.kt` | Modify | `requireAnchorUnit(unit)` invariante |
| `AutonomiaRepository.kt` · `DashboardViewModel.kt` | Modify | validar Minutes al asignar surface Anchor |
| `domain/dashboard/DashboardProjection.kt` | Modify | NIVEL 7 mapeo E (650–1100) |
| `BuildWeeklyScoreSnapshotUseCase.kt` | Verify | compila/persiste con campos mapeados |
| `app/src/test/.../domain/scoring/*Test.kt` | New | 27 asserts + AN12/VC4/SO6/TA6/O6/O9/PU2 |

## Interfaces / Contracts

```kotlin
// Adapter output (mins SIEMPRE minutos: ancla = Minutes-only por invariante)
data class AnchorWindow(val f: Int, val t: Int, val mins: List<Int>)   // días con actividad >0
data class LayerFacts(val anchors: List<AnchorWindow>, val supportDays: List<Int>, val nTasksToday: Int, val optIn: Double?)
// Motor
object AnchorScoringPolicy { fun r(f: Int, t: Int, mins: List<Int>): Double }
object BaseStatePolicy { fun band(estado: Double): ScoreState }
// ScoreReport gana:
val estado: Float            // ESTADO crudo ∈ [0,1.5]; weeklyBaseScore = estado; state = band(estado)
// Invariante
fun ActivityPolicy.requireAnchorUnit(unit: ActivityUnit)   // != Minutes → IllegalArgumentException (rechazo)
```

Mapeo seam: `weeklyBaseScore = estado`; `weeklyScore = estado`; `state = band(estado)`;
`visibleScore = points(estado)` (calculado en proyección, pasado al draft);
`worstLayerId = null` (deuda); `stability* = null` (aparcado).

## Testing Strategy

| Layer | What | Approach |
|-------|------|----------|
| Unit (motor) | NIVELES 1–6: 27 asserts §verificación + §1.4 | JUnit4, `Double`, tol `1e-9` (`±0.001` para §1.4) |
| Unit (adapter) | mins[7], dedup, NotDone/Omitted, supportDays, nTasksToday, relapseDays, M | hechos sintéticos que reproducen §1.4 |
| Unit (puntos) | PU1/PU3/PU4/PU5 (941/1011/650/1100) | JUnit en proyección |
| Unit (invariante) | ancla no-Minutes rechazada | `assertThrows` |

## Migration / Rollout

No migration (Camino A: sin migraciones Room). Bumpear `SCORING_VERSION` para no mezclar
snapshots viejos con la convención nueva (los snapshots son cache derivado, recalculables).

## Implementation Order (insumo para tasks) — PRs encadenados (ask-on-risk, >400 líneas)

1. **PR-A**: `ScoringConstants` (17 params) + NIVEL 1 ancla + tests. (corte limpio)
2. **PR-B**: NIVELES 2–3 (valor de capa, soportes, tasks, pesos) + tests.
3. **PR-C**: NIVELES 4–6 (opt-ins, agregación, bandas) + tests.
4. **PR-D**: invariante Minutes-only (helper + 2 puntos de validación) + tests.
5. **PR-E**: adapter + `WeeklyScoringContextBuilder` + tests de adapter.
6. **PR-F**: recableado `ScoreEngine` + `ScoreReport.estado` + seam verificado + borrado de policies viejas.
7. **PR-G**: NIVEL 7 puntos en proyección + tests.

## Open Questions (resueltas en ejecución)

- [x] **Spec delta (adapter):** parchada a Minutes-only — `mins[día] = actualValue`; `Done`
  sin `actualValue` = 0; Boolean/Count/Time NO aplican a anclas (invariante de dominio).
- [x] `SCORING_VERSION` nuevo: `core-v2` (confirmado, bumpeado en PR-F).

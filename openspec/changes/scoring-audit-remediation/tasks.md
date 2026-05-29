# Tasks: scoring-audit-remediation · slice 1 (base-state-policy)

## Review Workload Forecast

| Campo | Valor |
|-------|-------|
| Líneas cambiadas estimadas | 280–340 (test ~140, prod ~110, docs ~70) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | PR único |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Tests + prod + docs | PR único | Todos dentro del budget de 400 líneas |

---

## Phase 1: Constantes (Foundation)

- [x] 1.1 En `ScoringConstants.kt` agregar las nueve constantes: `STATE_RESTORATION_THRESHOLD = 0.40f`, `STATE_ATTENTION_THRESHOLD = 0.70f`, `STATE_PLENITUDE_THRESHOLD = 0.85f`, `WORST_LAYER_COLLAPSE = 0.30f`, `WORST_LAYER_MIN_FOR_MOTION = 0.55f`, `WORST_LAYER_MIN_FOR_PLENITUDE = 0.75f`, `WORST_LAYER_MIN_FOR_UNBREAKABLE = 0.80f`, `STATE_HYSTERESIS_MARGIN = 0.03f`, `UNBREAKABLE_BASE_MIN = 0.90f`, `UNBREAKABLE_STABILITY_MIN = 0.90f`. Vinculado a spec §Constants Extracted y a la corrección del orchestrador (ladder completo).

---

## Phase 2: Tests (RED — TDD strict, escribir antes de tocar prod)

- [x] 2.1 Crear `app/src/test/java/dev/panopt/autonomia/domain/scoring/BaseStatePolicyTest.kt`. Casos de banda (spec §Band Mapping): `base 0.399 → Restoration`, `base 0.40 → Attention`, `base 0.699 → Attention`, `base 0.70 → Motion`, `base 0.849 → Motion`, `base 0.85 → Plenitude`.
- [x] 2.2 En `BaseStatePolicyTest.kt`, casos de colapso (spec §Worst-Layer Collapse): `worstLayer 0.299 + base 0.95 → Restoration`, `worstLayer 0.30 + base 0.80 → Motion` (no colapsa). Verificar que el collapse ignora histéresis: `previousState=Plenitude, worst 0.25 → Restoration`.
- [x] 2.3 En `BaseStatePolicyTest.kt`, casos de histéresis (spec §State Hysteresis): mantiene (`previousState=Motion, base 0.69 → Motion`), cae (`previousState=Motion, base 0.66 → Attention`), no bloquea ascenso (`previousState=Attention, base 0.72 → Motion`), no suprime dos bandas (`previousState=Plenitude, base 0.66 → Attention`), `previousState=null base 0.69 → Attention`.
- [x] 2.4 En `BaseStatePolicyTest.kt`, casos del worst-layer ladder — corrección del orchestrador: `base 0.90, worst 0.50 → Attention` (capped por `WORST_LAYER_MIN_FOR_MOTION = 0.55`); `base 0.90, worst 0.74 → Motion` (capped por `WORST_LAYER_MIN_FOR_PLENITUDE = 0.75`); `base 0.92, worst 0.79 → Plenitude` (capped por `WORST_LAYER_MIN_FOR_UNBREAKABLE = 0.80`).
- [x] 2.5 En `BaseStatePolicyTest.kt`, casos Inquebrantable (spec §Inquebrantable Gate): `hasTemporalMemory=true + base 0.92 + worst 0.81 + stability 0.91 → Unbreakable`; `hasTemporalMemory=false + base 0.95 + worst 0.85 + stability 0.92 → Plenitude`; `hasTemporalMemory=true + worst 0.79 → Plenitude`.
- [x] 2.6 En `ScoreEngineTest.kt` agregar 1 test de regresión: `previousState` se deriva del `weeklyHistory` más reciente con `scoringVersion == SCORING_VERSION && weekStart != currentWeekStart`; verificar que el estado de semana previa llega al damping. Vinculado a design §Call-site ScoreEngine.

---

## Phase 3: Implementación (GREEN)

- [x] 3.1 Reescribir `BaseStatePolicy.stateFor(...)` en `BaseStatePolicy.kt`. Nueva firma: `fun stateFor(weeklyBaseScore: Float, worstLayerScore: Float, stability: StabilityEvaluation, previousState: ScoreState?): ScoreState`. Eliminar parámetro `visibleScore`. Implementar en este orden de precedencia: (1) `worstLayerScore < WORST_LAYER_COLLAPSE → Restoration` (override duro); (2) banda cruda sobre `weeklyBaseScore` con hysteresis; (3) caps del ladder: `worst < WORST_LAYER_MIN_FOR_MOTION → cap Attention`; `worst < WORST_LAYER_MIN_FOR_PLENITUDE → cap Motion`; (4) Inquebrantable gating. Sin literales numéricos — solo referencias a `ScoringConstants`. Vinculado a spec §Band Mapping, §Worst-Layer Collapse, §Inquebrantable Gate, §State Hysteresis, §Constants Extracted y a la corrección del orchestrador.
- [x] 3.2 En `ScoreEngine.kt`, antes del `ScoreReport`, derivar `previousState`: `val previousState = input.weeklyHistory.filter { it.scoringVersion == WeeklyScoreSnapshotConstants.SCORING_VERSION && it.weekStart != context.weekStart.toString() }.maxByOrNull { it.weekStart }?.state`. Ajustar el call-site de `BaseStatePolicy.stateFor(...)`: quitar `visibleScore =`, agregar `previousState = previousState`. Vinculado a design §Call-site ScoreEngine.
- [x] 3.3 En `VisibleScorePolicy.kt`, borrar el método `stateFor(visibleScore: Int): ScoreState` completo (dead code, cero referencias confirmadas en main y test). Conservar `visibleScore(internalScore: Float): Int`. Vinculado a spec §Single Source of Truth.

---

## Phase 4: Verificación y limpieza

- [x] 4.1 Ejecutar la suite completa con el test runner: `powershell.exe -Command "$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; Set-Location D:\APK-Personal; .\gradlew.bat test --tests 'dev.panopt.autonomia.domain.scoring.BaseStatePolicyTest' --no-daemon"`. Verificar que todos los tests (fases 2 y 3) pasan en verde. Corregir cualquier fallo antes de continuar.
- [x] 4.2 Confirmar que no quedan referencias a `VisibleScorePolicy.stateFor` y que no quedan literales numéricos de umbral en `BaseStatePolicy.kt` (verificar con búsqueda de texto).

---

## Phase 5: Documentación (D2)

- [x] 5.1 En `docs/arbol-scoring-vocal-v1.md` §16: actualizar la tabla de umbrales a valores finales (Restauración <0.40, Atención <0.70, En marcha <0.85, Plenitud ≥0.85); reflejar el ladder completo de peor capa (0.30 colapso, 0.55 mínimo para En marcha, 0.75 mínimo para Plenitud, 0.80 mínimo para Inquebrantable); cambiar cualquier mención de "propuesta" a "valor sellado". Vinculado a spec §D2 Asymmetry Documentation.
- [x] 5.2 En `docs/plan-tecnico-scoring-vocal.md` §7.1: agregar nota explícita que ratifica la asimetría `rawScore`/`baseScore` (superávit no compensa capas caídas) como decisión sellada; ratificar los umbrales del ladder; eliminar menciones de `visibleScore` como base de decisión de estado. Vinculado a spec §D2 Asymmetry Documentation.

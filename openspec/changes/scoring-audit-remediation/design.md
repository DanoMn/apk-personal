# Design: base-state-policy (scoring-audit-remediation · slice 1)

## Enfoque técnico

D1 = alinear. Reescribir `BaseStatePolicy.stateFor` para evaluar las bandas sobre `weeklyBaseScore` (0–1) en lugar de `visibleScore` (700–1000), agregar `worstLayerCollapse=0.30` y la histéresis `0.03`. Cambio de dominio puro: solo afecta la ETIQUETA de estado; `visibleScore`, `weeklyBaseScore` y `reasons` se exponen crudos (spec §7.1). D2 = ratificar: cero código, solo docs. Sin migración Room — el estado previo ya viaja en `WeeklyScoreHistoryEntry.state`.

## Decisiones de arquitectura

### Decisión: fuente del estado previo para histéresis
**Elección**: derivar `previousState` en `ScoreEngine` desde `input.weeklyHistory` — filtrar por `scoringVersion == SCORING_VERSION` y `weekStart != currentWeekStart`, ordenar desc por `weekStart`, tomar el `.state` más reciente. Pasarlo como nuevo parámetro `previousState: ScoreState?` a `stateFor`.
**Alternativas**: (a) recomputar el estado previo desde el `weeklyBaseScore` histórico — descartada: recomputar pierde la histéresis ya aplicada y duplica lógica; (b) añadir columna Room — descartada: `state` ya se persiste en `WeeklyScoreSnapshotEntity` y llega vía `WeeklyScoreHistoryEntry.state`.
**Rationale**: reutiliza el mismo patrón que `StabilityScoringPolicy` ya usa para semanas previas; cero migración; menor riesgo.

### Decisión: borrar `VisibleScorePolicy.stateFor()`
**Elección**: eliminar la función (dead code, sin callers). `BaseStatePolicy` es la única fuente de verdad de estado.
**Rationale**: evita una segunda tabla de umbrales divergente.

### Decisión: constantes a `ScoringConstants`
**Elección**: mover todos los magic-numbers de banda a constantes nombradas.

## Firma nueva de `stateFor`

```kotlin
fun stateFor(
    weeklyBaseScore: Float,
    worstLayerScore: Float,
    stability: StabilityEvaluation,
    previousState: ScoreState?,
): ScoreState
```
Se elimina el parámetro `visibleScore` (ya no participa en la decisión).

## Cambio en el call-site (`ScoreEngine.calculate`)

Antes de construir el `ScoreReport`, derivar:
```kotlin
val previousState = input.weeklyHistory
    .filter { it.scoringVersion == WeeklyScoreSnapshotConstants.SCORING_VERSION &&
              it.weekStart != context.weekStart.toString() }
    .maxByOrNull { it.weekStart }
    ?.state
```
Y en `state = BaseStatePolicy.stateFor(...)` quitar `visibleScore`, agregar `previousState = previousState`.

## Algoritmo de histéresis (pseudocódigo)

```
rawBand = bandFor(weeklyBaseScore)            // banda cruda, lower-inclusive/upper-exclusive

// orden de precedencia:
if previousState == null            -> return rawBand
if isOneStepBelow(rawBand, previousState):    // rawBand exactamente UN escalón por debajo
    boundary = upperBoundaryOf(rawBand)       // p.ej. rawBand=Attention -> 0.70
    if (boundary - weeklyBaseScore) <= stateHysteresisMargin   // dentro de 0.03 por debajo del umbral
        return previousState                  // amortiguar: mantener estado previo
return rawBand
```
- Solo amortigua descensos de UN escalón (Plenitude→Motion, Motion→Attention, Attention→Restoration).
- NUNCA bloquea ascensos (`rawBand` por encima de `previousState` se devuelve tal cual).
- NUNCA suprime más de una banda (si `rawBand` cae dos escalones, no aplica).
- Inquebrantable/Plenitude/NoData no entran al damping de banda (ver precedencia).

## Orden de precedencia (determinístico)

1. **NoData** — lo resuelve `noDataReport` antes de llamar a `stateFor` (sin cambio).
2. **worstLayerCollapse** — `worstLayerScore < 0.30f` → `Restoration` (override duro; ignora histéresis).
3. **banda cruda** sobre `weeklyBaseScore` con histéresis aplicada.
4. **Inquebrantable** — gating: `hasTemporalMemory && base≥0.90 && worst≥0.80 && stability≥0.90`.
5. **Plenitude** — `base≥0.85` (banda superior).

`bandFor` (lower-inclusive, upper-exclusive):
- `< 0.40` → Restoration
- `< 0.70` → Attention
- `< 0.85` → Motion
- `≥ 0.85` → Plenitude (luego Inquebrantable gating sobre ella)

## Constantes a agregar en `ScoringConstants.kt`

```kotlin
const val STATE_RESTORATION_THRESHOLD = 0.40f
const val STATE_ATTENTION_THRESHOLD = 0.70f
const val STATE_PLENITUDE_THRESHOLD = 0.85f
const val WORST_LAYER_COLLAPSE = 0.30f
const val STATE_HYSTERESIS_MARGIN = 0.03f
const val UNBREAKABLE_BASE_MIN = 0.90f
const val UNBREAKABLE_WORST_MIN = 0.80f
const val UNBREAKABLE_STABILITY_MIN = 0.90f
const val PLENITUDE_WORST_MIN = 0.75f
```

## Plan de tests (TDD — escribir ANTES de la reescritura)

Archivo nuevo recomendado: **`BaseStatePolicyTest.kt`** (probar la política aislada: determinística, sin armar `ScoreInput` completo). El damping y los bordes se prueban contra `weeklyBaseScore` directo. Los tests de Inquebrantable de extremo a extremo ya viven en `ScoreEngineTest.kt` y se conservan; se agrega 1 test de regresión de `previousState` en `ScoreEngineTest` para cubrir la derivación desde `weeklyHistory`.

Casos en `BaseStatePolicyTest.kt`:
1. `base 0.399 -> Restoration` / `base 0.40 -> Attention` (borde Restauración).
2. `base 0.699 -> Attention` / `base 0.70 -> Motion` (borde Atención).
3. `base 0.849 -> Motion` / `base 0.85 -> Plenitude` (borde Plenitud).
4. `worstLayer 0.299 con base 0.95 -> Restoration` (collapse override gana a Plenitude).
5. `worstLayer 0.30 -> NO collapse` (borde collapse, upper-exclusive: 0.30 no colapsa).
6. Histéresis MANTIENE: `previousState=Motion`, `base 0.69` (Attention crudo, dentro de 0.01 < 0.03 de 0.70) -> `Motion`.
7. Histéresis CAE: `previousState=Motion`, `base 0.66` (fuera de 0.03 de 0.70) -> `Attention`.
8. Histéresis NO bloquea ascenso: `previousState=Attention`, `base 0.72` -> `Motion`.
9. Histéresis NO suprime dos bandas: `previousState=Plenitude`, `base 0.66` (dos escalones) -> `Attention`.
10. `previousState=null` (primera semana): `base 0.69` -> `Attention` (sin damping).
11. Collapse ignora histéresis: `previousState=Plenitude`, `worst 0.25` -> `Restoration`.
12. Inquebrantable gating: `hasTemporalMemory + base 0.92 + worst 0.82 + stability 0.91` -> `Unbreakable`.
13. Plenitude sin memoria temporal: `base 0.92 + stability null` -> `Plenitude` (no Unbreakable).

Caso en `ScoreEngineTest.kt`:
14. `previousState` se deriva del `weeklyHistory` más reciente (estado de la semana previa propaga al damping).

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `domain/scoring/BaseStatePolicy.kt` | Modify | Bandas sobre `weeklyBaseScore`, collapse, histéresis, parámetro `previousState`; quitar `visibleScore` |
| `domain/scoring/ScoringConstants.kt` | Modify | Agregar constantes de banda/collapse/histéresis/Inquebrantable |
| `domain/scoring/ScoreEngine.kt` | Modify | Derivar `previousState` de `weeklyHistory`; ajustar call-site |
| `domain/scoring/VisibleScorePolicy.kt` | Modify | Borrar `stateFor()` (dead code) |
| `.../scoring/BaseStatePolicyTest.kt` | New | Tests de bandas + collapse + histéresis (TDD) |
| `docs/arbol-scoring-vocal-v1.md`, `docs/plan-tecnico-scoring-vocal.md` §7.1 | Modify | Ratificar umbrales; documentar asimetría D2 |

## Migración / Rollout

Sin migración. `WeeklyScoreSnapshotEntity.state` ya persiste el estado por semana; la histéresis solo lo lee. Rollback = `git revert` del commit del slice.

## Riesgos

| Riesgo | Mitigación |
|--------|------------|
| Estados de snapshots existentes cambian de etiqueta al releer | Es presentación; `weeklyBaseScore` persistido intacto; sin migración |
| `previousState` en primera ejecución es null | El algoritmo devuelve `rawBand` sin damping (caso 10) |
| `worstLayerScore < 0.55` viejo desaparece | Sustituido por `worstLayerCollapse=0.30` ratificado; documentar en §7.1 |

## Open Questions
- Ninguna que bloquee. D1/D2 ya resueltas por el dueño del spec.

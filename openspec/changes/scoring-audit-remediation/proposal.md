# Proposal: decisions-and-state (scoring-audit-remediation · slice 1)

## Intent

La auditoría verificó que `BaseStatePolicy.kt` evalúa los estados sobre `visibleScore` (700–1000) en lugar de `weeklyBaseScore` (0–1), como exige `docs/plan-tecnico-scoring-vocal.md` §7.1. Efecto real: Restauración recién dispara a base ≈0.167 (spec: <0.40) y Atención a ≈0.333 (spec: <0.70) — el árbol castiga mucho más tarde de lo diseñado. Faltan además `worstLayerCollapse` y la histéresis. Esta es la única deuda que bloquea la coherencia del contrato y se resuelve en dominio puro, sin tocar Room ni Compose. Cierra D1 (alinear) y D2 (ratificar la asimetría) dejando el árbol de estados fiel a la spec.

## Scope

### In Scope
- Reescribir `BaseStatePolicy.kt`: thresholds sobre `weeklyBaseScore` con bandas `<0.40` Restauración, `<0.70` Atención, `<0.85` En marcha (Motion), `≥0.85` Plenitud; `worstLayerCollapse=0.30` fuerza Restauración; histéresis `0.03`. Preservar la regla Inquebrantable existente.
- Mover los magic-numbers de umbral a `ScoringConstants.kt`.
- TDD primero: tests de cada borde de banda + worstLayerCollapse + histéresis (en `BaseStatePolicyTest.kt` nuevo, o `ScoreEngineTest.kt`) ANTES de la reescritura.
- Resolver `VisibleScorePolicy.stateFor()` (dead code, sin referencias) → eliminarla para una sola fuente de verdad.
- Docs: ratificar §7.1 como final y documentar la asimetría `rawScore`/`baseScore` (D2) como decisión intencional en `arbol-scoring-vocal-v1.md` y `plan-tecnico-scoring-vocal.md` §7.1.

### Out of Scope
- Slices 2-6: `sleep-sessions-infra`, `base-config-infra`, `dao-range-queries`, `abstinence-pending-status`, `legacy-cleanup` (cambios de seguimiento independientes).
- Fase 8 UI explicativa (cambio posterior, >400 líneas).
- Cualquier cambio de código en `WeeklyScorePolicy` (D2 = ratificar, solo docs).

## Capabilities

### New Capabilities
- `base-state-policy`: resolución del estado base del árbol vocal sobre `weeklyBaseScore`, con colapso de peor capa, histéresis e Inquebrantable.

### Modified Capabilities
None (no hay specs openspec previos; la spec canónica son los docs de `docs/`).

## Approach

D1 = alinear. La firma de `stateFor` ya recibe `weeklyBaseScore`, `worstLayerScore` y `stability`; se reescribe el `when` para ordenar: (1) `worstLayerScore < worstLayerCollapse` → Restauración; (2) bandas sobre `weeklyBaseScore`; (3) Inquebrantable e Inquebrantable→Plenitud por encima. Histéresis: la política necesita el estado de la semana anterior como nuevo parámetro `previousState`; cuando `weeklyBaseScore` cae dentro de `hysteresisMargin (0.03)` por debajo del umbral que separa el estado previo del inferior, se mantiene el estado previo. La histéresis SOLO afecta la etiqueta de estado — `visibleScore`, `weeklyBaseScore` crudos y las `reasons` se exponen sin alterar (spec §7.1: no ocultar el score real). D2 = ratificar: cero código; documentar la asimetría como anti-compensación sellada.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `domain/scoring/BaseStatePolicy.kt` | Modified | Thresholds sobre base + collapse + histéresis + `previousState` |
| `domain/scoring/ScoringConstants.kt` | Modified | Constantes de banda, `worstLayerCollapse`, `stateHysteresisMargin` |
| `domain/scoring/ScoreEngine.kt` | Modified | Pasar `previousState` desde el historial al llamar `stateFor` |
| `domain/scoring/VisibleScorePolicy.kt` | Removed | Borrar `stateFor()` (dead code, sin referencias) |
| `.../scoring/BaseStatePolicyTest.kt` | New | Tests de bandas + collapse + histéresis (TDD primero) |
| `docs/arbol-scoring-vocal-v1.md`, `docs/plan-tecnico-scoring-vocal.md` §7.1 | Modified | Ratificar umbrales; documentar asimetría D2 |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Histéresis necesita estado previo, hoy no propagado a `stateFor` | Med | Añadir `previousState` como parámetro; derivarlo del `weeklyHistory` ya disponible en `ScoreEngine` |
| Reescribir umbrales cambia estados de snapshots existentes | Low | Cambio de presentación de estado, no del `weeklyBaseScore` persistido; sin migración Room |
| Borrar `VisibleScorePolicy.stateFor` rompe un caller oculto | Low | `rg` confirmó cero referencias en main y test |

## Rollback Plan

Cambio acotado a dominio puro sin migración de DB. Revertir = `git revert` del commit de slice 1; `BaseStatePolicy` vuelve a thresholds sobre `visibleScore`, se restauran constantes y docs. Sin estado persistido afectado.

## Dependencies

- Ninguna externa. Depende solo de las decisiones D1 (alinear) y D2 (ratificar), ya resueltas por el dueño del spec.

## Success Criteria

- [ ] `BaseStatePolicy` evalúa bandas sobre `weeklyBaseScore` con los umbrales 0.40/0.70/0.85 y `worstLayerCollapse=0.30`.
- [ ] Histéresis 0.03 amortigua oscilación sin alterar `visibleScore`/`weeklyBaseScore`/`reasons`.
- [ ] Regla Inquebrantable preservada (base≥0.90 + worst≥0.80 + stability≥0.90 + memoria temporal).
- [ ] Tests de borde de banda, collapse e histéresis verdes; escritos ANTES de la reescritura (TDD).
- [ ] `VisibleScorePolicy.stateFor()` eliminada; una sola fuente de verdad de estado.
- [ ] §7.1 marcada como ratificada/final y asimetría `rawScore`/`baseScore` documentada como intencional.

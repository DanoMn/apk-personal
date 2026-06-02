# Mapa de cobertura de tests — scoring

> Plan de trabajo (no es contrato vivo). Foto al 2026-06-01 tras auditar el motor
> de scoring contra el contrato `docs/scoring/arbol-scoring-vocal-v1.md`.

## Veredicto de la auditoría

- **No hay bugs críticos ni divisiones por cero.** Todos los denominadores tienen
  guard: `targetDays().coerceIn(1,7)`, `targetDailyValue().coerceAtLeast(1)`,
  Support `if (expectedSupportDays <= 0) return 1f`, Sobriety `if (weekDates.isEmpty()) return null`,
  capa sin anclas usa `averageOrNull()` (da `null`, no NaN).
- La cobertura existente es **buena**: BaseStatePolicy (20 tests), Sleep
  (SleepScoring/SleepInterpreter/aggregation), Dashboard, Tasks, Abstinence.
- Los agujeros son de **caracterización**: fórmulas que andan pero sin test que
  las blinde contra un refactor futuro.

## Mapa por feature

| Feature | Contrato | Cobertura hoy | Agujero | Prioridad |
|---|---|---|---|---|
| Anclas: frecuencia 70 / valor 30 | §7.2 | `ScoreEngineTest.anchorsUseSeventy…` (integración) | sin unit directo de `AnchorScoringPolicy` | BAJA |
| Anclas: superhábit bonus (curva exp) | §8.2 | `anchorSurplus…` (solo verifica >0) | valores de la curva sin caracterizar | MEDIA |
| Soportes: opt-in / omisión | §9 | `supportsAreOptIn…` + DashboardProjection | multi-support, borde sin omisión | BAJA |
| TaskMomentum: curva de saturación | §10.3 | `completedLayerTasks…` (solo >0) | valores 1→.020, 2→.032, 3→.039, 5→.046 sin verificar | MEDIA |
| Cuerpo + sueño 70/30 + ADR-3 | §11 | SpecialLayerScoringPolicy (8) + gate sueño §16.7 | — cubierto | OK |
| Cuerpo: cap de estado sin sueño | §16.7 | `missingSleepRegistrationCaps…` | falta el unit directo en BaseStatePolicyTest | BAJA |
| Sobriedad: single-track (pending/relapse) | §13 | `ScoreEngineTest` (3 tests) | — | OK |
| **Sobriedad: MULTI-TRACK (70 avg / 30 worst)** | §13.4 | **ninguno** | fórmula core sin red | **ALTA** |
| Sobriedad: RelapseProtection `exp(-r/1.5)` | §13.3 | indirecto | valores de la curva sin caracterizar | MEDIA |
| WeeklyBase 75 avg / 25 worst | §14 | `worstLayerDrags…` | — | OK |
| Estados: bandas / caps / histéresis | §16 | BaseStatePolicy (20) | muy cubierto | OK |
| Puerta Inquebrantable | §16.4 | `unbreakableRequires…` + `perfectSingle…` | — | OK |
| VisibleScore: bordes | §3.2 | asserts en ScoreEngine | clamp <0 / >1, 700 y 1000 sin unit directo | BAJA |
| StabilityScore: fórmula + conteo semanas | §15 | vía unbreakable | fórmula directa sin test | MEDIA |
| Gate config mínima: 3+ capas con ancla | §7.4 | `fewerThanThree…` + `exactlyThree…` | — IMPLEMENTADO (commit 4af47ef) | OK |
| Guards div-por-cero (regresión) | — | ninguno | protegido en código, sin test que lo blinde | BAJA |

## Requisito de configuración mínima — 3 capas activas con ancla (IMPLEMENTADO 2026-06-01)

Regla del dueño: de las **5 capas**, el scoring exige **mínimo 3 capas activas con al
menos 1 ancla** (hasta 2 prescindibles). Una capa cuenta solo si está activa Y tiene
≥1 ancla. **Implementado** en `ScoreEngine` (§7.4 del árbol, commit 4af47ef): con < 3
capas válidas → `NoData`. Constante `MIN_ACTIVE_LAYERS_WITH_ANCHOR = 3`.

Queda aparte (UI, no scoring): el onboarding que OBLIGA a configurar esas 3 capas
—ver `pendientes.md`—.

## Orden sugerido de ataque

1. ~~Sobriedad multi-track (ALTA)~~ — HECHO (commit d3d4c1b).
2. ~~Gate de 3 capas con ancla~~ — HECHO (commit 4af47ef).
3. **Curvas (MEDIA)** — TaskMomentum §10.3, RelapseProtection §13.3, Stability §15.
4. **Defensivos (BAJA)** — VisibleScore bordes, guards div-cero, units directos de Anchor/Support.

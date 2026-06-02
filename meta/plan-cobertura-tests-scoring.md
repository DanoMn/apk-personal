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
| Anclas: frecuencia 70 / valor 30 | §7.2 | `AnchorScoringPolicyTest` (unit directo, 4 tests) | — CARACTERIZADO (2026-06-02) | OK |
| Anclas: superhábit bonus (curva exp) | §8.2 | `AnchorScoringPolicyTest` (curva + cero) | — CARACTERIZADO (2026-06-02) | OK |
| Soportes: opt-in / omisión | §9 | `SupportScoringPolicyTest` (5 tests, multi-support) | — CARACTERIZADO (2026-06-02) | OK |
| TaskMomentum: curva de saturación | §10.3 | `TaskMomentumPolicyTest` (6 tests, curva 0→0.050) | — CARACTERIZADO (2026-06-02) | OK |
| Cuerpo + sueño 70/30 + ADR-3 | §11 | SpecialLayerScoringPolicy (8) + gate sueño §16.7 | — cubierto | OK |
| Cuerpo: cap de estado sin sueño | §16.7 | `missingSleepRegistrationCaps…` + 3 units en `BaseStatePolicyTest` | — CARACTERIZADO (2026-06-02) | OK |
| Sobriedad: single-track (pending/relapse) | §13 | `ScoreEngineTest` (3 tests) | — | OK |
| Sobriedad: MULTI-TRACK (70 avg / 30 worst) | §13.4 | `SobrietyScoringPolicyTest` (2 tests) | — CARACTERIZADO (commit d3d4c1b) | OK |
| Sobriedad: RelapseProtection `exp(-r/1.5)` | §13.3 | `SobrietyScoringPolicyTest` (4 tests, r=0→1.0…3→0.077) | — CARACTERIZADO (2026-06-02) | OK |
| WeeklyBase 75 avg / 25 worst | §14 | `worstLayerDrags…` | — | OK |
| Estados: bandas / caps / histéresis | §16 | BaseStatePolicy (20) | muy cubierto | OK |
| Puerta Inquebrantable | §16.4 | `unbreakableRequires…` + `perfectSingle…` | — | OK |
| VisibleScore: bordes | §3.2 | `VisibleScorePolicyTest` (5 tests, clamp + bordes) | — CARACTERIZADO (2026-06-02) | OK |
| StabilityScore: fórmula + conteo semanas | §15 | `StabilityScoringPolicyTest` (6 tests) | — FÓRMULA VALIDADA + CARACTERIZADA (2026-06-02) | OK |
| Gate config mínima: 3+ capas con ancla | §7.4 | `fewerThanThree…` + `exactlyThree…` | — IMPLEMENTADO (commit 4af47ef) | OK |
| Guards div-por-cero (regresión) | — | `SupportScoringPolicyTest` + `SobrietyScoringPolicyTest` (empty weekDates/tracks) | — CARACTERIZADO (2026-06-02) | OK |

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
3. ~~**Curvas (MEDIA)** — TaskMomentum §10.3, RelapseProtection §13.3, Stability §15~~ — HECHO (2026-06-02).
   Nota: la fórmula de StabilityScore §15 estaba marcada "no canónica" en el contrato; el
   dueño VALIDÓ la implementada (0.75 avg + 0.25 worst sobre 6 semanas) → §15 actualizado.
4. ~~**Defensivos (BAJA)** — superhábit ancla §8.2, VisibleScore bordes §3.2, guards div-cero,
   units directos de Anchor/Support, cap de sueño en BaseStatePolicyTest, multi-support §9~~ — HECHO (2026-06-02).

## Estado: cobertura COMPLETA (2026-06-02)

Todos los agujeros del mapa están cerrados. El motor de scoring tiene red de
regresión completa contra el contrato `arbol-scoring-vocal-v1.md`. No quedan ítems
ALTA/MEDIA/BAJA pendientes. Lo que resta es trabajo de UI (onboarding de 3 capas +
sueño, notificaciones) — ver `meta/pendientes.md`, NO es scoring.

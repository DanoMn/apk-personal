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
| **Capa activa sin anclas** | §7.3 | **ninguno** | discrepancia código vs contrato (ver abajo) | **DECISIÓN** |
| Guards div-por-cero (regresión) | — | ninguno | protegido en código, sin test que lo blinde | BAJA |

## Requisito de configuración mínima — 3 capas activas con ancla (decisión del dueño 2026-06-01)

Regla definida por el dueño: de las **5 capas** (Interior, Cuerpo, Conducta, Vínculos,
Proyecto), el scoring exige **mínimo 3 capas activas, cada una con al menos 1 ancla**.
Hasta **2 capas se pueden prescindir** (dejar inactivas). Esta regla global **contiene**
el caso atómico §7.3 (una capa activa sin anclas no cuenta para el mínimo).

Estado del código (gap a implementar): `ScoreEngine` NO enforce esto — solo exige que
haya alguna capa activa con algún hecho (`hasAnyFact`). No cuenta "≥3 capas con ancla".
Pendiente: definir qué pasa si hay < 3 capas válidas (¿NoData? ¿estado "configuración
incompleta"?) e implementar el gate + su test. Hermano de los gates de sueño (§16.7) y
onboarding (ver `pendientes.md`).

## Orden sugerido de ataque

1. **Sobriedad multi-track (ALTA)** — fórmula core de §13.4, hoy sin ninguna red.
2. **Capa sin anclas** — decidir producto primero (como hicimos con sueño), luego test.
3. **Curvas (MEDIA)** — TaskMomentum §10.3, RelapseProtection §13.3, Stability §15.
4. **Defensivos (BAJA)** — VisibleScore bordes, guards div-cero, units directos de Anchor/Support.

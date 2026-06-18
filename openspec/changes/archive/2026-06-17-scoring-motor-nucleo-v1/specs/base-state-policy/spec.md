# Especificación: base-state-policy (MODIFIED)

Cambio: `scoring-motor-nucleo-v1`
Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` § NIVEL 6,
`docs/scoring/axiomas-modelo-scoring-v1.md` § 8 (BA1–BA3).
Supersede: `openspec/specs/base-state-policy/spec.md` (y el delta de
`scoring-audit-remediation`) — esta delta REEMPLAZA su modelo de resolución de estado.

> El modelo nuevo SUPERSEDE la `base-state-policy` previa, que asumía bandas sobre
> `weeklyBaseScore` con gates duros, colapso de peor capa, regla Inquebrantable e histéresis.
> El NIVEL 6 es `banda(ESTADO)`: una función PURA sobre cortes, sin gates, sin worst-layer, sin
> histéresis, sin memoria temporal.

> NOTA DE ARCHIVO: este delta quedó aplicado sobre el spec canónico
> `openspec/specs/base-state-policy/spec.md` al archivar el cambio (2026-06-17). Los requisitos
> viejos (Band Mapping `0.40/0.70/0.85`, Worst-Layer Collapse, Inquebrantable Gate, State
> Hysteresis, Constants Extracted) quedaron ELIMINADOS/REEMPLAZADOS; `Sin Datos (NoData)` se
> conservó. Este archivo es el audit trail del delta tal como se planeó.

## Propósito

`base-state-policy` resuelve la banda de estado a partir del `ESTADO ∈ [0, 1.5]` del motor
(NIVEL 5), como función PURA sobre los cortes `0.40 / 0.62 / 0.85 / 1.10`. La banda mira SOLO
el ESTADO de la ventana de 7 días — no la historia, no la peor capa, no el estado previo.

## REQUISITOS ELIMINADOS / REEMPLAZADOS / NUEVOS

- REMOVED `Band Mapping` (cortes viejos `0.40/0.70/0.85`) → REPLACED por cortes
  `0.40/0.62/0.85/1.10` sobre `ESTADO ∈ [0,1.5]`.
- REMOVED `Worst-Layer Collapse Override`, `Inquebrantable Gate`, `State Hysteresis`.
- REPLACED `Constants Extracted` (umbrales viejos eliminados; cortes nuevos + δ=0.10 agregados).
- NEW `Band Mapping (ESTADO, cortes nuevos)` (BA1/BA2/BA3 + 1.10 exacto).
- NEW `Banda pura — sin gates, sin worst-layer, sin histéresis, sin memoria`.
- CONSERVADO `Sin Datos (NoData)` (M=null no penaliza, vía facts-adapter).

(Tabla de bandas, escenarios y restricciones completas — ver spec canónico
`openspec/specs/base-state-policy/spec.md`.)

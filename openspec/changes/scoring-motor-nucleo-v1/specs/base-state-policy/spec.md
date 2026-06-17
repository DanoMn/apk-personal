# Especificación: base-state-policy (MODIFIED)

Cambio: `scoring-motor-nucleo-v1`
Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` § NIVEL 6,
`docs/scoring/axiomas-modelo-scoring-v1.md` § 8 (BA1–BA3).
Supersede: `openspec/specs/base-state-policy/spec.md` (y el delta de
`scoring-audit-remediation`) — esta delta REEMPLAZA su modelo de resolución de estado.

> El modelo nuevo SUPERSEDE la `base-state-policy` previa, que asumía bandas sobre
> `weeklyBaseScore` con gates duros, colapso de peor capa, regla Inquebrantable e histéresis.
> El NIVEL 6 es `banda(ESTADO)`: una función PURA sobre cortes, sin gates, sin worst-layer, sin
> histéresis, sin memoria temporal. Esta spec marca explícitamente qué requisitos viejos se
> ELIMINAN y cuáles se REEMPLAZAN.

## Propósito

`base-state-policy` resuelve la banda de estado del usuario a partir del `ESTADO ∈ [0, 1.5]`
del motor (NIVEL 5), como función PURA sobre los cortes `0.40 / 0.62 / 0.85 / 1.10`. Es la
única fuente de verdad de la banda; ningún otro componente repite esta lógica. La banda mira
SOLO el ESTADO de la ventana de 7 días — no la historia, no la peor capa, no el estado previo.

---

## REQUISITOS ELIMINADOS (la spec previa los retira)

Los siguientes requisitos de la `base-state-policy` previa quedan **ELIMINADOS** por este
cambio (el modelo de pesos puros los hace innecesarios o los contradice):

- **`Band Mapping` (cortes viejos `0.40/0.70/0.85`)** — REEMPLAZADO por los cortes
  `0.40/0.62/0.85/1.10` (ver `REMOVED: Requirement: Band Mapping` abajo). La banda opera sobre
  `ESTADO ∈ [0,1.5]`, no sobre `weeklyBaseScore ∈ [0,1]`.
- **`Worst-Layer Collapse Override`** — ELIMINADO. No hay colapso de peor capa; el motor de
  pesos puros no usa worst-term duro. La capa débil ya pesa menos por votos, sin override.
- **`Inquebrantable Gate`** (memoria temporal + `worstLayer ≥ 0.80` + `stability ≥ 0.90`) —
  ELIMINADO. `Inquebrantable` se gana con `ESTADO ≥ 1.10` (superhabit real), sin gate de
  estabilidad ni memoria temporal.
- **`State Hysteresis`** — ELIMINADO. La banda es función pura del ESTADO actual; no suprime
  transiciones a la baja, no mira estado previo.
- **`Constants Extracted` (umbrales viejos `0.70/0.30/0.80/0.03/0.90`)** — REEMPLAZADO: los
  umbrales `WORST_LAYER_*`, `UNBREAKABLE_*`, `STATE_HYSTERESIS_MARGIN` se ELIMINAN de
  `ScoringConstants`; los cortes de banda nuevos (`0.40/0.62/0.85/1.10`, `δ=0.10`) se agregan.

> El requisito `Sin Datos (NoData State)` se CONSERVA con matiz (ver abajo): sin hechos
> suficientes → `SinDatos`. La parte de "sueño ausente no hunde Cuerpo" sigue siendo válida a
> nivel de adapter (`scoring-facts-adapter`: `M = null` no penaliza), no a nivel de banda.

---

## REMOVED: Requirement: Band Mapping

(Cortes viejos `Restauración<0.40 · Atención[0.40,0.70) · En marcha[0.70,0.85) · Plenitud≥0.85`
sobre `weeklyBaseScore`.) Reemplazado por `Requirement: Band Mapping (ESTADO, cortes nuevos)`.

## REMOVED: Requirement: Worst-Layer Collapse Override

Eliminado por completo (motor de pesos puros, sin worst-term duro).

## REMOVED: Requirement: Inquebrantable Gate

Eliminado (Inquebrantable emerge de `ESTADO ≥ 1.10`, sin gate de estabilidad/memoria).

## REMOVED: Requirement: State Hysteresis

Eliminado (banda = función pura del ESTADO actual).

---

## REQUISITOS NUEVOS / REEMPLAZADOS

### Requirement: Band Mapping (ESTADO, cortes nuevos)

`base-state-policy` MUST mapear `ESTADO ∈ [0, 1.5]` a `ScoreState` con estos cortes
(límite inferior inclusivo, superior exclusivo), evaluados como función PURA sin más insumos:

| Banda | Condición | `ScoreState` |
|------|-----------|--------------|
| Restauración   | `ESTADO < 0.40`        | `Restoration` |
| Atención       | `0.40 ≤ ESTADO < 0.62` | `Attention`  |
| En marcha      | `0.62 ≤ ESTADO < 0.85` | `Motion`     |
| Plenitud       | `0.85 ≤ ESTADO < 1.10` | `Plenitude`  |
| Inquebrantable | `ESTADO ≥ 1.10`        | `Unbreakable` |

`1.10 = 1 + δ` con `δ = 0.10`. Los cortes MUST vivir en `ScoringConstants`. La función NO mira
peor capa, historia, estado previo ni estabilidad.

#### Scenario: BA1 — Cortes R/A/EM/P/I
- GIVEN `ESTADO ∈ {0.30, 0.50, 0.70, 0.90, 1.15}`
- WHEN se resuelve la banda
- THEN `0.30→Restoration`, `0.50→Attention`, `0.70→Motion`, `0.90→Plenitude`,
  `1.15→Unbreakable`

#### Scenario: BA2 — Plenitud entra en 0.85
- GIVEN `ESTADO = 0.85` y `ESTADO = 0.84`
- WHEN se resuelve la banda
- THEN `0.85 → Plenitude` y `0.84 → Motion`

#### Scenario: Inquebrantable entra exactamente en 1.10
- GIVEN `ESTADO = 1.10` y `ESTADO = 1.099`
- WHEN se resuelve la banda
- THEN `1.10 → Unbreakable` y `1.099 → Plenitude`

#### Scenario: BA3 — Cumplir-justo (1.0) cae DENTRO de Plenitud
- GIVEN `ESTADO = 1.0`
- WHEN se resuelve la banda
- THEN la banda es `Plenitude` (no Unbreakable)

### Requirement: Banda pura — sin gates, sin worst-layer, sin histéresis, sin memoria

`base-state-policy` MUST resolver la banda como función pura del ESTADO actual. Dos
evaluaciones con el mismo ESTADO MUST dar la misma banda, independientemente de la historia
semanal, el estado previo o el score de la peor capa.

#### Scenario: Misma banda con distinta historia
- GIVEN dos evaluaciones con `ESTADO = 0.90` y distinta historia/estado previo
- WHEN se resuelve la banda
- THEN ambas dan `Plenitude` (la historia no influye)

#### Scenario: ESTADO alto con una capa débil no se fuerza a Restauración
- GIVEN `ESTADO = 0.90` (alto) producido por un agregado donde una capa votó poco
- WHEN se resuelve la banda
- THEN la banda es `Plenitude` (no hay colapso de peor capa; la capa débil ya pesó menos)

### Requirement: Sin Datos (NoData) — conservado

Cuando no hay configuración base o hechos suficientes para computar el ESTADO, la policy MUST
resolver `ScoreState.NoData` y NO exponer score visible. La ausencia de señal de sueño
(`M = null`) NO produce una banda baja por sí sola (se maneja en `scoring-facts-adapter`: el
opt-in de sueño ausente no penaliza).

#### Scenario: Sin hechos → NoData
- GIVEN no hay configuración base activa ni hechos suficientes para la semana
- WHEN se resuelve el estado
- THEN el resultado es `NoData` y no se expone score visible

#### Scenario: Sueño ausente no hunde la banda
- GIVEN una semana con hechos de anclas pero `M = null` (sin dato de sueño)
- WHEN se resuelve la banda
- THEN la banda refleja el ESTADO de las anclas/soportes; la ausencia de sueño no la baja

---

## Restricciones

- **Dominio puro JVM.** La resolución de banda no toca Room ni Compose.
- **Pesos puros.** La banda NO aplica gates/caps/worst-term/histéresis. Todo emerge del ESTADO.
- **Cortes en `ScoringConstants`** (`0.40/0.62/0.85/1.10`, `δ=0.10`); los umbrales viejos
  (`WORST_LAYER_*`, `UNBREAKABLE_*`, `STATE_HYSTERESIS_MARGIN`) se ELIMINAN.
- **Estabilidad APARCADA:** `StabilityScoringPolicy` inerte; la banda no la consulta.
- **Strict TDD:** BA1/BA2/BA3 + cortes exactos como tests JUnit antes del código.

## Criterios de aceptación

- `base-state-policy` resuelve la banda como `banda(ESTADO)` pura sobre `0.40/0.62/0.85/1.10`;
  tests BA1/BA2/BA3 + cortes exactos (1.10 entra Inquebrantable; 0.85 entra Plenitud) verdes.
- Los requisitos viejos (worst-layer collapse, Inquebrantable gate, histéresis) eliminados del
  código; ningún caller los referencia; sus constantes retiradas de `ScoringConstants`.
- `NoData` conservado; sueño ausente no hunde la banda.
- Build verde con `testDebugUnitTest`.

# Especificación: base-state-policy

Cambio: `scoring-audit-remediation` · slice 1
Fuente canónica: `docs/plan-tecnico-scoring-vocal.md` §7.1 · `docs/arbol-scoring-vocal-v1.md` §16

## Purpose

`BaseStatePolicy` es la única fuente de verdad para resolver el estado del árbol vocal a partir de `weeklyBaseScore` (0.0–1.0). Las bandas, el colapso de peor capa, la histéresis y la regla Inquebrantable se evalúan en esta política; ningún otro componente del dominio repite esta lógica.

---

## Requirements

### Requirement: Band Mapping

`BaseStatePolicy` MUST map `weeklyBaseScore` to a `VocalState` using the following exclusive-upper-bound bands in evaluation order (lowest first):

| Band | Condition |
|------|-----------|
| Restauración | `weeklyBaseScore < 0.40` |
| Atención     | `0.40 ≤ weeklyBaseScore < 0.70` |
| En marcha    | `0.70 ≤ weeklyBaseScore < 0.85` |
| Plenitud     | `weeklyBaseScore ≥ 0.85` |

Boundaries are lower-inclusive / upper-exclusive. `weeklyBaseScore` MUST be in range [0.0, 1.0]; values outside this range SHALL be clamped before evaluation.

#### Scenario: Score exactamente en el límite inferior de Atención

- GIVEN `weeklyBaseScore = 0.40`
- WHEN `stateFor` es invocada (sin colapso, sin histéresis aplicable, sin memoria temporal)
- THEN el estado resultante es `Atención`

#### Scenario: Score justo debajo del límite de Atención cae a Restauración

- GIVEN `weeklyBaseScore = 0.399`
- WHEN `stateFor` es invocada
- THEN el estado resultante es `Restauración`

#### Scenario: Score en el límite inferior de En marcha

- GIVEN `weeklyBaseScore = 0.70`
- WHEN `stateFor` es invocada
- THEN el estado resultante es `En marcha`

#### Scenario: Score en el límite inferior de Plenitud

- GIVEN `weeklyBaseScore = 0.85`
- WHEN `stateFor` es invocada
- THEN el estado resultante es `Plenitud`

---

### Requirement: Worst-Layer Collapse Override

When the score of the worst active layer (`worstLayerScore`) is strictly below `worstLayerCollapse = 0.30`, the policy MUST force state to `Restauración` regardless of `weeklyBaseScore`. This check MUST be evaluated BEFORE band mapping — it takes precedence over all upward bands.

#### Scenario: Colapso de peor capa fuerza Restauración aunque base sea alta

- GIVEN `weeklyBaseScore = 0.80` (que correspondería a En marcha)
- AND `worstLayerScore = 0.28` (< 0.30)
- WHEN `stateFor` es invocada
- THEN el estado resultante es `Restauración`

#### Scenario: Peor capa exactamente en el umbral de colapso no fuerza Restauración

- GIVEN `weeklyBaseScore = 0.80`
- AND `worstLayerScore = 0.30` (igual al umbral, no menor)
- WHEN `stateFor` es invocada
- THEN el estado resultante NO es forzado a `Restauración` por colapso; pero como `worstLayerScore = 0.30 < worstLayerMinimumForMotion = 0.55`, la escalera de peor capa limita el estado a `Atención` (no `En marcha`)

---

### Requirement: Inquebrantable Gate

The policy MUST elevate state from `Plenitud` to `Inquebrantable` only when ALL of the following conditions hold simultaneously:

1. `hasTemporalMemory = true`
2. `weeklyBaseScore ≥ 0.90`
3. `worstLayerScore ≥ 0.80`
4. `stabilityScore ≥ 0.90`

If any condition fails, the resolved state MUST cap at `Plenitud`. A single perfect week without temporal memory MUST NOT produce `Inquebrantable`.

#### Scenario: Semana perfecta sin memoria temporal resulta en Plenitud, no Inquebrantable

- GIVEN `weeklyBaseScore = 0.95`, `worstLayerScore = 0.85`, `stabilityScore = 0.92`
- AND `hasTemporalMemory = false`
- WHEN `stateFor` es invocada
- THEN el estado resultante es `Plenitud`

#### Scenario: Todos los requisitos cumplidos con memoria temporal → Inquebrantable

- GIVEN `weeklyBaseScore = 0.92`, `worstLayerScore = 0.81`, `stabilityScore = 0.91`
- AND `hasTemporalMemory = true`
- WHEN `stateFor` es invocada
- THEN el estado resultante es `Inquebrantable`

#### Scenario: Inquebrantable rechazado por worstLayerScore bajo el mínimo

- GIVEN `weeklyBaseScore = 0.92`, `stabilityScore = 0.91`, `hasTemporalMemory = true`
- AND `worstLayerScore = 0.79` (< 0.80)
- WHEN `stateFor` es invocada
- THEN el estado resultante es `Plenitud`

---

### Requirement: State Hysteresis

The policy MUST suppress downward state transitions when `weeklyBaseScore` falls within `stateHysteresisMargin = 0.03` below the lower boundary of the current (previous) state band.

Hysteresis rules:
- MUST NOT lower the state label when: `lowerBoundary - weeklyBaseScore ≤ 0.03`
- MUST lower the state label when: `lowerBoundary - weeklyBaseScore > 0.03`
- The margin applies ONLY to the resolved state label.
- `weeklyBaseScore`, `visibleScore`, and `reasons` MUST be exposed raw and MUST NOT be altered by hysteresis.
- Hysteresis only prevents downward movement; it MUST NOT prevent upward transitions.

#### Scenario: Score cae dentro del margen → estado previo se mantiene

- GIVEN estado previo = `En marcha` (límite inferior del band = 0.70)
- AND `weeklyBaseScore = 0.69` (a 0.01 del límite, dentro de 0.03)
- WHEN `stateFor` es invocada
- THEN el estado resultante es `En marcha` (histéresis activa)
- AND `weeklyBaseScore` expuesto = 0.69 (sin alterar)

#### Scenario: Score cae por debajo del margen → descenso de estado

- GIVEN estado previo = `En marcha` (límite inferior = 0.70)
- AND `weeklyBaseScore = 0.66` (a 0.04 del límite, fuera del margen 0.03)
- WHEN `stateFor` es invocada
- THEN el estado resultante es `Atención`

#### Scenario: Histéresis no impide ascenso de estado

- GIVEN estado previo = `Atención`
- AND `weeklyBaseScore = 0.71` (supera el límite de En marcha)
- WHEN `stateFor` es invocada
- THEN el estado resultante es `En marcha` (sin bloqueo)

#### Scenario: Histéresis no altera visibleScore ni reasons

- GIVEN estado previo = `En marcha`, `weeklyBaseScore = 0.69`
- WHEN `stateFor` es invocada y la histéresis mantiene el estado en `En marcha`
- THEN el `weeklyBaseScore` publicado = 0.69
- AND `reasons` refleja el score real, sin mención a histéresis

---

### Requirement: Sin Datos (NoData State)

When no base configuration exists or there are insufficient facts to compute `weeklyBaseScore`, the policy MUST return state `SinDatos` and MUST NOT expose a visible score.

#### Scenario: Sin configuración base → Sin datos

- GIVEN no existe configuración base activa para la semana
- WHEN `stateFor` es invocada
- THEN el estado resultante es `SinDatos`
- AND no se expone `visibleScore`

---

### Requirement: Single Source of Truth

`BaseStatePolicy` MUST be the sole domain component that resolves `VocalState` from scoring inputs. `VisibleScorePolicy.stateFor()` MUST NOT exist in the codebase; its removal is a hard requirement of this change.

#### Scenario: Eliminación de VisibleScorePolicy.stateFor

- GIVEN que `VisibleScorePolicy.stateFor()` existía como función paralela
- WHEN se aplica el cambio slice 1
- THEN `VisibleScorePolicy.stateFor()` ya no existe en el código fuente
- AND ningún caller en `main` ni en `test` hace referencia a ella

---

### Requirement: Constants Extracted (no-magic-numbers)

All threshold values used by `BaseStatePolicy` MUST reside in `ScoringConstants`. No literal threshold values (0.40, 0.70, 0.85, 0.90, 0.30, 0.80, 0.03) SHALL appear inline in `BaseStatePolicy.kt`.

#### Scenario: Constantes centralizadas

- GIVEN `ScoringConstants` contiene las constantes `BAND_RESTORATION`, `BAND_ATTENTION`, `BAND_MOTION`, `BAND_PLENITUDE`, `WORST_LAYER_COLLAPSE`, `WORST_LAYER_MIN_UNBREAKABLE`, `STATE_HYSTERESIS_MARGIN`, `STABILITY_MIN_UNBREAKABLE`
- WHEN se lee `BaseStatePolicy.kt`
- THEN no aparece ningún literal numérico de umbral; todas las referencias usan los nombres de `ScoringConstants`

---

### Requirement: D2 — Asymmetry Documentation (non-functional)

The `rawScore`/`baseScore` asymmetry in `WeeklyScorePolicy` MUST be documented as an intentional anti-compensation design decision in `docs/arbol-scoring-vocal-v1.md` and `docs/plan-tecnico-scoring-vocal.md` §7.1. No code change to `WeeklyScorePolicy` is required or permitted in this slice.

#### Scenario: Asimetría ratificada en documentos canónicos

- GIVEN los docs canónicos antes del cambio no marcan la asimetría como intencional
- WHEN se aplica el cambio slice 1
- THEN `docs/plan-tecnico-scoring-vocal.md` §7.1 contiene una nota explícita ratificando la regla "superávit no compensa capas caídas" como decisión sellada
- AND `docs/arbol-scoring-vocal-v1.md` §16 refleja los umbrales como valores finales (no "propuesta a discutir")

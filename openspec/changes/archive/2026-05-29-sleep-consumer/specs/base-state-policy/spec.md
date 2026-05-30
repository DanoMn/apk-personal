# Base State Policy Specification

Source: `docs/decisiones-diseno-sueno-v1.md` §4.2 · Sleep Consumer Proposal Modified Capabilities

## Purpose

Defines how the base state policy handles missing sleep data and integrates sleep scoring into body layer calculation. This spec was modified by the `sleep-consumer` change to fix the NoData handling bug.

---

## Requirements

### Requirement: Sin Datos (NoData State)

When no base configuration exists or there are insufficient facts to compute `weeklyBaseScore`, the policy MUST return state `SinDatos` and MUST NOT expose a visible score.

When sleep data for the week is absent (`SleepWeeklyScore` is `NO_DATA`), the policy MUST treat this as an incomplete base, not as a zero-valued sleep score. Absence of sleep data MUST NOT lower `BodyScore` nor artificially produce a `Restauración` or `Atención` state. Sleep absence = base incomplete; scoring behavior is the same as `SinDatos` for the sleep component only.

(Previously: NoData state was defined only for missing base configuration. Sleep absence was not explicitly covered, and `null` sleep score was silently coerced to `0f`, incorrectly penalizing `BodyScore`.)

#### Scenario: Sin configuración base → Sin datos

- GIVEN no existe configuración base activa para la semana
- WHEN `stateFor` es invocada
- THEN el estado resultante es `SinDatos`
- AND no se expone `visibleScore`

#### Scenario: Noche NoData no produce 0f ni hunde Cuerpo

- GIVEN `SleepWeeklyScore = NO_DATA` (sin noches con dato en la semana)
- WHEN `BodyScore` es computado
- THEN `SleepWeeklyScore` no entra como `0f` en la fórmula de Cuerpo
- AND `BodyScore` refleja solo `BodyBaseWithoutSleep` (o la base incompleta se nota, pero no como piso fabricado)

#### Scenario: Pocas noches → techo, no piso fabricado

- GIVEN only 1 night with `sleepScore = 0.9`, rest of week `NO_DATA`
- WHEN scoring engine evaluates `BodyScore` and state
- THEN the result does NOT enable `Plenitud` or `Inquebrantable` state thresholds
- AND the result does NOT produce a state lower than what `BodyBaseWithoutSleep` alone would yield
- AND no fabricated `0f` score appears in the pipeline

#### Scenario: Sueño ausente vs sueño malo son distintos

- GIVEN `SleepWeeklyScore = NO_DATA` (sin señal) for week A
- AND `SleepWeeklyScore = 0.2` (ambiguous, poor sleep detected) for week B
- WHEN `BodyScore` is computed for each week
- THEN week A does NOT produce a `BodyScore` lower than week B purely due to absence
- AND the distinction between "no evidence" and "evidence of poor sleep" is preserved

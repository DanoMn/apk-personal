# Sleep Scoring v1 Specification

Change: `sleep-consumer`
Source: `docs/arbol-scoring-vocal-v1.md` §11 · `docs/decisiones-diseno-sueno-v1.md` §5, §8

## Purpose

Defines the four sealed scoring components derived from `SleepSegmentEntity` rows, the weekly aggregation rule, and how sleep integrates into the Body layer score. All formulas are sealed and MUST NOT be renegotiated.

---

## Requirements

### Requirement: Four Sealed Components

`SleepWeeklyScore` MUST be computed from exactly four components with the following sealed weights:

```
SleepWeeklyScore =
    0.40 · DurationScore
  + 0.25 · ContinuityScore
  + 0.20 · ScheduleAlignmentScore
  + 0.15 · DigitalInterruptionScore
```

No other weights, no additional components, and no removal of components are permitted in v1.

#### Scenario: Pesos sellados aplicados correctamente

- GIVEN `DurationScore=0.8`, `ContinuityScore=0.6`, `ScheduleAlignmentScore=0.7`, `DigitalInterruptionScore=1.0`
- WHEN `SleepWeeklyScore` is computed
- THEN `SleepWeeklyScore = 0.40·0.8 + 0.25·0.6 + 0.20·0.7 + 0.15·1.0 = 0.32 + 0.15 + 0.14 + 0.15 = 0.76`

---

### Requirement: Duration Score — No Over-Sleep Penalty

`DurationScore` MUST be `clamp(actualSleepMinutes / targetSleepMinutes, 0.0, 1.0)`. Sleeping MORE than the target MUST NOT reduce the score below 1.0. The duration "vase" fills to 1.0 and stays; there is no decay for over-sleeping.

#### Scenario: Dormir de más → neutro, no penaliza

- GIVEN `targetSleepMinutes = 360` (6h), `actualSleepMinutes = 480` (8h)
- WHEN `DurationScore` is computed
- THEN `DurationScore = 1.0` (capped, not decayed)

#### Scenario: Duración exacta → 1.0

- GIVEN `targetSleepMinutes = 360`, `actualSleepMinutes = 360`
- WHEN `DurationScore` is computed
- THEN `DurationScore = 1.0`

#### Scenario: Duración parcial → proporcional

- GIVEN `targetSleepMinutes = 360`, `actualSleepMinutes = 270` (4.5h)
- WHEN `DurationScore` is computed
- THEN `DurationScore = 0.75`

---

### Requirement: Continuity Score Derived from Segments

`ContinuityScore` MUST be derived from `SleepSegmentEntity` rows: it considers the count of `AwakeUse` episodes and the duration of the longest `Asleep` segment. A night with zero `AwakeUse` episodes and one long `Asleep` block MUST yield `ContinuityScore = 1.0`.

#### Scenario: Sin despertares reales → continuidad perfecta

- GIVEN a night with only `Asleep` segments (zero `AwakeUse` segments)
- WHEN `ContinuityScore` is computed
- THEN `ContinuityScore = 1.0`

#### Scenario: Noche fragmentada con N segmentos reduce continuidad

- GIVEN a night with three `AwakeUse` segments of varying duration interspersed with `Asleep` blocks
- WHEN `ContinuityScore` is computed
- THEN `ContinuityScore < 1.0`
- AND score decreases as number/duration of `AwakeUse` episodes increases

---

### Requirement: Schedule Alignment Score

`ScheduleAlignmentScore` MUST measure closeness of `sleepOnsetAt` and `definitiveWakeAt` to the goal window (`targetSleepAt`–`targetWakeAt`). Sleeping outside the goal window MUST only reduce the 20% alignment component; it MUST NOT affect `DurationScore`, `ContinuityScore`, or `DigitalInterruptionScore`.

#### Scenario: Dormir fuera del objetivo baja solo Alineación

- GIVEN goal window `00:00`–`06:00`, actual sleep `03:00`–`09:00`
- WHEN all four components are computed
- THEN `ScheduleAlignmentScore` is reduced (off-target)
- AND `DurationScore` reflects the actual 6h sleep and is NOT reduced by misalignment

#### Scenario: Dormir dentro del objetivo → alineación perfecta o alta

- GIVEN goal window `23:00`–`06:00`, actual `sleepOnsetAt = 23:00`, `definitiveWakeAt = 06:00`
- WHEN `ScheduleAlignmentScore` is computed
- THEN `ScheduleAlignmentScore = 1.0`

---

### Requirement: Digital Interruption Score Derived from AwakeUse Only

`DigitalInterruptionScore` MUST be computed from the total duration and count of `AwakeUse` segments during the night. `digitalWindDownMinutes` MUST remain inert (D3 deferred); it is a visual config reminder only and MUST NOT affect scoring in v1.

#### Scenario: Sin uso nocturno → sin penalización digital

- GIVEN zero `AwakeUse` segments in the night
- WHEN `DigitalInterruptionScore` is computed
- THEN `DigitalInterruptionScore = 1.0`

#### Scenario: digitalWindDownMinutes no afecta score

- GIVEN `digitalWindDownMinutes = 30` configured
- WHEN `DigitalInterruptionScore` is computed
- THEN the value is identical to what it would be if `digitalWindDownMinutes` were 0

---

### Requirement: Body Layer Integration

`BodyScore` MUST integrate `SleepWeeklyScore` at weight 0.30 using the sealed formula:

```
BodyScore = 0.70 · BodyBaseWithoutSleep + 0.30 · SleepWeeklyScore
```

The scoring engine MUST NOT read raw telemetry. It MUST only receive the pre-computed `SleepWeeklyScore` value.

#### Scenario: Sueño entra a Cuerpo al 30%

- GIVEN `BodyBaseWithoutSleep = 0.80`, `SleepWeeklyScore = 0.60`
- WHEN `BodyScore` is computed
- THEN `BodyScore = 0.70·0.80 + 0.30·0.60 = 0.56 + 0.18 = 0.74`

#### Scenario: Motor de scoring no toca telemetría cruda

- GIVEN the scoring engine's `ScoreEngine` / `SpecialLayerScoringPolicy`
- WHEN `BodyScore` is computed for a week
- THEN no code path in the scoring engine reads `DeviceActivityEvent` rows directly

---

### Requirement: Weekly Aggregation by Average of Nights With Data

`SleepWeeklyScore` MUST be the arithmetic average of `sleepScore` values from nights that have data (confidence `HIGH` or `AMBIGUOUS`) in the week. Nights with `NO_DATA` confidence MUST NOT enter the average as zero.

#### Scenario: Promedio solo de noches con dato

- GIVEN a week with 5 nights: scores `[0.8, 0.7, NO_DATA, 0.9, NO_DATA]`
- WHEN `SleepWeeklyScore` is computed
- THEN `SleepWeeklyScore = (0.8 + 0.7 + 0.9) / 3 = 0.80`

#### Scenario: Noche NoData no entra como cero

- GIVEN a week with 6 nights: 3 with scores `[1.0, 1.0, 1.0]` and 3 with `NO_DATA`
- WHEN `SleepWeeklyScore` is computed
- THEN `SleepWeeklyScore = 1.0` (NOT `(1.0+1.0+1.0+0+0+0)/6 = 0.5`)

#### Scenario: Semana sin ninguna noche con dato

- GIVEN all 7 nights in the week have `NO_DATA`
- WHEN `SleepWeeklyScore` is queried
- THEN no weekly score is produced (base incomplete — NOT `0f`)

#### Scenario: Pocas noches con dato → lectura débil (techo, no piso)

- GIVEN only 1 night with score `0.9` in the week, the rest `NO_DATA`
- WHEN the scoring engine evaluates `BodyScore`
- THEN the few-data-nights condition is noted as a weak reading
- AND it does NOT allow `BodyScore` to reach `Plenitud` state thresholds solely on 1 night

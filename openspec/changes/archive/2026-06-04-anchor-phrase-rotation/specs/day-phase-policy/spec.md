# Day Phase Policy Specification

Source: `docs/dominio/frases-ancla.md` §7 · anchor-phrase-rotation proposal §2

## Purpose

Defines how the system classifies any local clock time into one of two day phases
(`Dawn` or `Dusk`) without geolocation. The policy is pure domain logic: no Room,
no I/O, no suspend.

---

## Requirements

### Requirement: Dawn Window

The system MUST classify local time 05:00–14:59 (inclusive) as `DayPhase.Dawn`.

#### Scenario: Time inside Dawn window

- GIVEN local hour is any value in [05, 14]
- WHEN `DayPhasePolicy.phaseFor(localDateTime)` is called
- THEN the returned phase is `Dawn`

#### Scenario: Boundary at 14:59 is Dawn

- GIVEN local time is exactly 14:59
- WHEN `DayPhasePolicy.phaseFor(localDateTime)` is called
- THEN the returned phase is `Dawn`

---

### Requirement: Dusk Window

The system MUST classify local time 15:00–04:59 (i.e., 15:00 to midnight and
00:00–04:59 the next morning) as `DayPhase.Dusk`. No geolocation is used.

#### Scenario: Boundary at 15:00 is Dusk

- GIVEN local time is exactly 15:00
- WHEN `DayPhasePolicy.phaseFor(localDateTime)` is called
- THEN the returned phase is `Dusk`

#### Scenario: Time in late-night Dusk window

- GIVEN local hour is any value in [00, 04]
- WHEN `DayPhasePolicy.phaseFor(localDateTime)` is called
- THEN the returned phase is `Dusk`

#### Scenario: Time in evening Dusk window

- GIVEN local hour is any value in [15, 23]
- WHEN `DayPhasePolicy.phaseFor(localDateTime)` is called
- THEN the returned phase is `Dusk`

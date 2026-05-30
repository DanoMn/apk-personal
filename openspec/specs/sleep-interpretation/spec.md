# Sleep Interpretation Specification

Change: `sleep-consumer`
Source: `docs/decisiones-diseno-sueno-v1.md` §3, §4 · `proposal.md` Capabilities

## Purpose

`SleepInterpreter` converts a `List<DeviceActivityEvent>` from the detection window (`20:00`–`12:00`) into the night's segment timeline (`Asleep` / `AwakeUse`) plus a confidence level. This is pure JVM domain logic; it does not touch Room or Compose.

---

## Requirements

### Requirement: Detection Window Boundary

The interpreter MUST only process events within the fixed biological detection window `20:00`–`12:00` of the next calendar day. Events outside this window MUST be discarded before interpretation begins.

#### Scenario: Events outside the window are discarded

- GIVEN a list of events that includes entries at `13:00` (afternoon) and `22:00` (evening)
- WHEN the interpreter processes the list
- THEN only the `22:00` event enters interpretation; the `13:00` event is discarded

---

### Requirement: Real Wakeup vs Glance Discrimination

A real wakeup MUST be triggered only by `USER_INTERACTION` or `APP_FOREGROUND` events. A `SCREEN_ON` event that is NOT followed by `USER_INTERACTION` or `APP_FOREGROUND` within a configurable proximity threshold MUST be classified as a glance and MUST be ignored (does not break sleep, does not create an `AwakeUse` segment).

#### Scenario: SCREEN_ON sola es vistazo — se ignora

- GIVEN a `SCREEN_ON` event at `02:30` with no subsequent `USER_INTERACTION`/`APP_FOREGROUND` within the threshold
- WHEN the interpreter classifies this event
- THEN no `AwakeUse` segment is created at `02:30`
- AND the surrounding `Asleep` segment remains unbroken

#### Scenario: USER_INTERACTION crea despertar real

- GIVEN a `USER_INTERACTION` event at `03:15`
- WHEN the interpreter classifies this event
- THEN an `AwakeUse` segment begins at `03:15`

#### Scenario: APP_FOREGROUND cuenta como uso real

- GIVEN an `APP_FOREGROUND` event at `04:00` with no prior `SCREEN_ON`
- WHEN the interpreter classifies this event
- THEN an `AwakeUse` segment begins at `04:00`

---

### Requirement: Sleep Onset via Last Real Use

The sleep onset time (`sleepOnsetAt`) MUST be set to the point of quiet after the last `USER_INTERACTION`/`APP_FOREGROUND` event before the main sleep block. The interpreter MUST NOT use a button press or a `SCREEN_ON` event as the sleep onset anchor.

#### Scenario: Inicio de sueño = quietud tras último uso real

- GIVEN real-use events ending at `23:45`, then silence through `06:30`
- WHEN the interpreter determines `sleepOnsetAt`
- THEN `sleepOnsetAt = 23:45` (or the quiet point following the last use tanda)

#### Scenario: Detox activity does not count as sleep onset anchor

- GIVEN `APP_FOREGROUND` events during a digital wind-down phase ending at `23:50`, followed by silence
- WHEN the interpreter determines `sleepOnsetAt`
- THEN `sleepOnsetAt` is set after `23:50`, not before

---

### Requirement: Real-Use Episode Grouping

Consecutive `USER_INTERACTION`/`APP_FOREGROUND` events that are close together in time MUST be grouped into a single `AwakeUse` episode. The episode ends when the device becomes quiet again. One contiguous tanda of real use = one `AwakeUse` segment.

#### Scenario: Tanda de eventos agrupados en un solo AwakeUse

- GIVEN `USER_INTERACTION` at `02:00`, `APP_FOREGROUND` at `02:03`, `USER_INTERACTION` at `02:07`, then silence
- WHEN the interpreter groups episodes
- THEN exactly ONE `AwakeUse` segment is produced covering `02:00`–`02:07`

---

### Requirement: Siesta Disambiguation via Goal Anchor

A sleep block within the detection window MUST only be treated as the night's main sleep block if it overlaps or is temporally close to the user's configured goal window (`targetSleepAt`–`targetWakeAt`). An isolated block far from the goal window (e.g., a 20:00 nap when the goal is midnight) MUST NOT be counted as the main night.

#### Scenario: Siesta lejos del objetivo no cuenta como noche

- GIVEN a sleep block `20:30`–`22:00` and a goal window `00:00`–`05:00`
- WHEN the interpreter selects the main sleep block
- THEN the `20:30`–`22:00` block is classified as a nap and excluded from the night's segments

---

### Requirement: Night Belongs to Wake Day

The interpreter MUST assign the night to the calendar date of the definitive wakeup (`definitiveWakeAt`), not the date the sleep started. A night spanning Monday `23:00` to Tuesday `06:30` is "Tuesday's sleep".

#### Scenario: Noche pertenece al día de despertar

- GIVEN sleep onset at `2026-06-02 23:00`, definitive wake at `2026-06-03 06:30`
- WHEN the interpreter assigns the night to a date
- THEN the result is keyed to `2026-06-03`

---

### Requirement: Confidence Spectrum

The interpreter MUST assign one of three confidence levels based on signal quality:

| Situation | Confidence |
|-----------|------------|
| Device quiet during biological window (clean silence = signature of good sleep) | `HIGH` |
| Genuinely ambiguous or contradictory signal | `AMBIGUOUS` |
| No signal (device off, telemetry absent) | `NO_DATA` |

`NO_DATA` MUST NOT be treated as low performance. `NO_DATA` means absence of evidence, not evidence of poor sleep. A night with little signal (phone quiet) MUST resolve to `HIGH`, not `AMBIGUOUS`.

#### Scenario: Teléfono quieto → ALTA confianza

- GIVEN zero `USER_INTERACTION`/`APP_FOREGROUND` events in the biological window, with minimal/no `SCREEN_ON`
- WHEN the interpreter assigns confidence
- THEN `confidence = HIGH`

#### Scenario: Sin señal → NoData, nunca bajo score fabricado

- GIVEN no telemetry events at all for the biological window
- WHEN the interpreter assigns confidence
- THEN `confidence = NO_DATA`
- AND the interpreter produces NO segments and NO score value (not `0f`)

#### Scenario: Señal contradictoria → AMBIGUOUS

- GIVEN events that alternate `SCREEN_ON` and brief `USER_INTERACTION` throughout the night with no clear quiet block
- WHEN the interpreter assigns confidence
- THEN `confidence = AMBIGUOUS`

---

### Requirement: API 26/27 Signal Proxy Tolerance

On API 26/27 where `SCREEN_INTERACTIVE` / `SCREEN_NON_INTERACTIVE` may be absent, the interpreter MUST fall back to treating `APP_FOREGROUND`/`USER_INTERACTION` as the primary real-use indicators. Absence of `SCREEN_*` events on older APIs MUST NOT force `AMBIGUOUS` confidence.

#### Scenario: API 26 sin SCREEN events — proxy suficiente

- GIVEN API level 26, no `SCREEN_*` events, but `APP_FOREGROUND` and `USER_INTERACTION` present
- WHEN the interpreter classifies events
- THEN segments and confidence are derived from the proxy signals without downgrading to `AMBIGUOUS`

# Sleep Night Model Specification

Change: `sleep-consumer`
Source: `docs/decisiones-diseno-sueno-v1.md` §6, §2 · `proposal.md` Capabilities

## Purpose

Defines the durable data model for a night: a header entity (evolution of `SleepLog`) keyed to the wake date, plus `SleepSegmentEntity` as the primary durable fact. Specifies Room schema version and migration discipline.

---

## Requirements

### Requirement: Night Header Entity

The night header MUST store: `targetSleepAt`, `targetWakeAt` (goal window), `sleepOnsetAt`, `definitiveWakeAt`, confidence level (`HIGH` / `AMBIGUOUS` / `NO_DATA`), and an optional `note`. The `quality` field hardcoded to `Acceptable` MUST be removed. The primary key MUST be the wake date (date of `definitiveWakeAt`).

#### Scenario: Cabecera de noche creada con datos mínimos

- GIVEN a night with `sleepOnsetAt = 2026-06-02 23:45`, `definitiveWakeAt = 2026-06-03 06:30`, confidence `HIGH`
- WHEN the header entity is persisted
- THEN it is keyed to `2026-06-03` (wake date)
- AND `quality` field does not exist

#### Scenario: Campo quality eliminado

- GIVEN the old `SleepLog` entity that had `quality = Acceptable`
- WHEN the migration v11→v12 is applied
- THEN the `quality` column no longer exists in the schema
- AND existing rows are migrated without data loss in retained fields

---

### Requirement: Sleep Segment Entity

A separate `SleepSegmentEntity` table MUST exist as a child of the night header. Each row MUST have: `startAt`, `endAt`, `kind` (`Asleep` | `AwakeUse`), and a foreign key to the night (wake date). The table represents the complete timeline of the night.

#### Scenario: Segmentos escritos como hecho primario durable

- GIVEN a night with two `Asleep` blocks and one `AwakeUse` block in between
- WHEN the night is closed
- THEN three `SleepSegmentEntity` rows are persisted for that night's wake date

#### Scenario: Segmentos son HECHO primario, no cache descartable

- GIVEN segments persisted for a night
- WHEN raw telemetry for that window is purged
- THEN `SleepSegmentEntity` rows for that night still exist and can be used to recalculate scores

---

### Requirement: Room Migration v11→v12 Discipline

The schema migration from v11 to v12 MUST be implemented as a numbered `Migration(11, 12)` with:
- Index naming convention `index_<table>_<column>` (NOT `idx_*`)
- `MigrationTestHelper` test coverage
- `exportSchema = true` (JSON schema snapshot committed)

The migration MUST be validated with `MigrationTestHelper` before the change is considered complete. A migration that passes domain unit tests but fails on a real device install is not acceptable.

#### Scenario: Índice con nombre correcto

- GIVEN the `SleepSegmentEntity` table needs an index on `nightDate` (FK to header)
- WHEN the migration DDL is written
- THEN the index is named `index_sleep_segments_nightDate` (not `idx_sleep_segments_nightDate`)

#### Scenario: MigrationTestHelper valida v11→v12

- GIVEN a v11 database with existing `SleepLog` rows
- WHEN `Migration(11, 12)` is applied via `MigrationTestHelper`
- THEN the migration succeeds without exception
- AND `SleepSegmentEntity` table exists in the resulting schema

#### Scenario: exportSchema produce snapshot

- GIVEN `exportSchema = true` in `AutonomiaDatabase`
- WHEN the project is built after migration v11→v12 is added
- THEN a JSON schema file for version 12 is generated and committed to the repo

---

### Requirement: Objective Configuration Minimum

The user's sleep goal window (`targetSleepAt`–`targetWakeAt`) MUST span at least 5 hours. The system MUST NOT penalize the user for choosing a 5h, 6h, 7h, or 8h window; scoring reads fulfillment of the configured window, not absolute duration against a fixed target.

#### Scenario: Ventana mínima de 5h aceptada

- GIVEN user configures `targetSleepAt = 01:00`, `targetWakeAt = 06:00` (5h window)
- WHEN the configuration is saved
- THEN it is accepted without error or warning

#### Scenario: Ventana menor a 5h rechazada

- GIVEN user attempts to configure `targetSleepAt = 01:00`, `targetWakeAt = 04:30` (3.5h window)
- WHEN the configuration is validated
- THEN validation fails with an error indicating the minimum window is 5 hours

#### Scenario: Elegir 6h no penaliza vs elegir 8h

- GIVEN user A configures a 6h window and user B configures an 8h window
- WHEN each user fully sleeps their configured window
- THEN both `DurationScore = 1.0` for their respective nights

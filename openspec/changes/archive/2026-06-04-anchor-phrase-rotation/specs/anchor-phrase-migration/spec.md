# Anchor Phrase Migration Specification

> **ESTADO: INVÁLIDA — no mergeada a main specs.**
> La migración v12→v13 fue REVERTIDA por decisión "Camino A":
> en fase dev, DB descartable, sin migraciones Room manuales.
> La DB permanece en v12. Las 5 tablas `anchor_phrase*` existen
> en el esquema v12 y se crean en instalación limpia.
> Este spec se conserva en el archivo como registro histórico únicamente.

Source: anchor-phrase-rotation proposal §1 (bug latente) · `CLAUDE.md` (index naming rule)

## Purpose

Fixes the latent release bug: the 5 `anchor_phrase*` tables are registered in
Room DB schema v12 but no migration creates them. Any device that migrated from
v11 to v12 will crash on schema validation. This spec covers the migration and
its test coverage.

---

## Requirements

### Requirement: Migration Creates All Five Tables

A numbered Room migration (version boundary TBD during design, within the v12+
sequence) MUST create all five tables: `anchor_phrases`, `anchor_phrase_state_rules`,
`anchor_phrase_phase_rules`, `anchor_phrase_impressions`, `anchor_phrase_daily_slots`.
The migration MUST be listed in `AutonomiaDatabase`'s migration set.

#### Scenario: Migration executes on a v11→vN path

- GIVEN a Room database at schema version 11
- WHEN all registered migrations are applied in order up to the target version
- THEN all five `anchor_phrase*` tables exist in the resulting schema

---

### Requirement: Index Names Match Room-Generated Names

All indices created by the migration SQL MUST use the naming pattern
`index_<table>_<column>` to match what Room generates from `@Index(...)` annotations
on entities. Mismatched names cause Room schema validation to fail at startup.

#### Scenario: Index names are canonical

- GIVEN the migration has been applied
- WHEN Room validates the schema at startup
- THEN no `IllegalStateException` is thrown due to index name mismatch

---

### Requirement: MigrationTestHelper Coverage

The migration MUST be covered by at least one `MigrationTestHelper` test that:
(a) opens the database at the pre-migration version,
(b) runs migrations to the target version,
(c) validates the final schema matches the expected Room schema.

#### Scenario: MigrationTestHelper passes

- GIVEN the database is opened at the version before anchor-phrase tables were added
- WHEN `MigrationTestHelper.runMigrationsAndValidate(...)` is called
- THEN the test passes without exception
- AND all five tables are present with correct column types and index names

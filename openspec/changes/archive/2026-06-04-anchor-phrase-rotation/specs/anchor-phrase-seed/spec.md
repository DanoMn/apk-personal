# Anchor Phrase Seed Specification

Source: `docs/dominio/frases-ancla.md` §13, §15 · anchor-phrase-rotation proposal §2, §3.5

## Purpose

Defines the invariants that the canonical seed (`AnchorPhraseSeed.kt`) must satisfy
after `ensureSeeded` runs. These invariants are verifiable by a JUnit4 JVM test
without Room (by inspecting the in-memory seed objects directly).

---

## Requirements

### Requirement: 83 Active Phrases

The seed MUST declare exactly 83 phrases with `active = true`, distributed across
the 7 families as specified in `frases-ancla.md §15`:
Containment(12), MinimalAction(14), RegulationClarity(14), Persistence(10),
IdentityValues(10), Recognition(10), Contemplation(13).

#### Scenario: Seed count matches catalog

- GIVEN `AnchorPhraseSeed.phrases` is loaded
- WHEN active phrases are counted per family
- THEN `Containment = 12`, `MinimalAction = 14`, `RegulationClarity = 14`,
  `Persistence = 10`, `IdentityValues = 10`, `Recognition = 10`,
  `Contemplation = 13`
- AND total active = 83

---

### Requirement: No Empty authorReference on Active Phrases

Every phrase with `active = true` MUST have a non-empty `authorReference`.
This invariant is unconditional: there are no exceptions in v1.

#### Scenario: All active phrases have attribution

- GIVEN `AnchorPhraseSeed.phrases` is loaded
- WHEN all active phrases are inspected
- THEN every phrase has `authorReference.isNotBlank() == true`

---

### Requirement: State Rules Derived from Family-Weight Maps

State rules for `anchor_phrase_state_rules` MUST be derived programmatically from
a `stateWeights: Map<ScoreState, Map<PhraseFamily, Int>>` map — not hand-authored
per phrase. Each active phrase generates one state-rule row per (state, family)
entry in the map where the phrase's family appears. The resulting weights MUST match
`frases-ancla.md §9` (state weights table).

#### Scenario: State rule rows match map expectations

- GIVEN `AnchorPhraseSeed.stateRules` is loaded
- WHEN rules for a `MinimalAction` phrase under `Restoration` are inspected
- THEN the weight equals 3 (as per §9 Restoration→MinimalAction)

#### Scenario: No hand-authored rule rows for unsupported families

- GIVEN `AnchorPhraseSeed.stateRules` is loaded
- WHEN rules for a `Containment` phrase under `Unbreakable` are searched
- THEN no rule row exists (Containment is excluded from Unbreakable in §6)

---

### Requirement: Phase Rules Derived from Family-Weight Maps

Phase rules for `anchor_phrase_phase_rules` MUST be derived from a
`phaseWeights: Map<DayPhase, Map<PhraseFamily, Int>>` map. Only families listed
in the map generate phase-rule rows. Weights MUST match `frases-ancla.md §9`
(phase weights table).

#### Scenario: Phase weight row for Dawn MinimalAction

- GIVEN `AnchorPhraseSeed.phaseRules` is loaded
- WHEN phase rules for a `MinimalAction` phrase under `Dawn` are inspected
- THEN the weight equals 2 (§9 Dawn→MinimalAction +2)

#### Scenario: No phase-rule row for family absent from phase map

- GIVEN `AnchorPhraseSeed.phaseRules` is loaded
- AND `Persistence` is not in the `Dusk` phase map
- WHEN phase rules for a `Persistence` phrase under `Dusk` are searched
- THEN no row exists

---

### Requirement: Idempotent Upsert

Calling `ensureSeeded` multiple times MUST produce the same database state as
calling it once. Phrase rows already present MUST be replaced (not duplicated) via
`@Insert(onConflict = REPLACE)`.

#### Scenario: Double seed call — no duplicates

- GIVEN `ensureSeeded` has already been called once
- WHEN `ensureSeeded` is called again
- THEN `anchor_phrases` still contains exactly 83 rows
- AND no duplicate `phraseId` exists

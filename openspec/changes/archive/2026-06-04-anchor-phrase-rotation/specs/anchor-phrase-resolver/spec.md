# Anchor Phrase Resolver Specification

Source: `docs/dominio/frases-ancla.md` §8 · anchor-phrase-rotation proposal §2, §3

## Purpose

Defines the data-layer coordinator that enforces phase stability, reads the current
score state from the weekly snapshot, delegates selection to the pure selector, and
persists the daily slot and impression atomically. Only this component may write to
`anchor_phrase_daily_slots` and `anchor_phrase_impressions`.

---

## Requirements

### Requirement: Phase Stability (Reuse Existing Slot)

When a valid slot already exists for `(today, dayPhase)` AND its `scoreState`
matches the current score state, the resolver MUST reuse the slot and MUST NOT
write a new impression or overwrite the slot.

#### Scenario: Same phase, same state — slot reused

- GIVEN a slot exists for `(today, Dawn)` with `scoreState = Motion`
- AND the current score state is `Motion`
- WHEN the resolver is called
- THEN no new impression is written
- AND the existing slot is returned unchanged

---

### Requirement: Re-Selection on State Change

When a slot exists for `(today, dayPhase)` but its `scoreState` differs from the
current score state, the resolver MUST discard the cached slot and select a new
phrase appropriate for the current state.

#### Scenario: State changed within phase — new phrase selected

- GIVEN a slot exists for `(today, Dawn)` with `scoreState = Attention`
- AND the current score state is `Motion`
- WHEN the resolver is called
- THEN the selector is invoked with `scoreState = Motion`
- AND a new slot and impression are persisted atomically

---

### Requirement: Atomic Persistence

When a new phrase is selected, the resolver MUST write the daily slot and the
impression record in a single `@Transaction`. Partial writes are not acceptable.

#### Scenario: Slot and impression written together

- GIVEN no slot exists for `(today, Dawn)`
- AND the selector returns a valid selection
- WHEN the resolver is called
- THEN exactly one slot row is upserted for `(today, Dawn)`
- AND exactly one impression row is inserted for the same selection
- AND both writes succeed or both fail (atomicity)

---

### Requirement: Score State Source

The resolver MUST read `scoreState` from the current week's
`WeeklyScoreSnapshotEntity.state` (already refreshed by `refreshCurrentWeeklyScoreSnapshot`
before the resolver is called). The resolver MUST NOT recalculate score from raw
daily facts.

#### Scenario: Resolver reads snapshot state, not raw facts

- GIVEN `WeeklyScoreSnapshotEntity.state = Plenitude` for the current week
- WHEN the resolver determines which state to use
- THEN it uses `Plenitude` without invoking `ScoreEngine`

---

### Requirement: Graceful No-Op on Null Selection

When the selector returns `null` (no eligible phrase), the resolver MUST return
without writing anything to the database. The slot for that phase remains absent.

#### Scenario: Selector returns null — no write

- GIVEN the catalog is empty or all phrases are filtered out
- WHEN the resolver is called
- THEN no slot is upserted
- AND no impression is inserted

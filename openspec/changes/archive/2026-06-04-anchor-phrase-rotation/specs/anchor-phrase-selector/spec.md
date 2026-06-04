# Anchor Phrase Selector Specification

Source: `docs/dominio/frases-ancla.md` §6, §8, §9 · anchor-phrase-rotation proposal §2

## Purpose

Defines the pure, deterministic domain function that picks one anchor phrase given
a snapshot of inputs. No Room access, no I/O, no suspend. Every rule is independently
testable as a JUnit4 JVM test.

---

## Requirements

### Requirement: Eligibility Filter

The selector MUST consider only phrases where `active == true` AND
`authorReference` is non-empty. Phrases failing either condition MUST be excluded
before any other rule is applied.

#### Scenario: Inactive phrase excluded

- GIVEN a catalog containing a phrase with `active = false`
- WHEN the selector runs
- THEN that phrase is not a candidate for selection

#### Scenario: Phrase with empty authorReference excluded

- GIVEN a catalog containing a phrase with `active = true` and `authorReference = ""`
- WHEN the selector runs
- THEN that phrase is not a candidate for selection

---

### Requirement: State Family Filter

The selector MUST restrict candidates to families allowed for the current `scoreState`
according to the mapping in `frases-ancla.md §6`. `Contemplation` MUST NOT appear
for any state other than `Plenitude` or `Unbreakable` (§8.6).

#### Scenario: Contemplation excluded below Plenitude

- GIVEN `scoreState` is one of `NoData`, `Restoration`, `Attention`, or `Motion`
- AND the eligible catalog contains at least one phrase with `family = Contemplation`
- WHEN the selector runs
- THEN no `Contemplation` phrase is selected

#### Scenario: Contemplation allowed in Plenitude

- GIVEN `scoreState = Plenitude`
- AND the catalog contains only `Contemplation` phrases (all eligible)
- WHEN the selector runs
- THEN a result MAY be returned (Contemplation is a secondary family for Plenitude)

#### Scenario: Contemplation preferred in Unbreakable

- GIVEN `scoreState = Unbreakable`
- AND the catalog contains phrases from multiple families including `Contemplation`
- WHEN the selector runs many times with identical seed
- THEN `Contemplation` phrases win more frequently than other families
  (weight 5 vs IdentityValues 3 vs Recognition 2)

---

### Requirement: Seven-Day Non-Repetition Window

The selector MUST exclude phrases whose ID appears in the recent impressions set
(impressions from the last 7 days). If excluding recent phrases leaves zero
candidates, the selector MUST relax the repetition window and retry with the full
eligible+state-filtered set. Relaxing the state family rules is NOT permitted as
the fallback.

#### Scenario: Recent phrase excluded

- GIVEN `recentPhraseIds` contains phrase A
- AND the catalog after state filtering contains phrase A and phrase B
- WHEN the selector runs
- THEN phrase A is not selected

#### Scenario: All recent — relax window, not state rules

- GIVEN all state-eligible phrases appear in `recentPhraseIds`
- WHEN the selector runs
- THEN it retries with the repetition window relaxed (recentPhraseIds ignored)
- AND it does NOT relax the state family rules (Contemplation gating remains)

---

### Requirement: Weighted Deterministic Selection

The selector MUST compute a combined weight for each candidate as
`stateWeight(family) + phaseWeight(family)` from the maps defined in
`frases-ancla.md §9`. Selection MUST use `Random(seed)` where
`seed = hash(date, dayPhase)`, making the result **stable within a phase** and
**reproducible** given the same inputs.

#### Scenario: Same inputs produce same result

- GIVEN identical `(date, dayPhase, scoreState, catalog, rules, recentIds)`
- WHEN the selector is called twice
- THEN both calls return the same `phraseId`

#### Scenario: Different day phase produces potentially different result

- GIVEN the same `(date, scoreState, catalog, rules, recentIds)`
- AND `dayPhase` changes from `Dawn` to `Dusk`
- WHEN the selector runs
- THEN the selection seed changes (result MAY differ)

#### Scenario: Phase weights modify candidate probabilities

- GIVEN `dayPhase = Dawn` and `scoreState = Motion`
- AND the catalog contains phrases of families `MinimalAction` and `Persistence`
- WHEN the selector runs many times varying only the date seed
- THEN `MinimalAction` phrases win more often (Dawn +2 vs Persistence +1)

---

### Requirement: Null Result on Empty Eligible Set

The selector MUST return `null` when no eligible phrase exists after all filters
(including after window relaxation). The caller is responsible for graceful handling.

#### Scenario: Empty catalog returns null

- GIVEN the catalog is empty
- WHEN the selector runs
- THEN the result is `null`

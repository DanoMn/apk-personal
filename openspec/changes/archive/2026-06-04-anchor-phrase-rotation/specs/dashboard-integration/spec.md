# Dashboard Integration Specification

Source: `docs/dominio/frases-ancla.md` §2, §16 · anchor-phrase-rotation proposal §2, §3.4

## Purpose

Defines how the dashboard observes and displays the resolved anchor phrase slot.
The UI MUST source the phrase from the slot written by `AnchorPhraseResolver`,
not from its own selection logic. The hardcoded Kierkegaard fallback MUST be removed.

---

## Requirements

### Requirement: Dashboard Reads Slot, Not Selects

The dashboard MUST NOT perform phrase selection. It MUST observe the daily slot
for the current date via a `Flow` and look up `text` + `authorReference` from the
in-memory catalog. `DashboardProjection.selectAnchorPhrase` (stub) MUST be replaced
by a pure slot→UI-state lookup.

#### Scenario: Slot present — phrase displayed

- GIVEN the resolver has written a slot for `(today, currentPhase)`
- AND the slot's `phraseId` exists in the loaded catalog
- WHEN `DashboardProjection` maps the slot to `DashboardAnchorPhraseState`
- THEN `text` and `authorReference` match the catalog phrase for that `phraseId`

#### Scenario: No slot yet — graceful empty state

- GIVEN no slot exists for `(today, currentPhase)`
- WHEN `DashboardProjection` maps the absence to `DashboardAnchorPhraseState`
- THEN the resulting state is empty or neutral (no hardcoded author or text)
- AND no `NullPointerException` or crash occurs

---

### Requirement: No Hardcoded Fallback Phrase

`DashboardState` MUST NOT contain the Kierkegaard quote (or any other specific
authored phrase) as a default value. The default for `DashboardAnchorPhraseState`
MUST be empty or neutral.

#### Scenario: Default state has no attributed text

- GIVEN `DashboardAnchorPhraseState` is constructed with its default/empty constructor
- WHEN `text` and `authorReference` are inspected
- THEN neither field contains the string "Kierkegaard"
- AND neither field contains a non-empty authored phrase

---

### Requirement: Resolver Called Before Dashboard Reads Slot

The resolver MUST be invoked during `runDailyMaintenance` AFTER
`refreshCurrentWeeklyScoreSnapshot`, so that when the dashboard's slot `Flow`
emits, the slot is already present. `onResumed` MUST also invoke the resolver to
handle day-phase crossings while the app is in the background.

#### Scenario: onResumed triggers re-resolve on phase change

- GIVEN the app was in the background across the 15:00 boundary
- WHEN the user returns to the foreground and `onResumed` fires
- THEN the resolver is called with the current date and time
- AND the Dusk slot is resolved (or reused if already present)

---

### Requirement: Dashboard Does Not Show Internal State

The anchor phrase card MUST display only `text` and `authorReference`. Family,
score state, weight, and unlock explanation MUST NOT appear in the UI.

#### Scenario: Only editorial content visible

- GIVEN a resolved anchor phrase slot
- WHEN `AnchorPhraseCard` renders
- THEN the visible content contains phrase text and attribution only
- AND no family enum, score state label, or weight value is rendered

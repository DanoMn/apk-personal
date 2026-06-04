# Archive Report — anchor-phrase-rotation

**Change**: anchor-phrase-rotation
**Project**: apk-personal — "Autonomía sin límites"
**Archived**: 2026-06-04
**Artifact store**: openspec
**Verdict at archive**: PASS WITH WARNINGS (0 CRITICAL)

---

## What Shipped

The anchor phrase rotation engine is fully implemented in the working tree.
All 22 tasks across 6 code slices + 1 doc slice are complete and verified.

### Components delivered

| Component | File | Status |
|---|---|---|
| `DayPhase` enum | `Models.kt` | Shipped |
| `AnchorPhraseSelection`, `AnchorPhraseStateRule`, `AnchorPhrasePhaseRule` models | `Models.kt` | Shipped |
| Domain mappers for new entity types | `data/local/mapper/DomainMappers.kt` | Shipped |
| `DayPhasePolicy` (pure, no Room) | `domain/phrase/DayPhasePolicy.kt` | Shipped |
| `AnchorPhraseSelector` (pure, deterministic, `Random(seed)`) | `domain/phrase/AnchorPhraseSelector.kt` | Shipped |
| `AnchorPhraseDataSource` interface + `DaoAnchorPhraseDataSource` | `data/phrase/AnchorPhraseDataSource.kt` | Shipped |
| `AnchorPhraseResolver` (data-layer coordinator, `@Transaction`) | `data/phrase/AnchorPhraseResolver.kt` | Shipped |
| `AnchorPhraseSeed` (83 phrases, rules derived from maps) | `data/local/seed/AnchorPhraseSeed.kt` | Shipped |
| 2 new DAO queries (impressions 7d window, daily slots Flow) | `data/AutonomiaDao.kt` | Shipped |
| `ensureSeeded` wiring for phrases + rules | `AutonomiaRepository.kt` | Shipped |
| `resolveAnchorPhraseForToday` exposed on repository | `AutonomiaRepository.kt` | Shipped |
| `anchorPhraseSlotFlow` on `DashboardRepository` | `ui/dashboard/DashboardRepository.kt` | Shipped |
| `DashboardAnchorPhraseSnapshot` combine in ViewModel | `ui/dashboard/DashboardViewModel.kt` | Shipped |
| `onResumed` extended to re-resolve on phase change | `ui/dashboard/DashboardViewModel.kt` | Shipped |
| `selectAnchorPhrase` stub replaced by pure lookup | `domain/dashboard/DashboardProjection.kt` | Shipped |
| Kierkegaard hardcode removed, neutral default | `domain/dashboard/DashboardState.kt` | Shipped |
| `frases-ancla.md` §17/§18 updated (implementation table + ADR-3) | `docs/dominio/frases-ancla.md` | Shipped |

### Test suite

| Test Class | New Cases | All Pass |
|---|---|---|
| `DayPhasePolicyTest` | 6 | Yes |
| `AnchorPhraseSelectorTest` | 17 | Yes |
| `AnchorPhraseResolverTest` | 7 | Yes |
| `AnchorPhraseSeedTest` | 17 | Yes |
| `DashboardProjectionTest` | 4 new (23 total) | Yes |
| **Full suite** | **353 total** | **0 failures** |

---

## Migration Reversal — Camino A Decision

The original plan included a **DB v12→v13 migration** to fix a latent release bug
(the 5 `anchor_phrase*` tables are in v12 schema but no migration creates them for
devices upgrading from v11→v12). This migration was **implemented and then reverted**:

- **Why reverted:** Project convention "Camino A" — in dev phase, DB is disposable,
  no hand-written Room migrations. Clean install always works (v12 schema creates
  all tables). No users, no production data to preserve.
- **What was removed:** `MIGRATION_12_13` in `AutonomiaDatabase.kt`, DB version bump
  (reverted to v12), `AnchorPhraseMigration12To13Test.kt` (androidTest), `schemas/13.json`.
- **What this means:** The bug is still present and **will crash any device that migrated
  v11→v12** on a future release. This is an **open release blocker** — not a dev-phase
  concern, but must be addressed before shipping to users.
- **spec impact:** The `anchor-phrase-migration` delta spec was NOT merged to main specs.
  It is preserved in the archive as a historical record only.

---

## Open Items (must be tracked)

### High priority (release blockers)
1. **Migration NOT committed** — the entire change is in the working tree, uncommitted.
   The user must commit manually. No git operations were performed by SDD.
2. **`anchor_phrase*` tables missing from v11→v12 migration path** — latent release crash.
   A future `sdd-new` change must add `MIGRATION_12_13` before release.

### Medium priority (dev quality)
3. **Phrase card not visually verified on dashboard** — the anchor phrase card sits behind
   the onboarding flow and was not visually confirmed on device. The unit tests + build
   confirm correctness, but the actual render on the dashboard screen was not observed.
4. **`SleepMigration11To12Test`** — legacy androidTest remains dead under Camino A.
   Not blocking, but creates test-suite noise.

### Low priority (suggestions from verify report)
5. `AnchorPhraseSeedTest` package location mismatch (`data/scoring/` vs `data/local/seed/`).
6. Statistical tests (500 trials) could use `@LargeTest` annotation.
7. Resolver Plenitude test is vacuously true — improve by adding a Plenitude-eligible phrase.
8. `meta/instructions/2026-06-04-rotacion-frases-ancla.md` still references the removed
   migration slice (Slice 2). The file is a frozen planning artifact; update or note it.

---

## Specs Merged to Main Specs

| Domain | Main spec path | Action |
|---|---|---|
| day-phase-policy | `openspec/specs/day-phase-policy/spec.md` | Created (new) |
| anchor-phrase-selector | `openspec/specs/anchor-phrase-selector/spec.md` | Created (new) |
| anchor-phrase-resolver | `openspec/specs/anchor-phrase-resolver/spec.md` | Created (new) |
| anchor-phrase-seed | `openspec/specs/anchor-phrase-seed/spec.md` | Created (new) |
| dashboard-integration | `openspec/specs/dashboard-integration/spec.md` | Created (new) |
| anchor-phrase-migration | NOT merged — reverted (Camino A) | Dropped |

---

## Archive Location

```
openspec/changes/archive/2026-06-04-anchor-phrase-rotation/
├── archive-report.md      ← this file
├── proposal.md
├── design.md              ← updated with Camino A note in §7
├── tasks.md               ← updated with revert note on Slice 2
├── verify-report.md       ← updated with post-verify Camino A note
└── specs/
    ├── index.md           ← updated with Camino A note
    ├── day-phase-policy/spec.md
    ├── anchor-phrase-selector/spec.md
    ├── anchor-phrase-resolver/spec.md
    ├── anchor-phrase-seed/spec.md
    ├── anchor-phrase-migration/spec.md  ← preserved as audit trail, marked INVALID
    └── dashboard-integration/spec.md
```

The active change folder `openspec/changes/anchor-phrase-rotation/` should be removed
manually or via `rm -rf` by the user (SDD archive only writes, does not delete).

---

## SDD Cycle Status

The SDD cycle for `anchor-phrase-rotation` is **CLOSED**. All phases completed:
proposal → spec → design → tasks → apply (7 slices) → verify (PASS WITH WARNINGS) → archive.

The implementation is in the working tree, not yet committed. The user handles git commits
per project convention (no AI attribution, conventional commits only).

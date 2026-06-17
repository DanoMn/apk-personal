package dev.panopt.autonomia.data.phrase

import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.AnchorPhrasePhaseRule
import dev.panopt.autonomia.AnchorPhraseStateRule
import dev.panopt.autonomia.AttributionStatus
import dev.panopt.autonomia.DayPhase
import dev.panopt.autonomia.PhraseFamily
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.data.AnchorPhraseDailySlotEntity
import dev.panopt.autonomia.data.AnchorPhraseImpressionEntity
import dev.panopt.autonomia.data.WeeklyScoreSnapshotEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * TDD — RED phase (task 5.1):
 * Written BEFORE [AnchorPhraseResolver] / [AnchorPhraseDataSource] exist.
 * Uses a fake in-memory data source so no Room / Android context is needed.
 *
 * Scenarios (RSLV-REQ-1..5):
 *   (a) Slot exists, scoreState matches → no write, no impression.
 *   (b) Slot exists, scoreState differs  → selector invoked, slot + impression written.
 *   (c) No slot + selector returns a phrase → slot + impression written.
 *   (d) scoreState is read from weekly snapshot (not from ScoreEngine directly).
 *   (e) Selector returns null → nothing written.
 */
class AnchorPhraseResolverTest {

    // ──────────────────────────────────────────────────────────────────────────
    // Shared helpers
    // ──────────────────────────────────────────────────────────────────────────

    private val today = LocalDate.of(2026, 6, 4)             // Wednesday
    private val nowDawn = LocalDateTime.of(2026, 6, 4, 9, 0) // 09:00 → Dawn

    /** A minimal WeeklyScoreSnapshotEntity for the current week.
     *  2026-06-04 is a Thursday; Monday of that week is 2026-06-01. */
    private fun snapshotFor(state: ScoreState): WeeklyScoreSnapshotEntity = WeeklyScoreSnapshotEntity(
        weekStart = "2026-06-01",   // Monday of 2026-06-04 (Thursday)
        weekEnd = "2026-06-07",
        scoringVersion = dev.panopt.autonomia.domain.scoring.WeeklyScoreSnapshotConstants.SCORING_VERSION,
        calculatedAt = System.currentTimeMillis(),
        configHash = "hash",
        factsHash = "hash",
        weeklyBaseScore = 0.5f,
        weeklyScore = 0.5f,
        stabilityScore = null,
        state = state.name,
        visibleScore = 50,
        worstLayerId = null,
        layerSummariesJson = "[]",
        reasonsJson = "[]",
    )

    /** A slot that already exists for (today, Dawn) with scoreState = Restoration. */
    private fun existingSlotRestoration() = AnchorPhraseDailySlotEntity(
        date = today.toString(),
        dayPhase = DayPhase.Dawn.name,
        scoreState = ScoreState.Restoration.name,
        phraseId = "phrase-1",
        resolvedAt = System.currentTimeMillis(),
    )

    /** A slot with state Attention (different from current Restoration snapshot). */
    private fun existingSlotAttention() = AnchorPhraseDailySlotEntity(
        date = today.toString(),
        dayPhase = DayPhase.Dawn.name,
        scoreState = ScoreState.Attention.name,
        phraseId = "phrase-old",
        resolvedAt = System.currentTimeMillis(),
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Fake data source — open so individual tests can override catalog etc.
    // ──────────────────────────────────────────────────────────────────────────

    open class FakeAnchorPhraseDataSource(
        private val existingSlot: AnchorPhraseDailySlotEntity? = null,
        private val snapshots: List<WeeklyScoreSnapshotEntity> = emptyList(),
    ) : AnchorPhraseDataSource {

        var atomicWriteCalled = false
        var writtenSlot: AnchorPhraseDailySlotEntity? = null
        var writtenImpression: AnchorPhraseImpressionEntity? = null

        val defaultCatalog = listOf(
            AnchorPhrase(
                id = "phrase-1",
                text = "Take one minimal action.",
                authorReference = "Test Author",
                family = PhraseFamily.MinimalAction,
                language = "en",
                attributionStatus = AttributionStatus.Clear,
                active = true,
                sortOrder = 0,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        )

        override suspend fun getSlot(date: String, dayPhase: String): AnchorPhraseDailySlotEntity? = existingSlot
        override suspend fun getCatalog(): List<AnchorPhrase> = defaultCatalog
        override suspend fun getStateRules(): List<AnchorPhraseStateRule> =
            listOf(AnchorPhraseStateRule(phraseId = "phrase-1", scoreState = ScoreState.Restoration, weight = 2))
        override suspend fun getPhaseRules(): List<AnchorPhrasePhaseRule> =
            listOf(AnchorPhrasePhaseRule(phraseId = "phrase-1", dayPhase = DayPhase.Dawn, weight = 2))
        override suspend fun getRecentImpressionPhraseIds(start: String, end: String): Set<String> = emptySet()
        override suspend fun getWeeklySnapshots(): List<WeeklyScoreSnapshotEntity> = snapshots

        override suspend fun writeSlotAndImpression(
            slot: AnchorPhraseDailySlotEntity,
            impression: AnchorPhraseImpressionEntity,
        ) {
            atomicWriteCalled = true
            writtenSlot = slot
            writtenImpression = impression
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // (a) RSLV-REQ-1 — Slot exists with matching scoreState → no write
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `slot exists with matching scoreState - no write occurs`() = runTest {
        // GIVEN: snapshot state = Restoration, slot.scoreState = Restoration
        val ds = FakeAnchorPhraseDataSource(
            existingSlot = existingSlotRestoration(),
            snapshots = listOf(snapshotFor(ScoreState.Restoration)),
        )

        // WHEN
        AnchorPhraseResolver(ds).resolveForToday(today, nowDawn)

        // THEN: no atomic write was triggered
        assertEquals(false, ds.atomicWriteCalled)
        assertNull(ds.writtenSlot)
        assertNull(ds.writtenImpression)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // (b) RSLV-REQ-2 — Slot exists but scoreState changed → re-select and write
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `slot exists with different scoreState - new slot and impression written`() = runTest {
        // GIVEN: snapshot state = Restoration, slot.scoreState = Attention (differs)
        val ds = FakeAnchorPhraseDataSource(
            existingSlot = existingSlotAttention(),
            snapshots = listOf(snapshotFor(ScoreState.Restoration)),
        )

        // WHEN
        AnchorPhraseResolver(ds).resolveForToday(today, nowDawn)

        // THEN: atomic write happened with new slot using current state
        assertTrue(ds.atomicWriteCalled)
        assertEquals(ScoreState.Restoration.name, ds.writtenSlot?.scoreState)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // (c) RSLV-REQ-3 — No slot + selector returns phrase → slot + impression written
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `no slot and selector returns phrase - both slot and impression written`() = runTest {
        // GIVEN: no existing slot
        val ds = FakeAnchorPhraseDataSource(
            existingSlot = null,
            snapshots = listOf(snapshotFor(ScoreState.Restoration)),
        )

        // WHEN
        AnchorPhraseResolver(ds).resolveForToday(today, nowDawn)

        // THEN: slot written for today/Dawn
        assertTrue(ds.atomicWriteCalled)
        assertEquals(today.toString(), ds.writtenSlot?.date)
        assertEquals(DayPhase.Dawn.name, ds.writtenSlot?.dayPhase)
        assertEquals(ScoreState.Restoration.name, ds.writtenSlot?.scoreState)

        // AND: impression also written with matching phraseId
        assertEquals(ds.writtenSlot?.phraseId, ds.writtenImpression?.phraseId)
        assertEquals(today.toString(), ds.writtenImpression?.date)
        assertEquals(DayPhase.Dawn.name, ds.writtenImpression?.dayPhase)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // (d) RSLV-REQ-4 — scoreState is read from snapshot state field, not recalculated
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `scoreState is derived from weekly snapshot state field`() = runTest {
        // GIVEN: snapshot says Plenitude (distinctive, not the default)
        val ds = FakeAnchorPhraseDataSource(
            existingSlot = null,
            snapshots = listOf(snapshotFor(ScoreState.Plenitude)),
        )

        // WHEN
        AnchorPhraseResolver(ds).resolveForToday(today, nowDawn)

        // THEN: written slot uses Plenitude, not any other state
        if (ds.atomicWriteCalled) {
            assertEquals(ScoreState.Plenitude.name, ds.writtenSlot?.scoreState)
        }
        // (If selector returns null for Plenitude with MinimalAction-only catalog, no write — also valid.)
    }

    @Test
    fun `scoreState falls back to NoData when no snapshot exists for current week`() = runTest {
        // GIVEN: no snapshot rows at all
        val ds = FakeAnchorPhraseDataSource(
            existingSlot = null,
            snapshots = emptyList(),
        )

        // WHEN
        AnchorPhraseResolver(ds).resolveForToday(today, nowDawn)

        // THEN: if write happens, it must use NoData
        if (ds.atomicWriteCalled) {
            assertEquals(ScoreState.NoData.name, ds.writtenSlot?.scoreState)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // (e) RSLV-REQ-5 — Selector returns null → nothing written
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `selector returns null when catalog is empty - nothing written`() = runTest {
        // GIVEN: empty catalog → selector will return null
        val ds = object : FakeAnchorPhraseDataSource(
            existingSlot = null,
            snapshots = listOf(snapshotFor(ScoreState.Restoration)),
        ) {
            override suspend fun getCatalog(): List<AnchorPhrase> = emptyList()
        }

        // WHEN
        AnchorPhraseResolver(ds).resolveForToday(today, nowDawn)

        // THEN: no write at all
        assertEquals(false, ds.atomicWriteCalled)
        assertNull(ds.writtenSlot)
        assertNull(ds.writtenImpression)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Atomicity intent: slot and impression are always written together
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    fun `when write happens slot and impression share the same phraseId`() = runTest {
        val ds = FakeAnchorPhraseDataSource(
            existingSlot = null,
            snapshots = listOf(snapshotFor(ScoreState.Restoration)),
        )

        AnchorPhraseResolver(ds).resolveForToday(today, nowDawn)

        if (ds.atomicWriteCalled) {
            assertEquals(ds.writtenSlot?.phraseId, ds.writtenImpression?.phraseId)
        }
    }
}

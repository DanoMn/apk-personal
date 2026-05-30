package dev.panopt.autonomia.domain.sleep

import dev.panopt.autonomia.domain.sleep.interpretation.InterpretationParams
import dev.panopt.autonomia.domain.sleep.interpretation.NightTimeline
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import dev.panopt.autonomia.domain.sleep.interpretation.SleepSegment
import dev.panopt.autonomia.domain.sleep.interpretation.SleepSegmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/**
 * TDD RED phase — WU-2: domain models (SleepModels, InterpretationParams, SleepNightScore).
 * These tests reference production code that does NOT exist yet; they will fail until
 * the corresponding files are created (GREEN step).
 */
class SleepModelsTest {

    // ─── InterpretationParams ───────────────────────────────────────────────

    @Test
    fun interpretationParams_defaultExists() {
        val defaults = InterpretationParams.DEFAULT

        assertNotNull(defaults)
    }

    @Test
    fun interpretationParams_defaultHasExpectedQuietGap() {
        val defaults = InterpretationParams.DEFAULT

        // ~15 minutes in millis
        assertEquals(15L * 60_000L, defaults.quietGapMillis)
    }

    @Test
    fun interpretationParams_defaultHasExpectedNapSeparation() {
        val defaults = InterpretationParams.DEFAULT

        // ~90 minutes in millis
        assertEquals(90L * 60_000L, defaults.napSeparationMillis)
    }

    @Test
    fun interpretationParams_defaultHasExpectedNapAnchorWindow() {
        val defaults = InterpretationParams.DEFAULT

        // ~120 minutes
        assertEquals(120, defaults.napAnchorWindowMinutes)
    }

    @Test
    fun interpretationParams_defaultHasExpectedDefinitiveWakeMin() {
        val defaults = InterpretationParams.DEFAULT

        // ~10 minutes
        assertEquals(10, defaults.definitiveWakeMinMinutes)
    }

    @Test
    fun interpretationParams_defaultHasExpectedReturnToSleepMin() {
        val defaults = InterpretationParams.DEFAULT

        // ~30 minutes
        assertEquals(30, defaults.returnToSleepMinMinutes)
    }

    @Test
    fun interpretationParams_canBeCustomized() {
        val custom = InterpretationParams(
            quietGapMillis = 10L * 60_000L,
            napSeparationMillis = 60L * 60_000L,
            napAnchorWindowMinutes = 90,
            definitiveWakeMinMinutes = 5,
            returnToSleepMinMinutes = 20,
        )

        assertEquals(10L * 60_000L, custom.quietGapMillis)
        assertEquals(60L * 60_000L, custom.napSeparationMillis)
        assertEquals(90, custom.napAnchorWindowMinutes)
        assertEquals(5, custom.definitiveWakeMinMinutes)
        assertEquals(20, custom.returnToSleepMinMinutes)
    }

    // ─── SleepSegmentKind enum ───────────────────────────────────────────────

    @Test
    fun sleepSegmentKind_hasAsleepAndAwakeUse() {
        val asleep = SleepSegmentKind.Asleep
        val awakeUse = SleepSegmentKind.AwakeUse

        assertEquals("Asleep", asleep.name)
        assertEquals("AwakeUse", awakeUse.name)
    }

    // ─── SleepConfidence enum ────────────────────────────────────────────────

    @Test
    fun sleepConfidence_hasThreeLevels() {
        assertEquals("High", SleepConfidence.High.name)
        assertEquals("Ambiguous", SleepConfidence.Ambiguous.name)
        assertEquals("NoData", SleepConfidence.NoData.name)
    }

    // ─── SleepSegment ────────────────────────────────────────────────────────

    @Test
    fun sleepSegment_holdsStartEndAndKind() {
        val start = Instant.ofEpochMilli(1_000_000L)
        val end = Instant.ofEpochMilli(2_000_000L)
        val segment = SleepSegment(startAt = start, endAt = end, kind = SleepSegmentKind.Asleep)

        assertEquals(start, segment.startAt)
        assertEquals(end, segment.endAt)
        assertEquals(SleepSegmentKind.Asleep, segment.kind)
    }

    // ─── NightTimeline ───────────────────────────────────────────────────────

    @Test
    fun nightTimeline_holdsNightDateAndSegments() {
        val date = LocalDate.of(2026, 5, 29)
        val timeline = NightTimeline(
            nightDate = date,
            segments = emptyList(),
            sleepOnsetAt = null,
            definitiveWakeAt = null,
            confidence = SleepConfidence.NoData,
        )

        assertEquals(date, timeline.nightDate)
        assertEquals(SleepConfidence.NoData, timeline.confidence)
        assertNull(timeline.sleepOnsetAt)
        assertNull(timeline.definitiveWakeAt)
    }

    @Test
    fun nightTimeline_withHighConfidenceAndOnsetWake() {
        val date = LocalDate.of(2026, 5, 29)
        val onset = Instant.ofEpochMilli(1_000_000L)
        val wake = Instant.ofEpochMilli(9_000_000L)
        val timeline = NightTimeline(
            nightDate = date,
            segments = listOf(
                SleepSegment(onset, wake, SleepSegmentKind.Asleep),
            ),
            sleepOnsetAt = onset,
            definitiveWakeAt = wake,
            confidence = SleepConfidence.High,
        )

        assertEquals(SleepConfidence.High, timeline.confidence)
        assertEquals(onset, timeline.sleepOnsetAt)
        assertEquals(wake, timeline.definitiveWakeAt)
        assertEquals(1, timeline.segments.size)
    }

    // ─── SleepNightScore ─────────────────────────────────────────────────────

    @Test
    fun sleepNightScore_holdsAllFourComponents() {
        val score = SleepNightScore(
            duration = 0.9f,
            continuity = 0.8f,
            alignment = 0.7f,
            digitalInterruption = 1.0f,
            sleepScore = 0.87f,
            confidence = SleepConfidence.High,
        )

        assertEquals(0.9f, score.duration, 0.001f)
        assertEquals(0.8f, score.continuity, 0.001f)
        assertEquals(0.7f, score.alignment, 0.001f)
        assertEquals(1.0f, score.digitalInterruption, 0.001f)
        assertEquals(0.87f, score.sleepScore!!, 0.001f)
        assertEquals(SleepConfidence.High, score.confidence)
    }

    @Test
    fun sleepNightScore_noDataHasNullSleepScore() {
        // NoData nights should have sleepScore=null (per design §2)
        // SleepScoring returns null for NoData — this test validates the shape
        val score = SleepNightScore(
            duration = 0f,
            continuity = 0f,
            alignment = 0f,
            digitalInterruption = 0f,
            sleepScore = null,
            confidence = SleepConfidence.NoData,
        )

        assertNull(score.sleepScore)
        assertEquals(SleepConfidence.NoData, score.confidence)
    }

    // ─── SleepTargetWindow ───────────────────────────────────────────────────

    @Test
    fun sleepTargetWindow_holdsTargetTimes() {
        val window = SleepTargetWindow(
            targetSleepAt = "23:30",
            targetWakeAt = "07:30",
        )

        assertEquals("23:30", window.targetSleepAt)
        assertEquals("07:30", window.targetWakeAt)
    }
}

package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tests for weekly sleep aggregation in [WeeklyScoringContextBuilder].
 *
 * Spec (task 5.4): semana con noches mezcladas → promedio solo de noches con dato.
 * Design §5: NoData nights excluded from average (soft coverage).
 */
class WeeklySleepAggregationTest {

    private val today = LocalDate.of(2026, 5, 27) // Tuesday

    // ─── 5.4a: 5 nights [0.8, 0.7, NoData, 0.9, NoData] → avg of [0.8, 0.7, 0.9] ─

    @Test
    fun `weekly average excludes NoData nights — only scored nights count`() {
        // [0.8, 0.7, 0.9] → avg = 2.4 / 3 = 0.800
        val input = inputWith(
            sleepNights = listOf(
                night(sleepScore = 0.8f),
                night(sleepScore = 0.7f),
                nightNoData(),
                night(sleepScore = 0.9f),
                nightNoData(),
            ),
        )
        val ctx = WeeklyScoringContextBuilder.build(input)
        assertEquals(0.800f, ctx.sleepScore ?: 0f, 0.001f)
    }

    // ─── 5.4b: 3 nights 1.0 + 3 NoData → avg = 1.0 (not 0.5) ────────────────

    @Test
    fun `three perfect nights plus three NoData nights — average is 1_0 not 0_5`() {
        val input = inputWith(
            sleepNights = listOf(
                night(sleepScore = 1.0f),
                night(sleepScore = 1.0f),
                night(sleepScore = 1.0f),
                nightNoData(),
                nightNoData(),
                nightNoData(),
            ),
        )
        val ctx = WeeklyScoringContextBuilder.build(input)
        assertEquals(1.0f, ctx.sleepScore ?: 0f, 0.001f)
    }

    // ─── 5.4c: all NoData → sleepScore = null (not 0f) ───────────────────────

    @Test
    fun `all NoData nights — weekly sleepScore is null not 0`() {
        val input = inputWith(
            sleepNights = listOf(
                nightNoData(),
                nightNoData(),
                nightNoData(),
            ),
        )
        val ctx = WeeklyScoringContextBuilder.build(input)
        assertNull("All-NoData week must yield null sleepScore, not 0f", ctx.sleepScore)
    }

    // ─── 5.4 extra: empty list → sleepScore = null ────────────────────────────

    @Test
    fun `no sleep nights at all — weekly sleepScore is null`() {
        val input = inputWith(sleepNights = emptyList())
        val ctx = WeeklyScoringContextBuilder.build(input)
        assertNull("No sleep nights must yield null sleepScore", ctx.sleepScore)
    }

    // ─── 5.4 extra: single scored night → average is that night's score ───────

    @Test
    fun `single scored night — weekly sleepScore equals that night`() {
        val input = inputWith(
            sleepNights = listOf(night(sleepScore = 0.65f)),
        )
        val ctx = WeeklyScoringContextBuilder.build(input)
        assertEquals(0.65f, ctx.sleepScore ?: 0f, 0.001f)
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun night(sleepScore: Float, confidence: SleepConfidence = SleepConfidence.High) =
        SleepNightScore(
            duration = sleepScore,
            continuity = sleepScore,
            alignment = sleepScore,
            digitalInterruption = sleepScore,
            sleepScore = sleepScore,
            confidence = confidence,
        )

    private fun nightNoData() =
        SleepNightScore(
            duration = 0f,
            continuity = 0f,
            alignment = 0f,
            digitalInterruption = 0f,
            sleepScore = null, // NoData → excluded from average
            confidence = SleepConfidence.NoData,
        )

    private fun inputWith(sleepNights: List<SleepNightScore>): ScoreInput =
        ScoreInput(
            layers = listOf(layer("layer_cuerpo")),
            activities = emptyList(),
            todayActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            tasks = emptyList(),
            sleepNights = sleepNights,
            today = today,
        )

    private fun layer(id: String): Layer =
        Layer(id = id, name = id, description = "", sortOrder = 0, active = true)
}

package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildWeeklyScoreSnapshotUseCaseTest {
    private val weekStart = LocalDate.of(2026, 5, 25)
    private val weekEnd = LocalDate.of(2026, 5, 27)

    @Test
    fun createsVersionedDerivedSnapshotFromReport() {
        val snapshot = BuildWeeklyScoreSnapshotUseCase(
            WeeklyScoreSnapshotInput(
                weekStart = weekStart,
                weekEnd = weekEnd,
                calculatedAt = 123L,
                scoreInput = scoreInput(actualValue = 20),
                scoreReport = scoreReport(),
            ),
        )

        assertEquals("2026-05-25", snapshot.weekStart)
        assertEquals("2026-05-27", snapshot.weekEnd)
        assertEquals(WeeklyScoreSnapshotConstants.SCORING_VERSION, snapshot.scoringVersion)
        assertEquals(123L, snapshot.calculatedAt)
        assertEquals(0.82f, snapshot.weeklyBaseScore)
        assertEquals(0.82f, snapshot.weeklyScore)
        assertEquals(null, snapshot.stabilityScore)
        assertEquals(ScoreState.Motion.name, snapshot.state)
        assertEquals(946, snapshot.visibleScore)
        assertEquals("layer_cuerpo", snapshot.worstLayerId)
        assertTrue(snapshot.layerSummariesJson.contains("\"layerId\":\"layer_cuerpo\""))
        assertTrue(snapshot.layerSummariesJson.contains("\"score\":0.820"))
        assertEquals("[\"Volvamos al cuerpo.\"]", snapshot.reasonsJson)
    }

    @Test
    fun changesFactsHashWhenFactsChange() {
        val first = BuildWeeklyScoreSnapshotUseCase(
            WeeklyScoreSnapshotInput(
                weekStart = weekStart,
                weekEnd = weekEnd,
                calculatedAt = 123L,
                scoreInput = scoreInput(actualValue = 20),
                scoreReport = scoreReport(),
            ),
        )
        val second = BuildWeeklyScoreSnapshotUseCase(
            WeeklyScoreSnapshotInput(
                weekStart = weekStart,
                weekEnd = weekEnd,
                calculatedAt = 123L,
                scoreInput = scoreInput(actualValue = 35),
                scoreReport = scoreReport(),
            ),
        )

        assertEquals(first.configHash, second.configHash)
        assertNotEquals(first.factsHash, second.factsHash)
    }

    private fun scoreInput(actualValue: Int): ScoreInput =
        ScoreInput(
            layers = listOf(layer()),
            activities = emptyList(),
            todayActivityLogs = listOf(
                ActivityLog(
                    activityId = "act_meditation",
                    date = weekEnd.toString(),
                    completed = true,
                    actualValue = actualValue,
                    updatedAt = 10L,
                ),
            ),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            tasks = emptyList(),
            sleepLog = null,
            today = weekEnd,
        )

    private fun scoreReport(): ScoreReport =
        ScoreReport(
            state = ScoreState.Motion,
            visibleScore = 946,
            baseScore = 946,
            goalBonus = 0,
            progress = 0.946f,
            layerScores = listOf(
                LayerScore(
                    layerId = "layer_cuerpo",
                    name = "Cuerpo",
                    score = 0.82f,
                    configured = true,
                    baseScore = 0.80f,
                    rawScore = 0.82f,
                ),
            ),
            featureContributions = emptyList(),
            gates = emptyList(),
            weeklyBaseScore = 0.82f,
            weeklyScore = 0.82f,
            averageLayerScore = 0.90f,
            worstLayerScore = 0.80f,
            worstLayerId = "layer_cuerpo",
            reasons = listOf("Volvamos al cuerpo."),
        )

    private fun layer(): Layer =
        Layer(
            id = "layer_cuerpo",
            name = "Cuerpo",
            description = "",
            sortOrder = 10,
            active = true,
        )
}

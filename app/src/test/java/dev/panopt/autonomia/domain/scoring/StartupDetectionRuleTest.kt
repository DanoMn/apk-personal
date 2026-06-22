package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lote 2 (task 2.3) — `StartupDetectionRule.isStartup(...)`. Dominio puro JVM.
 *
 * Arranque = cuenta nueva cuyo motor real todavía dice NoData (anclas en gracia), PERO con la
 * cobertura mínima (≥3 capas con ancla) y SIN ningún score real en su historial semanal. El gate
 * de cobertura manda: < 3 capas con ancla → NoData real ("configurá tu base"), NUNCA arranque.
 */
class StartupDetectionRuleTest {
    private val today = LocalDate.of(2026, 5, 24)

    @Test
    fun emptyHistoryWithThreeAnchorLayersInGraceIsStartup() {
        // Historial vacío + 3 capas con ancla (en gracia, NoData real) → arranque.
        val report = noDataReport()
        val layers = coreThreeLayers()
        val activities = coreThreeAnchors()

        assertTrue(
            StartupDetectionRule.isStartup(report, activities, layers, weeklyHistory = emptyList(), today = today),
        )
    }

    @Test
    fun historyWithOneRealScoreIsNotStartup() {
        // Una sola entrada con score real (state != NoData) → la cuenta ya maduró alguna vez → NO arranque.
        val report = noDataReport()
        val history = listOf(
            historyEntry(ScoreState.Motion),
            historyEntry(ScoreState.NoData),
        )

        assertFalse(
            StartupDetectionRule.isStartup(report, coreThreeAnchors(), coreThreeLayers(), history, today),
        )
    }

    @Test
    fun fewerThanThreeAnchorLayersIsNotStartupGateWins() {
        // Solo 2 capas con ancla (< MIN=3) → NoData real, el gate manda → NO arranque.
        val report = noDataReport()
        val layers = listOf(layer("layer_interior", "Interior"), layer("layer_cuerpo", "Cuerpo"))
        val activities = listOf(anchor("act_a", "layer_interior"), anchor("act_b", "layer_cuerpo"))

        assertFalse(
            StartupDetectionRule.isStartup(report, activities, layers, weeklyHistory = emptyList(), today = today),
        )
    }

    @Test
    fun historyAllNoDataWithThreeAnchorLayersIsStartup() {
        // 6 entradas TODAS NoData → NoData no cuenta como score real → arranque.
        val report = noDataReport()
        val history = (0 until 6).map { historyEntry(ScoreState.NoData) }

        assertTrue(
            StartupDetectionRule.isStartup(report, coreThreeAnchors(), coreThreeLayers(), history, today),
        )
    }

    @Test
    fun reportWithRealStateIsNotStartup() {
        // Solo aplica sobre NoData real: si el motor ya dio un veredicto real → NO arranque.
        val report = noDataReport().copy(state = ScoreState.Plenitude)

        assertFalse(
            StartupDetectionRule.isStartup(report, coreThreeAnchors(), coreThreeLayers(), emptyList(), today),
        )
    }

    @Test
    fun archivedOrInactiveAnchorsDoNotCountTowardsGate() {
        // 3 capas pero una ancla archivada y otra inactiva → solo 1 capa válida con ancla (< 3) → NO arranque.
        val report = noDataReport()
        val layers = coreThreeLayers()
        val activities = listOf(
            anchor("act_interior", "layer_interior"),
            anchor("act_cuerpo", "layer_cuerpo").copy(archived = true),
            anchor("act_conducta", "layer_conducta").copy(active = false),
        )

        assertFalse(
            StartupDetectionRule.isStartup(report, activities, layers, weeklyHistory = emptyList(), today = today),
        )
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private fun noDataReport(): ScoreReport =
        ScoreReport(
            state = ScoreState.NoData,
            visibleScore = null,
            baseScore = null,
            goalBonus = 0,
            progress = 0f,
            layerScores = emptyList(),
            featureContributions = emptyList(),
            gates = emptyList(),
        )

    private fun historyEntry(state: ScoreState): WeeklyScoreHistoryEntry =
        WeeklyScoreHistoryEntry(
            weekStart = "2026-05-11",
            weekEnd = "2026-05-17",
            scoringVersion = "v1",
            weeklyBaseScore = 0f,
            weeklyScore = 0f,
            state = state,
        )

    private fun coreThreeLayers(): List<Layer> =
        listOf(
            layer("layer_interior", "Interior", 10),
            layer("layer_cuerpo", "Cuerpo", 20),
            layer("layer_conducta", "Conducta", 30),
        )

    private fun coreThreeAnchors(): List<ActivityDefinition> =
        coreThreeLayers().map { anchor("act_${it.id}", it.id) }

    private fun layer(id: String, name: String, sortOrder: Int = 10): Layer =
        Layer(id = id, name = name, description = "", sortOrder = sortOrder)

    private fun anchor(id: String, layerId: String): ActivityDefinition =
        ActivityDefinition(
            id = id,
            layerId = layerId,
            name = id,
            description = "",
            type = ActivityType.Time,
            role = ActivityRole.Practice,
            activityType = ActivitySurface.Anchor,
            contributionRole = ContributionRole.Core,
            importanceTier = ImportanceTier.Medium,
            cadence = ActivityCadence.Daily,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 4,
            sessionTargetMinutes = 30,
            unit = ActivityUnit.Minutes,
            sortOrder = 10,
            // createdAt reciente: dentro de gracia (no afecta la detección, que NO filtra gracia).
            createdAt = 0L,
        )
}

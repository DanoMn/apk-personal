package dev.panopt.autonomia.domain.dashboard

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
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
import dev.panopt.autonomia.domain.activity.ActivityTargetVersion
import dev.panopt.autonomia.domain.scoring.WeeklyScoreHistoryEntry
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lote 3 (task 3.2) — `DashboardProjection` computa el canal `DashboardState.startup`.
 *
 * En ARRANQUE (cuenta nueva, anclas en gracia, historial sin score real, ≥3 capas con ancla):
 * `state.startup != null` Y `state.status.scoreState == NoData` (el motor real sigue NoData; el
 * arranque es solo presentación). En cuenta MADURA o sin cobertura: `state.startup == null`.
 */
class DashboardProjectionStartupTest {
    private val today = LocalDate.of(2026, 5, 24)

    @Test
    fun startupAccountExposesStartupCardWhileScoreStateStaysNoData() {
        // 3 anclas en 3 capas, creadas hace 2 días (en gracia), con hechos en los días vividos.
        // Convención del design: createdAt hace N días → daysLived = N + 1 (el día de creación es
        // el día 1). Creadas hace 2 días → 3 días vividos → daysRemaining = 7 - 3 = 4.
        val createdDaysAgo = 2
        val daysLived = createdDaysAgo + 1 // 3
        val anchors = graceAnchors(createdDaysAgo, layerCount = 3)
        val logs = livedLogs(anchors, daysLived)

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = anchors,
            todayActivityLogs = logs.filter { it.date == today.toString() },
            weekActivityLogs = logs,
            periodActivityLogs = logs,
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            weeklyHistory = emptyList(),
            focusSignalActivityId = null,
            today = today,
            targetVersions = versionsFor(anchors, daysLived),
        )

        assertNotNull("cuenta en arranque debe exponer startup card", state.startup)
        val startup = state.startup!!
        assertEquals(ScoreState.NoData, state.status.scoreState)
        assertEquals(daysLived, 7 - startup.daysRemaining)
        assertTrue("el contador es un número visible", startup.counterPoints >= 0)
        assertTrue("copy menciona los días restantes", startup.daysRemainingLabel.contains("4"))
    }

    @Test
    fun matureAccountWithRealHistoryHasNoStartupCard() {
        // Historial con un score real (Motion) → la cuenta ya arrancó → startup == null.
        val daysLived = 3
        val anchors = graceAnchors(daysLived, layerCount = 3)
        val logs = livedLogs(anchors, daysLived)
        val history = listOf(
            WeeklyScoreHistoryEntry(
                weekStart = "2026-05-11",
                weekEnd = "2026-05-17",
                scoringVersion = "v1",
                weeklyBaseScore = 1.0f,
                weeklyScore = 1.0f,
                state = ScoreState.Motion,
            ),
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = anchors,
            todayActivityLogs = logs.filter { it.date == today.toString() },
            weekActivityLogs = logs,
            periodActivityLogs = logs,
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            weeklyHistory = history,
            focusSignalActivityId = null,
            today = today,
            targetVersions = versionsFor(anchors, daysLived),
        )

        assertNull("cuenta con score real previo NO está en arranque", state.startup)
    }

    @Test
    fun belowGateHasNoStartupAndStaysNoData() {
        // Solo 2 capas con ancla (< 3) → NoData real ("configurá tu base"), NO arranque.
        val daysLived = 3
        val anchors = graceAnchors(daysLived, layerCount = 2)
        val logs = livedLogs(anchors, daysLived)

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = anchors,
            todayActivityLogs = logs.filter { it.date == today.toString() },
            weekActivityLogs = logs,
            periodActivityLogs = logs,
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepNight = null,
            weeklyHistory = emptyList(),
            focusSignalActivityId = null,
            today = today,
            targetVersions = versionsFor(anchors, daysLived),
        )

        assertNull("sin cobertura mínima no hay arranque", state.startup)
        assertEquals(ScoreState.NoData, state.status.scoreState)
    }

    // --- helpers ---

    private fun createdAtDaysAgo(daysAgo: Int): Long =
        today.minusDays(daysAgo.toLong())
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private fun graceAnchors(daysLived: Int, layerCount: Int): List<ActivityDefinition> {
        val layerIds = defaultLayers().map { it.id }.take(layerCount)
        return layerIds.mapIndexed { i, layerId ->
            anchor("act_$layerId", layerId, createdAtDaysAgo(daysLived), sortOrder = i * 10)
        }
    }

    private fun livedLogs(anchors: List<ActivityDefinition>, daysLived: Int): List<ActivityLog> {
        val livedDays = (0 until daysLived).map { today.minusDays(it.toLong()) }
        return anchors.flatMap { a -> livedDays.map { log(a.id, it, 30) } }
    }

    private fun versionsFor(
        anchors: List<ActivityDefinition>,
        daysLived: Int,
    ): Map<String, List<ActivityTargetVersion>> {
        val firstLived = today.minusDays((daysLived - 1).toLong())
        return anchors.associate { a ->
            a.id to listOf(
                ActivityTargetVersion(a.id, firstLived, targetMinutes = 30, targetDays = 4, createdAt = 1L),
            )
        }
    }

    private fun defaultLayers(): List<Layer> = listOf(
        Layer("layer_interior", "Interior", "", 10),
        Layer("layer_cuerpo", "Cuerpo", "", 20),
        Layer("layer_conducta", "Conducta", "", 30),
        Layer("layer_vinculos", "Vinculos", "", 40),
        Layer("layer_proyecto", "Proyecto", "", 50),
    )

    private fun anchor(id: String, layerId: String, createdAt: Long, sortOrder: Int): ActivityDefinition =
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
            sortOrder = sortOrder,
            createdAt = createdAt,
        )

    private fun log(activityId: String, date: LocalDate, actualValue: Int): ActivityLog =
        ActivityLog(
            activityId = activityId,
            date = date.toString(),
            completed = true,
            actualValue = actualValue,
            updatedAt = 0L,
        )
}

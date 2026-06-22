package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.sleep.SleepNightScore
import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BuildScoreInputUseCaseTest {
    private val today = LocalDate.of(2026, 5, 27)

    @Test
    fun normalizesOnlyActiveScoringSurface() {
        val input = BuildScoreInputUseCase(
            ScoreInputSource(
                layers = listOf(
                    layer("layer_active", active = true, sortOrder = 20),
                    layer("layer_inactive", active = false, sortOrder = 10),
                ),
                activities = listOf(
                    activity("act_anchor", ActivitySurface.Anchor, active = true, archived = false, sortOrder = 30),
                    activity("act_archived", ActivitySurface.Anchor, active = true, archived = true, sortOrder = 10),
                    activity("task_surface", ActivitySurface.Task, active = true, archived = false, sortOrder = 20),
                ),
                todayActivityLogs = emptyList(),
                periodActivityLogs = emptyList(),
                abstinenceTracks = listOf(
                    track("trk_active", active = true, sortOrder = 20),
                    track("trk_inactive", active = false, sortOrder = 10),
                ),
                todayAbstinenceLogs = emptyList(),
                allAbstinenceLogs = emptyList(),
                tasks = emptyList(),
                sleepNights = emptyList(),
                today = today,
            ),
        )

        assertEquals(listOf("layer_active"), input.layers.map { it.id })
        assertEquals(listOf("act_anchor"), input.activities.map { it.id })
        assertEquals(listOf("trk_active"), input.abstinenceTracks.map { it.id })
        assertEquals(today, input.today)
    }

    @Test
    fun anchorsWithinGracePeriodAreExcludedButSupportsAreNot() {
        // Gracia (FASE 2 §3.3): un ancla creada HOY (< 7 días) se excluye del input; el ancla
        // madura entra; un soporte nuevo entra igual (la gracia es solo para anclas).
        val input = BuildScoreInputUseCase(
            ScoreInputSource(
                layers = listOf(layer("layer_active", active = true, sortOrder = 10)),
                activities = listOf(
                    activity("act_mature", ActivitySurface.Anchor, active = true, archived = false, sortOrder = 10, createdAt = 0L),
                    activity("act_new", ActivitySurface.Anchor, active = true, archived = false, sortOrder = 20, createdAt = millis(today)),
                    activity("sup_new", ActivitySurface.Support, active = true, archived = false, sortOrder = 30, createdAt = millis(today)),
                ),
                todayActivityLogs = emptyList(),
                periodActivityLogs = emptyList(),
                abstinenceTracks = emptyList(),
                todayAbstinenceLogs = emptyList(),
                allAbstinenceLogs = emptyList(),
                tasks = emptyList(),
                sleepNights = emptyList(),
                today = today,
            ),
        )

        assertEquals(listOf("act_mature", "sup_new"), input.activities.map { it.id })
    }

    private fun millis(date: LocalDate): Long =
        date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun layer(id: String, active: Boolean, sortOrder: Int): Layer =
        Layer(
            id = id,
            name = id,
            description = "",
            sortOrder = sortOrder,
            active = active,
        )

    private fun activity(
        id: String,
        surface: ActivitySurface,
        active: Boolean,
        archived: Boolean,
        sortOrder: Int,
        createdAt: Long = 0L,
    ): ActivityDefinition =
        ActivityDefinition(
            id = id,
            layerId = "layer_active",
            name = id,
            description = "",
            type = ActivityType.Time,
            role = ActivityRole.Practice,
            activityType = surface,
            contributionRole = ContributionRole.Core,
            importanceTier = ImportanceTier.Medium,
            cadence = ActivityCadence.Daily,
            targetValue = 20,
            minimumValue = 1,
            targetCount = null,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 4,
            sessionTargetMinutes = 20,
            unit = ActivityUnit.Minutes,
            active = active,
            archived = archived,
            sortOrder = sortOrder,
            createdAt = createdAt,
        )

    private fun track(id: String, active: Boolean, sortOrder: Int): AbstinenceTrack =
        AbstinenceTrack(
            id = id,
            name = id,
            substanceLabel = id,
            severity = AbstinenceSeverity.Critical,
            contributionRole = ContributionRole.Protective,
            importanceTier = ImportanceTier.Critical,
            active = active,
            sortOrder = sortOrder,
        )
}

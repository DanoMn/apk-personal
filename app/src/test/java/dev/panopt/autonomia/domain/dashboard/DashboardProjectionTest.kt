package dev.panopt.autonomia.domain.dashboard

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.DisplaySurface
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardProjectionTest {
    private val today = LocalDate.of(2026, 5, 21)
    private val dateKey = today.toString()

    @Test
    fun `support activities populate secondary checklist items`() {
        val anchor = supportActivity(
            id = "act_anchor",
            name = "Meditar",
            layerId = "layer_interior",
            activityType = ActivitySurface.Anchor,
        )
        val bath = supportActivity(
            id = "act_bath",
            name = "Bañarse",
            layerId = "layer_cuerpo",
            activityType = ActivitySurface.Support,
        )
        val eat = supportActivity(
            id = "act_eat",
            name = "Comer",
            layerId = "layer_cuerpo",
            activityType = ActivitySurface.Support,
        )
        val anchorLog = log("act_anchor")
        val bathLog = ActivityLog(
            activityId = "act_bath",
            date = dateKey,
            completed = true,
            actualValue = 1,
            updatedAt = 0L,
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = listOf(anchor, bath, eat),
            todayActivityLogs = listOf(anchorLog, bathLog),
            weekActivityLogs = listOf(anchorLog, bathLog),
            periodActivityLogs = listOf(anchorLog, bathLog),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepLog = null,
            focusSignalActivityId = null,
            today = today,
        )

        // Secondary checklist should contain support items
        assertFalse(
            "secondaryChecklistItems should not be empty when supports are configured",
            state.secondaryChecklistItems.isEmpty(),
        )
        assertEquals(2, state.secondaryChecklistItems.size)

        // Each support item should have activityType populated
        val bathItem = state.secondaryChecklistItems.first { it.title == "Bañarse" }
        assertEquals("Support", bathItem.activityType)

        val eatItem = state.secondaryChecklistItems.first { it.title == "Comer" }
        assertEquals("Support", eatItem.activityType)
    }

    @Test
    fun `anchor items have activityType Anchor in primary checklist`() {
        val anchor = supportActivity(
            id = "act_anchor",
            name = "Meditar",
            layerId = "layer_interior",
            activityType = ActivitySurface.Anchor,
        )
        val log = log("act_anchor")

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = listOf(anchor),
            todayActivityLogs = listOf(log),
            weekActivityLogs = listOf(log),
            periodActivityLogs = listOf(log),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepLog = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals(1, state.checklistItems.size)
        assertEquals("Anchor", state.checklistItems[0].activityType)
    }

    @Test
    fun `no supports configured produces empty secondary checklist`() {
        val anchor = supportActivity(
            id = "act_anchor",
            name = "Meditar",
            layerId = "layer_interior",
            activityType = ActivitySurface.Anchor,
        )

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = listOf(anchor),
            todayActivityLogs = emptyList(),
            weekActivityLogs = emptyList(),
            periodActivityLogs = emptyList(),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepLog = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertTrue(
            "secondaryChecklistItems should be empty when no supports configured",
            state.secondaryChecklistItems.isEmpty(),
        )
    }

    @Test
    fun `DashboardCheckItemState has activityType default value`() {
        val item = DashboardCheckItemState(
            id = "test",
            title = "Test",
            layerId = "layer_interior",
            layerName = "Interior",
            value = "30 min",
            completed = false,
        )
        // Default should be empty string (backward compatible)
        assertEquals("", item.activityType)
    }

    @Test
    fun `DashboardCheckItemState accepts explicit activityType`() {
        val item = DashboardCheckItemState(
            id = "test",
            title = "Test",
            layerId = "layer_interior",
            layerName = "Interior",
            value = "30 min",
            completed = false,
            activityType = "Support",
        )
        assertEquals("Support", item.activityType)
    }

    @Test
    fun `completed support items show as completed in secondary checklist`() {
        val bath = supportActivity(
            id = "act_bath",
            name = "Bañarse",
            layerId = "layer_cuerpo",
            activityType = ActivitySurface.Support,
        )
        val eat = supportActivity(
            id = "act_eat",
            name = "Comer",
            layerId = "layer_cuerpo",
            activityType = ActivitySurface.Support,
        )
        val bathLog = log("act_bath")

        val state = buildDashboardState(
            layers = defaultLayers(),
            activities = listOf(bath, eat),
            todayActivityLogs = listOf(bathLog),
            weekActivityLogs = listOf(bathLog),
            periodActivityLogs = listOf(bathLog),
            abstinenceTracks = emptyList(),
            todayAbstinenceLogs = emptyList(),
            allAbstinenceLogs = emptyList(),
            riskEvents = emptyList(),
            tasks = emptyList(),
            anchorPhrases = emptyList(),
            sleepLog = null,
            focusSignalActivityId = null,
            today = today,
        )

        assertEquals(2, state.secondaryChecklistItems.size)

        val bathItem = state.secondaryChecklistItems.first { it.title == "Bañarse" }
        assertTrue("Bañarse with log should be completed", bathItem.completed)

        val eatItem = state.secondaryChecklistItems.first { it.title == "Comer" }
        assertFalse("Comer without log should not be completed", eatItem.completed)
    }

    // --- helpers ---

    private fun defaultLayers(): List<Layer> = listOf(
        Layer("layer_interior", "Interior", "", 10),
        Layer("layer_cuerpo", "Cuerpo", "", 20),
        Layer("layer_conducta", "Conducta", "", 30),
        Layer("layer_vinculos", "Vinculos", "", 40),
        Layer("layer_proyecto", "Proyecto", "", 50),
    )

    private fun supportActivity(
        id: String,
        name: String,
        layerId: String,
        activityType: ActivitySurface,
    ): ActivityDefinition = ActivityDefinition(
        id = id,
        layerId = layerId,
        name = name,
        description = "",
        type = ActivityType.Check,
        role = ActivityRole.Practice,
        displaySurface = when (activityType) {
            ActivitySurface.Anchor -> DisplaySurface.PrimaryChecklist
            ActivitySurface.Support -> DisplaySurface.SecondaryChecklist
            ActivitySurface.Task -> DisplaySurface.PrimaryChecklist
        },
        activityType = activityType,
        contributionRole = if (activityType == ActivitySurface.Anchor) ContributionRole.Core else ContributionRole.Support,
        importanceTier = ImportanceTier.Medium,
        cadence = ActivityCadence.Daily,
        targetValue = if (activityType == ActivitySurface.Anchor) 30 else null,
        minimumValue = if (activityType == ActivitySurface.Anchor) 1 else null,
        targetCount = null,
        targetPeriod = TargetPeriod.Day,
        unit = if (activityType == ActivitySurface.Anchor) ActivityUnit.Minutes else ActivityUnit.Boolean,
        sortOrder = 10,
    )

    private fun log(activityId: String): ActivityLog = ActivityLog(
        activityId = activityId,
        date = dateKey,
        completed = true,
        actualValue = 30,
        updatedAt = 0L,
    )
}

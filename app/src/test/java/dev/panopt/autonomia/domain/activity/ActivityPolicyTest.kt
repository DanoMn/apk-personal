package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.DisplaySurface
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.TargetPeriod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPolicyTest {
    @Test
    fun primaryChecklistDefinitionIsAnchor() {
        val definition = activity(displaySurface = DisplaySurface.PrimaryChecklist)

        assertTrue(definition.isAnchor())
        assertFalse(definition.isGoal())
    }

    @Test
    fun weeklyGoalIsNotAnchorEvenWhenVisible() {
        val definition = activity(displaySurface = DisplaySurface.PrimaryChecklist).copy(
            cadence = ActivityCadence.Weekly,
            targetPeriod = TargetPeriod.Week,
        )

        assertTrue(definition.isGoal())
        assertFalse(definition.isAnchor())
    }

    @Test
    fun secondaryChecklistDefinitionIsSupport() {
        val definition = activity(displaySurface = DisplaySurface.SecondaryChecklist)

        assertTrue(definition.isSupport())
        assertFalse(definition.isAnchor())
    }

    private fun activity(displaySurface: DisplaySurface): ActivityDefinition =
        ActivityDefinition(
            id = "act_test",
            layerId = "layer_project",
            name = "Test",
            description = "",
            type = ActivityType.Time,
            role = ActivityRole.Practice,
            displaySurface = displaySurface,
            activityType = when (displaySurface) {
                DisplaySurface.PrimaryChecklist -> ActivitySurface.Anchor
                DisplaySurface.SecondaryChecklist -> ActivitySurface.Support
                else -> ActivitySurface.Anchor
            },
            contributionRole = ContributionRole.Core,
            importanceTier = ImportanceTier.Medium,
            cadence = ActivityCadence.Daily,
            targetValue = 30,
            minimumValue = 1,
            targetCount = null,
            targetPeriod = TargetPeriod.Day,
            unit = ActivityUnit.Minutes,
            sortOrder = 10,
        )
}

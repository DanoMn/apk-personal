package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.TargetPeriod
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityPolicyTest {
    @Test
    fun anchorDefinitionIsAnchor() {
        val definition = activity(activityType = ActivitySurface.Anchor)

        assertTrue(definition.isAnchor())
        assertFalse(definition.isGoal())
    }

    @Test
    fun `anchor with weekly goal is still an anchor`() {
        val definition = activity(activityType = ActivitySurface.Anchor).copy(
            cadence = ActivityCadence.Weekly,
            targetPeriod = TargetPeriod.Week,
        )

        assertTrue(definition.isGoal())
        assertTrue(definition.isAnchor()) // anchor with goal IS still an anchor
    }

    @Test
    fun supportDefinitionIsSupport() {
        val definition = activity(activityType = ActivitySurface.Support)

        assertTrue(definition.isSupport())
        assertFalse(definition.isAnchor())
    }

    private fun activity(activityType: ActivitySurface): ActivityDefinition =
        ActivityDefinition(
            id = "act_test",
            layerId = "layer_project",
            name = "Test",
            description = "",
            type = ActivityType.Time,
            role = ActivityRole.Practice,
            activityType = activityType,
            contributionRole = if (activityType == ActivitySurface.Anchor) {
                ContributionRole.Core
            } else {
                ContributionRole.Support
            },
            importanceTier = ImportanceTier.Medium,
            cadence = ActivityCadence.Daily,
            targetValue = if (activityType == ActivitySurface.Anchor) 30 else null,
            minimumValue = if (activityType == ActivitySurface.Anchor) 1 else null,
            targetCount = null,
            targetPeriod = if (activityType == ActivitySurface.Anchor) TargetPeriod.Day else null,
            unit = if (activityType == ActivitySurface.Anchor) ActivityUnit.Minutes else ActivityUnit.Boolean,
            sortOrder = 10,
        )
}

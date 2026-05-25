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

data class ActivityDefinition(
    val id: String,
    val layerId: String,
    val name: String,
    val description: String,
    val type: ActivityType,
    val role: ActivityRole,
    val displaySurface: DisplaySurface,      // DEPRECATED — will be removed in PR 3
    val activityType: ActivitySurface = ActivitySurface.Anchor, // NEW
    val contributionRole: ContributionRole,
    val importanceTier: ImportanceTier,
    val cadence: ActivityCadence? = null,
    val targetValue: Int? = null,
    val minimumValue: Int? = null,
    val targetCount: Int? = null,
    val targetPeriod: TargetPeriod? = null,
    val weeklyFrequencyTarget: Int? = null,
    val sessionTargetMinutes: Int? = null,
    val commitmentDurationMonths: Int? = null,
    val unit: ActivityUnit,
    val active: Boolean = true,
    val archived: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
)

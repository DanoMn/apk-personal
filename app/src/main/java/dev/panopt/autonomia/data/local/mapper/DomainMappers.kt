package dev.panopt.autonomia.data.local.mapper

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.AttributionStatus
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.PhraseFamily
import dev.panopt.autonomia.RiskEvent
import dev.panopt.autonomia.SleepConfig
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.SleepSessionState
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.data.AbstinenceLogEntity
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityDefinitionEntity
import dev.panopt.autonomia.data.ActivityLogEntity
import dev.panopt.autonomia.data.AnchorPhraseEntity
import dev.panopt.autonomia.data.LayerEntity
import dev.panopt.autonomia.data.RiskEventEntity
import dev.panopt.autonomia.data.SleepConfigEntity
import dev.panopt.autonomia.data.SleepLogEntity
import dev.panopt.autonomia.data.SleepSessionStateEntity
import dev.panopt.autonomia.data.TaskEntity
import dev.panopt.autonomia.data.UserActivityConfigEntity
import dev.panopt.autonomia.domain.activity.ActivityDefinition

internal fun LayerEntity.toDomain(): Layer =
    Layer(id = id, name = name, description = description, sortOrder = sortOrder, active = active)

// Mapper for merged domain model (definition + config) (used in PR 2 when repository merges flows)
internal fun mergeToDomain(
    definition: ActivityDefinitionEntity,
    config: UserActivityConfigEntity,
): ActivityDefinition = ActivityDefinition(
    id = definition.id,
    layerId = definition.layerId,
    name = config.customName ?: definition.name,
    description = config.customDescription ?: definition.description,
    type = runCatching { ActivityType.valueOf(definition.type) }.getOrDefault(ActivityType.Check),
    role = runCatching { ActivityRole.valueOf(definition.role) }.getOrDefault(ActivityRole.Practice),
    activityType = runCatching { ActivitySurface.valueOf(config.activityType) }.getOrDefault(ActivitySurface.Anchor),
    contributionRole = runCatching { ContributionRole.valueOf(definition.contributionRole) }.getOrDefault(ContributionRole.Core),
    importanceTier = runCatching { ImportanceTier.valueOf(definition.importanceTier) }.getOrDefault(ImportanceTier.Medium),
    cadence = config.cadence?.let { runCatching { ActivityCadence.valueOf(it) }.getOrNull() },
    targetValue = config.targetValue,
    minimumValue = config.minimumValue,
    targetCount = config.targetCount,
    targetPeriod = config.targetPeriod?.let { runCatching { TargetPeriod.valueOf(it) }.getOrNull() },
    weeklyFrequencyTarget = config.weeklyFrequencyTarget
        ?: legacyWeeklyFrequencyTarget(config.targetCount, config.targetPeriod),
    sessionTargetMinutes = config.sessionTargetMinutes ?: config.targetValue,
    commitmentDurationMonths = config.commitmentDurationMonths,
    unit = runCatching { ActivityUnit.valueOf(definition.unit) }.getOrDefault(ActivityUnit.Boolean),
    active = config.active,
    archived = config.archived,
    sortOrder = config.sortOrder,
    createdAt = definition.createdAt,
    updatedAt = config.updatedAt,
)

// Temporary helper for catalog browsing (definition-only, no config)
internal fun ActivityDefinitionEntity.toDomain(): ActivityDefinition = ActivityDefinition(
    id = id, layerId = layerId, name = name, description = description,
    type = runCatching { ActivityType.valueOf(type) }.getOrDefault(ActivityType.Check),
    role = runCatching { ActivityRole.valueOf(role) }.getOrDefault(ActivityRole.Practice),
    activityType = when (presetCategory) {
        "anchor" -> ActivitySurface.Anchor
        "support" -> ActivitySurface.Support
        null -> if (isCustomActivityId(id)) ActivitySurface.Anchor else ActivitySurface.Task
        else -> ActivitySurface.Task
    },
    contributionRole = runCatching { ContributionRole.valueOf(contributionRole) }.getOrDefault(ContributionRole.Core),
    importanceTier = runCatching { ImportanceTier.valueOf(importanceTier) }.getOrDefault(ImportanceTier.Medium),
    cadence = null, targetValue = null, minimumValue = null, targetCount = null, targetPeriod = null,
    weeklyFrequencyTarget = null, sessionTargetMinutes = null, commitmentDurationMonths = null,
    unit = runCatching { ActivityUnit.valueOf(unit) }.getOrDefault(ActivityUnit.Boolean),
    active = true, archived = false, sortOrder = sortOrder, createdAt = createdAt, updatedAt = updatedAt,
)

private fun legacyWeeklyFrequencyTarget(
    targetCount: Int?,
    targetPeriod: String?,
): Int? {
    val count = targetCount ?: return null
    return when (targetPeriod) {
        TargetPeriod.Week.name -> count.coerceIn(2, 7)
        TargetPeriod.Month.name -> ((count + 3) / 4).coerceIn(2, 7)
        else -> null
    }
}

private fun isCustomActivityId(activityId: String): Boolean =
    activityId.startsWith("act_custom_") || (!activityId.startsWith("act_") && !activityId.startsWith("sup_"))

internal fun ActivityLogEntity.toDomain(): ActivityLog =
    ActivityLog(
        activityId = activityId,
        date = date,
        completed = completed,
        actualValue = actualValue,
        note = note,
        updatedAt = updatedAt,
    )

internal fun AbstinenceTrackEntity.toDomain(): AbstinenceTrack =
    AbstinenceTrack(
        id = id,
        name = name,
        substanceLabel = substanceLabel,
        severity = runCatching { AbstinenceSeverity.valueOf(severity) }.getOrDefault(AbstinenceSeverity.Moderate),
        contributionRole = runCatching { ContributionRole.valueOf(contributionRole) }.getOrDefault(ContributionRole.Protective),
        importanceTier = runCatching { ImportanceTier.valueOf(importanceTier) }.getOrDefault(ImportanceTier.Medium),
        active = active,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun AbstinenceLogEntity.toDomain(): AbstinenceLog =
    AbstinenceLog(
        trackId = trackId,
        date = date,
        status = runCatching { AbstinenceStatus.valueOf(status) }.getOrDefault(AbstinenceStatus.Unknown),
        urge = urge,
        urgeIntensity = urgeIntensity,
        note = note,
        updatedAt = updatedAt,
    )

internal fun RiskEventEntity.toDomain(): RiskEvent =
    RiskEvent(
        id = id,
        date = date,
        createdAt = createdAt,
        intensity = intensity,
        trigger = trigger,
        actionTaken = actionTaken,
        actedOnImpulse = actedOnImpulse,
        note = note,
    )

internal fun TaskEntity.toDomain(): Task =
    Task(
        id = id,
        title = title,
        description = description,
        layerId = layerId,
        projectId = projectId,
        status = runCatching { TaskStatus.valueOf(status) }.getOrDefault(TaskStatus.Pending),
        contributionRole = runCatching { ContributionRole.valueOf(contributionRole) }.getOrDefault(ContributionRole.Neutral),
        importanceTier = runCatching { ImportanceTier.valueOf(importanceTier) }.getOrDefault(ImportanceTier.Medium),
        dueDate = dueDate,
        completedAt = completedAt,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun AnchorPhraseEntity.toDomain(): AnchorPhrase =
    AnchorPhrase(
        id = id,
        text = text,
        authorReference = authorReference,
        family = runCatching { PhraseFamily.valueOf(family) }.getOrDefault(PhraseFamily.Containment),
        language = language,
        attributionStatus = runCatching { AttributionStatus.valueOf(attributionStatus) }.getOrDefault(AttributionStatus.NeedsReview),
        active = active,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun SleepLogEntity.toDomain(): SleepLog =
    SleepLog(
        date = date,
        plannedSleepAt = plannedSleepAt,
        plannedWakeAt = plannedWakeAt,
        sleptAt = sleptAt,
        wokeAt = wokeAt,
        quality = runCatching { SleepQuality.valueOf(quality) }.getOrDefault(SleepQuality.Acceptable),
        note = note,
        updatedAt = updatedAt,
    )

internal fun SleepConfigEntity.toDomain(): SleepConfig =
    SleepConfig(
        id = id,
        targetSleepAt = targetSleepAt,
        targetWakeAt = targetWakeAt,
        digitalWindDownMinutes = digitalWindDownMinutes,
        updatedAt = updatedAt,
    )

internal fun SleepSessionStateEntity.toDomain(): SleepSessionState =
    SleepSessionState(
        id = id,
        date = date,
        startedAt = startedAt,
        updatedAt = updatedAt,
    )

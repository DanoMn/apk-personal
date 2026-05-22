package dev.panopt.autonomia.data.local.mapper

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityRole
import dev.panopt.autonomia.ActivityType
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.AttributionStatus
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.DisplaySurface
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.PhraseFamily
import dev.panopt.autonomia.RiskEvent
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.data.AbstinenceLogEntity
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityEntity
import dev.panopt.autonomia.data.ActivityLogEntity
import dev.panopt.autonomia.data.AnchorPhraseEntity
import dev.panopt.autonomia.data.LayerEntity
import dev.panopt.autonomia.data.RiskEventEntity
import dev.panopt.autonomia.data.SleepLogEntity
import dev.panopt.autonomia.data.TaskEntity
import dev.panopt.autonomia.domain.activity.ActivityDefinition

internal fun LayerEntity.toDomain(): Layer =
    Layer(id = id, name = name, description = description, sortOrder = sortOrder, active = active)

internal fun ActivityEntity.toDomain(): ActivityDefinition =
    ActivityDefinition(
        id = id,
        layerId = layerId,
        name = name,
        description = description,
        type = runCatching { ActivityType.valueOf(type) }.getOrDefault(ActivityType.Check),
        role = runCatching { ActivityRole.valueOf(role) }.getOrDefault(ActivityRole.Practice),
        displaySurface = runCatching { DisplaySurface.valueOf(displaySurface) }.getOrDefault(DisplaySurface.PrimaryChecklist),
        contributionRole = runCatching { ContributionRole.valueOf(contributionRole) }.getOrDefault(ContributionRole.Core),
        importanceTier = runCatching { ImportanceTier.valueOf(importanceTier) }.getOrDefault(ImportanceTier.Medium),
        cadence = cadence?.let { runCatching { ActivityCadence.valueOf(it) }.getOrNull() },
        targetValue = targetValue,
        minimumValue = minimumValue,
        targetCount = targetCount,
        targetPeriod = targetPeriod?.let { runCatching { TargetPeriod.valueOf(it) }.getOrNull() },
        unit = runCatching { ActivityUnit.valueOf(unit) }.getOrDefault(ActivityUnit.Boolean),
        active = active,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

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


package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal fun ActivityDefinition.targetDays(): Int {
    val fallback = when (cadence) {
        ActivityCadence.Daily -> 7
        ActivityCadence.Weekly -> 1
        ActivityCadence.Monthly -> 1
        ActivityCadence.Custom,
        ActivityCadence.EventBased,
        null -> 1
    }
    return (weeklyFrequencyTarget ?: targetCount ?: fallback).coerceIn(1, 7)
}

internal fun ActivityDefinition.targetDailyValue(): Int =
    when (unit) {
        ActivityUnit.Boolean,
        ActivityUnit.Text -> 1
        ActivityUnit.Minutes,
        ActivityUnit.Count,
        ActivityUnit.Time -> sessionTargetMinutes ?: targetValue ?: minimumValue ?: 1
    }.coerceAtLeast(1)

internal fun ActivityLog.countsAsDone(): Boolean {
    if (completed) return true
    return (actualValue ?: 0) > 0
}

internal fun ActivityLog.dateAsLocalDate(): LocalDate? =
    runCatching { LocalDate.parse(date) }.getOrNull()

internal fun Task.isScoringTaskCompletedIn(weekStart: LocalDate, today: LocalDate): Boolean {
    if (status != TaskStatus.Done || layerId == null || contributionRole == ContributionRole.Neutral) return false
    val completed = completedAt ?: return false
    val completedDate = Instant.ofEpochMilli(completed)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return completedDate in weekStart..today
}

internal fun LocalDate.datesUntilInclusive(end: LocalDate): List<LocalDate> {
    val days = ChronoUnit.DAYS.between(this, end).coerceAtLeast(0)
    return (0..days).map { plusDays(it) }
}

internal fun Iterable<Float>.averageOrZero(): Float {
    val values = toList()
    return if (values.isEmpty()) 0f else values.sum() / values.size
}

internal fun Iterable<Float>.averageOrNull(): Float? {
    val values = toList()
    return if (values.isEmpty()) null else values.sum() / values.size
}

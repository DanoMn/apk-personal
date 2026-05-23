package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.TargetPeriod

fun ActivityDefinition.isAnchor(): Boolean =
    activityType == ActivitySurface.Anchor && !isGoal()

fun ActivityDefinition.isSupport(): Boolean =
    activityType == ActivitySurface.Support && !isGoal()

fun ActivityDefinition.isGoal(): Boolean =
    cadence == ActivityCadence.Weekly ||
        cadence == ActivityCadence.Monthly ||
        targetPeriod == TargetPeriod.Week ||
        targetPeriod == TargetPeriod.Month

fun ActivityDefinition.defaultActualValue(): Int? =
    when (unit) {
        ActivityUnit.Minutes,
        ActivityUnit.Count,
        ActivityUnit.Time -> targetValue ?: minimumValue
        ActivityUnit.Boolean,
        ActivityUnit.Text -> targetValue
    }

fun ActivityDefinition.progressFor(log: ActivityLog?): Float {
    if (log == null) return 0f
    if (log.completed && unit == ActivityUnit.Boolean) return 1f
    val target = targetValue ?: minimumValue ?: if (unit == ActivityUnit.Boolean) 1 else 0
    if (target <= 0) return if (log.completed) 1f else 0f
    val actual = log.actualValue ?: if (log.completed) target else 0
    return (actual.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

fun ActivityDefinition.goalProgress(logs: List<ActivityLog>): Float {
    val expectedCount = targetCount ?: 1
    val target = when (unit) {
        ActivityUnit.Minutes,
        ActivityUnit.Count,
        ActivityUnit.Time -> (targetValue ?: minimumValue ?: 1) * expectedCount
        ActivityUnit.Boolean,
        ActivityUnit.Text -> expectedCount
    }.coerceAtLeast(1)
    val actual = logs.sumOf { log ->
        val defaultValue = if (log.completed) {
            when (unit) {
                ActivityUnit.Minutes,
                ActivityUnit.Count,
                ActivityUnit.Time -> targetValue ?: minimumValue ?: 1
                ActivityUnit.Boolean,
                ActivityUnit.Text -> 1
            }
        } else {
            0
        }
        (log.actualValue ?: defaultValue).coerceAtLeast(0)
    }
    return (actual.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

fun ActivityDefinition.importanceWeight(): Float =
    importanceTier.importanceWeight()

fun ImportanceTier.importanceWeight(): Float =
    when (this) {
        ImportanceTier.Low -> 0.75f
        ImportanceTier.Medium -> 1f
        ImportanceTier.High -> 1.20f
        ImportanceTier.Critical -> 1.35f
    }


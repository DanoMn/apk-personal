package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.ActivityUnit
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.TargetPeriod

/**
 * Invariante de dominio "ancla = solo Minutes".
 *
 * Una actividad configurada como ancla ([ActivitySurface.Anchor]) DEBE medirse en minutos: el
 * motor de scoring (`AnchorScoringPolicyV2`) y el adapter de hechos asumen `mins = actualValue`
 * sin conversión multi-unidad. `Boolean`/`Count`/`Time`/`Text` no son anclas válidas.
 */
fun ActivityUnit.isValidForAnchor(): Boolean =
    this == ActivityUnit.Minutes

/**
 * Aplica el invariante "ancla = solo Minutes" en los puntos donde se asigna la surface
 * [ActivitySurface.Anchor]. Rechaza unidades ilegales con [IllegalArgumentException] (decisión
 * del design: rechazo, no normalización silenciosa).
 */
fun requireAnchorUnit(unit: ActivityUnit) {
    require(unit.isValidForAnchor()) {
        "An anchor must be measured in Minutes; got $unit"
    }
}

fun ActivityDefinition.isAnchor(): Boolean =
    activityType == ActivitySurface.Anchor

fun ActivityDefinition.isSupport(): Boolean =
    activityType == ActivitySurface.Support

fun ActivityDefinition.isGoal(): Boolean =
    cadence == ActivityCadence.Weekly ||
        cadence == ActivityCadence.Monthly ||
        targetPeriod == TargetPeriod.Week ||
        targetPeriod == TargetPeriod.Month

fun ActivityDefinition.defaultActualValue(): Int? =
    when (unit) {
        ActivityUnit.Minutes,
        ActivityUnit.Count,
        ActivityUnit.Time -> sessionTargetMinutes ?: targetValue ?: minimumValue
        ActivityUnit.Boolean,
        ActivityUnit.Text -> targetValue
    }

fun ActivityDefinition.progressFor(log: ActivityLog?): Float {
    if (log == null) return 0f
    if (log.completed && unit == ActivityUnit.Boolean) return 1f
    val target = sessionTargetMinutes ?: targetValue ?: minimumValue ?: if (unit == ActivityUnit.Boolean) 1 else 0
    if (target <= 0) return if (log.completed) 1f else 0f
    val actual = log.actualValue ?: if (log.completed) target else 0
    return (actual.toFloat() / target.toFloat()).coerceIn(0f, 1f)
}

fun ActivityDefinition.goalProgress(logs: List<ActivityLog>): Float {
    val expectedCount = weeklyFrequencyTarget ?: targetCount ?: 1
    val target = when (unit) {
        ActivityUnit.Minutes,
        ActivityUnit.Count,
        ActivityUnit.Time -> (sessionTargetMinutes ?: targetValue ?: minimumValue ?: 1) * expectedCount
        ActivityUnit.Boolean,
        ActivityUnit.Text -> expectedCount
    }.coerceAtLeast(1)
    val actual = logs.sumOf { log ->
        val defaultValue = if (log.completed) {
            when (unit) {
                ActivityUnit.Minutes,
                ActivityUnit.Count,
                ActivityUnit.Time -> sessionTargetMinutes ?: targetValue ?: minimumValue ?: 1
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

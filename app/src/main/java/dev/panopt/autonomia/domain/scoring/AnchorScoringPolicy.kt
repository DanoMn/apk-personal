package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import kotlin.math.exp

internal object AnchorScoringPolicy {
    fun evaluate(activity: ActivityDefinition, logs: List<ActivityLog>): AnchorEvaluation {
        val targetDays = activity.targetDays()
        val targetDailyValue = activity.targetDailyValue()
        val targetWeeklyValue = (targetDays * targetDailyValue).coerceAtLeast(1)
        val doneDates = logs
            .filter { it.countsAsDone() }
            .mapNotNull { it.dateAsLocalDate() }
            .toSet()
        val actualValue = logs.sumOf { log ->
            val fallback = if (log.countsAsDone()) targetDailyValue else 0
            (log.actualValue ?: fallback).coerceAtLeast(0)
        }
        val frequencyRatio = doneDates.size.toFloat() / targetDays.toFloat()
        val valueRatio = actualValue.toFloat() / targetWeeklyValue.toFloat()
        val frequencyScore = frequencyRatio.coerceIn(0f, 1f)
        val valueScore = valueRatio.coerceIn(0f, 1f)
        val baseScore = ScoringConstants.ANCHOR_FREQUENCY_WEIGHT * frequencyScore +
            ScoringConstants.ANCHOR_VALUE_WEIGHT * valueScore
        val frequencySurplusBonus = surplusBonus(frequencyRatio - 1f)
        val valueSurplusBonus = surplusBonus(valueRatio - 1f)
        val surplusBonus = ScoringConstants.ANCHOR_FREQUENCY_WEIGHT * frequencySurplusBonus +
            ScoringConstants.ANCHOR_VALUE_WEIGHT * valueSurplusBonus
        return AnchorEvaluation(
            baseScore = baseScore.coerceIn(0f, 1f),
            surplusBonus = surplusBonus.coerceIn(0f, ScoringConstants.ANCHOR_SURPLUS_MAX),
        )
    }

    private fun surplusBonus(surplusMagnitude: Float): Float {
        if (surplusMagnitude <= 0f) return 0f
        return (0.100f * (1f - exp(-surplusMagnitude / 2f))).coerceIn(0f, 0.100f)
    }
}

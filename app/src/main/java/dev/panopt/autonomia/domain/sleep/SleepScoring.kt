package dev.panopt.autonomia.domain.sleep

import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.SleepQuality

object SleepScoring {
    fun score(log: SleepLog): Float {
        val plannedMinutes = SleepPolicy.plannedWindowMinutes(
            plannedSleepAt = log.plannedSleepAt,
            plannedWakeAt = log.plannedWakeAt,
        ) ?: SleepPolicy.DEFAULT_SLEEP_WINDOW_MINUTES
        val actualMinutes = SleepPolicy.minutesBetween(log.sleptAt, log.wokeAt) ?: 0
        val durationScore = when {
            actualMinutes <= 0 -> 0f
            actualMinutes <= plannedMinutes -> actualMinutes.toFloat() / plannedMinutes.toFloat()
            actualMinutes <= plannedMinutes + 90 -> 1f
            else -> (1f - ((actualMinutes - plannedMinutes - 90).toFloat() / 240f)).coerceIn(0.50f, 1f)
        }
        val scheduleScore = averageOf(
            SleepPolicy.scheduleCloseness(log.sleptAt, log.plannedSleepAt),
            SleepPolicy.scheduleCloseness(log.wokeAt, log.plannedWakeAt),
        )
        val qualityScore = when (log.quality) {
            SleepQuality.Low -> 0.35f
            SleepQuality.Acceptable -> 0.72f
            SleepQuality.Good -> 1f
        }
        return (durationScore * 0.55f + scheduleScore * 0.25f + qualityScore * 0.20f).coerceIn(0f, 1f)
    }

    private fun averageOf(first: Float, second: Float): Float =
        (first + second) / 2f
}


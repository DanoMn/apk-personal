package dev.panopt.autonomia.domain.sleep

import dev.panopt.autonomia.SleepLog

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
        return (durationScore * 0.70f + scheduleScore * 0.30f).coerceIn(0f, 1f)
    }

    private fun averageOf(first: Float, second: Float): Float =
        (first + second) / 2f
}

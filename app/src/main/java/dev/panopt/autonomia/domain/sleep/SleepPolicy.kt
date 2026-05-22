package dev.panopt.autonomia.domain.sleep

import java.time.LocalTime
import kotlin.math.abs

object SleepPolicy {
    const val MIN_SLEEP_WINDOW_MINUTES: Int = 300
    const val DEFAULT_SLEEP_WINDOW_MINUTES: Int = 480

    fun validatePlannedWindow(plannedSleepAt: String, plannedWakeAt: String): SleepWindowValidation {
        val minutes = minutesBetween(plannedSleepAt, plannedWakeAt)
            ?: return SleepWindowValidation.Invalid("Horario de sueno invalido.")
        return if (minutes >= MIN_SLEEP_WINDOW_MINUTES) {
            SleepWindowValidation.Valid(minutes)
        } else {
            SleepWindowValidation.Invalid("La ventana de sueno debe ser de al menos 5 horas.")
        }
    }

    fun plannedWindowMinutes(plannedSleepAt: String, plannedWakeAt: String): Int? =
        minutesBetween(plannedSleepAt, plannedWakeAt)?.takeIf { it >= MIN_SLEEP_WINDOW_MINUTES }

    fun minutesBetween(start: String, end: String): Int? {
        val startTime = parseTime(start) ?: return null
        val endTime = parseTime(end) ?: return null
        val startMinutes = startTime.hour * 60 + startTime.minute
        val endMinutes = endTime.hour * 60 + endTime.minute
        val raw = endMinutes - startMinutes
        return if (raw >= 0) raw else raw + 24 * 60
    }

    fun scheduleCloseness(actual: String, planned: String): Float {
        val actualTime = parseTime(actual) ?: return 0f
        val plannedTime = parseTime(planned) ?: return 0f
        val distance = circularMinutesDistance(actualTime, plannedTime)
        return (1f - (distance.toFloat() / 120f)).coerceIn(0f, 1f)
    }

    private fun parseTime(value: String): LocalTime? =
        runCatching { LocalTime.parse(value) }.getOrNull()

    private fun circularMinutesDistance(a: LocalTime, b: LocalTime): Int {
        val aMinutes = a.hour * 60 + a.minute
        val bMinutes = b.hour * 60 + b.minute
        val raw = abs(aMinutes - bMinutes)
        return minOf(raw, 24 * 60 - raw)
    }
}

sealed interface SleepWindowValidation {
    data class Valid(val minutes: Int) : SleepWindowValidation
    data class Invalid(val message: String) : SleepWindowValidation
}


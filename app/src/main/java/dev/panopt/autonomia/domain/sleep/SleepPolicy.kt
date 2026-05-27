package dev.panopt.autonomia.domain.sleep

import dev.panopt.autonomia.SleepConfig
import java.time.LocalTime
import kotlin.math.abs

object SleepPolicy {
    const val DEFAULT_CONFIG_ID: String = "default"
    const val DEFAULT_SESSION_ID: String = "default"
    const val DEFAULT_TARGET_SLEEP_AT: String = "23:30"
    const val DEFAULT_TARGET_WAKE_AT: String = "07:30"
    const val DEFAULT_DIGITAL_WIND_DOWN_MINUTES: Int = 0
    const val MIN_SLEEP_WINDOW_MINUTES: Int = 300
    const val DEFAULT_SLEEP_WINDOW_MINUTES: Int = 480
    val allowedDigitalWindDownMinutes: Set<Int> = setOf(0, 10, 20, 30, 45, 60)

    fun defaultConfig(): SleepConfig =
        SleepConfig(
            id = DEFAULT_CONFIG_ID,
            targetSleepAt = DEFAULT_TARGET_SLEEP_AT,
            targetWakeAt = DEFAULT_TARGET_WAKE_AT,
            digitalWindDownMinutes = DEFAULT_DIGITAL_WIND_DOWN_MINUTES,
        )

    fun validateConfig(
        targetSleepAt: String,
        targetWakeAt: String,
        digitalWindDownMinutes: Int,
    ): SleepConfigValidation {
        val normalizedSleepAt = targetSleepAt.trim()
        val normalizedWakeAt = targetWakeAt.trim()
        val minutes = minutesBetween(normalizedSleepAt, normalizedWakeAt)
            ?: return SleepConfigValidation.Invalid("Horario de sueno invalido.")
        if (minutes < MIN_SLEEP_WINDOW_MINUTES) {
            return SleepConfigValidation.Invalid("La ventana minima es de 5 horas.")
        }
        if (digitalWindDownMinutes !in allowedDigitalWindDownMinutes) {
            return SleepConfigValidation.Invalid("Descanso digital fuera de rango.")
        }
        return SleepConfigValidation.Valid(
            SleepConfig(
                id = DEFAULT_CONFIG_ID,
                targetSleepAt = normalizedSleepAt,
                targetWakeAt = normalizedWakeAt,
                digitalWindDownMinutes = digitalWindDownMinutes,
            ),
        )
    }

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

    fun formatDuration(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return if (rest == 0) "${hours}h" else "${hours}h ${rest}m"
    }
}

sealed interface SleepWindowValidation {
    data class Valid(val minutes: Int) : SleepWindowValidation
    data class Invalid(val message: String) : SleepWindowValidation
}

sealed interface SleepConfigValidation {
    data class Valid(val config: SleepConfig) : SleepConfigValidation
    data class Invalid(val message: String) : SleepConfigValidation
}

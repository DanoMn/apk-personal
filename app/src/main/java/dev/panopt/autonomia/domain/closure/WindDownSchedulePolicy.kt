package dev.panopt.autonomia.domain.closure

import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException

/**
 * Pure domain policy for computing the initial delay until the next occurrence
 * of a wind-down notification time.
 *
 * Mirrors [DailyClosureSchedulePolicy] — same shape, anchored to a configurable
 * local time instead of midnight+1 min.
 */
object WindDownSchedulePolicy {

    /**
     * Computes how long to wait from [now] until the next occurrence of [targetSleepAt].
     *
     * If the target time is in the past for today, the delay is calculated to
     * the same time tomorrow. Returns [Duration.ZERO] if parsing fails or the
     * resulting delay would be negative.
     *
     * @param now           Current zoned moment.
     * @param targetSleepAt HH:mm string representing the daily reminder time.
     * @return Non-negative [Duration].
     */
    fun initialDelay(now: ZonedDateTime, targetSleepAt: String): Duration {
        val target = try {
            LocalTime.parse(targetSleepAt)
        } catch (_: DateTimeParseException) {
            return Duration.ZERO
        }

        val todayTarget = now.toLocalDate()
            .atTime(target)
            .atZone(now.zone)

        val nextTarget = if (now.isBefore(todayTarget)) {
            todayTarget
        } else {
            todayTarget.plusDays(1)
        }

        return Duration.between(now, nextTarget).coerceAtLeast(Duration.ZERO)
    }
}

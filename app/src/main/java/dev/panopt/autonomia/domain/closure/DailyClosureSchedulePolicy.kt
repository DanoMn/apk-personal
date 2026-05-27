package dev.panopt.autonomia.domain.closure

import java.time.Duration
import java.time.ZonedDateTime

object DailyClosureSchedulePolicy {
    private const val CLOSURE_OFFSET_MINUTES = 1L

    fun nextClosureAt(now: ZonedDateTime): ZonedDateTime {
        val todayClosure = now.toLocalDate()
            .atStartOfDay(now.zone)
            .plusMinutes(CLOSURE_OFFSET_MINUTES)

        return if (now.isBefore(todayClosure)) {
            todayClosure
        } else {
            now.toLocalDate()
                .plusDays(1)
                .atStartOfDay(now.zone)
                .plusMinutes(CLOSURE_OFFSET_MINUTES)
        }
    }

    fun initialDelay(now: ZonedDateTime): Duration =
        Duration.between(now, nextClosureAt(now)).coerceAtLeast(Duration.ZERO)
}

package dev.panopt.autonomia.domain.closure

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyClosureSchedulePolicyTest {
    private val zoneId = ZoneId.of("America/Lima")

    @Test
    fun schedulesTodayClosureWhenAppStartsBeforeMidnightWindowCloses() {
        val now = ZonedDateTime.of(2026, 5, 27, 0, 0, 30, 0, zoneId)

        assertEquals(
            ZonedDateTime.of(2026, 5, 27, 0, 1, 0, 0, zoneId),
            DailyClosureSchedulePolicy.nextClosureAt(now),
        )
    }

    @Test
    fun schedulesTomorrowClosureAfterTodayClosureWindow() {
        val now = ZonedDateTime.of(2026, 5, 27, 12, 0, 0, 0, zoneId)

        assertEquals(
            ZonedDateTime.of(2026, 5, 28, 0, 1, 0, 0, zoneId),
            DailyClosureSchedulePolicy.nextClosureAt(now),
        )
    }

    @Test
    fun delayNeverGoesNegativeAtClosureInstant() {
        val now = ZonedDateTime.of(2026, 5, 27, 0, 1, 0, 0, zoneId)

        assertEquals(
            24L * 60L * 60L * 1000L,
            DailyClosureSchedulePolicy.initialDelay(now).toMillis(),
        )
    }
}

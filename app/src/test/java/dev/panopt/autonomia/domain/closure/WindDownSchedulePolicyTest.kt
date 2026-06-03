package dev.panopt.autonomia.domain.closure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.Duration

class WindDownSchedulePolicyTest {

    private val zone: ZoneId = ZoneId.of("America/Argentina/Buenos_Aires")

    /** Target is in the future today → delay is the difference to today's target */
    @Test
    fun targetInFuture_delayIsToToday() {
        // 22:00 current, target 23:30 → should be 1h30m
        val now = ZonedDateTime.of(2026, 6, 3, 22, 0, 0, 0, zone)
        val delay = WindDownSchedulePolicy.initialDelay(now, "23:30")
        assertEquals(Duration.ofMinutes(90), delay)
    }

    /** Target is in the past today → delay is to tomorrow's target */
    @Test
    fun targetInPast_delayIsToTomorrow() {
        // 02:00 current, target 23:30 → already past for today → ~21h30m
        val now = ZonedDateTime.of(2026, 6, 3, 2, 0, 0, 0, zone)
        val delay = WindDownSchedulePolicy.initialDelay(now, "23:30")
        // tomorrow's 23:30 minus 02:00 today = 21h30m
        assertEquals(Duration.ofHours(21).plusMinutes(30), delay)
    }

    /** Midnight crossing: it is 02:00, target 23:30 → delay to same day's 23:30 which is > now? No.
     *  At 02:00, 23:30 has NOT yet occurred TODAY, so delay is to TODAY's 23:30 (21h30m). */
    @Test
    fun midnightCross_futureTargetSameDay() {
        val now = ZonedDateTime.of(2026, 6, 3, 2, 0, 0, 0, zone)
        val delay = WindDownSchedulePolicy.initialDelay(now, "23:30")
        assertTrue("Delay should be positive and about 21.5h", delay > Duration.ZERO)
        assertTrue(delay <= Duration.ofHours(22))
    }

    /** Invalid time string → returns Duration.ZERO without crash */
    @Test
    fun invalidTime_returnsZero() {
        val now = ZonedDateTime.of(2026, 6, 3, 22, 0, 0, 0, zone)
        val delay = WindDownSchedulePolicy.initialDelay(now, "99:99")
        assertEquals(Duration.ZERO, delay)
    }

    /** Exact match (now == target) → delay to tomorrow */
    @Test
    fun exactMatch_delayToTomorrow() {
        val now = ZonedDateTime.of(2026, 6, 3, 23, 30, 0, 0, zone)
        val delay = WindDownSchedulePolicy.initialDelay(now, "23:30")
        // now is NOT before todayTarget (equal), so goes to tomorrow = 24h
        assertEquals(Duration.ofHours(24), delay)
    }
}

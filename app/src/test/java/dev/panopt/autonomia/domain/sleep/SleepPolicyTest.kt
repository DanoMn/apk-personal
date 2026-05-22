package dev.panopt.autonomia.domain.sleep

import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.SleepQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepPolicyTest {
    @Test
    fun rejectsPlannedWindowUnderFiveHours() {
        val result = SleepPolicy.validatePlannedWindow("01:00", "05:59")

        assertTrue(result is SleepWindowValidation.Invalid)
    }

    @Test
    fun acceptsFiveHourPlannedWindow() {
        val result = SleepPolicy.validatePlannedWindow("01:00", "06:00")

        assertEquals(SleepWindowValidation.Valid(300), result)
    }

    @Test
    fun scoringUsesPersonalTargetWithoutUpperLimit() {
        val eightHourScore = SleepScoring.score(
            sleep(plannedSleepAt = "23:00", plannedWakeAt = "07:00", sleptAt = "23:00", wokeAt = "07:00"),
        )
        val elevenHourScore = SleepScoring.score(
            sleep(plannedSleepAt = "21:00", plannedWakeAt = "08:00", sleptAt = "21:00", wokeAt = "08:00"),
        )

        assertEquals(1f, eightHourScore, 0.001f)
        assertEquals(1f, elevenHourScore, 0.001f)
    }

    private fun sleep(
        plannedSleepAt: String,
        plannedWakeAt: String,
        sleptAt: String,
        wokeAt: String,
    ): SleepLog =
        SleepLog(
            date = "2026-05-21",
            plannedSleepAt = plannedSleepAt,
            plannedWakeAt = plannedWakeAt,
            sleptAt = sleptAt,
            wokeAt = wokeAt,
            quality = SleepQuality.Good,
        )
}


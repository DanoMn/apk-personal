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
    fun acceptsFiveHourConfigAcrossMidnight() {
        val result = SleepPolicy.validateConfig(
            targetSleepAt = "23:30",
            targetWakeAt = "04:30",
            digitalWindDownMinutes = 20,
        )

        assertTrue(result is SleepConfigValidation.Valid)
        assertEquals(300, SleepPolicy.minutesBetween("23:30", "04:30"))
    }

    @Test
    fun rejectsConfigUnderFiveHours() {
        val result = SleepPolicy.validateConfig(
            targetSleepAt = "23:30",
            targetWakeAt = "04:29",
            digitalWindDownMinutes = 20,
        )

        assertEquals(
            SleepConfigValidation.Invalid("La ventana minima es de 5 horas."),
            result,
        )
    }

    @Test
    fun validatesAllowedDigitalWindDownChips() {
        val valid = SleepPolicy.validateConfig(
            targetSleepAt = "23:30",
            targetWakeAt = "07:30",
            digitalWindDownMinutes = 45,
        )
        val invalid = SleepPolicy.validateConfig(
            targetSleepAt = "23:30",
            targetWakeAt = "07:30",
            digitalWindDownMinutes = 15,
        )

        assertTrue(valid is SleepConfigValidation.Valid)
        assertEquals(
            SleepConfigValidation.Invalid("Descanso digital fuera de rango."),
            invalid,
        )
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

    @Test
    fun scoringIgnoresSubjectiveQuality() {
        val lowQuality = sleep(
            plannedSleepAt = "23:30",
            plannedWakeAt = "07:30",
            sleptAt = "23:30",
            wokeAt = "07:30",
            quality = SleepQuality.Low,
        )
        val goodQuality = lowQuality.copy(quality = SleepQuality.Good)

        assertEquals(SleepScoring.score(goodQuality), SleepScoring.score(lowQuality), 0.001f)
    }

    private fun sleep(
        plannedSleepAt: String,
        plannedWakeAt: String,
        sleptAt: String,
        wokeAt: String,
        quality: SleepQuality = SleepQuality.Good,
    ): SleepLog =
        SleepLog(
            date = "2026-05-21",
            plannedSleepAt = plannedSleepAt,
            plannedWakeAt = plannedWakeAt,
            sleptAt = sleptAt,
            wokeAt = wokeAt,
            quality = quality,
        )
}

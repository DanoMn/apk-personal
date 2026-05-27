package dev.panopt.autonomia.data.local.mapper

import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.DailyActivityStatus
import dev.panopt.autonomia.data.DailyActivityLogEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyActivityLogMapperTest {
    @Test
    fun anchorDoneMapsToCompletedPractice() {
        val log = entity(
            subjectType = ActivitySurface.Anchor.name,
            status = DailyActivityStatus.Done.name,
        ).toDomain()

        assertTrue(log.completed)
        assertEquals(DailyActivityStatus.Done, log.status)
    }

    @Test
    fun supportDoneDoesNotMapToOmission() {
        val log = entity(
            subjectType = ActivitySurface.Support.name,
            status = DailyActivityStatus.Done.name,
        ).toDomain()

        assertFalse(log.completed)
        assertEquals(DailyActivityStatus.Done, log.status)
    }

    @Test
    fun supportOmittedMapsToExistingOmissionContract() {
        val log = entity(
            subjectType = ActivitySurface.Support.name,
            status = DailyActivityStatus.Omitted.name,
        ).toDomain()

        assertTrue(log.completed)
        assertEquals(DailyActivityStatus.Omitted, log.status)
    }

    private fun entity(
        subjectType: String,
        status: String,
    ): DailyActivityLogEntity =
        DailyActivityLogEntity(
            date = "2026-05-27",
            timezoneId = "America/Lima",
            subjectType = subjectType,
            subjectId = "subject",
            layerId = "layer_cuerpo",
            status = status,
            actualValue = 1,
            createdAt = 1L,
            updatedAt = 1L,
        )
}

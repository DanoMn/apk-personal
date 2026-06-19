package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.data.UserActivityConfigEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * R6 — Editar config preserva campos de estado (`createdAt`, `active`, `archived`, `sortOrder`);
 * crear conserva el comportamiento actual. Dominio puro: sin Room ni Compose.
 */
class ConfigEditRuleTest {

    private val activityId = "act_custom_1"

    private fun previous(
        createdAt: Long,
        active: Boolean = true,
        archived: Boolean = false,
        sortOrder: Int = 0,
    ): UserActivityConfigEntity = UserActivityConfigEntity(
        activityId = activityId,
        activityType = ActivitySurface.Anchor.name,
        active = active,
        archived = archived,
        customName = "viejo",
        customDescription = "desc vieja",
        cadence = ActivityCadence.Weekly.name,
        targetValue = 10,
        minimumValue = 1,
        targetCount = 3,
        targetPeriod = TargetPeriod.Week.name,
        weeklyFrequencyTarget = 3,
        sessionTargetMinutes = 10,
        commitmentDurationMonths = null,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    // R6-E1 — editar preserva createdAt=T0, actualiza updatedAt=T1
    @Test
    fun `edit preserves createdAt and updates updatedAt`() {
        val t0 = 1_000L
        val t1 = 2_000L
        val result = ConfigEditRule.resolve(
            previous = previous(createdAt = t0),
            activityId = activityId,
            activityType = ActivitySurface.Anchor,
            cadence = ActivityCadence.Weekly,
            targetValue = 20,
            minimumValue = 1,
            targetCount = 5,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 5,
            sessionTargetMinutes = 20,
            commitmentDurationMonths = null,
            customName = "nuevo",
            customDescription = "desc nueva",
            now = t1,
        )

        assertEquals(t0, result.createdAt)
        assertEquals(t1, result.updatedAt)
        // los campos de configuración toman los valores nuevos
        assertEquals(20, result.sessionTargetMinutes)
        assertEquals(5, result.weeklyFrequencyTarget)
        assertEquals("nuevo", result.customName)
    }

    // R6-E2 — editar no reactiva una archivada
    @Test
    fun `edit does not silently reactivate an archived config`() {
        val result = ConfigEditRule.resolve(
            previous = previous(createdAt = 1_000L, active = false, archived = true),
            activityId = activityId,
            activityType = ActivitySurface.Anchor,
            cadence = ActivityCadence.Weekly,
            targetValue = 20,
            minimumValue = 1,
            targetCount = 5,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 5,
            sessionTargetMinutes = 20,
            commitmentDurationMonths = null,
            customName = "nuevo",
            customDescription = null,
            now = 2_000L,
        )

        assertFalse(result.active)
        assertTrue(result.archived)
    }

    // R6-E3 — editar preserva sortOrder
    @Test
    fun `edit preserves sortOrder`() {
        val s0 = 42
        val result = ConfigEditRule.resolve(
            previous = previous(createdAt = 1_000L, sortOrder = s0),
            activityId = activityId,
            activityType = ActivitySurface.Anchor,
            cadence = ActivityCadence.Weekly,
            targetValue = 20,
            minimumValue = 1,
            targetCount = 5,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 5,
            sessionTargetMinutes = 20,
            commitmentDurationMonths = null,
            customName = "nuevo",
            customDescription = null,
            now = 2_000L,
        )

        assertEquals(s0, result.sortOrder)
    }

    // R6-E4 — crear (previa == null) conserva el comportamiento actual
    @Test
    fun `create with no previous config uses now and defaults`() {
        val now = 5_000L
        val result = ConfigEditRule.resolve(
            previous = null,
            activityId = activityId,
            activityType = ActivitySurface.Anchor,
            cadence = ActivityCadence.Weekly,
            targetValue = 20,
            minimumValue = 1,
            targetCount = 5,
            targetPeriod = TargetPeriod.Week,
            weeklyFrequencyTarget = 5,
            sessionTargetMinutes = 20,
            commitmentDurationMonths = null,
            customName = "nuevo",
            customDescription = null,
            now = now,
        )

        assertEquals(now, result.createdAt)
        assertEquals(now, result.updatedAt)
        assertTrue(result.active)
        assertFalse(result.archived)
        assertEquals(now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), result.sortOrder)
        assertEquals(activityId, result.activityId)
        assertEquals(ActivitySurface.Anchor.name, result.activityType)
    }

    // Camino límite: el customName en blanco se normaliza a null (paridad con configureActivity)
    @Test
    fun `blank customName is normalized to null`() {
        val result = ConfigEditRule.resolve(
            previous = null,
            activityId = activityId,
            activityType = ActivitySurface.Support,
            cadence = null,
            targetValue = null,
            minimumValue = null,
            targetCount = null,
            targetPeriod = null,
            weeklyFrequencyTarget = null,
            sessionTargetMinutes = null,
            commitmentDurationMonths = null,
            customName = "   ",
            customDescription = "  ",
            now = 1_000L,
        )

        assertEquals(null, result.customName)
        assertEquals(null, result.customDescription)
    }
}

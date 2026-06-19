package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.data.UserActivityConfigEntity

/**
 * R9 — Construye la config de un **soporte** válida.
 *
 * Reglas de superficie del soporte: `activityType = Support`, SIN targets (todos los campos de target
 * en `null`), `active = true`, `archived = false`. Esta es la misma forma que produce
 * `AutonomiaRepository.addSupport`; centralizarla acá garantiza que la creación de soporte custom
 * (ruteada por `AutonomiaRepository`) construya exactamente la misma config sin duplicar la regla.
 *
 * Dominio puro: sin Room ni Compose ([UserActivityConfigEntity] es un POJO `data class`).
 */
object SupportConfigFactory {

    fun buildSupportConfig(activityId: String, now: Long): UserActivityConfigEntity =
        UserActivityConfigEntity(
            activityId = activityId,
            activityType = ActivitySurface.Support.name,
            active = true,
            archived = false,
            // Support has no targets by domain design
            cadence = null,
            targetValue = null,
            minimumValue = null,
            targetCount = null,
            targetPeriod = null,
            weeklyFrequencyTarget = null,
            sessionTargetMinutes = null,
            commitmentDurationMonths = null,
            sortOrder = now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            createdAt = now,
            updatedAt = now,
        )
}

package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.ActivityCadence
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.data.UserActivityConfigEntity

/**
 * R6 — Regla pura de **crear vs. editar** una config de actividad.
 *
 * `AutonomiaRepository.configureActivity` debe distinguir CREAR de EDITAR. Al EDITAR una config ya
 * existente, los campos que describen el **ciclo de vida** de la actividad se PRESERVAN de la previa
 * (`createdAt`, `active`, `archived`, `sortOrder`); solo `updatedAt` y los campos de **configuración**
 * (tipo de superficie, cadence, targets, nombre/descripción) toman los valores nuevos. Al CREAR
 * (previa == null) se conserva el comportamiento actual: `createdAt`/`updatedAt`/`sortOrder = now`,
 * `active = true`, `archived = false`.
 *
 * Pisar `createdAt = now` al editar haría que el cierre diario (que filtra configs por
 * `date >= config.createdLocalDate(zoneId)`) deje de materializar hechos para los días previos a la
 * edición; pisar `sortOrder = now` reordenaría la actividad al final de la lista. Esta regla evita
 * ambos efectos colaterales.
 *
 * Dominio puro: sin Room ni Compose ([UserActivityConfigEntity] es un POJO `data class` sin deps
 * Android, construible/aserible en JVM puro).
 */
object ConfigEditRule {

    /**
     * Devuelve la [UserActivityConfigEntity] a persistir.
     *
     * @param previous config existente para [activityId] leída del DAO, o `null` si no existe (CREAR).
     */
    fun resolve(
        previous: UserActivityConfigEntity?,
        activityId: String,
        activityType: ActivitySurface,
        cadence: ActivityCadence?,
        targetValue: Int?,
        minimumValue: Int?,
        targetCount: Int?,
        targetPeriod: TargetPeriod?,
        weeklyFrequencyTarget: Int?,
        sessionTargetMinutes: Int?,
        commitmentDurationMonths: Int?,
        customName: String?,
        customDescription: String?,
        now: Long,
    ): UserActivityConfigEntity {
        val normalizedName = customName?.trim()?.takeIf { it.isNotBlank() }
        val normalizedDescription = customDescription?.trim()?.takeIf { it.isNotBlank() }
        return UserActivityConfigEntity(
            activityId = activityId,
            activityType = activityType.name,
            // Ciclo de vida: PRESERVAR de la previa al editar; defaults de creación si no existe.
            active = previous?.active ?: true,
            archived = previous?.archived ?: false,
            sortOrder = previous?.sortOrder ?: now.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            createdAt = previous?.createdAt ?: now,
            // Configuración: SIEMPRE los valores nuevos.
            cadence = cadence?.name,
            targetValue = targetValue,
            minimumValue = minimumValue,
            targetCount = targetCount,
            targetPeriod = targetPeriod?.name,
            weeklyFrequencyTarget = weeklyFrequencyTarget,
            sessionTargetMinutes = sessionTargetMinutes,
            commitmentDurationMonths = commitmentDurationMonths,
            customName = normalizedName,
            customDescription = normalizedDescription,
            updatedAt = now,
        )
    }
}

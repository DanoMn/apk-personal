package dev.panopt.autonomia.data.local.mapper

import dev.panopt.autonomia.data.ActivityTargetVersionEntity
import dev.panopt.autonomia.domain.activity.ActivityTargetVersion
import java.time.LocalDate

/** Mapea la entidad Room de versión de vara al modelo de dominio puro. */
fun ActivityTargetVersionEntity.toDomain(): ActivityTargetVersion =
    ActivityTargetVersion(
        activityId = activityId,
        validFrom = LocalDate.parse(validFrom),
        targetMinutes = targetMinutes,
        targetDays = targetDays,
        createdAt = createdAt,
    )

/** Agrupa una lista de versiones por `activityId`, en la forma que consume `ScoreInput.targetVersions`. */
fun List<ActivityTargetVersionEntity>.toVersionsByActivity(): Map<String, List<ActivityTargetVersion>> =
    map { it.toDomain() }.groupBy { it.activityId }

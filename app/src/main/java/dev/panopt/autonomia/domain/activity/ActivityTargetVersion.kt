package dev.panopt.autonomia.domain.activity

import java.time.LocalDate

/**
 * Una versión de la "vara" (metas) de un ancla, vigente desde [validFrom]. El motor lee, por cada
 * fecha de la ventana, la versión que regía ese día para NO reescribir el pasado cuando el usuario
 * edita una meta (subir/bajar minutos o frecuencia). Ver
 * `docs/scoring/cambios-config-en-el-tiempo-v1.md`.
 *
 * Modelo de dominio puro JVM (sin Room): el mapeo entidad↔dominio vive en `data/`.
 *
 * @param validFrom fecha desde la que rige esta vara (inclusive).
 * @param targetMinutes meta de minutos por sesión vigente desde [validFrom].
 * @param targetDays meta de frecuencia (días/semana) vigente desde [validFrom].
 * @param createdAt timestamp de inserción; desempata dos versiones del mismo [validFrom]
 *   (editar dos veces el mismo día → gana la última).
 */
data class ActivityTargetVersion(
    val activityId: String,
    val validFrom: LocalDate,
    val targetMinutes: Int,
    val targetDays: Int,
    val createdAt: Long,
)

package dev.panopt.autonomia.domain.activity

import java.time.LocalDate

/**
 * Resuelve QUÉ versión de la vara ([ActivityTargetVersion]) regía en una fecha dada: la de mayor
 * [ActivityTargetVersion.validFrom] que no sea posterior a la fecha. Si dos comparten `validFrom`
 * (editadas el mismo día), gana la última insertada (mayor `createdAt`).
 *
 * Esta es la pieza que permite "cada día con su meta": el adapter de hechos la consulta por cada
 * día de la ventana. Dominio puro JVM, testeable sin Room.
 */
object ActiveTargetVersionRule {

    /** La versión vigente en [date], o `null` si no hay ninguna versión con `validFrom <= date`. */
    fun resolve(versions: List<ActivityTargetVersion>, date: LocalDate): ActivityTargetVersion? =
        versions
            .filter { !it.validFrom.isAfter(date) }
            .maxWithOrNull(compareBy({ it.validFrom }, { it.createdAt }))
}

package dev.panopt.autonomia.domain.activity

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Período de GRACIA de un ancla nueva (FASE 2, §3.3 de `cambios-config-en-el-tiempo-v1.md`):
 * durante sus primeros [GRACE_DAYS] días desde la creación, el ancla NO entra al puntaje (ni suma
 * ni resta) — todavía no tiene una ventana de historial suficiente para juzgarla con justicia. Al
 * cumplirse la gracia (día 8) entra con su ventana real. Dominio puro JVM.
 */
object AnchorGraceRule {
    const val GRACE_DAYS = 7L

    /**
     * `true` si el ancla está dentro de su gracia (creada hace `< GRACE_DAYS` días respecto de
     * [today]) → debe excluirse del puntaje.
     */
    fun isWithinGrace(
        createdAtMillis: Long,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean {
        val createdDate = Instant.ofEpochMilli(createdAtMillis).atZone(zoneId).toLocalDate()
        return ChronoUnit.DAYS.between(createdDate, today) < GRACE_DAYS
    }
}

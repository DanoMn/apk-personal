package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.domain.activity.AnchorGraceRule

/**
 * Política de CONTADOR de arranque (`scoring-arranque-cuenta`, Lote 2). Dominio puro JVM.
 *
 * La BARRA DE CARGA: atenúa el ESTADO proyectado por la fracción de ventana vivida (`× d/7`) y lo
 * mapea a puntos visibles vía [PointsMappingPolicy]. Día 1 → `estado × 1/7`; día 7 →
 * `estado × 7/7 = estado` (sin atenuar → converge con el score maduro del día 8, sin salto).
 *
 * Pieza SEPARADA de la justicia de ventana (`windowDays` en [AnchorScoringPolicy]): la proyección
 * produce el score JUSTO de la ventana parcial; este policy lo atenúa para que el número SUBA.
 */
object StartupCounterPolicy {

    /**
     * @param projectedEstado ESTADO `∈ [0, 1.5]` proyectado por [StartupProjectionUseCase].
     * @param daysLived `d` días vividos; se clampa a `[1, 7]`.
     * @return contador (puntos visibles atenuados), progreso `d/7` y días restantes hasta el real.
     */
    fun counter(projectedEstado: Double, daysLived: Int): StartupCounter {
        val grace = AnchorGraceRule.GRACE_DAYS.toInt()
        val d = daysLived.coerceIn(1, grace)
        val attenuated = projectedEstado * (d.toDouble() / grace.toDouble())
        return StartupCounter(
            counterPoints = PointsMappingPolicy.points(attenuated),
            daysLived = d,
            daysRemaining = (grace - d).coerceAtLeast(0),
            windowProgress = (d.toFloat() / grace.toFloat()).coerceIn(0f, 1f),
        )
    }
}

/**
 * Resultado del contador de arranque. `counterPoints` es el número central de la barra de carga
 * (puntos visibles atenuados); `windowProgress` (`d/7`) alimenta el arco; `daysRemaining` el copy.
 */
data class StartupCounter(
    val counterPoints: Int,
    val daysLived: Int,
    val daysRemaining: Int,
    val windowProgress: Float,
)

package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.domain.activity.AnchorGraceRule
import kotlin.math.roundToInt

/**
 * Política de CONTADOR de arranque (`scoring-arranque-cuenta`). Dominio puro JVM.
 *
 * La BARRA DE CARGA: mapea el ESTADO proyectado a PUNTOS VISIBLES vía [PointsMappingPolicy] y luego
 * atenúa ESOS PUNTOS por la fracción de ventana vivida (`× d/7`). Día 1 → `points(estado) × 1/7`;
 * día 7 → `points(estado) × 7/7 = points(estado)` (sin atenuar → converge con el score maduro del
 * día 8, sin salto).
 *
 * **Por qué atenuar los PUNTOS y no el ESTADO** (FIX A): `PointsMappingPolicy.points` tiene piso 650
 * (`ESTADO 0 → 650`). Atenuar el estado ANTES de mapear (`points(estado × d/7)`) dejaba el contador
 * SIEMPRE `≥ 650`, nunca recorría la zona muerta `0–650`. La intención del modelo es que el contador
 * arranque cerca de 0 y suba hacia el score real: por eso se atenúan los puntos ya mapeados
 * (`round(points(estado) × d/7)`). La convergencia día 7→8 se mantiene exacta: en `d=7`, `×7/7=1` →
 * `points(estado)` == los puntos maduros del día 8 con los mismos 7 días de hechos.
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
        val visiblePoints = PointsMappingPolicy.points(projectedEstado)
        val attenuatedPoints = (visiblePoints.toDouble() * d.toDouble() / grace.toDouble()).roundToInt()
        return StartupCounter(
            counterPoints = attenuatedPoints,
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

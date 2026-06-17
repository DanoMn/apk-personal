package dev.panopt.autonomia.domain.scoring

import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * NIVEL 7 — mapeo E `ESTADO ∈ [0, 1.5] → PUNTOS ∈ [650, 1100]`.
 *
 * Enfoque E (suma de rampas logísticas): el número visible del dashboard crece de forma continua,
 * monótona y "de a 1 punto" sobre el `ESTADO` crudo que emite el motor. El "1000" se gana al ENTRAR
 * a Inquebrantable (`ESTADO ≈ 1.10 → ≈ 1011`), no cumpliendo justo (`ESTADO = 1.0 → 941`).
 *
 * Dominio puro JVM (sin Compose): el cálculo del número visible es presentación de dominio, no
 * lógica de Composable ni de ViewModel. Lo consumen DOS callers:
 *  - la PROYECCIÓN (`DashboardProjection`), para el dashboard, y
 *  - el motor (`ScoreEngine`), al poblar `ScoreReport.visibleScore` para el seam de persistencia
 *    semanal (`WeeklyScoreSnapshotDraft`).
 *
 * Fórmula (de [ScoringConstantsV2], nunca hardcodeada):
 * ```
 *   σ(x)      = 1 / (1 + e^−x)
 *   raw(e)    = POINTS_FLOOR + Σ Aᵢ · σ((e − cᵢ)/wᵢ)        sobre POINTS_MILESTONES (cᵢ, wᵢ, Aᵢ)
 *   PUNTOS(e) = POINTS_FLOOR + (raw(e) − raw(0)) · (POINTS_CEILING − POINTS_FLOOR)
 *                                                 / (raw(1.5) − raw(0))
 * ```
 * con `e` clampeado a `[0, 1.5]`. La normalización fija `PUNTOS(0) = 650` y `PUNTOS(1.5) = 1100`.
 *
 * Fuente canónica: `docs/scoring/modelo-matematico-nucleo-v1.md` § NIVEL 7 y
 * `docs/scoring/verificacion_modelo_oficial.py` (`sig`, `HITOS`, `rawpt`, `P`).
 */
object PointsMappingPolicy {

    private const val ESTADO_FLOOR = 0.0
    private const val ESTADO_CEILING = 1.5

    /** σ(x) = 1 / (1 + e^−x). */
    private fun sigmoid(x: Double): Double = 1.0 / (1.0 + exp(-x))

    /** raw(e) = piso + Σ Aᵢ·σ((e − cᵢ)/wᵢ) sobre los hitos del NIVEL 7. */
    private fun raw(e: Double): Double =
        ScoringConstantsV2.POINTS_FLOOR +
            ScoringConstantsV2.POINTS_MILESTONES.sumOf { (c, w, a) -> a * sigmoid((e - c) / w) }

    // Normalización: anclas raw(0) → piso y raw(1.5) → tope. Constantes (no dependen de `e`).
    private val rawAtFloor: Double = raw(ESTADO_FLOOR)
    private val rawSpan: Double = raw(ESTADO_CEILING) - rawAtFloor

    /**
     * Mapea `estado` (crudo, `∈ [0, 1.5]`) al número visible del dashboard `∈ [650, 1100]`.
     * Continuo, monótono no decreciente y redondeado al punto entero más cercano. Clampea
     * defensivamente `estado` fuera de `[0, 1.5]` al piso/tope.
     */
    fun points(estado: Double): Int {
        val clamped = estado.coerceIn(ESTADO_FLOOR, ESTADO_CEILING)
        val span = if (rawSpan == 0.0) 0.0 else (raw(clamped) - rawAtFloor) / rawSpan
        val points = ScoringConstantsV2.POINTS_FLOOR +
            span * (ScoringConstantsV2.POINTS_CEILING - ScoringConstantsV2.POINTS_FLOOR)
        return points.roundToInt()
    }
}

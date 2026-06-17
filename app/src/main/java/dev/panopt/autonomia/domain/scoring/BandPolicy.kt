package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState

/**
 * NIVEL 6 del modelo de núcleo v1 — bandas `banda(ESTADO)`.
 *
 * Traducción verbatim de `banda(e)` de `docs/scoring/verificacion_modelo_oficial.py`
 * (ver §NIVEL 6 de `docs/scoring/modelo-matematico-nucleo-v1.md`): función PURA sobre los cortes
 * de [ScoringConstantsV2]. Cada corte es límite inferior INCLUSIVO de la banda superior.
 * ```
 * ESTADO < 0.40  → Restauración
 * ESTADO < 0.62  → Atención
 * ESTADO < 0.85  → En marcha (Motion)
 * ESTADO < 1.10  → Plenitud
 * ESTADO ≥ 1.10  → Inquebrantable
 * ```
 *
 * Sin gates, sin worst-layer, sin histéresis, sin memoria: el ESTADO ya trae toda la historia.
 *
 * NIVEL 6 del motor núcleo v1 — única resolución de banda tras PR-F (la `BaseStatePolicy` vieja,
 * con gates/worst-layer/histéresis, se eliminó). El gate `NoData` lo decide el orquestador
 * [ScoreEngine], no esta función pura.
 */
internal object BandPolicy {

    /** Mapea un ESTADO crudo `∈ [0, 1.5]` a su [ScoreState]. Función pura. */
    fun band(estado: Double): ScoreState = when {
        estado < ScoringConstantsV2.BAND_ATTENTION -> ScoreState.Restoration
        estado < ScoringConstantsV2.BAND_MOTION -> ScoreState.Attention
        estado < ScoringConstantsV2.BAND_PLENITUDE -> ScoreState.Motion
        estado < ScoringConstantsV2.BAND_UNBREAKABLE -> ScoreState.Plenitude
        else -> ScoreState.Unbreakable
    }
}

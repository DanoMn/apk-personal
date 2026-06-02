package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState

internal object BaseStatePolicy {

    fun stateFor(
        weeklyBaseScore: Float,
        worstLayerScore: Float,
        stability: StabilityEvaluation,
        previousState: ScoreState?,
        hasSleepData: Boolean,
    ): ScoreState {
        val base = weeklyBaseScore.coerceIn(0f, 1f)

        // 1. Worst-layer collapse override (hard, ignores hysteresis)
        if (worstLayerScore < ScoringConstants.WORST_LAYER_COLLAPSE) {
            return ScoreState.Restoration
        }

        // 2. Raw band from weeklyBaseScore with hysteresis
        val rawBand = bandFor(base)
        val dampedBand = applyHysteresis(rawBand, previousState, base)

        // 3. Worst-layer ladder caps
        val cappedByWorst = applyWorstLayerCaps(dampedBand, worstLayerScore)

        // 3b. Sleep registration cap (§16.7): sin registro de sueño el estado se
        // topea en Motion — sueño es CORE, no opt-in; sin él la base no está completa.
        // No toca weeklyBaseScore/visibleScore (crudos); ADR-3 intacto.
        val capped = if (!hasSleepData) minOf(cappedByWorst, ScoreState.Motion) else cappedByWorst

        // 4. Inquebrantable gate (only if we reached Plenitude after caps)
        if (capped == ScoreState.Plenitude &&
            stability.hasTemporalMemory &&
            base >= ScoringConstants.UNBREAKABLE_BASE_MIN &&
            worstLayerScore >= ScoringConstants.WORST_LAYER_MIN_FOR_UNBREAKABLE &&
            (stability.stabilityScore ?: 0f) >= ScoringConstants.UNBREAKABLE_STABILITY_MIN
        ) {
            return ScoreState.Unbreakable
        }

        return capped
    }

    /** Lower-inclusive / upper-exclusive band mapping over weeklyBaseScore. */
    private fun bandFor(base: Float): ScoreState = when {
        base < ScoringConstants.STATE_RESTORATION_THRESHOLD -> ScoreState.Restoration
        base < ScoringConstants.STATE_ATTENTION_THRESHOLD   -> ScoreState.Attention
        base < ScoringConstants.STATE_PLENITUDE_THRESHOLD   -> ScoreState.Motion
        else                                                 -> ScoreState.Plenitude
    }

    /**
     * Applies one-step downward hysteresis.
     * Only damps when rawBand is exactly ONE step below previousState and the score
     * is within [stateHysteresisMargin] of the lower boundary.
     * Never blocks upward transitions. Never suppresses two bands.
     */
    private fun applyHysteresis(
        rawBand: ScoreState,
        previousState: ScoreState?,
        base: Float,
    ): ScoreState {
        if (previousState == null) return rawBand

        // Only apply when rawBand is exactly one step below previousState
        if (!isOneStepBelow(rawBand, previousState)) return rawBand

        // The upper boundary of rawBand is the lower boundary of previousState
        val boundary = lowerBoundaryOf(previousState)
        return if ((boundary - base) <= ScoringConstants.STATE_HYSTERESIS_MARGIN) {
            previousState // dampen — stay in previous state
        } else {
            rawBand
        }
    }

    /** Returns true only if `candidate` is exactly one ordinal step below `reference`. */
    private fun isOneStepBelow(candidate: ScoreState, reference: ScoreState): Boolean {
        val order = listOf(
            ScoreState.Restoration,
            ScoreState.Attention,
            ScoreState.Motion,
            ScoreState.Plenitude,
        )
        val candidateIdx = order.indexOf(candidate)
        val referenceIdx = order.indexOf(reference)
        return candidateIdx >= 0 && referenceIdx >= 0 && referenceIdx - candidateIdx == 1
    }

    /** Lower boundary of the given band (used for hysteresis margin calculation). */
    private fun lowerBoundaryOf(state: ScoreState): Float = when (state) {
        ScoreState.Attention -> ScoringConstants.STATE_RESTORATION_THRESHOLD
        ScoreState.Motion    -> ScoringConstants.STATE_ATTENTION_THRESHOLD
        ScoreState.Plenitude -> ScoringConstants.STATE_PLENITUDE_THRESHOLD
        else                 -> 0f
    }

    /** Applies worst-layer caps: prevents reaching higher bands when worst is too low. */
    private fun applyWorstLayerCaps(band: ScoreState, worstLayerScore: Float): ScoreState = when {
        worstLayerScore < ScoringConstants.WORST_LAYER_MIN_FOR_MOTION    -> minOf(band, ScoreState.Attention)
        worstLayerScore < ScoringConstants.WORST_LAYER_MIN_FOR_PLENITUDE -> minOf(band, ScoreState.Motion)
        else -> band
    }

    /** Caps state at the minimum of two states based on ordinal ordering. */
    private fun minOf(a: ScoreState, b: ScoreState): ScoreState {
        val order = listOf(
            ScoreState.Restoration,
            ScoreState.Attention,
            ScoreState.Motion,
            ScoreState.Plenitude,
            ScoreState.Unbreakable,
        )
        val aIdx = order.indexOf(a)
        val bIdx = order.indexOf(b)
        return if (aIdx <= bIdx) a else b
    }
}

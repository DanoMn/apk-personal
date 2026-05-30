package dev.panopt.autonomia.domain.sleep

/**
 * Calibration constants for the [SleepScoring] 4-component formula.
 *
 * All values are calibration proposals (D1/future); centralizing them here
 * allows recalibrating without touching formula logic.
 *
 * See design §2.2, §2.4, §2.5 for rationale.
 *
 * @property continuityDecayFactor   k ≈ 2 in exp(-awakeCount/k) for ContinuityScore.
 *   0 despertares → penalty = 1.0; 2 despertares → ≈ 0.37; same pattern as TaskMomentumRaw.
 * @property digitalInterruptionDecayFactor  m ≈ 30 (minutes) in exp(-awakeUseMinutes/m).
 *   0 min → 1.0; 30 min → ≈ 0.37; 60 min → ≈ 0.14.
 * @property ambiguousConfidenceFactor  Factor applied to sleepScore when confidence=Ambiguous.
 *   ≈ 0.85 (calibrable). Materializes "genuinely ambiguous signal → score attenuated".
 *   High confidence is never attenuated. NoData returns null (no factor applies).
 */
object SleepScoringParams {
    /** Decay factor for ContinuityScore fragmentation penalty. Default k ≈ 2. */
    const val continuityDecayFactor: Float = 2.0f

    /**
     * Decay factor (minutes) for DigitalInterruptionScore.
     * D3: digitalWindDownMinutes is INERT — not used in this formula.
     * Default m ≈ 30 minutes.
     */
    const val digitalInterruptionDecayFactor: Float = 30.0f

    /**
     * Attenuation factor applied to sleepScore when confidence = Ambiguous.
     * Default ≈ 0.85. Signal genuinely ambiguous → score slightly lowered.
     */
    const val ambiguousConfidenceFactor: Float = 0.85f
}

package dev.panopt.autonomia.domain.scoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [SpecialLayerScoringPolicy] — focused on the NoData fix (ADR-3).
 *
 * Bug §10: `sleepScore ?: 0f` → hunde Cuerpo al 30% cuando no hay dato de sueño.
 * Fix: cuando sleepScore == null, Cuerpo = baseWithoutSpecial (sin término de sueño,
 * sin fabricar un 0). Ausencia ≠ sueño malo.
 *
 * Spec: openspec/changes/sleep-consumer/specs/base-state-policy/spec.md
 * Design: design.md §4
 */
class SpecialLayerScoringPolicyTest {

    private val bodyLayerId = ScoringConstants.BODY_LAYER_ID

    // ─── NoData → no hunde Cuerpo (bug fix §10) ───────────────────────────────

    @Test
    fun `baseScore NoData sleep — Cuerpo equals baseWithoutSpecial (not penalized)`() {
        // sleepScore == null → should use only baseWithoutSpecial (re-normalized)
        val base = 0.80f
        val result = SpecialLayerScoringPolicy.baseScore(
            layerId = bodyLayerId,
            baseWithoutSpecial = base,
            sleepScore = null,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        // Ausencia de dato → Cuerpo = base solo, sin mezclar con 0
        assertEquals(base, result, 0.001f)
    }

    @Test
    fun `rawScore NoData sleep — Cuerpo equals baseWithPositiveMargin (not penalized)`() {
        val base = 0.85f
        val result = SpecialLayerScoringPolicy.rawScore(
            layerId = bodyLayerId,
            baseWithPositiveMargin = base,
            sleepScore = null,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        assertEquals(base, result, 0.001f)
    }

    @Test
    fun `baseScore semana A sin dato vs semana B con sueño malo — A debe ser mayor o igual a B`() {
        // NoData no debe hundir mas que un sueño malo
        // Semana A: sin dato → Cuerpo = base
        val bodyA = SpecialLayerScoringPolicy.baseScore(
            layerId = bodyLayerId,
            baseWithoutSpecial = 0.75f,
            sleepScore = null,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        // Semana B: sueño muy malo 0.2 → Cuerpo mezclado
        val bodyB = SpecialLayerScoringPolicy.baseScore(
            layerId = bodyLayerId,
            baseWithoutSpecial = 0.75f,
            sleepScore = 0.2f,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        // Ausencia de dato (null) no debe hundir más que dato explícito de mala calidad
        assertTrue(
            "NoData body ($bodyA) should be >= poor-sleep body ($bodyB)",
            bodyA >= bodyB,
        )
    }

    @Test
    fun `baseScore NoData sleep does not inflate beyond base — not higher than perfect sleep`() {
        val base = 0.70f
        val bodyNoData = SpecialLayerScoringPolicy.baseScore(
            layerId = bodyLayerId,
            baseWithoutSpecial = base,
            sleepScore = null,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        val bodyPerfect = SpecialLayerScoringPolicy.baseScore(
            layerId = bodyLayerId,
            baseWithoutSpecial = base,
            sleepScore = 1.0f,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        // No data should not be better than perfect sleep (removes term, doesn't boost)
        assertTrue(
            "NoData body ($bodyNoData) should not exceed perfect-sleep body ($bodyPerfect)",
            bodyNoData <= bodyPerfect,
        )
    }

    // ─── With sleep data — formula unchanged ──────────────────────────────────

    @Test
    fun `baseScore with sleepScore applies 70-30 formula`() {
        // Body = (1-0.30)*base + 0.30*sleep = 0.70*0.80 + 0.30*0.60 = 0.56 + 0.18 = 0.74
        val result = SpecialLayerScoringPolicy.baseScore(
            layerId = bodyLayerId,
            baseWithoutSpecial = 0.80f,
            sleepScore = 0.60f,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        assertEquals(0.74f, result, 0.001f)
    }

    @Test
    fun `rawScore with sleepScore applies formula`() {
        // rawBody = (1-0.30)*0.85 + 0.30*0.70 = 0.595 + 0.210 = 0.805
        val result = SpecialLayerScoringPolicy.rawScore(
            layerId = bodyLayerId,
            baseWithPositiveMargin = 0.85f,
            sleepScore = 0.70f,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        assertEquals(0.805f, result, 0.001f)
    }

    // ─── Non-body layers pass through unchanged ────────────────────────────────

    @Test
    fun `baseScore non-body layer returns baseWithoutSpecial unchanged`() {
        val base = 0.65f
        val result = SpecialLayerScoringPolicy.baseScore(
            layerId = "layer_interior",
            baseWithoutSpecial = base,
            sleepScore = null,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        assertEquals(base, result, 0.001f)
    }

    @Test
    fun `baseScore non-body layer ignores sleepScore`() {
        val base = 0.65f
        val resultNoSleep = SpecialLayerScoringPolicy.baseScore(
            layerId = "layer_interior",
            baseWithoutSpecial = base,
            sleepScore = null,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        val resultWithSleep = SpecialLayerScoringPolicy.baseScore(
            layerId = "layer_interior",
            baseWithoutSpecial = base,
            sleepScore = 1.0f,
            sobrietyScore = null,
            hasActiveSobriety = false,
        )
        assertEquals(resultNoSleep, resultWithSleep, 0.001f)
    }
}

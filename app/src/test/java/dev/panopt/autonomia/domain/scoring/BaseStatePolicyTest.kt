package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.ScoreState
import org.junit.Assert.assertEquals
import org.junit.Test

class BaseStatePolicyTest {

    // region Band Mapping (task 2.1)

    @Test
    fun `base 0,399 maps to Restoration`() {
        val state = stateFor(weeklyBaseScore = 0.399f)
        assertEquals(ScoreState.Restoration, state)
    }

    @Test
    fun `base 0,40 maps to Attention`() {
        val state = stateFor(weeklyBaseScore = 0.40f)
        assertEquals(ScoreState.Attention, state)
    }

    @Test
    fun `base 0,699 maps to Attention`() {
        val state = stateFor(weeklyBaseScore = 0.699f)
        assertEquals(ScoreState.Attention, state)
    }

    @Test
    fun `base 0,70 maps to Motion`() {
        val state = stateFor(weeklyBaseScore = 0.70f)
        assertEquals(ScoreState.Motion, state)
    }

    @Test
    fun `base 0,849 maps to Motion`() {
        val state = stateFor(weeklyBaseScore = 0.849f)
        assertEquals(ScoreState.Motion, state)
    }

    @Test
    fun `base 0,85 maps to Plenitude`() {
        val state = stateFor(weeklyBaseScore = 0.85f)
        assertEquals(ScoreState.Plenitude, state)
    }

    // endregion

    // region Worst-Layer Collapse (task 2.2)

    @Test
    fun `worstLayer 0,299 with high base forces Restoration`() {
        val state = stateFor(weeklyBaseScore = 0.95f, worstLayerScore = 0.299f)
        assertEquals(ScoreState.Restoration, state)
    }

    @Test
    fun `worstLayer 0,30 does not collapse — ladder cap applies instead`() {
        // worst=0.30 is NOT < 0.30, so no collapse override (not Restoration).
        // But worst=0.30 < WORST_LAYER_MIN_FOR_MOTION=0.55, so ladder caps at Attention.
        val state = stateFor(weeklyBaseScore = 0.80f, worstLayerScore = 0.30f)
        assertEquals(ScoreState.Attention, state)
    }

    @Test
    fun `collapse ignores hysteresis — previousState Plenitude with worst 0,25 gives Restoration`() {
        val state = stateFor(
            weeklyBaseScore = 0.95f,
            worstLayerScore = 0.25f,
            previousState = ScoreState.Plenitude,
        )
        assertEquals(ScoreState.Restoration, state)
    }

    // endregion

    // region State Hysteresis (task 2.3)

    @Test
    fun `hysteresis maintains Motion when base 0,69 and previousState Motion`() {
        // 0.70 - 0.69 = 0.01 <= 0.03 margin → stays in Motion
        val state = stateFor(weeklyBaseScore = 0.69f, previousState = ScoreState.Motion)
        assertEquals(ScoreState.Motion, state)
    }

    @Test
    fun `hysteresis drops to Attention when base 0,66 and previousState Motion`() {
        // 0.70 - 0.66 = 0.04 > 0.03 margin → falls to Attention
        val state = stateFor(weeklyBaseScore = 0.66f, previousState = ScoreState.Motion)
        assertEquals(ScoreState.Attention, state)
    }

    @Test
    fun `hysteresis does not block upward transition from Attention to Motion`() {
        val state = stateFor(weeklyBaseScore = 0.72f, previousState = ScoreState.Attention)
        assertEquals(ScoreState.Motion, state)
    }

    @Test
    fun `hysteresis does not suppress two bands — Plenitude to Attention falls`() {
        // base 0.66 is two steps below Plenitude; damping only applies one step
        val state = stateFor(weeklyBaseScore = 0.66f, previousState = ScoreState.Plenitude)
        assertEquals(ScoreState.Attention, state)
    }

    @Test
    fun `null previousState means no damping — base 0,69 gives Attention`() {
        val state = stateFor(weeklyBaseScore = 0.69f, previousState = null)
        assertEquals(ScoreState.Attention, state)
    }

    // endregion

    // region Worst-Layer Ladder Caps (task 2.4)

    @Test
    fun `base 0,90 with worst 0,50 caps at Attention — below WORST_LAYER_MIN_FOR_MOTION`() {
        // worst 0.50 < 0.55 → cap at Attention (not even Motion)
        val state = stateFor(weeklyBaseScore = 0.90f, worstLayerScore = 0.50f)
        assertEquals(ScoreState.Attention, state)
    }

    @Test
    fun `base 0,90 with worst 0,74 caps at Motion — below WORST_LAYER_MIN_FOR_PLENITUDE`() {
        // worst 0.74 < 0.75 → cap at Motion (not Plenitude)
        val state = stateFor(weeklyBaseScore = 0.90f, worstLayerScore = 0.74f)
        assertEquals(ScoreState.Motion, state)
    }

    @Test
    fun `base 0,92 with worst 0,79 caps at Plenitude — below WORST_LAYER_MIN_FOR_UNBREAKABLE`() {
        // worst 0.79 < 0.80 → Plenitude, not Unbreakable (even with temporal memory and good stability)
        val state = stateFor(
            weeklyBaseScore = 0.92f,
            worstLayerScore = 0.79f,
            stability = stabilityWith(hasMemory = true, score = 0.91f),
        )
        assertEquals(ScoreState.Plenitude, state)
    }

    // endregion

    // region Inquebrantable Gate (task 2.5)

    @Test
    fun `all gates met with temporal memory gives Unbreakable`() {
        val state = stateFor(
            weeklyBaseScore = 0.92f,
            worstLayerScore = 0.81f,
            stability = stabilityWith(hasMemory = true, score = 0.91f),
        )
        assertEquals(ScoreState.Unbreakable, state)
    }

    @Test
    fun `no temporal memory prevents Unbreakable — stays at Plenitude`() {
        val state = stateFor(
            weeklyBaseScore = 0.95f,
            worstLayerScore = 0.85f,
            stability = stabilityWith(hasMemory = false, score = 0.92f),
        )
        assertEquals(ScoreState.Plenitude, state)
    }

    @Test
    fun `low worst layer prevents Unbreakable — stays at Plenitude`() {
        val state = stateFor(
            weeklyBaseScore = 0.92f,
            worstLayerScore = 0.79f,
            stability = stabilityWith(hasMemory = true, score = 0.91f),
        )
        assertEquals(ScoreState.Plenitude, state)
    }

    @Test
    fun `missing sleep data caps Plenitude at Motion (gate sleep)`() {
        // Base 0.90 (banda Plenitud) + peor capa 0.85 (sin cap por peor capa).
        // Sin registro de sueño → el estado se topea en Motion. No toca weeklyBaseScore.
        val state = stateFor(
            weeklyBaseScore = 0.90f,
            worstLayerScore = 0.85f,
            hasSleepData = false,
        )
        assertEquals(ScoreState.Motion, state)
    }

    @Test
    fun `missing sleep data blocks Unbreakable even with full stability (gate sleep)`() {
        // Todo lo demás habilitaría Inquebrantable (base 0.92, peor 0.85, estabilidad 0.95
        // con memoria). Sin sueño, el cap a Motion gana ANTES de la puerta Inquebrantable.
        val state = stateFor(
            weeklyBaseScore = 0.92f,
            worstLayerScore = 0.85f,
            stability = stabilityWith(hasMemory = true, score = 0.95f),
            hasSleepData = false,
        )
        assertEquals(ScoreState.Motion, state)
    }

    @Test
    fun `with sleep data the same inputs reach Plenitude (control)`() {
        // Mismo caso que el cap, pero CON registro de sueño → llega a Plenitud.
        // Demuestra que la diferencia la hace exclusivamente el sueño.
        val state = stateFor(
            weeklyBaseScore = 0.90f,
            worstLayerScore = 0.85f,
            hasSleepData = true,
        )
        assertEquals(ScoreState.Plenitude, state)
    }

    // endregion

    // region helpers

    private fun stateFor(
        weeklyBaseScore: Float,
        worstLayerScore: Float = 0.80f,
        stability: StabilityEvaluation = stabilityWith(hasMemory = false, score = null),
        previousState: ScoreState? = null,
        hasSleepData: Boolean = true,
    ): ScoreState = BaseStatePolicy.stateFor(
        weeklyBaseScore = weeklyBaseScore,
        worstLayerScore = worstLayerScore,
        stability = stability,
        previousState = previousState,
        hasSleepData = hasSleepData,
    )

    private fun stabilityWith(hasMemory: Boolean, score: Float?): StabilityEvaluation =
        StabilityEvaluation(
            stabilityScore = score,
            evaluatedWeeks = if (hasMemory) 6 else 1,
            hasTemporalMemory = hasMemory,
        )

    // endregion
}

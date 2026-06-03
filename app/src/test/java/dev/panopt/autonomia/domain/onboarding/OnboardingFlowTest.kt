package dev.panopt.autonomia.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {

    // region resolve — spec §Persisted State, §Invalid Persisted Step

    @Test
    fun `resolve sin paso persistido empieza en Welcome no completado`() {
        val state = OnboardingFlow.resolve(
            completed = false,
            persistedStepName = null,
            persistedIntention = null,
        )
        assertEquals(OnboardingStep.Welcome, state.currentStep)
        assertFalse(state.completed)
        assertEquals(OnboardingIntention.STANDARD, state.intention)
    }

    @Test
    fun `resolve con paso valido reanuda en ese paso`() {
        val state = OnboardingFlow.resolve(
            completed = false,
            persistedStepName = "Sleep",
            persistedIntention = null,
        )
        assertEquals(OnboardingStep.Sleep, state.currentStep)
        assertFalse(state.completed)
    }

    @Test
    fun `resolve con paso invalido cae de forma segura a Welcome`() {
        val state = OnboardingFlow.resolve(
            completed = false,
            persistedStepName = "Inexistente",
            persistedIntention = null,
        )
        assertEquals(OnboardingStep.Welcome, state.currentStep)
    }

    @Test
    fun `resolve preserva completed true aunque haya paso persistido`() {
        val state = OnboardingFlow.resolve(
            completed = true,
            persistedStepName = "Sleep",
            persistedIntention = null,
        )
        assertTrue(state.completed)
        assertEquals(OnboardingStep.Sleep, state.currentStep)
    }

    // endregion

    // region next / previous — secuencia STANDARD (5 pasos, sin Sobriety)

    @Test
    fun `next avanza al bloque siguiente`() {
        assertEquals(OnboardingStep.Intention, OnboardingFlow.next(OnboardingStep.Welcome, OnboardingIntention.STANDARD))
    }

    @Test
    fun `next en el ultimo bloque se mantiene`() {
        assertEquals(OnboardingStep.Closing, OnboardingFlow.next(OnboardingStep.Closing, OnboardingIntention.STANDARD))
    }

    @Test
    fun `next recorre la secuencia completa STANDARD hasta Closing sin Sobriety`() {
        var step = OnboardingStep.Welcome
        val visited = mutableListOf(step)
        var iterations = 0
        while (iterations < 10) {
            val nextStep = OnboardingFlow.next(step, OnboardingIntention.STANDARD)
            if (nextStep == step) break
            visited.add(nextStep)
            step = nextStep
            iterations++
        }
        // STANDARD: Welcome → Intention → Anchors → Sleep → Closing (5 pasos, sin Sobriety)
        assertEquals(
            listOf(
                OnboardingStep.Welcome,
                OnboardingStep.Intention,
                OnboardingStep.Anchors,
                OnboardingStep.Sleep,
                OnboardingStep.Closing,
            ),
            visited,
        )
    }

    @Test
    fun `next recorre la secuencia completa PROTECTION hasta Closing con Sobriety`() {
        var step = OnboardingStep.Welcome
        val visited = mutableListOf(step)
        var iterations = 0
        while (iterations < 10) {
            val nextStep = OnboardingFlow.next(step, OnboardingIntention.PROTECTION)
            if (nextStep == step) break
            visited.add(nextStep)
            step = nextStep
            iterations++
        }
        // PROTECTION: Welcome → Intention → Anchors → Sleep → Sobriety → Closing (6 pasos)
        assertEquals(
            listOf(
                OnboardingStep.Welcome,
                OnboardingStep.Intention,
                OnboardingStep.Anchors,
                OnboardingStep.Sleep,
                OnboardingStep.Sobriety,
                OnboardingStep.Closing,
            ),
            visited,
        )
    }

    @Test
    fun `previous retrocede al bloque anterior`() {
        assertEquals(OnboardingStep.Anchors, OnboardingFlow.previous(OnboardingStep.Sleep, OnboardingIntention.STANDARD))
    }

    @Test
    fun `previous en el primer bloque se mantiene`() {
        assertEquals(OnboardingStep.Welcome, OnboardingFlow.previous(OnboardingStep.Welcome, OnboardingIntention.STANDARD))
    }

    // endregion

    // region Ramificación Sleep ↔ Sobriety ↔ Closing (S4-D3)

    @Test
    fun `next_Sleep_STANDARD_returnsClosing`() {
        assertEquals(OnboardingStep.Closing, OnboardingFlow.next(OnboardingStep.Sleep, OnboardingIntention.STANDARD))
    }

    @Test
    fun `next_Sleep_PROTECTION_returnsSobriety`() {
        assertEquals(OnboardingStep.Sobriety, OnboardingFlow.next(OnboardingStep.Sleep, OnboardingIntention.PROTECTION))
    }

    @Test
    fun `next_Sobriety_PROTECTION_returnsClosing`() {
        assertEquals(OnboardingStep.Closing, OnboardingFlow.next(OnboardingStep.Sobriety, OnboardingIntention.PROTECTION))
    }

    @Test
    fun `next_Sobriety_STANDARD_returnsClosing_defensivo`() {
        // Caso borde: Sobriety no debería aparecer en STANDARD, pero si lo hace → Closing
        assertEquals(OnboardingStep.Closing, OnboardingFlow.next(OnboardingStep.Sobriety, OnboardingIntention.STANDARD))
    }

    @Test
    fun `previous_Closing_STANDARD_returnsSleep`() {
        assertEquals(OnboardingStep.Sleep, OnboardingFlow.previous(OnboardingStep.Closing, OnboardingIntention.STANDARD))
    }

    @Test
    fun `previous_Closing_PROTECTION_returnsSobriety`() {
        assertEquals(OnboardingStep.Sobriety, OnboardingFlow.previous(OnboardingStep.Closing, OnboardingIntention.PROTECTION))
    }

    @Test
    fun `previous_Sobriety_PROTECTION_returnsSleep`() {
        assertEquals(OnboardingStep.Sleep, OnboardingFlow.previous(OnboardingStep.Sobriety, OnboardingIntention.PROTECTION))
    }

    // endregion

    // region resolve — campo intention (S4-D2)

    @Test
    fun `resolve_intentionPROTECTION_hidrata`() {
        val state = OnboardingFlow.resolve(
            completed = false,
            persistedStepName = "Sleep",
            persistedIntention = "PROTECTION",
        )
        assertEquals(OnboardingIntention.PROTECTION, state.intention)
    }

    @Test
    fun `resolve_intentionNull_defaultsSTANDARD`() {
        val state = OnboardingFlow.resolve(
            completed = false,
            persistedStepName = null,
            persistedIntention = null,
        )
        assertEquals(OnboardingIntention.STANDARD, state.intention)
    }

    @Test
    fun `resolve_intentionInvalida_defaultsSTANDARD`() {
        val state = OnboardingFlow.resolve(
            completed = false,
            persistedStepName = null,
            persistedIntention = "basura",
        )
        assertEquals(OnboardingIntention.STANDARD, state.intention)
    }

    // endregion

    // region shouldStartOnboarding (task 2.3) — spec §First-Run Routing

    @Test
    fun `shouldStartOnboarding es true cuando no esta completado`() {
        val state = OnboardingState(
            completed = false,
            currentStep = OnboardingStep.Welcome,
            intention = OnboardingIntention.STANDARD,
        )
        assertTrue(OnboardingFlow.shouldStartOnboarding(state))
    }

    @Test
    fun `shouldStartOnboarding es false cuando esta completado`() {
        val state = OnboardingState(
            completed = true,
            currentStep = OnboardingStep.Closing,
            intention = OnboardingIntention.STANDARD,
        )
        assertFalse(OnboardingFlow.shouldStartOnboarding(state))
    }

    // endregion
}

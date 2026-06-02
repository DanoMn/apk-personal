package dev.panopt.autonomia.domain.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingFlowTest {

    // region resolve (task 2.1) — spec §Persisted State, §Invalid Persisted Step

    @Test
    fun `resolve sin paso persistido empieza en Welcome no completado`() {
        val state = OnboardingFlow.resolve(completed = false, persistedStepName = null)
        assertEquals(OnboardingState(completed = false, currentStep = OnboardingStep.Welcome), state)
    }

    @Test
    fun `resolve con paso valido reanuda en ese paso`() {
        val state = OnboardingFlow.resolve(completed = false, persistedStepName = "Sleep")
        assertEquals(OnboardingStep.Sleep, state.currentStep)
        assertFalse(state.completed)
    }

    @Test
    fun `resolve con paso invalido cae de forma segura a Welcome`() {
        val state = OnboardingFlow.resolve(completed = false, persistedStepName = "Inexistente")
        assertEquals(OnboardingStep.Welcome, state.currentStep)
    }

    @Test
    fun `resolve preserva completed true aunque haya paso persistido`() {
        val state = OnboardingFlow.resolve(completed = true, persistedStepName = "Sleep")
        assertTrue(state.completed)
        assertEquals(OnboardingStep.Sleep, state.currentStep)
    }

    // endregion

    // region next / previous (task 2.2) — spec §Block Navigation Skeleton

    @Test
    fun `next avanza al bloque siguiente`() {
        assertEquals(OnboardingStep.Intention, OnboardingFlow.next(OnboardingStep.Welcome))
    }

    @Test
    fun `next en el ultimo bloque se mantiene`() {
        assertEquals(OnboardingStep.Closing, OnboardingFlow.next(OnboardingStep.Closing))
    }

    @Test
    fun `next recorre la secuencia completa hasta Closing`() {
        var step = OnboardingStep.Welcome
        val visited = mutableListOf(step)
        repeat(OnboardingStep.entries.size) {
            val nextStep = OnboardingFlow.next(step)
            if (nextStep != step) visited.add(nextStep)
            step = nextStep
        }
        assertEquals(OnboardingStep.entries.toList(), visited)
    }

    @Test
    fun `previous retrocede al bloque anterior`() {
        assertEquals(OnboardingStep.Anchors, OnboardingFlow.previous(OnboardingStep.Sleep))
    }

    @Test
    fun `previous en el primer bloque se mantiene`() {
        assertEquals(OnboardingStep.Welcome, OnboardingFlow.previous(OnboardingStep.Welcome))
    }

    // endregion

    // region shouldStartOnboarding (task 2.3) — spec §First-Run Routing

    @Test
    fun `shouldStartOnboarding es true cuando no esta completado`() {
        val state = OnboardingState(completed = false, currentStep = OnboardingStep.Welcome)
        assertTrue(OnboardingFlow.shouldStartOnboarding(state))
    }

    @Test
    fun `shouldStartOnboarding es false cuando esta completado`() {
        val state = OnboardingState(completed = true, currentStep = OnboardingStep.Closing)
        assertFalse(OnboardingFlow.shouldStartOnboarding(state))
    }

    // endregion
}

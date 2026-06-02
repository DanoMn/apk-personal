package dev.panopt.autonomia.domain.onboarding

/**
 * Estado del onboarding: si terminó y en qué bloque está parado el usuario.
 *
 * Es un valor puro derivado de lo persistido en prefs (flag de completitud +
 * nombre del paso). No conoce Android ni Compose.
 */
data class OnboardingState(
    val completed: Boolean,
    val currentStep: OnboardingStep,
)

/**
 * Lógica pura del flujo de onboarding: resolución del estado desde lo persistido y
 * navegación entre bloques. Sin dependencias de Android — testeable en JVM puro.
 */
object OnboardingFlow {

    /** Primer bloque de la secuencia. */
    val firstStep: OnboardingStep = OnboardingStep.Welcome

    /**
     * Reconstruye el [OnboardingState] desde lo persistido. Un [persistedStepName]
     * nulo o inválido (no corresponde a ningún [OnboardingStep]) cae de forma segura
     * a [firstStep].
     */
    fun resolve(completed: Boolean, persistedStepName: String?): OnboardingState {
        val step = persistedStepName
            ?.let { name -> runCatching { OnboardingStep.valueOf(name) }.getOrNull() }
            ?: firstStep
        return OnboardingState(completed = completed, currentStep = step)
    }

    /** Bloque siguiente; en el último, se mantiene. */
    fun next(step: OnboardingStep): OnboardingStep {
        val entries = OnboardingStep.entries
        return entries.getOrElse(entries.indexOf(step) + 1) { step }
    }

    /** Bloque anterior; en el primero, se mantiene. */
    fun previous(step: OnboardingStep): OnboardingStep {
        val entries = OnboardingStep.entries
        val index = entries.indexOf(step)
        return if (index <= 0) step else entries[index - 1]
    }

    /** true → mostrar el onboarding; false → ir directo al dashboard. */
    fun shouldStartOnboarding(state: OnboardingState): Boolean = !state.completed
}

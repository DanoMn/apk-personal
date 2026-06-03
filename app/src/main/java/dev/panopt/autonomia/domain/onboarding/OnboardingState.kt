package dev.panopt.autonomia.domain.onboarding

/**
 * Estado del onboarding: si terminó, en qué bloque está parado y con qué intención.
 *
 * Es un valor puro derivado de lo persistido en prefs (flag de completitud +
 * nombre del paso + intención). No conoce Android ni Compose.
 */
data class OnboardingState(
    val completed: Boolean,
    val currentStep: OnboardingStep,
    val intention: OnboardingIntention = OnboardingIntention.STANDARD,
)

/**
 * Lógica pura del flujo de onboarding: resolución del estado desde lo persistido y
 * navegación entre bloques. Sin dependencias de Android — testeable en JVM puro.
 *
 * La intención del usuario ramifica la secuencia entre [OnboardingStep.Sleep] y
 * [OnboardingStep.Closing]: la ruta [OnboardingIntention.STANDARD] omite Sobriety;
 * la ruta [OnboardingIntention.PROTECTION] la incluye.
 */
object OnboardingFlow {

    /** Primer bloque de la secuencia. */
    val firstStep: OnboardingStep = OnboardingStep.Welcome

    /**
     * Reconstruye el [OnboardingState] desde lo persistido. Un [persistedStepName]
     * nulo o inválido cae de forma segura a [firstStep]. Un [persistedIntention]
     * nulo o inválido cae de forma segura a [OnboardingIntention.STANDARD].
     */
    fun resolve(
        completed: Boolean,
        persistedStepName: String?,
        persistedIntention: String?,
    ): OnboardingState {
        val step = persistedStepName
            ?.let { name -> runCatching { OnboardingStep.valueOf(name) }.getOrNull() }
            ?: firstStep
        val intention = persistedIntention
            ?.let { raw -> runCatching { OnboardingIntention.valueOf(raw) }.getOrNull() }
            ?: OnboardingIntention.STANDARD
        return OnboardingState(completed = completed, currentStep = step, intention = intention)
    }

    /**
     * Bloque siguiente dado el paso actual y la intención del usuario.
     *
     * Ramas explícitas para los pasos que ramifican:
     * - [OnboardingStep.Sleep] → [OnboardingStep.Closing] (STANDARD) o [OnboardingStep.Sobriety] (PROTECTION)
     * - [OnboardingStep.Sobriety] → siempre [OnboardingStep.Closing]
     * - [OnboardingStep.Closing] → se mantiene (es el último)
     * - Resto → avance lineal por orden de declaración del enum.
     */
    fun next(step: OnboardingStep, intention: OnboardingIntention): OnboardingStep =
        when (step) {
            OnboardingStep.Sleep -> when (intention) {
                OnboardingIntention.PROTECTION -> OnboardingStep.Sobriety
                OnboardingIntention.STANDARD -> OnboardingStep.Closing
            }
            OnboardingStep.Sobriety -> OnboardingStep.Closing
            OnboardingStep.Closing -> OnboardingStep.Closing
            else -> {
                val entries = OnboardingStep.entries
                entries.getOrElse(entries.indexOf(step) + 1) { step }
            }
        }

    /**
     * Bloque anterior dado el paso actual y la intención del usuario.
     *
     * Ramas explícitas para los pasos que ramifican:
     * - [OnboardingStep.Closing] → [OnboardingStep.Sleep] (STANDARD) o [OnboardingStep.Sobriety] (PROTECTION)
     * - [OnboardingStep.Sobriety] → siempre [OnboardingStep.Sleep]
     * - [OnboardingStep.Welcome] → se mantiene (es el primero)
     * - Resto → retroceso lineal por orden de declaración del enum.
     */
    fun previous(step: OnboardingStep, intention: OnboardingIntention): OnboardingStep =
        when (step) {
            OnboardingStep.Closing -> when (intention) {
                OnboardingIntention.PROTECTION -> OnboardingStep.Sobriety
                OnboardingIntention.STANDARD -> OnboardingStep.Sleep
            }
            OnboardingStep.Sobriety -> OnboardingStep.Sleep
            OnboardingStep.Welcome -> OnboardingStep.Welcome
            else -> {
                val entries = OnboardingStep.entries
                val index = entries.indexOf(step)
                if (index <= 0) step else entries[index - 1]
            }
        }

    /** true → mostrar el onboarding; false → ir directo al dashboard. */
    fun shouldStartOnboarding(state: OnboardingState): Boolean = !state.completed
}

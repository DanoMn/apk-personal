package dev.panopt.autonomia.domain.onboarding

/**
 * Reglas puras del Bloque Intención.
 * No conoce Android ni Compose — testeable en JVM puro.
 */
object OnboardingIntentionRule {
    /**
     * Devuelve true si el usuario ya eligió una intención (el botón "Continuar"
     * puede habilitarse). null significa "sin selección" → falso.
     */
    fun canAdvance(selection: OnboardingIntention?): Boolean = selection != null
}

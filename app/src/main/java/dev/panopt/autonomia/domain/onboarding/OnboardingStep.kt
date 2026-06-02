package dev.panopt.autonomia.domain.onboarding

/**
 * Bloques del onboarding de introducción, en orden de secuencia.
 *
 * El orden de declaración ES la secuencia de navegación: [Welcome] primero,
 * [Closing] último. La persistencia de reanudación guarda el [name] de cada paso
 * (no su ordinal), para que reordenar o quitar bloques en un update no corrompa la
 * reanudación por desplazamiento de índice.
 */
enum class OnboardingStep {
    Welcome,
    Intention,
    Anchors,
    Sleep,
    Sobriety,
    Closing,
}

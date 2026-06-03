package dev.panopt.autonomia.domain.onboarding

/**
 * Intención del usuario al iniciar el onboarding. Determina la ruta de navegación:
 * [STANDARD] omite el Bloque Sobriedad; [PROTECTION] lo incluye.
 *
 * Dominio puro — sin dependencias de Android.
 */
enum class OnboardingIntention {
    STANDARD,
    PROTECTION,
}

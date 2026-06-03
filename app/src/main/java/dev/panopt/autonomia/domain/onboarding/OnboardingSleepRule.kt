package dev.panopt.autonomia.domain.onboarding

import dev.panopt.autonomia.domain.sleep.SleepPolicy
import dev.panopt.autonomia.domain.sleep.SleepWindowValidation

/**
 * Regla de avance del Bloque Sueño del onboarding.
 *
 * Envuelve la segunda compuerta del motor ([SleepPolicy.validatePlannedWindow]) sin
 * duplicar la fórmula de minutos. El onboarding no avanza hasta que la ventana
 * planificada sea válida (duración ≥ [SleepPolicy.MIN_SLEEP_WINDOW_MINUTES]).
 *
 * Espejo de [OnboardingAnchorsRule] sobre [SleepPolicy] (mismo patrón).
 */
object OnboardingSleepRule {

    /** Umbral mínimo de ventana (espejo de la constante del motor; no diverge). */
    val minWindowMinutes: Int = SleepPolicy.MIN_SLEEP_WINDOW_MINUTES

    /**
     * true cuando la ventana planificada cumple la 2.ª compuerta del motor.
     */
    fun canAdvance(plannedSleepAt: String, plannedWakeAt: String): Boolean =
        SleepPolicy.validatePlannedWindow(plannedSleepAt, plannedWakeAt) is SleepWindowValidation.Valid

    /**
     * Duración derivada de la ventana en minutos, para mostrar en la UI.
     * Devuelve null si los horarios son imparseable.
     */
    fun derivedWindowMinutes(plannedSleepAt: String, plannedWakeAt: String): Int? =
        SleepPolicy.minutesBetween(plannedSleepAt, plannedWakeAt)
}

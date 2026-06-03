package dev.panopt.autonomia.domain.onboarding

import dev.panopt.autonomia.domain.sleep.SleepPolicy
import dev.panopt.autonomia.domain.sleep.SleepWindowValidation

/**
 * Razón por la que la ventana planificada no es válida, para que la UI muestre el
 * mensaje correcto en lugar de asumir siempre "ventana corta".
 *
 * - [NONE]: la ventana es válida, o el usuario todavía está tecleando (no molestar).
 * - [INVALID_FORMAT]: ambos campos están completos pero no forman una hora real.
 * - [TOO_SHORT]: las horas parsean bien pero la duración es menor al mínimo.
 */
enum class WindowFeedback { NONE, INVALID_FORMAT, TOO_SHORT }

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

    /**
     * Diagnóstico de la ventana planificada para la UI. Mientras algún campo siga
     * incompleto (menos de "HH:mm") devuelve [WindowFeedback.NONE] para no molestar
     * a mitad de tipeo. Con ambos completos, distingue formato inválido de duración
     * corta — lo que el mensaje hardcodeado anterior confundía.
     */
    fun windowFeedback(plannedSleepAt: String, plannedWakeAt: String): WindowFeedback {
        val bothComplete = plannedSleepAt.length == TIME_LENGTH && plannedWakeAt.length == TIME_LENGTH
        if (!bothComplete) return WindowFeedback.NONE
        return when (SleepPolicy.validatePlannedWindow(plannedSleepAt, plannedWakeAt)) {
            is SleepWindowValidation.Valid -> WindowFeedback.NONE
            is SleepWindowValidation.Invalid ->
                if (SleepPolicy.minutesBetween(plannedSleepAt, plannedWakeAt) == null) {
                    WindowFeedback.INVALID_FORMAT
                } else {
                    WindowFeedback.TOO_SHORT
                }
        }
    }

    /** Longitud de una hora completa "HH:mm". */
    private const val TIME_LENGTH = 5
}

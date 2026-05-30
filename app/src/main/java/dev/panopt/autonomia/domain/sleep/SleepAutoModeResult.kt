package dev.panopt.autonomia.domain.sleep

/**
 * Result of [toggleSleepAutoMode] — tells the UI what happened so it can react
 * without knowing about Android permission internals.
 *
 * The UI should:
 *   - [Success]             → update the toggle visual state.
 *   - [PermissionRequired]  → show the compassionate permission UX (design §7, tono AGENTS.md).
 */
sealed interface SleepAutoModeResult {
    /** The mode was activated or deactivated successfully. */
    data class Success(val enabled: Boolean) : SleepAutoModeResult

    /** Activation was requested but the telemetry permission is not yet granted.
     *  The toggle must NOT be flipped — show the permission prompt instead. */
    object PermissionRequired : SleepAutoModeResult
}

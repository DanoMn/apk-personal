package dev.panopt.autonomia.platform.telemetry

/** Decision for the periodic collection worker based on active consumer leases. */
enum class GatingAction { SCHEDULE, CANCEL, NO_OP }

/**
 * Pure opt-in gating. Telemetry stays blind: it only counts opaque consumer leases
 * and decides when to start/stop the periodic drain. It never knows which feature a
 * lease belongs to — only "is at least one consumer active". See D3/D4 in
 * device-telemetry/scoping-decisions.
 */
object TelemetryGatingPolicy {
    fun onRegister(previousActiveCount: Int): GatingAction =
        if (previousActiveCount == 0) GatingAction.SCHEDULE else GatingAction.NO_OP

    fun onUnregister(newActiveCount: Int): GatingAction =
        if (newActiveCount == 0) GatingAction.CANCEL else GatingAction.NO_OP
}

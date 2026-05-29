package dev.panopt.autonomia.platform.telemetry

/**
 * Generic, consumer-blind vocabulary of raw device activity facts.
 *
 * MUST NOT contain consumer concepts (no "sleep"/"wakeUp"): telemetry only records
 * what the device did; consumers interpret it. See sdd/device-telemetry/spec.
 */
enum class DeviceActivityEventType {
    SCREEN_ON,
    SCREEN_OFF,
    UNLOCK,
    LOCK,
    APP_FOREGROUND,
    APP_BACKGROUND,
    USER_INTERACTION,
}

/** A single raw device activity fact. Pure model — no Android types. */
data class DeviceActivityEvent(
    val eventType: DeviceActivityEventType,
    val packageName: String?,
    val timestamp: Long,
    val source: String,
)

package dev.panopt.autonomia.platform.telemetry

/**
 * Platform-facing source of raw device activity events.
 *
 * Implementations read events from the OS and normalize them into the generic
 * [DeviceActivityEvent] contract via [TelemetryEventMapper]. Consumers never touch
 * this — they read materialized facts through the repository (PULL).
 */
interface TelemetryCaptureSource {
    /**
     * Reads device events in the half-open range `[from, to)`.
     *
     * @return the events, or `null` when they cannot be read right now (e.g. the
     *   device is locked on Android R+, the service is unavailable, or the permission
     *   is missing). `null` means "skip this run" — NOT "nothing happened".
     */
    fun capture(from: Long, to: Long): List<DeviceActivityEvent>?
}

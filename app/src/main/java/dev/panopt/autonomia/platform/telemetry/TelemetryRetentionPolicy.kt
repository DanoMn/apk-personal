package dev.panopt.autonomia.platform.telemetry

/**
 * Pure retention math. Raw events with `timestamp < purgeThreshold` are deleted by
 * the drain worker after each run, keeping the buffer bounded (~retention window).
 */
object TelemetryRetentionPolicy {
    fun purgeThreshold(now: Long, retentionMillis: Long): Long = now - retentionMillis
}

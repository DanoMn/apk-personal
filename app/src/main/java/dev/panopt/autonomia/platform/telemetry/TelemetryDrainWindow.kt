package dev.panopt.autonomia.platform.telemetry

/** Half-open time range `[from, to)` to query for new events. */
data class DrainWindow(val from: Long, val to: Long)

/**
 * Pure incremental cursor logic. The cursor is the latest already-drained timestamp
 * (derived from `MAX(timestamp)` in Room — no separate cursor state). Purge never
 * removes the newest events, so `MAX` is always a safe cursor.
 */
object TelemetryDrainWindow {
    fun compute(lastTimestamp: Long?, now: Long, initialWindowMillis: Long): DrainWindow {
        val from = if (lastTimestamp != null) lastTimestamp + 1 else now - initialWindowMillis
        return DrainWindow(from = from, to = now)
    }
}

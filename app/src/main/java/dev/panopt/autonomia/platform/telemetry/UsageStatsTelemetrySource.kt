package dev.panopt.autonomia.platform.telemetry

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * Android implementation backed by [UsageStatsManager]. Requires the
 * `PACKAGE_USAGE_STATS` permission (granted by the user in Settings).
 *
 * Mapping/cursor/retention logic lives in the pure JVM helpers; this adapter only
 * bridges the Android API into the generic contract.
 */
class UsageStatsTelemetrySource(
    private val context: Context,
) : TelemetryCaptureSource {

    override fun capture(from: Long, to: Long): List<DeviceActivityEvent>? {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null

        // On Android R+ this returns null when the device is locked → "skip this run".
        val usageEvents = usageStatsManager.queryEvents(from, to) ?: return null

        val result = mutableListOf<DeviceActivityEvent>()
        val event = UsageEvents.Event()
        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            TelemetryEventMapper.map(
                typeCode = event.eventType,
                packageName = event.packageName,
                timestamp = event.timeStamp,
            )?.let(result::add)
        }
        return result
    }
}

package dev.panopt.autonomia.platform.telemetry

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.panopt.autonomia.data.AutonomiaDatabase
import dev.panopt.autonomia.data.DeviceActivityEventEntity

/**
 * Periodic drain: reads new device events from the OS since the last drained
 * timestamp (cursor = `MAX(timestamp)`), materializes them into Room, and purges
 * events older than the retention window. Mirrors the existing DailyClosureWorker.
 *
 * Safe no-ops when the permission is missing or the source can't read (device locked
 * on Android R+ → `capture` returns null). Never throws on the happy path; failures
 * retry. Only runs at all while a consumer lease keeps it scheduled (opt-in gating).
 */
class DeviceTelemetryDrainWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (!TelemetryPermission.isGranted(applicationContext)) return Result.success()
        return runCatching {
            val dao = AutonomiaDatabase.getInstance(applicationContext).autonomiaDao()
            val source: TelemetryCaptureSource = UsageStatsTelemetrySource(applicationContext)
            val now = System.currentTimeMillis()

            val window = TelemetryDrainWindow.compute(
                lastTimestamp = dao.latestDeviceActivityEventTimestamp(),
                now = now,
                initialWindowMillis = INITIAL_WINDOW_MILLIS,
            )

            val captured = source.capture(window.from, window.to) // null = skip this run
            if (!captured.isNullOrEmpty()) {
                dao.insertDeviceActivityEvents(captured.map { it.toEntity(createdAt = now) })
            }

            dao.deleteDeviceActivityEventsOlderThan(
                TelemetryRetentionPolicy.purgeThreshold(now = now, retentionMillis = RETENTION_MILLIS),
            )
        }.fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() },
        )
    }

    private fun DeviceActivityEvent.toEntity(createdAt: Long): DeviceActivityEventEntity =
        DeviceActivityEventEntity(
            eventType = eventType.name,
            packageName = packageName,
            timestamp = timestamp,
            source = source,
            createdAt = createdAt,
        )

    companion object {
        // ~3h initial backfill when there is no cursor yet (first run).
        private const val INITIAL_WINDOW_MILLIS = 3L * 60 * 60 * 1000
        // ~14 days of raw events (D5); consumers materialize their own inferences.
        private const val RETENTION_MILLIS = 14L * 24 * 60 * 60 * 1000
    }
}

package dev.panopt.autonomia.platform.telemetry

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dev.panopt.autonomia.data.AutonomiaDatabase
import dev.panopt.autonomia.data.TelemetryCollectionLeaseEntity
import java.util.concurrent.TimeUnit

/**
 * Opt-in gating for telemetry collection (D4). Consumer features register/unregister
 * an opaque lease key; the scheduler counts active leases and starts/stops the
 * periodic drain via [TelemetryGatingPolicy]. Telemetry never learns which feature a
 * key belongs to — only whether at least one consumer is active.
 *
 * Mirrors DailyClosureWorkScheduler (enqueueUniquePeriodicWork + KEEP).
 */
object DeviceTelemetryWorkScheduler {

    private const val UNIQUE_WORK_NAME = "device_telemetry_drain"
    private const val REPEAT_INTERVAL_HOURS = 3L

    /** A consumer feature turns collection ON. Idempotent. */
    suspend fun register(context: Context, consumerKey: String) {
        val dao = AutonomiaDatabase.getInstance(context.applicationContext).autonomiaDao()
        val previousActiveCount = dao.countActiveTelemetryLeases()
        dao.upsertTelemetryLease(TelemetryCollectionLeaseEntity(consumerKey))
        if (TelemetryGatingPolicy.onRegister(previousActiveCount) == GatingAction.SCHEDULE) {
            schedule(context)
        }
    }

    /** A consumer feature turns collection OFF. Idempotent. */
    suspend fun unregister(context: Context, consumerKey: String) {
        val dao = AutonomiaDatabase.getInstance(context.applicationContext).autonomiaDao()
        dao.deleteTelemetryLease(consumerKey)
        val newActiveCount = dao.countActiveTelemetryLeases()
        if (TelemetryGatingPolicy.onUnregister(newActiveCount) == GatingAction.CANCEL) {
            cancel(context)
        }
    }

    private fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DeviceTelemetryDrainWorker>(
            REPEAT_INTERVAL_HOURS,
            TimeUnit.HOURS,
        ).build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
    }

    private fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_WORK_NAME)
    }
}

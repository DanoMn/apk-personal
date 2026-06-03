package dev.panopt.autonomia.data.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.panopt.autonomia.app.AppGraph
import dev.panopt.autonomia.domain.notifications.SleepNotificationPolicy
import dev.panopt.autonomia.platform.notifications.SleepNotificationChannels
import dev.panopt.autonomia.platform.notifications.SleepNotifier

/**
 * Delivers Notification A (wind-down reminder) when the daily alarm fires.
 *
 * Re-verifies the scheduling condition at delivery time (the user may have
 * changed their consent or time since the job was enqueued). Does NOT request
 * the POST_NOTIFICATIONS permission: permission is managed lazily by
 * [dev.panopt.autonomia.MainActivity].
 */
class WindDownNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Ensure channels exist even when the process was restarted without MainActivity.
        SleepNotificationChannels.ensureCreated(applicationContext)

        val repository = AppGraph.autonomiaRepository(applicationContext)

        // Re-verify condition at delivery time.
        val consent = repository.sleepWindDownConsentFlow().value
        val targetSleepAt = repository.getSleepConfig().targetSleepAt

        if (!SleepNotificationPolicy.shouldScheduleWindDown(consent, targetSleepAt)) {
            // User changed their mind or config is now invalid; cancel this work.
            WindDownNotificationScheduler.cancel(applicationContext)
            return Result.success()
        }

        // SleepNotifier.postWindDown checks the permission internally; if not granted it
        // silently no-ops. No retry with value here — reminders are best-effort.
        SleepNotifier.postWindDown(applicationContext)
        return Result.success()
    }
}

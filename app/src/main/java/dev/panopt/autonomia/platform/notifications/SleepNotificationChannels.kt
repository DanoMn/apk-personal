package dev.panopt.autonomia.platform.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import dev.panopt.autonomia.R

/**
 * Registers the two sleep notification channels idempotently.
 *
 * Must be called from [MainActivity.onCreate] and defensively from each worker
 * before posting a notification, to handle the case where the worker runs
 * after a process restart before [MainActivity] was ever opened.
 *
 * Idempotent: calling [ensureCreated] when the channel already exists is a no-op.
 */
object SleepNotificationChannels {

    const val CHANNEL_WIND_DOWN = "sleep_wind_down"
    const val CHANNEL_DATA_ALERT = "sleep_data_alert"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val windDownChannel = NotificationChannel(
            CHANNEL_WIND_DOWN,
            context.getString(R.string.notif_channel_wind_down_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_wind_down_desc)
        }

        val dataAlertChannel = NotificationChannel(
            CHANNEL_DATA_ALERT,
            context.getString(R.string.notif_channel_data_alert_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notif_channel_data_alert_desc)
        }

        manager.createNotificationChannel(windDownChannel)
        manager.createNotificationChannel(dataAlertChannel)
    }
}

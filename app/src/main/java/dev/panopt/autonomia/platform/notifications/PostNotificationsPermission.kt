package dev.panopt.autonomia.platform.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility for checking whether [android.Manifest.permission.POST_NOTIFICATIONS] is
 * available for use.
 *
 * On API < 33 (Android < Tiramisu), the permission does not exist as a runtime permission
 * and is considered automatically granted. This function encapsulates that guard so
 * callers never need to branch on the SDK level themselves.
 */
object PostNotificationsPermission {

    /**
     * Returns true if the app is allowed to post notifications.
     *
     * Always true on API < 33. On API ≥ 33, checks [PackageManager.PERMISSION_GRANTED].
     */
    fun isGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
}

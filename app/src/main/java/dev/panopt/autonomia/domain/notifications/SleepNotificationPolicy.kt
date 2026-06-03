package dev.panopt.autonomia.domain.notifications

import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import java.time.LocalTime

/**
 * Domain-pure policy for sleep notifications.
 *
 * No Android dependencies. All decision logic lives here so it can be tested
 * with JVM-only tests.
 */
object SleepNotificationPolicy {

    /**
     * Number of consecutive nights with NoData (or absent records) that trigger
     * Notification B (Sleep Data Alert). Calibrate with real-world data post-launch.
     */
    const val NIGHTS_WITHOUT_DATA_THRESHOLD = 3

    /**
     * Returns true if the wind-down reminder (Notif A) should be scheduled.
     *
     * Conditions (AND):
     * - [consent] is explicitly true (null / false → do not schedule)
     * - [targetSleepAt] is a parseable HH:mm time string
     */
    fun shouldScheduleWindDown(consent: Boolean?, targetSleepAt: String?): Boolean =
        consent == true && isValidTime(targetSleepAt)

    /**
     * Returns true if the sleep data alert (Notif B) should be fired.
     *
     * [confidences] must be the most-recent-first list of sleep confidence values
     * for the last [threshold] nights. Null entries mean the night has no record
     * and are treated as [SleepConfidence.NoData].
     *
     * A [threshold] of 0 is a defence guard and always returns false.
     */
    fun shouldFireDataAlert(
        confidences: List<SleepConfidence?>,
        threshold: Int = NIGHTS_WITHOUT_DATA_THRESHOLD,
    ): Boolean {
        if (threshold <= 0) return false
        val window = confidences.take(threshold)
        if (window.size < threshold) return false
        return window.all { it == null || it == SleepConfidence.NoData }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    private fun isValidTime(value: String?): Boolean =
        value != null && runCatching { LocalTime.parse(value) }.getOrNull() != null
}

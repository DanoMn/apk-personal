package dev.panopt.autonomia.domain.sleep.interpretation

/**
 * Calibration thresholds for the sleep interpretation algorithm.
 *
 * All defaults are calibration proposals (D1/future); centralizing them here
 * allows recalculating scores from durable segments without touching logic.
 *
 * @param quietGapMillis          Millis of device silence that ends an AwakeUse episode (~15 min).
 * @param napSeparationMillis     Millis gap that separates two distinct sleep blocks (~90 min).
 * @param napAnchorWindowMinutes  Minutes of proximity to the objective window for nap exclusion (~120 min).
 * @param definitiveWakeMinMinutes Minimum duration in minutes for a wake episode to be "definitive" (~10 min).
 * @param returnToSleepMinMinutes  Minimum Asleep block in minutes to count as "returned to sleep" (~30 min).
 */
data class InterpretationParams(
    val quietGapMillis: Long,
    val napSeparationMillis: Long,
    val napAnchorWindowMinutes: Int,
    val definitiveWakeMinMinutes: Int,
    val returnToSleepMinMinutes: Int,
) {
    companion object {
        /** Calibration defaults (D1: fine-tune with real user data). */
        val DEFAULT = InterpretationParams(
            quietGapMillis = 15L * 60_000L,       // 15 minutes
            napSeparationMillis = 90L * 60_000L,  // 90 minutes
            napAnchorWindowMinutes = 120,
            definitiveWakeMinMinutes = 10,
            returnToSleepMinMinutes = 30,
        )
    }
}

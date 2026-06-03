package dev.panopt.autonomia.domain.notifications

import dev.panopt.autonomia.domain.sleep.interpretation.SleepConfidence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepNotificationPolicyTest {

    // ─────────────────────────────────────────────────────────────────────────
    // shouldFireDataAlert — threshold-related
    // ─────────────────────────────────────────────────────────────────────────

    /** (a) Three consecutive NoData nights → fire alert */
    @Test
    fun threeNoData_firesAlert() {
        val result = SleepNotificationPolicy.shouldFireDataAlert(
            listOf(SleepConfidence.NoData, SleepConfidence.NoData, SleepConfidence.NoData),
        )
        assertTrue(result)
    }

    /** (b) Only two NoData nights (history shorter than threshold) → no alert */
    @Test
    fun twoNoData_noAlert_historyShort() {
        val result = SleepNotificationPolicy.shouldFireDataAlert(
            listOf(SleepConfidence.NoData, SleepConfidence.NoData),
        )
        assertFalse(result)
    }

    /** (c) First night (most recent) has data → counter not consecutive → no alert */
    @Test
    fun firstNightHasData_noAlert() {
        val result = SleepNotificationPolicy.shouldFireDataAlert(
            listOf(SleepConfidence.High, SleepConfidence.NoData, SleepConfidence.NoData),
        )
        assertFalse(result)
    }

    /** (d) Middle night has data → chain broken → no alert */
    @Test
    fun middleNightHasData_noAlert() {
        val result = SleepNotificationPolicy.shouldFireDataAlert(
            listOf(SleepConfidence.NoData, SleepConfidence.High, SleepConfidence.NoData),
        )
        assertFalse(result)
    }

    /** (e) Null entries count as NoData → three nulls fire alert */
    @Test
    fun nullNightsAreNoData_fires() {
        val result = SleepNotificationPolicy.shouldFireDataAlert(
            listOf(null, null, null),
        )
        assertTrue(result)
    }

    /** (f) Four NoData nights but default threshold is 3 → still fires (takes latest 3) */
    @Test
    fun fourNoData_defaultThreshold3_fires() {
        val result = SleepNotificationPolicy.shouldFireDataAlert(
            listOf(
                SleepConfidence.NoData,
                SleepConfidence.NoData,
                SleepConfidence.NoData,
                SleepConfidence.NoData,
            ),
        )
        assertTrue(result)
    }

    /** (g) Custom threshold=1 with one NoData → fires */
    @Test
    fun threshold1_oneNoData_fires() {
        val result = SleepNotificationPolicy.shouldFireDataAlert(
            listOf(SleepConfidence.NoData),
            threshold = 1,
        )
        assertTrue(result)
    }

    /** (h) Threshold 0 is a defence guard — always false */
    @Test
    fun threshold0_defenseGuard() {
        val result = SleepNotificationPolicy.shouldFireDataAlert(
            listOf(SleepConfidence.NoData, SleepConfidence.NoData, SleepConfidence.NoData),
            threshold = 0,
        )
        assertFalse(result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // shouldScheduleWindDown
    // ─────────────────────────────────────────────────────────────────────────

    /** (i) consent=true + valid time → schedule */
    @Test
    fun consentTrue_validTime_schedules() {
        val result = SleepNotificationPolicy.shouldScheduleWindDown(true, "23:30")
        assertTrue(result)
    }

    /** (j) consent=false → do not schedule */
    @Test
    fun consentFalse_doesNotSchedule() {
        val result = SleepNotificationPolicy.shouldScheduleWindDown(false, "23:30")
        assertFalse(result)
    }

    /** (k) consent=null → do not schedule */
    @Test
    fun consentNull_doesNotSchedule() {
        val result = SleepNotificationPolicy.shouldScheduleWindDown(null, "23:30")
        assertFalse(result)
    }

    /** (l) consent=true but targetSleepAt=null → do not schedule */
    @Test
    fun consentTrue_nullTime_doesNotSchedule() {
        val result = SleepNotificationPolicy.shouldScheduleWindDown(true, null)
        assertFalse(result)
    }

    /** (m) consent=true but time is unparseable → false without crash */
    @Test
    fun consentTrue_invalidTime_nocrash() {
        val result = SleepNotificationPolicy.shouldScheduleWindDown(true, "99:99")
        assertFalse(result)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Constant
    // ─────────────────────────────────────────────────────────────────────────

    /** (n) Default threshold constant is exactly 3 */
    @Test
    fun thresholdConstantIs3() {
        assertEquals(3, SleepNotificationPolicy.NIGHTS_WITHOUT_DATA_THRESHOLD)
    }
}

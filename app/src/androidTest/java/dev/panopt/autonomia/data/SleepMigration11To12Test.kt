package dev.panopt.autonomia.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * MigrationTestHelper test for MIGRATION_11_12.
 *
 * TDD cycle (androidTest — requires device or emulator):
 *   RED  : test references MIGRATION_11_12 SQL before it was written.
 *   GREEN: migration registered in AutonomiaDatabase + correct SQL → test passes.
 *
 * Verifies:
 *  - Table sleep_nights is created with all expected columns.
 *  - Table sleep_segments is created with all expected columns.
 *  - Index index_sleep_segments_nightDate exists (exact naming, not idx_*).
 *  - Table sleep_logs no longer exists (DROPped by migration).
 *  - Final schema matches Room-generated entity schema for v12.
 *
 * NOTE: this is an androidTest (instrumented). Run via:
 *   gradlew connectedDebugAndroidTest --tests '...SleepMigration11To12Test'
 * It cannot run on the JVM (no Room in-memory + MigrationTestHelper without device).
 */
@RunWith(AndroidJUnit4::class)
class SleepMigration11To12Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AutonomiaDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrates11To12_sleepNightsTableExists() {
        // Create the v11 database from the exported schema JSON
        val db = helper.createDatabase(TEST_DB_NAME, 11)
        db.close()

        // Run migration 11 → 12
        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            12,
            true,
            AutonomiaDatabase.MIGRATION_10_11,
            AutonomiaDatabase.MIGRATION_11_12,
        )

        // Verify sleep_nights table exists with expected columns
        val cursor = migratedDb.query(
            "SELECT * FROM pragma_table_info('sleep_nights')",
        )
        assertTrue("sleep_nights table should exist", cursor.count > 0)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrates11To12_sleepSegmentsTableExists() {
        helper.createDatabase(TEST_DB_NAME, 11).close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            12,
            true,
            AutonomiaDatabase.MIGRATION_10_11,
            AutonomiaDatabase.MIGRATION_11_12,
        )

        val cursor = migratedDb.query(
            "SELECT * FROM pragma_table_info('sleep_segments')",
        )
        assertTrue("sleep_segments table should exist", cursor.count > 0)
        cursor.close()
    }

    @Test
    @Throws(IOException::class)
    fun migrates11To12_segmentIndexHasCorrectName() {
        helper.createDatabase(TEST_DB_NAME, 11).close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            12,
            true,
            AutonomiaDatabase.MIGRATION_10_11,
            AutonomiaDatabase.MIGRATION_11_12,
        )

        // Verify the index name is exactly index_sleep_segments_nightDate (NOT idx_*)
        val cursor = migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type='index' AND tbl_name='sleep_segments'",
        )
        var foundIndex = false
        while (cursor.moveToNext()) {
            if (cursor.getString(0) == "index_sleep_segments_nightDate") {
                foundIndex = true
            }
        }
        cursor.close()
        assertTrue("Index index_sleep_segments_nightDate must exist (exact name)", foundIndex)
    }

    @Test
    @Throws(IOException::class)
    fun migrates11To12_sleepLogsTableDropped() {
        helper.createDatabase(TEST_DB_NAME, 11).close()

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB_NAME,
            12,
            true,
            AutonomiaDatabase.MIGRATION_10_11,
            AutonomiaDatabase.MIGRATION_11_12,
        )

        // sleep_logs must be gone after the migration
        val cursor = migratedDb.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name='sleep_logs'",
        )
        val tableExists = cursor.count > 0
        cursor.close()
        assertTrue("sleep_logs table should NOT exist after migration", !tableExists)
    }

    companion object {
        private const val TEST_DB_NAME = "migration-test"
    }
}

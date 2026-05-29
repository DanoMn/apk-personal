package dev.panopt.autonomia.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        LayerEntity::class,
        ActivityDefinitionEntity::class,
        UserActivityConfigEntity::class,
        DailyActivityLogEntity::class,
        AbstinenceTrackEntity::class,
        AbstinenceLogEntity::class,
        AbstinenceRelapseEventEntity::class,
        RiskEventEntity::class,
        TaskEntity::class,
        AnchorPhraseEntity::class,
        AnchorPhraseStateRuleEntity::class,
        AnchorPhrasePhaseRuleEntity::class,
        AnchorPhraseImpressionEntity::class,
        AnchorPhraseDailySlotEntity::class,
        SleepConfigEntity::class,
        SleepSessionStateEntity::class,
        SleepLogEntity::class,
        DailyClosureEntity::class,
        WeeklyScoreSnapshotEntity::class,
        DeviceActivityEventEntity::class,
        TelemetryCollectionLeaseEntity::class,
    ],
    version = 11,
    exportSchema = false
)
abstract class AutonomiaDatabase : RoomDatabase() {
    abstract fun autonomiaDao(): AutonomiaDao

    companion object {
        @Volatile
        private var INSTANCE: AutonomiaDatabase? = null

        fun getInstance(context: Context): AutonomiaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AutonomiaDatabase::class.java,
                    "autonomia_db"
                )
                    // Development phase (AGENTS.md #29, CLAUDE.md): dev data is
                    // disposable, so any schema/migration mismatch recreates the DB
                    // instead of crashing the app on open.
                    //
                    // The hand-written MIGRATION_* below have known schema-mismatch
                    // bugs (index names `idx_*` and spurious column DEFAULTs that do
                    // not match the Room-generated entity schema — engram bug #587).
                    // They are intentionally NOT registered while in dev so Room takes
                    // the destructive-recreate path on version changes.
                    //
                    // BEFORE RELEASE: fix the MIGRATION_* SQL to match the entities,
                    // re-enable addMigrations(...), remove this destructive fallback,
                    // and add MigrationTestHelper coverage (domain `gradlew test` does
                    // NOT exercise real Room migrations).
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sleep_logs (
                        date TEXT NOT NULL PRIMARY KEY,
                        plannedSleepAt TEXT NOT NULL,
                        plannedWakeAt TEXT NOT NULL,
                        sleptAt TEXT NOT NULL,
                        wokeAt TEXT NOT NULL,
                        quality TEXT NOT NULL,
                        note TEXT NOT NULL DEFAULT '',
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE abstinence_tracks
                    SET active = 0
                    WHERE id IN ('trk_alcohol', 'trk_sexual', 'trk_marihuana')
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create activity_definitions table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS activity_definitions (
                        id TEXT NOT NULL PRIMARY KEY,
                        layerId TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        type TEXT NOT NULL,
                        role TEXT NOT NULL,
                        unit TEXT NOT NULL,
                        contributionRole TEXT NOT NULL,
                        importanceTier TEXT NOT NULL,
                        presetCategory TEXT,
                        sortOrder INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_activity_definitions_layerId ON activity_definitions(layerId)")

                // 2. Create user_activity_configs table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_activity_configs (
                        activityId TEXT NOT NULL PRIMARY KEY,
                        activityType TEXT NOT NULL,
                        active INTEGER NOT NULL DEFAULT 1,
                        archived INTEGER NOT NULL DEFAULT 0,
                        customName TEXT,
                        customDescription TEXT,
                        cadence TEXT,
                        targetValue INTEGER,
                        minimumValue INTEGER,
                        targetCount INTEGER,
                        targetPeriod TEXT,
                        sortOrder INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        FOREIGN KEY (activityId) REFERENCES activity_definitions(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                // 3. Copy definitions from old activities table
                db.execSQL(
                    """
                    INSERT INTO activity_definitions (
                        id, layerId, name, description, type, role, unit,
                        contributionRole, importanceTier, presetCategory,
                        sortOrder, createdAt, updatedAt
                    )
                    SELECT id, layerId, name, description, type, role, unit,
                        contributionRole, importanceTier,
                        CASE
                            WHEN id LIKE 'act_custom_%' THEN NULL
                            WHEN displaySurface IN ('PrimaryChecklist', 'Contextual', 'Compact', 'Silent') THEN 'anchor'
                            WHEN displaySurface = 'SecondaryChecklist' THEN 'support'
                            ELSE NULL
                        END,
                        sortOrder, createdAt, updatedAt
                    FROM activities
                    """.trimIndent(),
                )

                // 4. Copy configs for non-Available activities
                db.execSQL(
                    """
                    INSERT INTO user_activity_configs (
                        activityId, activityType, active, archived,
                        cadence, targetValue, minimumValue, targetCount,
                        targetPeriod, sortOrder, createdAt, updatedAt
                    )
                    SELECT id,
                        CASE
                            WHEN displaySurface IN ('PrimaryChecklist', 'Contextual', 'Compact') THEN 'Anchor'
                            WHEN displaySurface = 'SecondaryChecklist' THEN 'Support'
                            WHEN displaySurface = 'Silent' THEN 'Anchor'
                        END,
                        CASE WHEN displaySurface = 'Silent' THEN 0 ELSE active END,
                        CASE WHEN displaySurface = 'Silent' THEN 1 ELSE archived END,
                        cadence, targetValue, minimumValue, targetCount,
                        targetPeriod, sortOrder, createdAt, updatedAt
                    FROM activities
                    WHERE displaySurface NOT IN ('Available')
                    """.trimIndent(),
                )

                // 5. Drop old activities table
                db.execSQL("DROP TABLE IF EXISTS activities")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_activity_configs ADD COLUMN weeklyFrequencyTarget INTEGER")
                db.execSQL("ALTER TABLE user_activity_configs ADD COLUMN sessionTargetMinutes INTEGER")
                db.execSQL("ALTER TABLE user_activity_configs ADD COLUMN commitmentDurationMonths INTEGER")
                db.execSQL(
                    """
                    UPDATE user_activity_configs
                    SET sessionTargetMinutes = targetValue
                    WHERE activityType = 'Anchor' AND targetValue IS NOT NULL
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    UPDATE user_activity_configs
                    SET weeklyFrequencyTarget = CASE
                        WHEN targetPeriod = 'Week' AND targetCount IS NOT NULL THEN
                            MIN(MAX(targetCount, 2), 7)
                        WHEN targetPeriod = 'Month' AND targetCount IS NOT NULL THEN
                            MIN(MAX(((targetCount + 3) / 4), 2), 7)
                        ELSE NULL
                    END
                    WHERE activityType = 'Anchor'
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sleep_config (
                        id TEXT NOT NULL PRIMARY KEY,
                        targetSleepAt TEXT NOT NULL,
                        targetWakeAt TEXT NOT NULL,
                        digitalWindDownMinutes INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createSleepSessionStateTable(db)
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_closures (
                        date TEXT NOT NULL PRIMARY KEY,
                        timezoneId TEXT NOT NULL,
                        closedAt INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        closureVersion INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS weekly_score_snapshots (
                        weekStart TEXT NOT NULL,
                        weekEnd TEXT NOT NULL,
                        scoringVersion TEXT NOT NULL,
                        calculatedAt INTEGER NOT NULL,
                        configHash TEXT NOT NULL,
                        factsHash TEXT NOT NULL,
                        weeklyBaseScore REAL NOT NULL,
                        weeklyScore REAL NOT NULL,
                        stabilityScore REAL,
                        state TEXT NOT NULL,
                        visibleScore INTEGER NOT NULL,
                        worstLayerId TEXT,
                        layerSummariesJson TEXT NOT NULL,
                        reasonsJson TEXT NOT NULL,
                        PRIMARY KEY(weekStart, scoringVersion)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_weekly_score_snapshots_weekEnd ON weekly_score_snapshots(weekEnd)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_weekly_score_snapshots_calculatedAt ON weekly_score_snapshots(calculatedAt)")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createDailyActivityLogsTable(db)
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO daily_activity_logs (
                        date,
                        timezoneId,
                        subjectType,
                        subjectId,
                        layerId,
                        status,
                        actualValue,
                        note,
                        createdAt,
                        updatedAt
                    )
                    SELECT
                        logs.date,
                        'system',
                        COALESCE(configs.activityType, 'Anchor'),
                        logs.activityId,
                        definitions.layerId,
                        CASE
                            WHEN configs.activityType = 'Support' AND logs.completed = 1 THEN 'Omitted'
                            WHEN configs.activityType = 'Support' THEN 'Done'
                            WHEN logs.completed = 1 THEN 'Done'
                            ELSE 'NotDone'
                        END,
                        logs.actualValue,
                        logs.note,
                        logs.updatedAt,
                        logs.updatedAt
                    FROM activity_logs AS logs
                    LEFT JOIN user_activity_configs AS configs
                        ON configs.activityId = logs.activityId
                    LEFT JOIN activity_definitions AS definitions
                        ON definitions.id = logs.activityId
                    """.trimIndent(),
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createAbstinenceRelapseEventsTable(db)
            }
        }

        // NOTE: like the other MIGRATION_* above, this is intentionally NOT
        // registered while in the development phase (destructive recreate handles
        // version bumps). It is written with the CORRECT Room index naming
        // (`index_<table>_<col>`, not `idx_*`) so it is ready to register before
        // release together with MigrationTestHelper coverage.
        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createDeviceActivityEventsTable(db)
                createTelemetryLeaseTable(db)
            }
        }

        private fun createSleepSessionStateTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS sleep_session_state (
                    id TEXT NOT NULL PRIMARY KEY,
                    date TEXT NOT NULL,
                    startedAt TEXT NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
        }

        private fun createDailyActivityLogsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS daily_activity_logs (
                    date TEXT NOT NULL,
                    timezoneId TEXT NOT NULL,
                    subjectType TEXT NOT NULL,
                    subjectId TEXT NOT NULL,
                    layerId TEXT,
                    status TEXT NOT NULL,
                    actualValue INTEGER,
                    note TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL,
                    PRIMARY KEY(date, subjectType, subjectId)
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_activity_logs_date ON daily_activity_logs(date)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_activity_logs_subjectId ON daily_activity_logs(subjectId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_activity_logs_layerId ON daily_activity_logs(layerId)")
        }

        private fun createAbstinenceRelapseEventsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS abstinence_relapse_events (
                    id TEXT NOT NULL PRIMARY KEY,
                    trackId TEXT NOT NULL,
                    startDate TEXT NOT NULL,
                    endDate TEXT NOT NULL,
                    source TEXT NOT NULL,
                    userAdjusted INTEGER NOT NULL DEFAULT 0,
                    note TEXT NOT NULL DEFAULT '',
                    createdAt INTEGER NOT NULL,
                    updatedAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_abstinence_relapse_events_trackId ON abstinence_relapse_events(trackId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_abstinence_relapse_events_startDate ON abstinence_relapse_events(startDate)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_abstinence_relapse_events_endDate ON abstinence_relapse_events(endDate)")
        }

        private fun createDeviceActivityEventsTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS device_activity_events (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    eventType TEXT NOT NULL,
                    packageName TEXT,
                    timestamp INTEGER NOT NULL,
                    source TEXT NOT NULL,
                    createdAt INTEGER NOT NULL
                )
                """.trimIndent(),
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS index_device_activity_events_timestamp ON device_activity_events(timestamp)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_device_activity_events_eventType ON device_activity_events(eventType)")
        }

        private fun createTelemetryLeaseTable(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS telemetry_collection_lease (
                    consumerKey TEXT NOT NULL PRIMARY KEY
                )
                """.trimIndent(),
            )
        }
    }
}

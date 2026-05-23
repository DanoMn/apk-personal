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
        ActivityEntity::class,
        ActivityDefinitionEntity::class,
        UserActivityConfigEntity::class,
        ActivityLogEntity::class,
        AbstinenceTrackEntity::class,
        AbstinenceLogEntity::class,
        RiskEventEntity::class,
        TaskEntity::class,
        AnchorPhraseEntity::class,
        AnchorPhraseStateRuleEntity::class,
        AnchorPhrasePhaseRuleEntity::class,
        AnchorPhraseImpressionEntity::class,
        AnchorPhraseDailySlotEntity::class,
        SleepLogEntity::class
    ],
    version = 4,
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
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
    }
}

package dev.panopt.autonomia.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AutonomiaDao {
    @Query("SELECT COUNT(*) FROM layers")
    suspend fun layerCount(): Int

    @Query("SELECT * FROM layers ORDER BY sortOrder")
    fun observeLayers(): Flow<List<LayerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLayers(layers: List<LayerEntity>)

    @Query("SELECT * FROM activity_logs WHERE date = :date")
    fun observeActivityLogsForDate(date: String): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs")
    fun observeAllActivityLogs(): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE date >= :startDate AND date <= :endDate")
    fun observeActivityLogsBetween(startDate: String, endDate: String): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityLog(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs WHERE activityId = :activityId AND date = :date")
    suspend fun deleteActivityLog(activityId: String, date: String)

    @Query("SELECT * FROM abstinence_tracks ORDER BY sortOrder")
    fun observeAbstinenceTracks(): Flow<List<AbstinenceTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertAbstinenceTracks(tracks: List<AbstinenceTrackEntity>)

    @Query("SELECT * FROM abstinence_logs WHERE date = :date")
    fun observeAbstinenceLogsForDate(date: String): Flow<List<AbstinenceLogEntity>>

    @Query("SELECT * FROM abstinence_logs")
    fun observeAllAbstinenceLogs(): Flow<List<AbstinenceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceLog(log: AbstinenceLogEntity)

    @Query("DELETE FROM abstinence_logs WHERE trackId = :trackId AND date = :date")
    suspend fun deleteAbstinenceLog(trackId: String, date: String)

    @Query("SELECT * FROM risk_events WHERE date = :date")
    fun observeRiskEventsForDate(date: String): Flow<List<RiskEventEntity>>

    @Query("SELECT * FROM risk_events ORDER BY createdAt DESC")
    fun observeRiskEvents(): Flow<List<RiskEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRiskEvent(event: RiskEventEntity)
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    // Anchor Phrase Queries
    @Query("SELECT * FROM anchor_phrases WHERE active = 1")
    fun observeAnchorPhrases(): Flow<List<AnchorPhraseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnchorPhrases(phrases: List<AnchorPhraseEntity>)
    
    @Query("SELECT * FROM anchor_phrase_state_rules")
    suspend fun getAnchorPhraseStateRules(): List<AnchorPhraseStateRuleEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnchorPhraseStateRules(rules: List<AnchorPhraseStateRuleEntity>)
    
    @Query("SELECT * FROM anchor_phrase_phase_rules")
    suspend fun getAnchorPhrasePhaseRules(): List<AnchorPhrasePhaseRuleEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnchorPhrasePhaseRules(rules: List<AnchorPhrasePhaseRuleEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnchorPhraseImpression(impression: AnchorPhraseImpressionEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAnchorPhraseDailySlot(slot: AnchorPhraseDailySlotEntity)
    
    @Query("SELECT * FROM anchor_phrase_daily_slots WHERE date = :date AND dayPhase = :dayPhase LIMIT 1")
    suspend fun getAnchorPhraseDailySlot(date: String, dayPhase: String): AnchorPhraseDailySlotEntity?

    @Query("SELECT * FROM sleep_logs WHERE date = :date LIMIT 1")
    fun observeSleepLogForDate(date: String): Flow<SleepLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleepLog(log: SleepLogEntity)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM activity_logs")
    suspend fun clearAllActivityLogs()

    @Query("DELETE FROM abstinence_logs")
    suspend fun clearAllAbstinenceLogs()

    @Query("DELETE FROM risk_events")
    suspend fun clearAllRiskEvents()

    @Query("DELETE FROM tasks")
    suspend fun clearAllTasks()

    // --- New queries for activity_definitions ---

    @Query("SELECT * FROM activity_definitions ORDER BY sortOrder")
    fun observeActivityDefinitions(): Flow<List<ActivityDefinitionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityDefinition(definition: ActivityDefinitionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertActivityDefinitions(definitions: List<ActivityDefinitionEntity>)

    // --- New queries for user_activity_configs ---

    @Query("SELECT * FROM user_activity_configs WHERE active = 1 AND archived = 0")
    fun observeUserActivityConfigs(): Flow<List<UserActivityConfigEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserActivityConfig(config: UserActivityConfigEntity)

    @Query(
        "UPDATE user_activity_configs SET activityType = :activityType, " +
            "targetValue = :targetValue, targetCount = :targetCount, " +
            "targetPeriod = :targetPeriod, cadence = :cadence, active = :active, " +
            "archived = :archived, sortOrder = :sortOrder, updatedAt = :updatedAt " +
            "WHERE activityId = :activityId",
    )
    suspend fun updateUserActivityConfig(
        activityId: String,
        activityType: String,
        targetValue: Int?,
        targetCount: Int?,
        targetPeriod: String?,
        cadence: String?,
        active: Boolean,
        archived: Boolean,
        sortOrder: Int,
        updatedAt: Long,
    )

    @Query("DELETE FROM user_activity_configs WHERE activityId = :activityId")
    suspend fun deleteUserActivityConfig(activityId: String)

    @Query(
        "UPDATE user_activity_configs SET active = :active, archived = :archived, " +
            "updatedAt = :updatedAt WHERE activityId = :activityId",
    )
    suspend fun toggleUserActivityConfigActive(
        activityId: String,
        active: Boolean,
        archived: Boolean,
        updatedAt: Long,
    )

    @Query("DELETE FROM user_activity_configs")
    suspend fun clearAllUserActivityConfigs()

    @Query("DELETE FROM activity_definitions")
    suspend fun clearAllActivityDefinitions()

    @Query("DELETE FROM sleep_logs")
    suspend fun clearAllSleepLogs()
}

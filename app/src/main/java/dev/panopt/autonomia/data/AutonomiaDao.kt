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

    @Query("SELECT * FROM layers ORDER BY sortOrder")
    suspend fun getLayersSnapshot(): List<LayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLayers(layers: List<LayerEntity>)

    @Query("SELECT * FROM daily_activity_logs WHERE date = :date")
    fun observeActivityLogsForDate(date: String): Flow<List<DailyActivityLogEntity>>

    @Query("SELECT * FROM daily_activity_logs")
    fun observeAllActivityLogs(): Flow<List<DailyActivityLogEntity>>

    @Query("SELECT * FROM daily_activity_logs WHERE date >= :startDate AND date <= :endDate")
    fun observeActivityLogsBetween(startDate: String, endDate: String): Flow<List<DailyActivityLogEntity>>

    @Query("SELECT * FROM daily_activity_logs WHERE date >= :startDate AND date <= :endDate")
    suspend fun getActivityLogsBetween(startDate: String, endDate: String): List<DailyActivityLogEntity>

    @Query("SELECT * FROM daily_activity_logs WHERE date = :date")
    suspend fun getActivityLogsForDate(date: String): List<DailyActivityLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityLog(log: DailyActivityLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivityLogs(logs: List<DailyActivityLogEntity>)

    @Query("DELETE FROM daily_activity_logs WHERE subjectId = :activityId AND date = :date")
    suspend fun deleteActivityLog(activityId: String, date: String)

    @Query("DELETE FROM daily_activity_logs WHERE subjectId = :activityId")
    suspend fun deleteActivityLogsForActivity(activityId: String)

    @Query("SELECT * FROM abstinence_tracks ORDER BY sortOrder")
    fun observeAbstinenceTracks(): Flow<List<AbstinenceTrackEntity>>

    @Query("SELECT * FROM abstinence_tracks ORDER BY sortOrder")
    suspend fun getAbstinenceTracksSnapshot(): List<AbstinenceTrackEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertAbstinenceTracks(tracks: List<AbstinenceTrackEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceTrack(track: AbstinenceTrackEntity)

    @Query("SELECT * FROM abstinence_tracks WHERE id = :trackId LIMIT 1")
    suspend fun getAbstinenceTrack(trackId: String): AbstinenceTrackEntity?

    @Query("UPDATE abstinence_tracks SET active = :active, updatedAt = :updatedAt WHERE id = :trackId")
    suspend fun setAbstinenceTrackActive(trackId: String, active: Boolean, updatedAt: Long)

    @Query("DELETE FROM abstinence_tracks WHERE id = :trackId")
    suspend fun deleteAbstinenceTrack(trackId: String)

    @Query("SELECT * FROM abstinence_logs WHERE date = :date")
    fun observeAbstinenceLogsForDate(date: String): Flow<List<AbstinenceLogEntity>>

    @Query("SELECT * FROM abstinence_logs")
    fun observeAllAbstinenceLogs(): Flow<List<AbstinenceLogEntity>>

    @Query("SELECT * FROM abstinence_logs")
    suspend fun getAllAbstinenceLogsSnapshot(): List<AbstinenceLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceLog(log: AbstinenceLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceLogs(logs: List<AbstinenceLogEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceRelapseEvents(events: List<AbstinenceRelapseEventEntity>)

    @Query("SELECT * FROM abstinence_relapse_events ORDER BY startDate DESC")
    suspend fun getAbstinenceRelapseEventsSnapshot(): List<AbstinenceRelapseEventEntity>

    @Query("DELETE FROM abstinence_logs WHERE trackId = :trackId AND date = :date")
    suspend fun deleteAbstinenceLog(trackId: String, date: String)

    @Query("DELETE FROM abstinence_logs WHERE trackId = :trackId")
    suspend fun deleteAbstinenceLogsForTrack(trackId: String)

    @Query("SELECT * FROM risk_events WHERE date = :date")
    fun observeRiskEventsForDate(date: String): Flow<List<RiskEventEntity>>

    @Query("SELECT * FROM risk_events ORDER BY createdAt DESC")
    fun observeRiskEvents(): Flow<List<RiskEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRiskEvent(event: RiskEventEntity)
    
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    suspend fun getTasksSnapshot(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTask(taskId: String): TaskEntity?
    
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

    @Query("SELECT * FROM anchor_phrase_impressions WHERE date BETWEEN :start AND :end")
    suspend fun getAnchorPhraseImpressionsBetween(start: String, end: String): List<AnchorPhraseImpressionEntity>

    @Query("SELECT * FROM anchor_phrase_daily_slots WHERE date = :date")
    fun observeAnchorPhraseDailySlots(date: String): Flow<List<AnchorPhraseDailySlotEntity>>

    // --- Sleep (v12+: sleep_nights + sleep_segments) ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleepNight(night: SleepNightEntity)

    @Query("SELECT * FROM sleep_nights WHERE nightDate = :date LIMIT 1")
    suspend fun getSleepNight(date: String): SleepNightEntity?

    @Query("SELECT * FROM sleep_nights WHERE nightDate BETWEEN :from AND :to ORDER BY nightDate")
    suspend fun getSleepNightsInRange(from: String, to: String): List<SleepNightEntity>

    @Query("SELECT * FROM sleep_nights WHERE nightDate = :date LIMIT 1")
    fun observeSleepNightForDate(date: String): Flow<SleepNightEntity?>

    @Query("SELECT * FROM sleep_segments WHERE nightDate = :date ORDER BY startAt")
    suspend fun getSleepSegments(date: String): List<SleepSegmentEntity>

    @Query("DELETE FROM sleep_segments WHERE nightDate = :date")
    suspend fun deleteSleepSegmentsForNight(date: String)

    @Insert
    suspend fun insertSleepSegments(segments: List<SleepSegmentEntity>)

    @Query("SELECT * FROM sleep_config WHERE id = :id LIMIT 1")
    fun observeSleepConfig(id: String): Flow<SleepConfigEntity?>

    @Query("SELECT * FROM sleep_config WHERE id = :id LIMIT 1")
    suspend fun getSleepConfig(id: String): SleepConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleepConfig(config: SleepConfigEntity)

    @Query("SELECT * FROM sleep_session_state WHERE id = :id LIMIT 1")
    fun observeSleepSessionState(id: String): Flow<SleepSessionStateEntity?>

    @Query("SELECT * FROM sleep_session_state WHERE id = :id LIMIT 1")
    suspend fun getSleepSessionState(id: String): SleepSessionStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSleepSessionState(state: SleepSessionStateEntity)

    @Query("DELETE FROM sleep_session_state WHERE id = :id")
    suspend fun deleteSleepSessionState(id: String)

    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :updatedAt WHERE id = :taskId")
    suspend fun updateTaskStatus(taskId: String, status: String, completedAt: Long?, updatedAt: Long)

    @Query("DELETE FROM daily_activity_logs")
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

    @Query("DELETE FROM activity_definitions WHERE id = :activityId")
    suspend fun deleteActivityDefinition(activityId: String)

    // --- New queries for user_activity_configs ---

    @Query("SELECT * FROM user_activity_configs WHERE active = 1 AND archived = 0")
    fun observeUserActivityConfigs(): Flow<List<UserActivityConfigEntity>>

    @Query("SELECT * FROM user_activity_configs WHERE active = 1 AND archived = 0")
    suspend fun getActiveUserActivityConfigs(): List<UserActivityConfigEntity>

    @Query("SELECT * FROM activity_definitions")
    suspend fun getActivityDefinitionsSnapshot(): List<ActivityDefinitionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserActivityConfig(config: UserActivityConfigEntity)

    @Query(
        "UPDATE user_activity_configs SET activityType = :activityType, " +
            "targetValue = :targetValue, targetCount = :targetCount, " +
            "targetPeriod = :targetPeriod, cadence = :cadence, " +
            "weeklyFrequencyTarget = :weeklyFrequencyTarget, " +
            "sessionTargetMinutes = :sessionTargetMinutes, " +
            "commitmentDurationMonths = :commitmentDurationMonths, active = :active, " +
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
        weeklyFrequencyTarget: Int?,
        sessionTargetMinutes: Int?,
        commitmentDurationMonths: Int?,
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

    @Query("DELETE FROM sleep_nights")
    suspend fun clearAllSleepNights()

    // --- Single-entity fetch queries (suspend, non-Flow) ---

    @Query("SELECT * FROM activity_definitions WHERE id = :activityId LIMIT 1")
    suspend fun getActivityDefinition(activityId: String): ActivityDefinitionEntity?

    @Query("SELECT * FROM layers WHERE id = :layerId LIMIT 1")
    suspend fun getLayer(layerId: String): LayerEntity?

    @Query("SELECT * FROM user_activity_configs WHERE activityId = :activityId LIMIT 1")
    suspend fun getUserActivityConfig(activityId: String): UserActivityConfigEntity?

    @Query("SELECT * FROM daily_closures WHERE date = :date LIMIT 1")
    suspend fun getDailyClosure(date: String): DailyClosureEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyClosure(closure: DailyClosureEntity)

    @Query("SELECT * FROM weekly_score_snapshots ORDER BY weekStart DESC")
    fun observeWeeklyScoreSnapshots(): Flow<List<WeeklyScoreSnapshotEntity>>

    @Query("SELECT * FROM weekly_score_snapshots ORDER BY weekStart DESC")
    suspend fun getWeeklyScoreSnapshotsSnapshot(): List<WeeklyScoreSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyScoreSnapshot(snapshot: WeeklyScoreSnapshotEntity)

    @Query("DELETE FROM weekly_score_snapshots WHERE weekStart = :weekStart AND scoringVersion = :scoringVersion")
    suspend fun deleteWeeklyScoreSnapshot(weekStart: String, scoringVersion: String)

    // -- Device telemetry (raw device activity facts) --

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceActivityEvents(events: List<DeviceActivityEventEntity>)

    @Query("SELECT * FROM device_activity_events WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp ASC")
    suspend fun getDeviceActivityEventsInRange(from: Long, to: Long): List<DeviceActivityEventEntity>

    @Query("SELECT MAX(timestamp) FROM device_activity_events")
    suspend fun latestDeviceActivityEventTimestamp(): Long?

    @Query("DELETE FROM device_activity_events WHERE timestamp < :threshold")
    suspend fun deleteDeviceActivityEventsOlderThan(threshold: Long)

    // -- Telemetry collection leases (opt-in gating; opaque consumer keys) --

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTelemetryLease(lease: TelemetryCollectionLeaseEntity)

    @Query("DELETE FROM telemetry_collection_lease WHERE consumerKey = :consumerKey")
    suspend fun deleteTelemetryLease(consumerKey: String)

    @Query("SELECT COUNT(*) FROM telemetry_collection_lease")
    suspend fun countActiveTelemetryLeases(): Int
}

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

    @Query("SELECT * FROM activities ORDER BY sortOrder")
    fun observeActivities(): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActivities(activities: List<ActivityEntity>)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceTracks(tracks: List<AbstinenceTrackEntity>)

    @Query("SELECT * FROM abstinence_logs WHERE date = :date")
    fun observeAbstinenceLogsForDate(date: String): Flow<List<AbstinenceLogEntity>>

    @Query("SELECT * FROM abstinence_logs")
    fun observeAllAbstinenceLogs(): Flow<List<AbstinenceLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAbstinenceLog(log: AbstinenceLogEntity)

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
}

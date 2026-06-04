package dev.panopt.autonomia.ui.dashboard

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.AutonomiaRepository
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.RiskEvent
import dev.panopt.autonomia.SleepConfig
import dev.panopt.autonomia.SleepNight
import dev.panopt.autonomia.SleepSessionState
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.data.ActivityDefinitionEntity
import dev.panopt.autonomia.data.AnchorPhraseDailySlotEntity
import dev.panopt.autonomia.data.UserActivityConfigEntity
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import dev.panopt.autonomia.domain.scoring.WeeklyScoreHistoryEntry
import dev.panopt.autonomia.domain.sleep.SleepAutoModeResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Seam testeable entre [dev.panopt.autonomia.ui.dashboard.DashboardViewModel] y la
 * persistencia. Declara SOLO lo que el dashboard consume, de modo que el ViewModel
 * pueda fakearse en tests JVM puros sin Context ni Room.
 *
 * El bug que motiva esta extracción (la fecha "de hoy" quedaba congelada al construir
 * el ViewModel y rompía la racha de sobriedad al cruzar medianoche) vive en el wiring
 * reactivo del VM, una capa que los tests de dominio puro no podían cubrir. Esta interfaz
 * hace ese seam observable desde un test.
 */
internal interface DashboardRepository {
    // --- Flows (lectura reactiva de hechos) ---
    fun observeConfiguredActivities(): Flow<List<ActivityDefinition>>
    fun observeCatalogActivities(): Flow<List<ActivityDefinition>>
    fun activityLogsForDateFlow(date: String): Flow<List<ActivityLog>>
    fun activityLogsBetweenFlow(startDate: String, endDate: String): Flow<List<ActivityLog>>
    fun sleepNightForDateFlow(date: String): Flow<SleepNight?>
    fun sleepConfigFlow(): Flow<SleepConfig>
    fun sleepSessionStateFlow(): Flow<SleepSessionState?>
    fun isDarkModeFlow(): StateFlow<Boolean>
    fun isSleepAutoModeEnabledFlow(): StateFlow<Boolean>
    fun layersFlow(): Flow<List<Layer>>
    fun abstinenceTracksFlow(): Flow<List<AbstinenceTrack>>
    fun abstinenceLogsForDateFlow(date: String): Flow<List<AbstinenceLog>>
    fun allAbstinenceLogsFlow(): Flow<List<AbstinenceLog>>
    fun riskEventsForDateFlow(date: String): Flow<List<RiskEvent>>
    fun tasksFlow(): Flow<List<Task>>
    fun anchorPhrasesFlow(): Flow<List<AnchorPhrase>>
    fun anchorPhraseSlotFlow(dateKey: String): Flow<List<AnchorPhraseDailySlotEntity>>
    fun focusSignalActivityIdFlow(): StateFlow<String?>
    fun weeklyScoreHistoryFlow(): Flow<List<WeeklyScoreHistoryEntry>>

    // --- Mantenimiento diario (corre en init y al volver del background) ---
    suspend fun ensureSeeded()
    suspend fun materializeAssumedAbstinenceRelapses(
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    )
    suspend fun closeElapsedActivityDays(
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
        source: String = "app_open",
    )
    suspend fun materializeSleepNight(
        nightDate: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Boolean
    suspend fun refreshCurrentWeeklyScoreSnapshot(today: LocalDate)
    suspend fun closeElapsedWeeklyScoreSnapshots(today: LocalDate)
    suspend fun resolveAnchorPhraseForToday(
        today: LocalDate,
        now: java.time.LocalDateTime,
    )

    // --- Acciones ---
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun setActivityCompleted(activity: ActivityDefinition, completed: Boolean, date: String)
    suspend fun setActivityValue(activity: ActivityDefinition, actualValue: Int, date: String)
    suspend fun deleteCustomActivity(activityId: String)
    suspend fun markAbstinenceClean(trackId: String, date: String)
    suspend fun clearAbstinenceLog(trackId: String, date: String)
    suspend fun markAbstinenceRelapse(trackId: String, date: String)
    suspend fun setAbstinenceTrackActive(trackId: String, active: Boolean)
    suspend fun createCustomAbstinenceTrack(name: String)
    suspend fun deleteCustomAbstinenceTrack(trackId: String)
    suspend fun startSleepSession(): Boolean
    suspend fun finishSleepSession(note: String): Boolean
    suspend fun saveSleepConfig(
        targetSleepAt: String,
        targetWakeAt: String,
        digitalWindDownMinutes: Int,
    ): Boolean
    suspend fun toggleSleepAutoMode(enabled: Boolean): SleepAutoModeResult
    suspend fun setFocusSignalActivity(activityId: String?)
    suspend fun upsertActivityDefinition(definition: ActivityDefinitionEntity)
    suspend fun upsertUserActivityConfig(config: UserActivityConfigEntity)
    suspend fun createTask(title: String, layerId: String?)
    suspend fun completeTask(taskId: String)
    suspend fun reactivateTask(taskId: String)
    suspend fun addActivityAsAnchor(
        activityId: String,
        sessionTargetMinutes: Int,
        weeklyFrequencyTarget: Int,
        commitmentDurationMonths: Int?,
    )
    suspend fun removeActivityAsAnchor(activityId: String)
    suspend fun addSupport(activityId: String)
    suspend fun removeSupport(activityId: String)
}

/**
 * Adaptador de producción: reenvía cada llamada al [AutonomiaRepository] real.
 * Mantiene el repositorio intacto (no lo obliga a implementar la interfaz), por lo que
 * el seam no agrega riesgo sobre las ~1000 líneas existentes.
 */
internal class AutonomiaDashboardRepository(
    private val delegate: AutonomiaRepository,
) : DashboardRepository {
    override fun observeConfiguredActivities() = delegate.observeConfiguredActivities()
    override fun observeCatalogActivities() = delegate.observeCatalogActivities()
    override fun activityLogsForDateFlow(date: String) = delegate.activityLogsForDateFlow(date)
    override fun activityLogsBetweenFlow(startDate: String, endDate: String) =
        delegate.activityLogsBetweenFlow(startDate, endDate)
    override fun sleepNightForDateFlow(date: String) = delegate.sleepNightForDateFlow(date)
    override fun sleepConfigFlow() = delegate.sleepConfigFlow()
    override fun sleepSessionStateFlow() = delegate.sleepSessionStateFlow()
    override fun isDarkModeFlow() = delegate.isDarkModeFlow()
    override fun isSleepAutoModeEnabledFlow() = delegate.isSleepAutoModeEnabledFlow()
    override fun layersFlow() = delegate.layersFlow()
    override fun abstinenceTracksFlow() = delegate.abstinenceTracksFlow()
    override fun abstinenceLogsForDateFlow(date: String) = delegate.abstinenceLogsForDateFlow(date)
    override fun allAbstinenceLogsFlow() = delegate.allAbstinenceLogsFlow()
    override fun riskEventsForDateFlow(date: String) = delegate.riskEventsForDateFlow(date)
    override fun tasksFlow() = delegate.tasksFlow()
    override fun anchorPhrasesFlow() = delegate.anchorPhrasesFlow()
    override fun anchorPhraseSlotFlow(dateKey: String) = delegate.observeAnchorPhraseDailySlots(dateKey)
    override fun focusSignalActivityIdFlow() = delegate.focusSignalActivityIdFlow()
    override fun weeklyScoreHistoryFlow() = delegate.weeklyScoreHistoryFlow()

    override suspend fun ensureSeeded() = delegate.ensureSeeded()
    override suspend fun materializeAssumedAbstinenceRelapses(today: LocalDate, zoneId: ZoneId) =
        delegate.materializeAssumedAbstinenceRelapses(today, zoneId)
    override suspend fun closeElapsedActivityDays(today: LocalDate, zoneId: ZoneId, source: String) =
        delegate.closeElapsedActivityDays(today, zoneId, source)
    override suspend fun materializeSleepNight(nightDate: LocalDate, zoneId: ZoneId) =
        delegate.materializeSleepNight(nightDate, zoneId)
    override suspend fun refreshCurrentWeeklyScoreSnapshot(today: LocalDate) =
        delegate.refreshCurrentWeeklyScoreSnapshot(today)
    override suspend fun closeElapsedWeeklyScoreSnapshots(today: LocalDate) =
        delegate.closeElapsedWeeklyScoreSnapshots(today)
    override suspend fun resolveAnchorPhraseForToday(
        today: LocalDate,
        now: java.time.LocalDateTime,
    ) = delegate.resolveAnchorPhraseForToday(today, now)

    override suspend fun setDarkMode(enabled: Boolean) = delegate.setDarkMode(enabled)
    override suspend fun setActivityCompleted(activity: ActivityDefinition, completed: Boolean, date: String) =
        delegate.setActivityCompleted(activity, completed, date)
    override suspend fun setActivityValue(activity: ActivityDefinition, actualValue: Int, date: String) =
        delegate.setActivityValue(activity, actualValue, date)
    override suspend fun deleteCustomActivity(activityId: String) = delegate.deleteCustomActivity(activityId)
    override suspend fun markAbstinenceClean(trackId: String, date: String) =
        delegate.markAbstinenceClean(trackId, date)
    override suspend fun clearAbstinenceLog(trackId: String, date: String) =
        delegate.clearAbstinenceLog(trackId, date)
    override suspend fun markAbstinenceRelapse(trackId: String, date: String) =
        delegate.markAbstinenceRelapse(trackId, date)
    override suspend fun setAbstinenceTrackActive(trackId: String, active: Boolean) =
        delegate.setAbstinenceTrackActive(trackId, active)
    override suspend fun createCustomAbstinenceTrack(name: String) =
        delegate.createCustomAbstinenceTrack(name)
    override suspend fun deleteCustomAbstinenceTrack(trackId: String) =
        delegate.deleteCustomAbstinenceTrack(trackId)
    override suspend fun startSleepSession() = delegate.startSleepSession()
    override suspend fun finishSleepSession(note: String) = delegate.finishSleepSession(note)
    override suspend fun saveSleepConfig(
        targetSleepAt: String,
        targetWakeAt: String,
        digitalWindDownMinutes: Int,
    ) = delegate.saveSleepConfig(targetSleepAt, targetWakeAt, digitalWindDownMinutes)
    override suspend fun toggleSleepAutoMode(enabled: Boolean) = delegate.toggleSleepAutoMode(enabled)
    override suspend fun setFocusSignalActivity(activityId: String?) =
        delegate.setFocusSignalActivity(activityId)
    override suspend fun upsertActivityDefinition(definition: ActivityDefinitionEntity) =
        delegate.upsertActivityDefinition(definition)
    override suspend fun upsertUserActivityConfig(config: UserActivityConfigEntity) =
        delegate.upsertUserActivityConfig(config)
    override suspend fun createTask(title: String, layerId: String?) = delegate.createTask(title, layerId)
    override suspend fun completeTask(taskId: String) = delegate.completeTask(taskId)
    override suspend fun reactivateTask(taskId: String) = delegate.reactivateTask(taskId)
    override suspend fun addActivityAsAnchor(
        activityId: String,
        sessionTargetMinutes: Int,
        weeklyFrequencyTarget: Int,
        commitmentDurationMonths: Int?,
    ) = delegate.addActivityAsAnchor(activityId, sessionTargetMinutes, weeklyFrequencyTarget, commitmentDurationMonths)
    override suspend fun removeActivityAsAnchor(activityId: String) = delegate.removeActivityAsAnchor(activityId)
    override suspend fun addSupport(activityId: String) = delegate.addSupport(activityId)
    override suspend fun removeSupport(activityId: String) = delegate.removeSupport(activityId)
}

package dev.panopt.autonomia.ui.dashboard

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceSeverity
import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.AnchorPhrase
import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
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
import dev.panopt.autonomia.domain.sleep.SleepPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

/**
 * Fake en memoria del [DashboardRepository] para tests JVM puros del
 * [DashboardViewModel]. Implementa con comportamiento real solo el camino de sobriedad
 * (un track `trk_alcohol` + sus logs); el resto devuelve vacíos / no-ops para aislar el
 * wiring del VM. La materialización de recaídas asumidas es un no-op acá: se prueba aparte
 * como dominio puro en `AbstinenceRelapseMaterializationPolicyTest`.
 */
internal class FakeDashboardRepository(
    severity: AbstinenceSeverity = AbstinenceSeverity.Critical,
) : DashboardRepository {
    private val tracks = MutableStateFlow(
        listOf(
            AbstinenceTrack(
                id = "trk_alcohol",
                name = "Alcohol",
                substanceLabel = "Alcohol",
                severity = severity,
                contributionRole = ContributionRole.Protective,
                importanceTier = ImportanceTier.Critical,
                active = true,
                sortOrder = 10,
            ),
        ),
    )
    private val logs = MutableStateFlow<List<AbstinenceLog>>(emptyList())

    private fun upsert(trackId: String, date: String, status: AbstinenceStatus) {
        logs.value = logs.value.filterNot { it.trackId == trackId && it.date == date } +
            AbstinenceLog(trackId = trackId, date = date, status = status, updatedAt = 0L)
    }

    override fun abstinenceTracksFlow(): Flow<List<AbstinenceTrack>> = tracks
    override fun allAbstinenceLogsFlow(): Flow<List<AbstinenceLog>> = logs
    override fun abstinenceLogsForDateFlow(date: String): Flow<List<AbstinenceLog>> =
        logs.map { all -> all.filter { it.date == date } }

    override suspend fun markAbstinenceClean(trackId: String, date: String) =
        upsert(trackId, date, AbstinenceStatus.Clean)
    override suspend fun markAbstinenceRelapse(trackId: String, date: String) =
        upsert(trackId, date, AbstinenceStatus.Relapse)
    override suspend fun clearAbstinenceLog(trackId: String, date: String) {
        logs.value = logs.value.filterNot { it.trackId == trackId && it.date == date }
    }

    // --- Resto: vacíos / no-ops ---
    override fun observeConfiguredActivities(): Flow<List<ActivityDefinition>> = flowOf(emptyList())
    override fun observeCatalogActivities(): Flow<List<ActivityDefinition>> = flowOf(emptyList())
    override fun activityLogsForDateFlow(date: String): Flow<List<ActivityLog>> = flowOf(emptyList())
    override fun activityLogsBetweenFlow(startDate: String, endDate: String): Flow<List<ActivityLog>> =
        flowOf(emptyList())
    override fun sleepNightForDateFlow(date: String): Flow<SleepNight?> = flowOf(null)
    override fun sleepConfigFlow(): Flow<SleepConfig> = flowOf(SleepPolicy.defaultConfig())
    override fun sleepSessionStateFlow(): Flow<SleepSessionState?> = flowOf(null)
    override fun isDarkModeFlow(): StateFlow<Boolean> = MutableStateFlow(false)
    override fun isSleepAutoModeEnabledFlow(): StateFlow<Boolean> = MutableStateFlow(false)
    override fun layersFlow(): Flow<List<Layer>> = flowOf(
        listOf(
            Layer("layer_interior", "Interior", "", 10),
            Layer("layer_cuerpo", "Cuerpo", "", 20),
            Layer("layer_conducta", "Conducta", "", 30),
            Layer("layer_vinculos", "Vinculos", "", 40),
            Layer("layer_proyecto", "Proyecto", "", 50),
        ),
    )
    override fun riskEventsForDateFlow(date: String): Flow<List<RiskEvent>> = flowOf(emptyList())
    override fun tasksFlow(): Flow<List<Task>> = flowOf(emptyList())
    override fun anchorPhrasesFlow(): Flow<List<AnchorPhrase>> = flowOf(emptyList())
    override fun anchorPhraseSlotFlow(dateKey: String): Flow<List<AnchorPhraseDailySlotEntity>> = flowOf(emptyList())
    override fun focusSignalActivityIdFlow(): StateFlow<String?> = MutableStateFlow(null)
    override fun weeklyScoreHistoryFlow(): Flow<List<WeeklyScoreHistoryEntry>> = flowOf(emptyList())

    override suspend fun ensureSeeded() = Unit
    override suspend fun materializeAssumedAbstinenceRelapses(today: LocalDate, zoneId: ZoneId) = Unit
    override suspend fun closeElapsedActivityDays(today: LocalDate, zoneId: ZoneId, source: String) = Unit
    override suspend fun materializeSleepNight(nightDate: LocalDate, zoneId: ZoneId): Boolean = true
    override suspend fun refreshCurrentWeeklyScoreSnapshot(today: LocalDate) = Unit
    override suspend fun closeElapsedWeeklyScoreSnapshots(today: LocalDate) = Unit
    override suspend fun resolveAnchorPhraseForToday(today: LocalDate, now: java.time.LocalDateTime) = Unit
    override suspend fun setDarkMode(enabled: Boolean) = Unit
    override suspend fun setActivityCompleted(activity: ActivityDefinition, completed: Boolean, date: String) = Unit
    override suspend fun setActivityValue(activity: ActivityDefinition, actualValue: Int, date: String) = Unit
    override suspend fun deleteCustomActivity(activityId: String) = Unit
    override suspend fun setAbstinenceTrackActive(trackId: String, active: Boolean) = Unit
    override suspend fun createCustomAbstinenceTrack(name: String) = Unit
    override suspend fun deleteCustomAbstinenceTrack(trackId: String) = Unit
    override suspend fun startSleepSession(): Boolean = false
    override suspend fun finishSleepSession(note: String): Boolean = false
    override suspend fun saveSleepConfig(
        targetSleepAt: String,
        targetWakeAt: String,
        digitalWindDownMinutes: Int,
    ): Boolean = true
    override suspend fun toggleSleepAutoMode(enabled: Boolean): SleepAutoModeResult =
        SleepAutoModeResult.Success(enabled)
    override suspend fun setFocusSignalActivity(activityId: String?) = Unit
    override suspend fun upsertActivityDefinition(definition: ActivityDefinitionEntity) = Unit
    override suspend fun upsertUserActivityConfig(config: UserActivityConfigEntity) = Unit
    override suspend fun createTask(title: String, layerId: String?) = Unit
    override suspend fun completeTask(taskId: String) = Unit
    override suspend fun reactivateTask(taskId: String) = Unit
    override suspend fun addActivityAsAnchor(
        activityId: String,
        sessionTargetMinutes: Int,
        weeklyFrequencyTarget: Int,
        commitmentDurationMonths: Int?,
    ) = Unit
    override suspend fun removeActivityAsAnchor(activityId: String) = Unit
    override suspend fun addSupport(activityId: String) = Unit
    override suspend fun removeSupport(activityId: String) = Unit
}

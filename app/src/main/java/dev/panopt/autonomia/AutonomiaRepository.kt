package dev.panopt.autonomia

import android.content.Context
import dev.panopt.autonomia.data.AbstinenceLogEntity
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityEntity
import dev.panopt.autonomia.data.ActivityLogEntity
import dev.panopt.autonomia.data.AutonomiaDatabase
import dev.panopt.autonomia.data.LayerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class AutonomiaRepository(context: Context) {
    private val prefs = context.getSharedPreferences("autonomia_prefs", Context.MODE_PRIVATE)
    private val dao = AutonomiaDatabase.getInstance(context).autonomiaDao()

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))

    fun isDarkModeFlow(): StateFlow<Boolean> = _isDarkMode.asStateFlow()

    suspend fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("dark_mode", enabled).apply()
        _isDarkMode.value = enabled
    }

    fun allActivityLogsFlow(): Flow<List<ActivityLog>> =
        dao.observeAllActivityLogs().map { logs -> logs.map { it.toDomain() } }

    fun allAbstinenceLogsFlow(): Flow<List<AbstinenceLog>> =
        dao.observeAllAbstinenceLogs().map { logs -> logs.map { it.toDomain() } }

    suspend fun ensureSeeded() {
        if (dao.layerCount() > 0) return

        dao.upsertLayers(DefaultLayers)
        dao.upsertActivities(DefaultActivities)
        dao.upsertAbstinenceTracks(DefaultAbstinenceTracks)
    }

    suspend fun markAbstinenceClean(track: AbstinenceTrack, date: String = todayKey()) {
        dao.upsertAbstinenceLog(
            AbstinenceLogEntity(
                trackId = track.id,
                date = date,
                status = AbstinenceStatus.Clean.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun markAbstinenceRelapse(track: AbstinenceTrack, date: String = todayKey()) {
        dao.upsertAbstinenceLog(
            AbstinenceLogEntity(
                trackId = track.id,
                date = date,
                status = AbstinenceStatus.Relapse.name,
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}

private fun LayerEntity.toDomain(): Layer =
    Layer(id = id, name = name, description = description, sortOrder = sortOrder, active = active)

private fun ActivityEntity.toDomain(): TrackedActivity =
    TrackedActivity(
        id = id,
        layerId = layerId,
        name = name,
        description = description,
        type = runCatching { ActivityType.valueOf(type) }.getOrDefault(ActivityType.Check),
        role = runCatching { ActivityRole.valueOf(role) }.getOrDefault(ActivityRole.Practice),
        displaySurface = runCatching { DisplaySurface.valueOf(displaySurface) }.getOrDefault(DisplaySurface.PrimaryChecklist),
        contributionRole = runCatching { ContributionRole.valueOf(contributionRole) }.getOrDefault(ContributionRole.Core),
        importanceTier = runCatching { ImportanceTier.valueOf(importanceTier) }.getOrDefault(ImportanceTier.Medium),
        cadence = cadence?.let { runCatching { ActivityCadence.valueOf(it) }.getOrNull() },
        targetValue = targetValue,
        minimumValue = minimumValue,
        targetCount = targetCount,
        targetPeriod = targetPeriod?.let { runCatching { TargetPeriod.valueOf(it) }.getOrNull() },
        unit = runCatching { ActivityUnit.valueOf(unit) }.getOrDefault(ActivityUnit.Boolean),
        active = active,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun ActivityLogEntity.toDomain(): ActivityLog =
    ActivityLog(
        activityId = activityId,
        date = date,
        completed = completed,
        actualValue = actualValue,
        note = note,
        updatedAt = updatedAt,
    )

private fun AbstinenceTrackEntity.toDomain(): AbstinenceTrack =
    AbstinenceTrack(
        id = id,
        name = name,
        substanceLabel = substanceLabel,
        severity = runCatching { AbstinenceSeverity.valueOf(severity) }.getOrDefault(AbstinenceSeverity.Moderate),
        contributionRole = runCatching { ContributionRole.valueOf(contributionRole) }.getOrDefault(ContributionRole.Protective),
        importanceTier = runCatching { ImportanceTier.valueOf(importanceTier) }.getOrDefault(ImportanceTier.Medium),
        active = active,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun AbstinenceLogEntity.toDomain(): AbstinenceLog =
    AbstinenceLog(
        trackId = trackId,
        date = date,
        status = runCatching { AbstinenceStatus.valueOf(status) }.getOrDefault(AbstinenceStatus.Unknown),
        urge = urge,
        urgeIntensity = urgeIntensity,
        note = note,
        updatedAt = updatedAt,
    )

private val DefaultLayers = listOf(
    LayerEntity("layer_interior", "Interior", "Mundo interno: conciencia, aprendizaje, reflexion.", 10),
    LayerEntity("layer_cuerpo", "Cuerpo", "Base fisica: movimiento, descanso, alimentacion e higiene.", 20),
    LayerEntity("layer_conducta", "Conducta", "Autocontrol, limites y estructura diaria.", 30),
    LayerEntity("layer_vinculos", "Vinculos", "Contacto humano y relaciones importantes.", 40),
    LayerEntity("layer_proyecto", "Proyecto", "Futuro, identidad, trabajo y creacion.", 50),
)

private val DefaultActivities = listOf(
    ActivityEntity("act_meditar", "layer_interior", "Meditar", "", ActivityType.Time.name, ActivityRole.Practice.name, DisplaySurface.PrimaryChecklist.name, ContributionRole.Core.name, ImportanceTier.High.name, null, 5, null, null, null, ActivityUnit.Minutes.name, sortOrder = 10, createdAt = 0L, updatedAt = 0L),
    ActivityEntity("act_ejercicio", "layer_cuerpo", "Ejercicio", "", ActivityType.Time.name, ActivityRole.Practice.name, DisplaySurface.PrimaryChecklist.name, ContributionRole.Core.name, ImportanceTier.High.name, null, 40, null, null, null, ActivityUnit.Minutes.name, sortOrder = 20, createdAt = 0L, updatedAt = 0L),
    ActivityEntity("act_digitaliza", "layer_proyecto", "Proyecto Digitaliza", "", ActivityType.Time.name, ActivityRole.ProjectWork.name, DisplaySurface.PrimaryChecklist.name, ContributionRole.Core.name, ImportanceTier.Critical.name, null, 360, null, null, null, ActivityUnit.Minutes.name, sortOrder = 30, createdAt = 0L, updatedAt = 0L),
    ActivityEntity("act_musica", "layer_proyecto", "Proyecto musical / Anatomia de la ausencia", "", ActivityType.Time.name, ActivityRole.ProjectWork.name, DisplaySurface.PrimaryChecklist.name, ContributionRole.Core.name, ImportanceTier.High.name, null, 180, null, null, null, ActivityUnit.Minutes.name, sortOrder = 40, createdAt = 0L, updatedAt = 0L),
    ActivityEntity("act_dientes", "layer_cuerpo", "Cepillarse los dientes", "", ActivityType.Count.name, ActivityRole.SelfCare.name, DisplaySurface.SecondaryChecklist.name, ContributionRole.Support.name, ImportanceTier.Medium.name, null, 2, null, null, null, ActivityUnit.Count.name, sortOrder = 50, createdAt = 0L, updatedAt = 0L),
    ActivityEntity("act_banarse", "layer_cuerpo", "Banarse", "", ActivityType.Check.name, ActivityRole.SelfCare.name, DisplaySurface.SecondaryChecklist.name, ContributionRole.Support.name, ImportanceTier.Medium.name, null, null, null, null, null, ActivityUnit.Boolean.name, sortOrder = 60, createdAt = 0L, updatedAt = 0L),
    ActivityEntity("act_cocinar", "layer_cuerpo", "Cocinar en casa", "", ActivityType.Check.name, ActivityRole.DomesticOrder.name, DisplaySurface.SecondaryChecklist.name, ContributionRole.Support.name, ImportanceTier.Medium.name, null, null, null, null, null, ActivityUnit.Boolean.name, sortOrder = 70, createdAt = 0L, updatedAt = 0L),
    ActivityEntity("act_trastes", "layer_conducta", "Limpiar los trastes", "", ActivityType.Check.name, ActivityRole.DomesticOrder.name, DisplaySurface.SecondaryChecklist.name, ContributionRole.Support.name, ImportanceTier.Low.name, null, null, null, null, null, ActivityUnit.Boolean.name, sortOrder = 80, createdAt = 0L, updatedAt = 0L)
)

private val DefaultAbstinenceTracks = listOf(
    AbstinenceTrackEntity("trk_alcohol", "Alcohol", "alcohol", AbstinenceSeverity.Critical.name, ContributionRole.Protective.name, ImportanceTier.Critical.name, active = true, sortOrder = 10, createdAt = 0L, updatedAt = 0L),
    AbstinenceTrackEntity("trk_sexual", "Conducta sexual / masturbacion", "conducta sexual", AbstinenceSeverity.Critical.name, ContributionRole.Protective.name, ImportanceTier.High.name, active = true, sortOrder = 20, createdAt = 0L, updatedAt = 0L),
    AbstinenceTrackEntity("trk_marihuana", "Marihuana", "marihuana", AbstinenceSeverity.Moderate.name, ContributionRole.Protective.name, ImportanceTier.Medium.name, active = false, sortOrder = 30, createdAt = 0L, updatedAt = 0L),
)

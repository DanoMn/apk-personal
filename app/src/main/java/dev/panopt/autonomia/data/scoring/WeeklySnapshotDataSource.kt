package dev.panopt.autonomia.data.scoring

import dev.panopt.autonomia.data.AbstinenceLogEntity
import dev.panopt.autonomia.data.AbstinenceTrackEntity
import dev.panopt.autonomia.data.ActivityDefinitionEntity
import dev.panopt.autonomia.data.AutonomiaDao
import dev.panopt.autonomia.data.DailyActivityLogEntity
import dev.panopt.autonomia.data.LayerEntity
import dev.panopt.autonomia.data.SleepNightEntity
import dev.panopt.autonomia.data.TaskEntity
import dev.panopt.autonomia.data.UserActivityConfigEntity
import dev.panopt.autonomia.data.WeeklyScoreSnapshotEntity

/**
 * Seam testeable entre [WeeklyScoreSnapshotWriter] y Room. Declara SOLO las consultas que
 * el writer necesita para registrar un snapshot semanal, de modo que el writer pueda
 * probarse en JVM puro con un fake (sin instanciar Room ni un Context).
 *
 * El registro semanal (juntar hechos de la semana → calcular → upsert) vivía sin tests:
 * la matemática (use case / engine) sí estaba cubierta, pero esta orquestación no. Este
 * seam la hace observable desde un test.
 */
interface WeeklySnapshotDataSource {
    suspend fun getLayersSnapshot(): List<LayerEntity>
    suspend fun getActivityLogsBetween(startDate: String, endDate: String): List<DailyActivityLogEntity>
    suspend fun getAbstinenceTracksSnapshot(): List<AbstinenceTrackEntity>
    suspend fun getAllAbstinenceLogsSnapshot(): List<AbstinenceLogEntity>
    suspend fun getTasksSnapshot(): List<TaskEntity>
    suspend fun getSleepNightsInRange(from: String, to: String): List<SleepNightEntity>
    suspend fun getActiveUserActivityConfigs(): List<UserActivityConfigEntity>
    suspend fun getActivityDefinitionsSnapshot(): List<ActivityDefinitionEntity>
    suspend fun getWeeklyScoreSnapshotsSnapshot(): List<WeeklyScoreSnapshotEntity>
    suspend fun upsertWeeklyScoreSnapshot(snapshot: WeeklyScoreSnapshotEntity)
}

/**
 * Adaptador de producción: reenvía cada consulta al [AutonomiaDao] real. Mantiene el DAO
 * intacto (no lo obliga a implementar el seam), por lo que no agrega riesgo de codegen Room.
 */
class DaoWeeklySnapshotDataSource(
    private val dao: AutonomiaDao,
) : WeeklySnapshotDataSource {
    override suspend fun getLayersSnapshot() = dao.getLayersSnapshot()
    override suspend fun getActivityLogsBetween(startDate: String, endDate: String) =
        dao.getActivityLogsBetween(startDate, endDate)
    override suspend fun getAbstinenceTracksSnapshot() = dao.getAbstinenceTracksSnapshot()
    override suspend fun getAllAbstinenceLogsSnapshot() = dao.getAllAbstinenceLogsSnapshot()
    override suspend fun getTasksSnapshot() = dao.getTasksSnapshot()
    override suspend fun getSleepNightsInRange(from: String, to: String) =
        dao.getSleepNightsInRange(from, to)
    override suspend fun getActiveUserActivityConfigs() = dao.getActiveUserActivityConfigs()
    override suspend fun getActivityDefinitionsSnapshot() = dao.getActivityDefinitionsSnapshot()
    override suspend fun getWeeklyScoreSnapshotsSnapshot() = dao.getWeeklyScoreSnapshotsSnapshot()
    override suspend fun upsertWeeklyScoreSnapshot(snapshot: WeeklyScoreSnapshotEntity) =
        dao.upsertWeeklyScoreSnapshot(snapshot)
}

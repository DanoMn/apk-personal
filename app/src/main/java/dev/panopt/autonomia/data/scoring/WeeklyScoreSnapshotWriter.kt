package dev.panopt.autonomia.data.scoring

import dev.panopt.autonomia.data.local.mapper.mergeToDomain
import dev.panopt.autonomia.data.local.mapper.toDomain
import dev.panopt.autonomia.data.local.mapper.toSleepNightScore
import dev.panopt.autonomia.domain.scoring.BuildScoreInputUseCase
import dev.panopt.autonomia.domain.scoring.BuildWeeklyScoreSnapshotUseCase
import dev.panopt.autonomia.domain.scoring.ScoreEngine
import dev.panopt.autonomia.domain.scoring.ScoreInputSource
import dev.panopt.autonomia.domain.scoring.WeeklyScoreSnapshotConstants
import dev.panopt.autonomia.domain.scoring.WeeklyScoreSnapshotInput
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class WeeklyScoreSnapshotWriter(
    private val dataSource: WeeklySnapshotDataSource,
) {
    /** Recalcula el snapshot de la semana en curso (lunes → [today], semana parcial). */
    suspend fun refreshCurrentWeek(today: LocalDate) {
        writeWeek(weekStart = mondayOf(today), weekEnd = today)
    }

    /**
     * Rellena los snapshots de las semanas YA VENCIDAS que faltan, dentro de una ventana
     * de [maxLookbackWeeks]. Análogo semanal del cierre diario: si una semana pasó sin que
     * corriera el worker ni se abriera la app, su snapshot nunca se escribió y quedaba un
     * hueco permanente en el historial (lo que además rompe la estabilidad, que necesita
     * 5 semanas consecutivas).
     *
     * Conservador a propósito: solo rellena semanas con AL MENOS UN HECHO real (log de
     * actividad o de sobriedad). El back-fill usa la configuración actual; snapshotear
     * semanas vacías o previas al uso las puntuaría como "tenías todo configurado y no
     * hiciste nada", fabricando historia falsa. Se procesan de la más vieja a la más nueva
     * para que cada semana vea a las anteriores como memoria.
     */
    suspend fun closeElapsedWeeks(today: LocalDate, maxLookbackWeeks: Long = MAX_BACKFILL_WEEKS) {
        val currentWeekStart = mondayOf(today)
        val existingWeekStarts = dataSource.getWeeklyScoreSnapshotsSnapshot()
            .filter { it.scoringVersion == WeeklyScoreSnapshotConstants.SCORING_VERSION }
            .map { it.weekStart }
            .toSet()

        for (offset in maxLookbackWeeks downTo 1L) {
            val weekStart = currentWeekStart.minusWeeks(offset)
            if (weekStart.toString() in existingWeekStarts) continue
            val weekEnd = weekStart.plusDays(6)
            if (!weekHasFacts(weekStart, weekEnd)) continue
            writeWeek(weekStart = weekStart, weekEnd = weekEnd)
        }
    }

    private suspend fun weekHasFacts(weekStart: LocalDate, weekEnd: LocalDate): Boolean {
        val startKey = weekStart.toString()
        val endKey = weekEnd.toString()
        if (dataSource.getActivityLogsBetween(startKey, endKey).isNotEmpty()) return true
        return dataSource.getAllAbstinenceLogsSnapshot().any { it.date in startKey..endKey }
    }

    private suspend fun writeWeek(weekStart: LocalDate, weekEnd: LocalDate) {
        val weekStartKey = weekStart.toString()
        val weekEndKey = weekEnd.toString()
        val periodActivityLogs = dataSource.getActivityLogsBetween(
            startDate = weekStartKey,
            endDate = weekEndKey,
        ).map { it.toDomain() }
        val allAbstinenceLogs = dataSource.getAllAbstinenceLogsSnapshot().map { it.toDomain() }
        // Solo memoria ANTERIOR a esta semana: para la semana en curso es equivalente al
        // filtro de la policy (que excluye la semana actual), y para el back-fill evita que
        // semanas posteriores ya escritas se filtren como "previas" de una semana vieja.
        val weeklyHistory = dataSource.getWeeklyScoreSnapshotsSnapshot()
            .map { it.toHistoryEntry() }
            .filter { it.weekStart < weekStartKey }
        val source = ScoreInputSource(
            layers = dataSource.getLayersSnapshot().map { it.toDomain() },
            activities = configuredActivitiesSnapshot(),
            todayActivityLogs = periodActivityLogs.filter { it.date == weekEndKey },
            periodActivityLogs = periodActivityLogs,
            abstinenceTracks = dataSource.getAbstinenceTracksSnapshot().map { it.toDomain() },
            todayAbstinenceLogs = allAbstinenceLogs.filter { it.date == weekEndKey },
            allAbstinenceLogs = allAbstinenceLogs,
            tasks = dataSource.getTasksSnapshot().map { it.toDomain() },
            // 5.8: weekly sleep — get all nights in range, map to SleepNightScore,
            // filter out NoData (toSleepNightScore returns null for NoData nights).
            sleepNights = dataSource.getSleepNightsInRange(
                from = weekStartKey,
                to = weekEndKey,
            ).mapNotNull { it.toSleepNightScore() },
            today = weekEnd,
            weeklyHistory = weeklyHistory,
        )
        val scoreInput = BuildScoreInputUseCase(source)
        val scoreReport = ScoreEngine.calculate(scoreInput)
        val snapshot = BuildWeeklyScoreSnapshotUseCase(
            WeeklyScoreSnapshotInput(
                weekStart = weekStart,
                weekEnd = weekEnd,
                calculatedAt = System.currentTimeMillis(),
                scoreInput = scoreInput,
                scoreReport = scoreReport,
            ),
        )
        dataSource.upsertWeeklyScoreSnapshot(snapshot.toEntity())
    }

    private fun mondayOf(date: LocalDate): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    private suspend fun configuredActivitiesSnapshot() =
        dataSource.getActivityDefinitionsSnapshot()
            .associateBy { it.id }
            .let { definitionsById ->
                dataSource.getActiveUserActivityConfigs()
                    .mapNotNull { config ->
                        definitionsById[config.activityId]?.let { definition ->
                            mergeToDomain(definition, config)
                        }
                    }
                    .sortedBy { it.sortOrder }
            }

    companion object {
        /** Tope de semanas hacia atrás a rellenar (cubre la ventana de estabilidad de 5 + 1). */
        const val MAX_BACKFILL_WEEKS = 6L
    }
}

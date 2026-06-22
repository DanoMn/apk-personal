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
    /**
     * Recalcula el snapshot de la semana en curso. La CLAVE sigue siendo la semana calendario
     * (lunes → [today]) para que la historia tenga UNA fila por semana; pero los HECHOS que lo
     * alimentan son la ventana MÓVIL de 7 días terminada en [today], coherente con el cálculo
     * en vivo del dashboard (#858). Para la fila en curso, `weekStart` es clave de indexado, no
     * borde de datos (los hechos arrancan en `today-6`, que cruza al lunes anterior).
     */
    suspend fun refreshCurrentWeek(today: LocalDate) {
        writeWindow(keyWeekStart = mondayOf(today), keyWeekEnd = today, windowEnd = today)
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
            // Semana vencida completa: windowEnd = domingo → ventana móvil (domingo-6 .. domingo)
            // ≡ semana calendario (lunes .. domingo). Clave y datos coinciden.
            writeWindow(keyWeekStart = weekStart, keyWeekEnd = weekEnd, windowEnd = weekEnd)
        }
    }

    private suspend fun weekHasFacts(weekStart: LocalDate, weekEnd: LocalDate): Boolean {
        val startKey = weekStart.toString()
        val endKey = weekEnd.toString()
        if (dataSource.getActivityLogsBetween(startKey, endKey).isNotEmpty()) return true
        return dataSource.getAllAbstinenceLogsSnapshot().any { it.date in startKey..endKey }
    }

    /**
     * Escribe un snapshot bajo la CLAVE de semana calendario [keyWeekStart]..[keyWeekEnd], pero
     * calculado sobre la ventana MÓVIL de 7 días terminada en [windowEnd] (los hechos van de
     * `windowEnd-6` a `windowEnd`). Para semanas vencidas completas (windowEnd = domingo) la
     * ventana móvil coincide con la semana calendario; para la semana en curso, los hechos
     * cruzan al lunes anterior pero la clave se mantiene en el lunes en curso.
     */
    private suspend fun writeWindow(
        keyWeekStart: LocalDate,
        keyWeekEnd: LocalDate,
        windowEnd: LocalDate,
    ) {
        val windowStartKey = windowEnd.minusDays(6).toString()
        val windowEndKey = windowEnd.toString()
        val periodActivityLogs = dataSource.getActivityLogsBetween(
            startDate = windowStartKey,
            endDate = windowEndKey,
        ).map { it.toDomain() }
        val allAbstinenceLogs = dataSource.getAllAbstinenceLogsSnapshot().map { it.toDomain() }
        // Solo memoria ANTERIOR a la semana-clave: para la semana en curso es equivalente al
        // filtro de la policy (que excluye la semana actual), y para el back-fill evita que
        // semanas posteriores ya escritas se filtren como "previas" de una semana vieja.
        val keyWeekStartKey = keyWeekStart.toString()
        val weeklyHistory = dataSource.getWeeklyScoreSnapshotsSnapshot()
            .map { it.toHistoryEntry() }
            .filter { it.weekStart < keyWeekStartKey }
        val source = ScoreInputSource(
            layers = dataSource.getLayersSnapshot().map { it.toDomain() },
            activities = configuredActivitiesSnapshot(),
            todayActivityLogs = periodActivityLogs.filter { it.date == windowEndKey },
            periodActivityLogs = periodActivityLogs,
            abstinenceTracks = dataSource.getAbstinenceTracksSnapshot().map { it.toDomain() },
            todayAbstinenceLogs = allAbstinenceLogs.filter { it.date == windowEndKey },
            allAbstinenceLogs = allAbstinenceLogs,
            tasks = dataSource.getTasksSnapshot().map { it.toDomain() },
            // 5.8: weekly sleep — get all nights in range, map to SleepNightScore,
            // filter out NoData (toSleepNightScore returns null for NoData nights).
            sleepNights = dataSource.getSleepNightsInRange(
                from = windowStartKey,
                to = windowEndKey,
            ).mapNotNull { it.toSleepNightScore() },
            today = windowEnd,
            weeklyHistory = weeklyHistory,
        )
        val scoreInput = BuildScoreInputUseCase(source)
        val scoreReport = ScoreEngine.calculate(scoreInput)
        val snapshot = BuildWeeklyScoreSnapshotUseCase(
            WeeklyScoreSnapshotInput(
                weekStart = keyWeekStart,
                weekEnd = keyWeekEnd,
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

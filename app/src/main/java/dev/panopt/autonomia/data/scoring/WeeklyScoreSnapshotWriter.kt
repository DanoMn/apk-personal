package dev.panopt.autonomia.data.scoring

import dev.panopt.autonomia.data.AutonomiaDao
import dev.panopt.autonomia.data.WeeklyScoreSnapshotEntity
import dev.panopt.autonomia.data.local.mapper.mergeToDomain
import dev.panopt.autonomia.data.local.mapper.toDomain
import dev.panopt.autonomia.domain.scoring.BuildScoreInputUseCase
import dev.panopt.autonomia.domain.scoring.BuildWeeklyScoreSnapshotUseCase
import dev.panopt.autonomia.domain.scoring.ScoreEngine
import dev.panopt.autonomia.domain.scoring.ScoreInputSource
import dev.panopt.autonomia.domain.scoring.WeeklyScoreSnapshotDraft
import dev.panopt.autonomia.domain.scoring.WeeklyScoreSnapshotInput
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

class WeeklyScoreSnapshotWriter(
    private val dao: AutonomiaDao,
) {
    suspend fun refreshCurrentWeek(today: LocalDate) {
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val dateKey = today.toString()
        val periodActivityLogs = dao.getActivityLogsBetween(
            startDate = weekStart.toString(),
            endDate = dateKey,
        ).map { it.toDomain() }
        val allAbstinenceLogs = dao.getAllAbstinenceLogsSnapshot().map { it.toDomain() }
        val source = ScoreInputSource(
            layers = dao.getLayersSnapshot().map { it.toDomain() },
            activities = configuredActivitiesSnapshot(),
            todayActivityLogs = periodActivityLogs.filter { it.date == dateKey },
            periodActivityLogs = periodActivityLogs,
            abstinenceTracks = dao.getAbstinenceTracksSnapshot().map { it.toDomain() },
            todayAbstinenceLogs = allAbstinenceLogs.filter { it.date == dateKey },
            allAbstinenceLogs = allAbstinenceLogs,
            tasks = dao.getTasksSnapshot().map { it.toDomain() },
            sleepLog = dao.getSleepLogForDate(dateKey)?.toDomain(),
            today = today,
        )
        val scoreInput = BuildScoreInputUseCase(source)
        val scoreReport = ScoreEngine.calculate(scoreInput)
        val snapshot = BuildWeeklyScoreSnapshotUseCase(
            WeeklyScoreSnapshotInput(
                weekStart = weekStart,
                weekEnd = today,
                calculatedAt = System.currentTimeMillis(),
                scoreInput = scoreInput,
                scoreReport = scoreReport,
            ),
        )
        dao.upsertWeeklyScoreSnapshot(snapshot.toEntity())
    }

    private suspend fun configuredActivitiesSnapshot() =
        dao.getActivityDefinitionsSnapshot()
            .associateBy { it.id }
            .let { definitionsById ->
                dao.getActiveUserActivityConfigs()
                    .mapNotNull { config ->
                        definitionsById[config.activityId]?.let { definition ->
                            mergeToDomain(definition, config)
                        }
                    }
                    .sortedBy { it.sortOrder }
            }
}

private fun WeeklyScoreSnapshotDraft.toEntity(): WeeklyScoreSnapshotEntity =
    WeeklyScoreSnapshotEntity(
        weekStart = weekStart,
        weekEnd = weekEnd,
        scoringVersion = scoringVersion,
        calculatedAt = calculatedAt,
        configHash = configHash,
        factsHash = factsHash,
        weeklyBaseScore = weeklyBaseScore,
        weeklyScore = weeklyScore,
        stabilityScore = stabilityScore,
        state = state,
        visibleScore = visibleScore,
        worstLayerId = worstLayerId,
        layerSummariesJson = layerSummariesJson,
        reasonsJson = reasonsJson,
    )

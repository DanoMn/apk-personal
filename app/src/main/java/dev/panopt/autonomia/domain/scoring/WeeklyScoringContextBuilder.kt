package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceStatus
import dev.panopt.autonomia.TaskStatus
import dev.panopt.autonomia.domain.sleep.SleepScoring
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters

internal object WeeklyScoringContextBuilder {
    fun build(input: ScoreInput): WeeklyScoringContext {
        val activeLayers = input.layers.filter { it.active }.sortedBy { it.sortOrder }
        val weekStart = input.today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = weekStart.datesUntilInclusive(input.today)
        val visibleActivities = input.activities
            .filter { it.active && !it.archived }
            .sortedBy { it.sortOrder }
        val weeklyLogsByActivity = (input.periodActivityLogs + input.todayActivityLogs)
            .filter { it.dateAsLocalDate()?.let { date -> date in weekDates } == true }
            .distinctBy { "${it.activityId}:${it.date}" }
            .groupBy { it.activityId }
        val activeSobrietyTracks = input.abstinenceTracks
            .filter { it.active }
            .sortedBy { it.sortOrder }

        return WeeklyScoringContext(
            weekStart = weekStart,
            weekDates = weekDates,
            activeLayers = activeLayers,
            visibleActivities = visibleActivities,
            weeklyLogsByActivity = weeklyLogsByActivity,
            activeSobrietyTracks = activeSobrietyTracks,
            sleepScore = input.sleepLog?.let(SleepScoring::score),
            sobrietyScore = SobrietyScoringPolicy.score(
                tracks = activeSobrietyTracks,
                allLogs = input.allAbstinenceLogs,
                todayLogs = input.todayAbstinenceLogs,
                weekDates = weekDates,
                today = input.today,
            ),
            completedTasksByLayer = input.tasks
                .filter { it.isScoringTaskCompletedIn(weekStart, input.today) }
                .groupBy { it.layerId.orEmpty() },
        )
    }

    fun hasAnyFact(input: ScoreInput): Boolean {
        val activeTrackIds = input.abstinenceTracks.filter { it.active }.map { it.id }.toSet()
        val hasAbstinenceFact = (input.todayAbstinenceLogs + input.allAbstinenceLogs).any { log ->
            log.trackId in activeTrackIds && log.status != AbstinenceStatus.Unknown
        }
        val hasTaskFact = input.tasks.any { it.status == TaskStatus.Done && it.layerId != null }
        return input.todayActivityLogs.isNotEmpty() ||
            input.periodActivityLogs.isNotEmpty() ||
            hasAbstinenceFact ||
            hasTaskFact ||
            input.sleepLog != null
    }
}

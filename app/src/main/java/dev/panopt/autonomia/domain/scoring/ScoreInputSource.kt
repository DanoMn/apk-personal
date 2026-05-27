package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.AbstinenceLog
import dev.panopt.autonomia.AbstinenceTrack
import dev.panopt.autonomia.ActivityLog
import dev.panopt.autonomia.Layer
import dev.panopt.autonomia.SleepLog
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.domain.activity.ActivityDefinition
import java.time.LocalDate

data class ScoreInputSource(
    val layers: List<Layer>,
    val activities: List<ActivityDefinition>,
    val todayActivityLogs: List<ActivityLog>,
    val periodActivityLogs: List<ActivityLog>,
    val abstinenceTracks: List<AbstinenceTrack>,
    val todayAbstinenceLogs: List<AbstinenceLog>,
    val allAbstinenceLogs: List<AbstinenceLog>,
    val tasks: List<Task>,
    val sleepLog: SleepLog?,
    val today: LocalDate = LocalDate.now(),
    val weeklyHistory: List<WeeklyScoreHistoryEntry> = emptyList(),
)

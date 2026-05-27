package dev.panopt.autonomia.data.scoring

import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.data.WeeklyScoreSnapshotEntity
import dev.panopt.autonomia.domain.scoring.WeeklyScoreHistoryEntry
import dev.panopt.autonomia.domain.scoring.WeeklyScoreSnapshotDraft

internal fun WeeklyScoreSnapshotEntity.toHistoryEntry(): WeeklyScoreHistoryEntry =
    WeeklyScoreHistoryEntry(
        weekStart = weekStart,
        weekEnd = weekEnd,
        scoringVersion = scoringVersion,
        weeklyBaseScore = weeklyBaseScore,
        weeklyScore = weeklyScore,
        state = runCatching { ScoreState.valueOf(state) }.getOrDefault(ScoreState.NoData),
    )

internal fun WeeklyScoreSnapshotDraft.toEntity(): WeeklyScoreSnapshotEntity =
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

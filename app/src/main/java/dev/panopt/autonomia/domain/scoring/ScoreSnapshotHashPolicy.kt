package dev.panopt.autonomia.domain.scoring

import java.security.MessageDigest

internal object ScoreSnapshotHashPolicy {
    fun configHash(input: ScoreInput): String =
        sha256(
            buildList {
                input.layers.sortedBy { it.id }.forEach { layer ->
                    add("layer:${layer.id}:${layer.active}:${layer.sortOrder}")
                }
                input.activities.sortedBy { it.id }.forEach { activity ->
                    add(
                        listOf(
                            "activity",
                            activity.id,
                            activity.layerId,
                            activity.activityType.name,
                            activity.active.toString(),
                            activity.archived.toString(),
                            activity.weeklyFrequencyTarget?.toString().orEmpty(),
                            activity.sessionTargetMinutes?.toString().orEmpty(),
                            activity.updatedAt.toString(),
                        ).joinToString(":"),
                    )
                }
                input.abstinenceTracks.sortedBy { it.id }.forEach { track ->
                    add("track:${track.id}:${track.active}:${track.updatedAt}")
                }
            },
        )

    fun factsHash(input: ScoreInput): String =
        sha256(
            buildList {
                (input.periodActivityLogs + input.todayActivityLogs)
                    .distinctBy { "${it.activityId}:${it.date}" }
                    .sortedWith(compareBy({ it.date }, { it.activityId }))
                    .forEach { log ->
                        add("activityLog:${log.date}:${log.activityId}:${log.completed}:${log.actualValue}:${log.updatedAt}")
                    }
                input.allAbstinenceLogs.sortedWith(compareBy({ it.date }, { it.trackId })).forEach { log ->
                    add("abstinenceLog:${log.date}:${log.trackId}:${log.status.name}:${log.updatedAt}")
                }
                input.tasks.sortedBy { it.id }.forEach { task ->
                    add("task:${task.id}:${task.layerId}:${task.status.name}:${task.completedAt}:${task.updatedAt}")
                }
                // 5.7: hash each scored sleep night; NoData nights (sleepScore==null) still fingerprinted
                input.sleepNights
                    .sortedBy { it.confidence.name } // deterministic order (nightDate not available here)
                    .forEach { night ->
                        add("sleep:${night.confidence.name}:${night.sleepScore}:${night.duration}:${night.continuity}")
                    }
            },
        )

    private fun sha256(parts: List<String>): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val payload = parts.joinToString(separator = "\n").toByteArray(Charsets.UTF_8)
        return digest.digest(payload).joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}

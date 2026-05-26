package dev.panopt.autonomia.domain.task

import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus

data class TaskDraft(
    val title: String,
    val layerId: String?,
    val contributionRole: ContributionRole,
    val importanceTier: ImportanceTier = ImportanceTier.Medium,
)

object TaskPolicy {
    fun createDraft(title: String, layerId: String?): TaskDraft? {
        val normalizedTitle = normalizeTitle(title)
        if (normalizedTitle.isBlank()) return null

        val normalizedLayerId = layerId?.trim()?.takeIf { it.isNotBlank() }
        return TaskDraft(
            title = normalizedTitle,
            layerId = normalizedLayerId,
            contributionRole = if (normalizedLayerId == null) {
                ContributionRole.Neutral
            } else {
                ContributionRole.Support
            },
        )
    }

    fun canComplete(task: Task): Boolean =
        task.status == TaskStatus.Pending

    fun canReactivate(task: Task): Boolean =
        task.status == TaskStatus.Done

    private fun normalizeTitle(title: String): String =
        title.trim().replace(Regex("\\s+"), " ")
}

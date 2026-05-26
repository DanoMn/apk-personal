package dev.panopt.autonomia.domain.task

import dev.panopt.autonomia.ContributionRole
import dev.panopt.autonomia.ImportanceTier
import dev.panopt.autonomia.Task
import dev.panopt.autonomia.TaskStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPolicyTest {
    @Test
    fun `blank title is rejected`() {
        assertNull(TaskPolicy.createDraft("   ", layerId = null))
    }

    @Test
    fun `title is trimmed and normalized`() {
        val draft = checkNotNull(TaskPolicy.createDraft("  Pagar   recibo  ", layerId = null))

        assertEquals("Pagar recibo", draft.title)
    }

    @Test
    fun `task without layer is neutral`() {
        val draft = checkNotNull(TaskPolicy.createDraft("Ordenar idea", layerId = null))

        assertEquals(null, draft.layerId)
        assertEquals(ContributionRole.Neutral, draft.contributionRole)
        assertEquals(ImportanceTier.Medium, draft.importanceTier)
    }

    @Test
    fun `task with layer contributes as support`() {
        val draft = checkNotNull(TaskPolicy.createDraft("Comprar comida", layerId = " layer_cuerpo "))

        assertEquals("layer_cuerpo", draft.layerId)
        assertEquals(ContributionRole.Support, draft.contributionRole)
    }

    @Test
    fun `only pending task can be completed`() {
        assertTrue(TaskPolicy.canComplete(task(status = TaskStatus.Pending)))
        assertFalse(TaskPolicy.canComplete(task(status = TaskStatus.Done)))
        assertFalse(TaskPolicy.canComplete(task(status = TaskStatus.Archived)))
    }

    @Test
    fun `only done task can be reactivated`() {
        assertTrue(TaskPolicy.canReactivate(task(status = TaskStatus.Done)))
        assertFalse(TaskPolicy.canReactivate(task(status = TaskStatus.Pending)))
        assertFalse(TaskPolicy.canReactivate(task(status = TaskStatus.Archived)))
    }

    private fun task(status: TaskStatus): Task =
        Task(
            id = "task_test",
            title = "Test",
            description = "",
            layerId = null,
            projectId = null,
            status = status,
            contributionRole = ContributionRole.Neutral,
            importanceTier = ImportanceTier.Medium,
            dueDate = null,
            completedAt = if (status == TaskStatus.Done) 1L else null,
            createdAt = 0L,
            updatedAt = 0L,
        )
}

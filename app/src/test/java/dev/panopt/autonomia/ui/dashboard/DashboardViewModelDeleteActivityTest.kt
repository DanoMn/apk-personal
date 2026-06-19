package dev.panopt.autonomia.ui.dashboard

import dev.panopt.autonomia.domain.activity.RemoveAnchorResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * El borrado de actividad desde el dashboard respeta el CANDADO: cuando
 * `deleteCustomActivity` devuelve [RemoveAnchorResult.BlockedByMinimum] (ancla activa que dejaría
 * la app sin cobertura), el VM alimenta el `anchorRemovalBlockedMessage` con un mensaje compasivo
 * (reusando el mismo flow que `removeActivityAsAnchor`). Cuando procede ([RemoveAnchorResult.Removed],
 * p. ej. un soporte), NO se emite mensaje.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelDeleteActivityTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 6, 18)

    @Test
    fun `delete bloqueado por minimo emite mensaje compasivo`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeDashboardRepository().apply {
                deleteCustomActivityResult = RemoveAnchorResult.BlockedByMinimum
            }
            val viewModel = DashboardViewModel(repository, clock = { today })

            assertNull(viewModel.anchorRemovalBlockedMessage.value)

            viewModel.deleteActivity("ancla_x")
            advanceUntilIdle()

            val message = viewModel.anchorRemovalBlockedMessage.value
            assertNotNull("El delete bloqueado debe emitir un mensaje", message)
            assertTrue(
                "El mensaje menciona la cobertura mínima de capas",
                message!!.contains("capas"),
            )
        }

    @Test
    fun `delete que procede no emite mensaje de bloqueo`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeDashboardRepository().apply {
                deleteCustomActivityResult = RemoveAnchorResult.Removed
            }
            val viewModel = DashboardViewModel(repository, clock = { today })

            viewModel.deleteActivity("soporte_x")
            advanceUntilIdle()

            assertNull(
                "Un delete que procede no debe emitir bloqueo",
                viewModel.anchorRemovalBlockedMessage.value,
            )
        }
}

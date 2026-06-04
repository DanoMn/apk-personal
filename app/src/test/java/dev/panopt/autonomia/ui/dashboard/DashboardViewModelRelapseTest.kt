package dev.panopt.autonomia.ui.dashboard

import dev.panopt.autonomia.domain.dashboard.DashboardDimensionStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

/**
 * Cobertura del registro de RECAÍDA a través del seam del [DashboardViewModel]
 * (`toggleAbstinenceRelapse`): marcar una recaída la deja señalada y volver a tocar el
 * mismo gesto limpia el registro (vuelve a "desconocido"). Verifica que la acción usa la
 * fecha viva y que la proyección refleja el estado correcto.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelRelapseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.of(2026, 5, 21)

    @Test
    fun `marking relapse flags the track and clearing reverts to unknown`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeDashboardRepository() // track crítico por defecto
            val viewModel = DashboardViewModel(repository, clock = { today })

            val collector = launch { viewModel.dashboardState.collect {} }
            advanceUntilIdle()

            // Estado inicial: sin registro → desconocido.
            val initial = viewModel.dashboardState.value.sobrietyTracks.single { it.id == "trk_alcohol" }
            assertFalse(initial.isRelapseToday)
            assertFalse(initial.isMarkedCleanToday)
            assertEquals(DashboardDimensionStatus.Unknown, initial.status)

            // Marcar recaída (gesto desde una card aún no señalada).
            viewModel.toggleAbstinenceRelapse("trk_alcohol", isRelapseToday = false)
            advanceUntilIdle()
            val afterRelapse = viewModel.dashboardState.value.sobrietyTracks.single { it.id == "trk_alcohol" }
            assertTrue("La recaída queda señalada hoy", afterRelapse.isRelapseToday)
            assertFalse(afterRelapse.isMarkedCleanToday)
            assertEquals("Una recaída no acumula racha", 0, afterRelapse.days)
            assertEquals(
                "Track crítico en recaída → Restauración",
                DashboardDimensionStatus.Restoration,
                afterRelapse.status,
            )

            // Volver a tocar el mismo gesto limpia el registro.
            viewModel.toggleAbstinenceRelapse("trk_alcohol", isRelapseToday = afterRelapse.isRelapseToday)
            advanceUntilIdle()
            val afterClear = viewModel.dashboardState.value.sobrietyTracks.single { it.id == "trk_alcohol" }
            assertFalse("El toggle inverso limpia la recaída", afterClear.isRelapseToday)
            assertFalse(afterClear.isMarkedCleanToday)
            assertEquals(DashboardDimensionStatus.Unknown, afterClear.status)

            collector.cancel()
        }
}

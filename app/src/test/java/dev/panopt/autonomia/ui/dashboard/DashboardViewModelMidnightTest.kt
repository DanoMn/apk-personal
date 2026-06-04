package dev.panopt.autonomia.ui.dashboard

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
 * Regresión del bug "la racha de sobriedad se resetea a 0 al marcar al día siguiente".
 *
 * Causa raíz: el [DashboardViewModel] capturaba `LocalDate.now()` al construirse y nunca
 * lo refrescaba. Como el proceso/VM sobrevive a la medianoche (app en background/recents),
 * la fecha quedaba congelada en el día anterior; la tarjeta de racha —un toggle— se
 * renderizaba como "ya marcada" (porque leía el log de ayer, que sí estaba Clean) y al
 * tocarla BORRABA ese log, dejando la racha en 0.
 *
 * Este test dirige un reloj virtual a través de la medianoche y exige que la racha se
 * preserve e incremente. Con el código viejo (fecha congelada) la card del día 2 vendría
 * `isMarkedCleanToday = true` y el toggle la limpiaría → falla.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelMidnightTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val day1 = LocalDate.of(2026, 5, 20)
    private val day2 = LocalDate.of(2026, 5, 21)

    @Test
    fun `clean streak survives midnight rollover and increments next day`() =
        runTest(mainDispatcherRule.dispatcher.scheduler) {
            val repository = FakeDashboardRepository()
            var now = day1
            val viewModel = DashboardViewModel(repository, clock = { now })

            // Mantener caliente el StateFlow (WhileSubscribed) durante el test.
            val collector = launch { viewModel.dashboardState.collect {} }
            advanceUntilIdle()

            // --- Día 1: marcar alcohol limpio ---
            viewModel.toggleAbstinenceClean("trk_alcohol", isMarkedCleanToday = false)
            advanceUntilIdle()
            val day1Track = viewModel.dashboardState.value.sobrietyTracks.single { it.id == "trk_alcohol" }
            assertEquals("Tras marcar el día 1, la racha es 1", 1, day1Track.days)
            assertTrue("El día 1 queda marcado", day1Track.isMarkedCleanToday)

            // --- Cruza la medianoche; el VM sobrevive y llega ON_RESUME del día 2 ---
            now = day2
            viewModel.onResumed()
            advanceUntilIdle()

            val day2BeforeMark = viewModel.dashboardState.value.sobrietyTracks.single { it.id == "trk_alcohol" }
            assertFalse(
                "La card del nuevo día NO debe venir pre-marcada",
                day2BeforeMark.isMarkedCleanToday,
            )
            assertEquals(
                "La racha de ayer no debe perderse al cruzar la medianoche",
                1,
                day2BeforeMark.days,
            )

            // --- Marcar hoy: debe sumar a 2, nunca resetear a 0 ---
            viewModel.toggleAbstinenceClean("trk_alcohol", isMarkedCleanToday = day2BeforeMark.isMarkedCleanToday)
            advanceUntilIdle()
            val day2AfterMark = viewModel.dashboardState.value.sobrietyTracks.single { it.id == "trk_alcohol" }
            assertEquals("Marcar el día 2 incrementa la racha a 2", 2, day2AfterMark.days)

            collector.cancel()
        }
}

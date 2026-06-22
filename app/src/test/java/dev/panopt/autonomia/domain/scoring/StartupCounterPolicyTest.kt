package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.domain.activity.AnchorGraceRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lote 2 (task 2.5) — `StartupCounterPolicy.counter(projectedEstado, daysLived)`. Dominio puro JVM.
 *
 * La barra de carga: atenúa el ESTADO proyectado por `d/7` y lo mapea a puntos visibles. Día 1 →
 * `estado × 1/7`; día 7 → `estado × 7/7 = estado` (sin atenuar, converge con el maduro). `d` se
 * clampa a `[1, 7]`. `daysRemaining = GRACE_DAYS - d`.
 */
class StartupCounterPolicyTest {
    private val grace = AnchorGraceRule.GRACE_DAYS.toInt() // 7

    @Test
    fun dayOneAttenuatesByOneSeventh() {
        val estado = 1.0
        val result = StartupCounterPolicy.counter(projectedEstado = estado, daysLived = 1)

        assertEquals(PointsMappingPolicy.points(estado * 1.0 / 7.0), result.counterPoints)
        assertEquals(1, result.daysLived)
        assertEquals(6, result.daysRemaining)
        assertEquals(1f / 7f, result.windowProgress, 1e-6f)
    }

    @Test
    fun dayFourAttenuatesByFourSevenths() {
        val estado = 1.0
        val result = StartupCounterPolicy.counter(projectedEstado = estado, daysLived = 4)

        assertEquals(PointsMappingPolicy.points(estado * 4.0 / 7.0), result.counterPoints)
        assertEquals(4, result.daysLived)
        assertEquals(3, result.daysRemaining)
        assertEquals(4f / 7f, result.windowProgress, 1e-6f)
    }

    @Test
    fun daySevenIsFullScoreNoAttenuation() {
        val estado = 1.0
        val result = StartupCounterPolicy.counter(projectedEstado = estado, daysLived = 7)

        // d=7 → × 7/7 = sin atenuar: contador == score real proyectado.
        assertEquals(PointsMappingPolicy.points(estado), result.counterPoints)
        assertEquals(7, result.daysLived)
        assertEquals(0, result.daysRemaining)
        assertEquals(1f, result.windowProgress, 1e-6f)
    }

    @Test
    fun daysLivedAboveSevenClampsToSeven() {
        val estado = 1.0
        val result = StartupCounterPolicy.counter(projectedEstado = estado, daysLived = 9)

        assertEquals(7, result.daysLived)
        assertEquals(0, result.daysRemaining)
        assertEquals(1f, result.windowProgress, 1e-6f)
        assertEquals(PointsMappingPolicy.points(estado), result.counterPoints)
    }

    @Test
    fun daysLivedBelowOneClampsToOne() {
        val result = StartupCounterPolicy.counter(projectedEstado = 1.0, daysLived = 0)

        assertEquals(1, result.daysLived)
        assertEquals(6, result.daysRemaining)
        assertEquals(1f / 7f, result.windowProgress, 1e-6f)
    }

    @Test
    fun zeroProjectedEstadoStillProducesActiveCardNotNoData() {
        // Cuenta sin hechos: estado proyectado 0 → contador = points(0) (= piso 650, NO null).
        // La card sigue activa (informativa/compasiva), no se cae a NoData.
        val result = StartupCounterPolicy.counter(projectedEstado = 0.0, daysLived = 3)

        assertEquals(PointsMappingPolicy.points(0.0), result.counterPoints)
        assertEquals(3, result.daysLived)
        assertEquals(4, result.daysRemaining)
        assertTrue("contador con estado 0 mapea al piso visible", result.counterPoints >= 650)
    }

    @Test
    fun daysRemainingDerivesFromGraceDays() {
        // daysRemaining = GRACE_DAYS - d (fuente única AnchorGraceRule.GRACE_DAYS).
        (1..7).forEach { d ->
            val result = StartupCounterPolicy.counter(projectedEstado = 0.8, daysLived = d)
            assertEquals(grace - d, result.daysRemaining)
        }
    }
}

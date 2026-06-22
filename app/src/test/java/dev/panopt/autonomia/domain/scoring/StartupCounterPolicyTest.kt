package dev.panopt.autonomia.domain.scoring

import dev.panopt.autonomia.domain.activity.AnchorGraceRule
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lote 2 (task 2.5) + FIX A (Lote 3) — `StartupCounterPolicy.counter(projectedEstado, daysLived)`.
 * Dominio puro JVM.
 *
 * La barra de carga atenúa los PUNTOS VISIBLES por `d/7` (NO el estado antes de mapear): el contador
 * vive en la zona muerta `0–650` y sube hacia el score real. Día 1 → `points(estado) × 1/7`; día 7 →
 * `points(estado) × 7/7 = points(estado)` (sin atenuar, converge con el maduro del día 8). `d` se
 * clampa a `[1, 7]`. `daysRemaining = GRACE_DAYS - d`.
 */
class StartupCounterPolicyTest {
    private val grace = AnchorGraceRule.GRACE_DAYS.toInt() // 7

    /** Puntos visibles atenuados esperados: `round(points(estado) × d/7)`. */
    private fun expected(estado: Double, d: Int): Int =
        (PointsMappingPolicy.points(estado).toDouble() * d.toDouble() / 7.0).roundToInt()

    @Test
    fun dayOneAttenuatesByOneSeventh() {
        val estado = 1.0
        val result = StartupCounterPolicy.counter(projectedEstado = estado, daysLived = 1)

        assertEquals(expected(estado, 1), result.counterPoints)
        assertEquals(1, result.daysLived)
        assertEquals(6, result.daysRemaining)
        assertEquals(1f / 7f, result.windowProgress, 1e-6f)
    }

    @Test
    fun dayFourAttenuatesByFourSevenths() {
        val estado = 1.0
        val result = StartupCounterPolicy.counter(projectedEstado = estado, daysLived = 4)

        assertEquals(expected(estado, 4), result.counterPoints)
        assertEquals(4, result.daysLived)
        assertEquals(3, result.daysRemaining)
        assertEquals(4f / 7f, result.windowProgress, 1e-6f)
    }

    @Test
    fun daySevenIsFullScoreNoAttenuation() {
        val estado = 1.0
        val result = StartupCounterPolicy.counter(projectedEstado = estado, daysLived = 7)

        // d=7 → × 7/7 = sin atenuar: contador == score real proyectado (puntos completos).
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
        assertEquals(expected(1.0, 1), result.counterPoints)
    }

    @Test
    fun counterLivesInDeadZoneBelowFloorOnEarlyDays() {
        // FIX A: con puntos atenuados, los días tempranos viven en 0–650 (zona muerta), NO ≥650.
        // estado alto (900 pts) día 1 → ~129, NO 650.
        val estado = 1.0 // points(1.0) ≈ 941
        val day1 = StartupCounterPolicy.counter(projectedEstado = estado, daysLived = 1)
        assertTrue(
            "el contador del día 1 debe vivir en la zona muerta (<650): ${day1.counterPoints}",
            day1.counterPoints < 650,
        )
    }

    @Test
    fun ownerExamplesNineHundredPointScore() {
        // Ejemplos del dueño: score proyectado 900 pts → d=1→129, d=4→514, d=7→900.
        // Buscamos un estado cuyo points() == 900 para fijar el ejemplo exacto.
        val estado = estadoForPoints(900)
        assertEquals(900, PointsMappingPolicy.points(estado))
        assertEquals(129, StartupCounterPolicy.counter(estado, 1).counterPoints)
        assertEquals(514, StartupCounterPolicy.counter(estado, 4).counterPoints)
        assertEquals(900, StartupCounterPolicy.counter(estado, 7).counterPoints)
    }

    @Test
    fun zeroProjectedEstadoStillProducesActiveCardNotNoData() {
        // Cuenta sin hechos: estado proyectado 0 → points(0) = piso 650, atenuado por d/7.
        // La card sigue activa (informativa/compasiva), no se cae a NoData.
        val result = StartupCounterPolicy.counter(projectedEstado = 0.0, daysLived = 3)

        assertEquals(expected(0.0, 3), result.counterPoints)
        assertEquals(3, result.daysLived)
        assertEquals(4, result.daysRemaining)
        assertTrue("contador con estado 0 sigue siendo un número visible", result.counterPoints >= 0)
    }

    @Test
    fun daysRemainingDerivesFromGraceDays() {
        // daysRemaining = GRACE_DAYS - d (fuente única AnchorGraceRule.GRACE_DAYS).
        (1..7).forEach { d ->
            val result = StartupCounterPolicy.counter(projectedEstado = 0.8, daysLived = d)
            assertEquals(grace - d, result.daysRemaining)
        }
    }

    /** Busca por bisección un `estado ∈ [0, 1.5]` cuyo `points()` sea exactamente [target]. */
    private fun estadoForPoints(target: Int): Double {
        var lo = 0.0
        var hi = 1.5
        repeat(80) {
            val mid = (lo + hi) / 2.0
            val p = PointsMappingPolicy.points(mid)
            if (p < target) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }
}

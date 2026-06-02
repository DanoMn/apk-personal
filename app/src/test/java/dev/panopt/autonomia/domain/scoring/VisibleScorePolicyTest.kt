package dev.panopt.autonomia.domain.scoring

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Caracteriza la escala visible (árbol §3.2):
 *   VisibleScore = 700 + round(clamp(WeeklyBaseScore, 0, 1) * 300)
 * acotado a [700, 1000].
 *
 * Reglas del contrato: nunca muestra valores humillantes bajo 700, y el superhábit
 * (interno > 1) NO rompe el techo de 1000. Hojas de respuesta desde el contrato.
 */
class VisibleScorePolicyTest {

    @Test
    fun zeroMapsToFloor700() {
        // 700 + round(0*300) = 700
        assertEquals(700, VisibleScorePolicy.visibleScore(0f))
    }

    @Test
    fun oneMapsToCeiling1000() {
        // 700 + round(1*300) = 1000
        assertEquals(1000, VisibleScorePolicy.visibleScore(1f))
    }

    @Test
    fun halfMapsToMidpoint850() {
        // 700 + round(0.5*300) = 700 + 150 = 850
        assertEquals(850, VisibleScorePolicy.visibleScore(0.5f))
    }

    @Test
    fun negativeInternalIsClampedToFloor() {
        // clamp(-0.5, 0, 1) = 0 → 700. Nunca por debajo de 700.
        assertEquals(700, VisibleScorePolicy.visibleScore(-0.5f))
    }

    @Test
    fun surplusAboveOneIsClampedToCeiling() {
        // clamp(1.5, 0, 1) = 1 → 1000. El superhábit no rompe el techo visible.
        assertEquals(1000, VisibleScorePolicy.visibleScore(1.5f))
    }
}

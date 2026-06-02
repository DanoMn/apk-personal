package dev.panopt.autonomia.domain.scoring

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Caracteriza la curva de saturación de TaskMomentum (árbol §10.3):
 *   TaskMomentumBonus = 0.050 * (1 - exp(-completedLayerTasks / 2))
 *
 * Hojas de respuesta calculadas a mano DESDE EL CONTRATO (no desde el código):
 * el verde certifica que la implementación coincide con el árbol. La curva satura
 * para evitar abuso: muchas tasks NO superan el techo de 0.050.
 */
class TaskMomentumPolicyTest {

    @Test
    fun zeroTasksGivesNoBonus() {
        // Sin tasks completadas no hay momentum (guard: completedLayerTasks <= 0 → 0).
        assertEquals(0f, TaskMomentumPolicy.bonus(0), 0f)
    }

    @Test
    fun oneTaskGivesAboutTwoCents() {
        // 0.050 * (1 - exp(-0.5)) = 0.050 * 0.393469 = 0.019673  (contrato: ~0.020)
        assertEquals(0.019673f, TaskMomentumPolicy.bonus(1), 0.0005f)
    }

    @Test
    fun twoTasksFollowTheCurve() {
        // 0.050 * (1 - exp(-1.0)) = 0.050 * 0.632121 = 0.031606  (contrato: ~0.032)
        assertEquals(0.031606f, TaskMomentumPolicy.bonus(2), 0.0005f)
    }

    @Test
    fun threeTasksFollowTheCurve() {
        // 0.050 * (1 - exp(-1.5)) = 0.050 * 0.776870 = 0.038843  (contrato: ~0.039)
        assertEquals(0.038843f, TaskMomentumPolicy.bonus(3), 0.0005f)
    }

    @Test
    fun fiveTasksFollowTheCurve() {
        // 0.050 * (1 - exp(-2.5)) = 0.050 * 0.917915 = 0.045896  (contrato: ~0.046)
        assertEquals(0.045896f, TaskMomentumPolicy.bonus(5), 0.0005f)
    }

    @Test
    fun manyTasksSaturateAtTheCeilingNotAbove() {
        // La curva tiende a 0.050 pero NUNCA lo supera: superhábit de tasks no
        // compensa anclas ni capas. Con n grande exp(-n/2) ≈ 0 → ~0.050.
        assertEquals(0.050f, TaskMomentumPolicy.bonus(40), 0.0005f)
    }
}

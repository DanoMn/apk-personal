package dev.panopt.autonomia.domain.activity

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Decisión pura del CANDADO aplicada a operaciones de ciclo de vida (eliminar/archivar un ancla).
 *
 * Estas operaciones (`deleteCustomActivity`, `toggleActivityArchive`) reusan el mismo umbral que
 * [AnchorCoverageRule.canRemoveAnchor], pero solo deben bloquear cuando el objetivo es **un ancla
 * activa**. Soportes, tasks y actividades no-ancla NUNCA disparan el candado: proceden siempre,
 * incluso si el sistema está justo en el mínimo (su remoción no toca la cobertura de capas).
 *
 * Esta decisión vive en dominio puro para ser testeable sin Room/Compose; el Repository solo
 * recolecta hechos (anclas activas + si el objetivo es ancla activa) y delega acá.
 */
class AnchorOperationGuardTest {

    // Sistema justo en el mínimo: 3 capas distintas, cada una con su única ancla.
    private val anchorsAtMinimum = listOf(
        AnchorRef("a1", "interior"),
        AnchorRef("a2", "cuerpo"),
        AnchorRef("a3", "conducta"),
    )

    @Test
    fun `ancla activa unica de su capa estando en el minimo, BLOQUEA`() {
        val result = AnchorCoverageRule.resolveAnchorOperation(
            activeAnchors = anchorsAtMinimum,
            activityId = "a3",
            isActiveAnchor = true,
        )
        assertEquals(RemoveAnchorResult.BlockedByMinimum, result)
    }

    @Test
    fun `ancla activa con capa de sobra, PROCEDE`() {
        val anchors = anchorsAtMinimum + AnchorRef("a4", "vinculos")
        val result = AnchorCoverageRule.resolveAnchorOperation(
            activeAnchors = anchors,
            activityId = "a4",
            isActiveAnchor = true,
        )
        assertEquals(RemoveAnchorResult.Removed, result)
    }

    @Test
    fun `ancla activa que no es la unica de su capa, PROCEDE`() {
        val anchors = anchorsAtMinimum + AnchorRef("a4", "conducta") // conducta tiene 2 anclas
        val result = AnchorCoverageRule.resolveAnchorOperation(
            activeAnchors = anchors,
            activityId = "a4",
            isActiveAnchor = true,
        )
        assertEquals(RemoveAnchorResult.Removed, result)
    }

    @Test
    fun `soporte o no-ancla estando en el minimo, PROCEDE sin consultar la cobertura`() {
        // El objetivo NO es ancla activa: aunque el sistema esté en el mínimo, su remoción no
        // toca la cobertura de capas → nunca se bloquea.
        val result = AnchorCoverageRule.resolveAnchorOperation(
            activeAnchors = anchorsAtMinimum,
            activityId = "soporte_x",
            isActiveAnchor = false,
        )
        assertEquals(RemoveAnchorResult.Removed, result)
    }
}

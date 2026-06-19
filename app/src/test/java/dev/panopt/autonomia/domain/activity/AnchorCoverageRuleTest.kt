package dev.panopt.autonomia.domain.activity

import dev.panopt.autonomia.domain.scoring.ScoringConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regla canónica de cobertura de anclas: FUENTE ÚNICA del umbral "≥ N capas distintas con ancla".
 * Onboarding (`canAdvance`) y el candado de configuración (`canRemoveAnchor`) se derivan de acá.
 */
class AnchorCoverageRuleTest {

    @Test
    fun `el minimo espeja la constante del motor`() {
        assertEquals(ScoringConstants.MIN_ACTIVE_LAYERS_WITH_ANCHOR, AnchorCoverageRule.minLayers)
    }

    @Test
    fun `cuenta capas distintas, no anclas`() {
        assertEquals(1, AnchorCoverageRule.distinctLayersWithAnchor(listOf("interior", "interior", "interior")))
        assertEquals(3, AnchorCoverageRule.distinctLayersWithAnchor(listOf("interior", "cuerpo", "conducta")))
    }

    @Test
    fun `meetsMinimum exige el umbral`() {
        assertFalse(AnchorCoverageRule.meetsMinimum(listOf("interior", "cuerpo")))
        assertTrue(AnchorCoverageRule.meetsMinimum(listOf("interior", "cuerpo", "conducta")))
    }

    // ---- canRemoveAnchor: el candado ----

    @Test
    fun `quitar la unica ancla de una capa teniendo justo 3 capas con ancla, BLOQUEA`() {
        val anchors = listOf(
            AnchorRef("a1", "interior"),
            AnchorRef("a2", "cuerpo"),
            AnchorRef("a3", "conducta"),
        )
        // quitar a3 deja 2 capas con ancla → bajo el mínimo
        assertFalse(AnchorCoverageRule.canRemoveAnchor(anchors, "a3"))
    }

    @Test
    fun `quitar una de varias anclas de la MISMA capa con 3 capas, PERMITE`() {
        val anchors = listOf(
            AnchorRef("a1", "interior"),
            AnchorRef("a2", "cuerpo"),
            AnchorRef("a3", "conducta"),
            AnchorRef("a4", "conducta"), // conducta tiene 2 anclas
        )
        // quitar a4 deja conducta con a3 → siguen 3 capas con ancla
        assertTrue(AnchorCoverageRule.canRemoveAnchor(anchors, "a4"))
    }

    @Test
    fun `quitar un ancla teniendo 4 capas con ancla, PERMITE`() {
        val anchors = listOf(
            AnchorRef("a1", "interior"),
            AnchorRef("a2", "cuerpo"),
            AnchorRef("a3", "conducta"),
            AnchorRef("a4", "vinculos"),
        )
        assertTrue(AnchorCoverageRule.canRemoveAnchor(anchors, "a4"))
    }

    @Test
    fun `quitar un ancla inexistente no rompe y respeta el estado actual`() {
        val anchors = listOf(
            AnchorRef("a1", "interior"),
            AnchorRef("a2", "cuerpo"),
            AnchorRef("a3", "conducta"),
        )
        assertTrue(AnchorCoverageRule.canRemoveAnchor(anchors, "noexiste"))
    }
}

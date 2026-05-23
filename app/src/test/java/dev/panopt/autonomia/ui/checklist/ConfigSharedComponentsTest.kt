package dev.panopt.autonomia.ui.checklist

import androidx.compose.ui.graphics.Color
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ConfigSharedComponentsTest {

    private val palette = DashboardPalette(
        bgBase = Color(0xFF1F1E1D),
        bgSurface = Color(0xFF2A2927),
        bgSurface2 = Color(0xFF33312E),
        bgElevated = Color(0xFF3A3733),
        drawer = Color(0xFF272522),
        colorCardboard = Color(0xFFE0D8C3),
        colorCardboardSoft = Color(0xFFCFC4AA),
        colorCoral = Color(0xFFE57B65),
        textMain = Color(0xFFEAE5D9),
        textMuted = Color(0xFFA6A097),
        textFaint = Color(0xFF777169),
        layerInterior = Color(0xFF8893A5),
        layerBody = Color(0xFF7F9E83),
        layerConduct = Color(0xFFC49B55),
        layerVinculos = Color(0xFFA38491),
        layerProject = Color(0xFF708A9E),
        stateMotion = Color(0xFF5A8296),
        risk = Color(0xFFD45B51),
    )

    @Test
    fun `layerColor matches interior layer id`() {
        assertEquals(palette.layerInterior, layerColor("layer_interior_main", palette))
        assertEquals(palette.layerInterior, layerColor("some_interior_thing", palette))
    }

    @Test
    fun `layerColor matches cuerpo layer id`() {
        assertEquals(palette.layerBody, layerColor("layer_cuerpo", palette))
        assertEquals(palette.layerBody, layerColor("cuerpo_salud", palette))
    }

    @Test
    fun `layerColor matches conducta layer id`() {
        assertEquals(palette.layerConduct, layerColor("layer_conducta", palette))
        assertEquals(palette.layerConduct, layerColor("conducta_habitos", palette))
    }

    @Test
    fun `layerColor matches vinculos layer id`() {
        assertEquals(palette.layerVinculos, layerColor("layer_vinculos", palette))
        assertEquals(palette.layerVinculos, layerColor("vinculos_sociales", palette))
    }

    @Test
    fun `layerColor matches proyecto layer id`() {
        assertEquals(palette.layerProject, layerColor("layer_proyecto", palette))
        assertEquals(palette.layerProject, layerColor("proyecto_personal", palette))
    }

    @Test
    fun `layerColor returns textMuted for unknown layer id`() {
        assertEquals(palette.textMuted, layerColor("layer_unknown", palette))
        assertEquals(palette.textMuted, layerColor("", palette))
    }

    @Test
    fun `layerColor is deterministic`() {
        val first = layerColor("layer_interior_test", palette)
        val second = layerColor("layer_interior_test", palette)
        assertEquals("Same inputs should produce same output", first, second)
    }

    @Test
    fun `layerColor different layers return different colors`() {
        val interior = layerColor("layer_interior", palette)
        val cuerpo = layerColor("layer_cuerpo", palette)
        val conducta = layerColor("layer_conducta", palette)
        val vinculos = layerColor("layer_vinculos", palette)
        val proyecto = layerColor("layer_proyecto", palette)

        val colors = setOf(interior, cuerpo, conducta, vinculos, proyecto)
        assertEquals("All 5 layer colors should be distinct", 5, colors.size)
    }
}

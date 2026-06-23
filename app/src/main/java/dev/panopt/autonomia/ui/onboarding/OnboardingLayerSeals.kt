package dev.panopt.autonomia.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.InfinityIcon
import dev.panopt.autonomia.ui.dashboard.InteriorLayerIcon
import dev.panopt.autonomia.ui.dashboard.ProjectTriangleIcon
import dev.panopt.autonomia.ui.dashboard.VinculosLayerIcon
import dev.panopt.autonomia.ui.dashboard.WavesIcon

/**
 * Sellos de capa del onboarding: una sola fuente de verdad compartida por la Bienvenida
 * (momento "Las cinco") y el Cierre (los sellos de las capas que el usuario cubrió).
 *
 * NO inventa iconografía: usa los iconos de capa CANÓNICOS de la app (`DashboardIcons`), el mismo
 * mapeo `layerId → icono` que `LayerPill`/dashboard. El sello agrega solo un halo de color detrás
 * para darle el "peso visual" de un sello (AGENTS.md), pero el glifo es el oficial de cada capa.
 */
internal data class OnboardingLayerSeal(
    val layerId: String,
    val name: String,
    val phrase: String,
    val color: Color,
)

/** Las cinco capas en orden canónico (el del seed), con su color oficial. */
internal fun onboardingLayerSeals(palette: DashboardPalette): List<OnboardingLayerSeal> = listOf(
    OnboardingLayerSeal("layer_interior", "El Interior", "La biblioteca secreta donde conversás con vos mismo.", palette.layerInterior),
    OnboardingLayerSeal("layer_cuerpo", "El Cuerpo", "El frágil navío que te lleva.", palette.layerBody),
    OnboardingLayerSeal("layer_conducta", "La Conducta", "El timón frente a lo que te desvía.", palette.layerConduct),
    OnboardingLayerSeal("layer_vinculos", "Los Vínculos", "Nadie es un hombre solo.", palette.layerVinculos),
    OnboardingLayerSeal("layer_proyecto", "El Proyecto", "Lo que construís contra el olvido.", palette.layerProject),
)

/** Sello de capa: halo de color (peso visual) + el icono CANÓNICO de la capa encima. */
@Composable
internal fun LayerSeal(layerId: String, color: Color, size: Dp) {
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        // Halo: el medallón de color que le da peso de "sello".
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = color.copy(alpha = 0.16f), radius = this.size.minDimension * 0.5f)
        }
        // Glifo: el icono oficial de la capa (mismo mapeo que LayerPill/dashboard).
        val iconModifier = Modifier.size(size * 0.56f)
        when (layerId) {
            "layer_interior" -> InteriorLayerIcon(color = color, modifier = iconModifier)
            "layer_cuerpo" -> WavesIcon(color = color, modifier = iconModifier)
            "layer_conducta" -> InfinityIcon(color = color, modifier = iconModifier)
            "layer_vinculos" -> VinculosLayerIcon(color = color, modifier = iconModifier)
            "layer_proyecto" -> ProjectTriangleIcon(color = color, modifier = iconModifier)
        }
    }
}

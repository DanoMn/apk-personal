package dev.panopt.autonomia.ui.checklist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.InfinityIcon
import dev.panopt.autonomia.ui.dashboard.InteriorLayerIcon
import dev.panopt.autonomia.ui.dashboard.ProjectTriangleIcon
import dev.panopt.autonomia.ui.dashboard.VinculosLayerIcon
import dev.panopt.autonomia.ui.dashboard.WavesIcon

/**
 * Shared composable stamp used to render a layer icon at the given [size].
 * Used by both the full-screen config and the bottom-sheet config panel.
 */
@Composable
internal fun LayerStamp(
    layerId: String,
    color: Color,
    size: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(size.dp),
        contentAlignment = Alignment.Center,
    ) {
        val iconMod = Modifier.size((size - 2).dp)
        when {
            layerId.contains("interior") -> InteriorLayerIcon(color = color, modifier = iconMod)
            layerId.contains("cuerpo") -> WavesIcon(color = color, modifier = iconMod)
            layerId.contains("conducta") -> InfinityIcon(color = color, modifier = iconMod)
            layerId.contains("vinculos") -> VinculosLayerIcon(color = color, modifier = iconMod)
            layerId.contains("proyecto") -> ProjectTriangleIcon(color = color, modifier = iconMod)
        }
    }
}

/**
 * Smaller variant of [LayerStamp] at 20dp — used in compact panels and lists.
 */
@Composable
internal fun LayerStampSmall(
    layerId: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            layerId.contains("interior") -> InteriorLayerIcon(color = color, modifier = Modifier.size(18.dp))
            layerId.contains("cuerpo") -> WavesIcon(color = color, modifier = Modifier.size(18.dp))
            layerId.contains("conducta") -> InfinityIcon(color = color, modifier = Modifier.size(18.dp))
            layerId.contains("vinculos") -> VinculosLayerIcon(color = color, modifier = Modifier.size(18.dp))
            layerId.contains("proyecto") -> ProjectTriangleIcon(color = color, modifier = Modifier.size(18.dp))
        }
    }
}

/**
 * Maps a [layerId] to its corresponding colour from the [palette].
 * Returns [DashboardPalette.textMuted] when the layer cannot be identified.
 */
internal fun layerColor(layerId: String, palette: DashboardPalette): Color = when {
    layerId.contains("interior") -> palette.layerInterior
    layerId.contains("cuerpo") -> palette.layerBody
    layerId.contains("conducta") -> palette.layerConduct
    layerId.contains("vinculos") -> palette.layerVinculos
    layerId.contains("proyecto") -> palette.layerProject
    else -> palette.textMuted
}

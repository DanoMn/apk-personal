package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.ui.dashboard.InfinityIcon
import dev.panopt.autonomia.ui.dashboard.InteriorLayerIcon
import dev.panopt.autonomia.ui.dashboard.ProjectTriangleIcon
import dev.panopt.autonomia.ui.dashboard.VinculosLayerIcon
import dev.panopt.autonomia.ui.dashboard.WavesIcon

private val DashboardSans = FontFamily.SansSerif
private val DashboardSerif = FontFamily.Serif

@Composable
internal fun SectionHeader(
    palette: DashboardPalette,
    title: String,
    note: String,
    titleColor: Color = palette.colorCardboard,
    titleSize: Float = 19.84f,
) {
    Spacer(modifier = Modifier.height(21.6.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 11.52.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = titleColor,
            fontFamily = DashboardSerif,
            fontWeight = FontWeight.Medium,
            fontSize = titleSize.sp,
            lineHeight = (titleSize * 1.15f).sp,
        )
        Text(
            text = note,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 12.48.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun LayersSection(
    palette: DashboardPalette,
    layers: List<DashboardLayerState>,
) {
    SectionHeader(
        palette = palette,
        title = "Capas de hoy",
        note = "${layers.count { it.progress > 0f }} de ${layers.size} activas",
    )

    if (layers.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.2.dp),
    ) {
        layers.forEach { layer ->
            val color = layer.layerColor(palette)
            LayerPill(
                label = layer.name,
                color = color,
                progress = layer.progress,
                palette = palette,
                modifier = Modifier.weight(1f),
            ) { iconColor ->
                LayerIcon(layerId = layer.id, color = iconColor)
            }
        }
    }
}

@Composable
internal fun LayerPill(
    label: String,
    color: Color,
    progress: Float,
    palette: DashboardPalette,
    modifier: Modifier,
    icon: @Composable (Color) -> Unit,
) {
    Column(
        modifier = modifier
            .height(110.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon(color)
        }
        Text(
            text = label,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(99.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun LayerIcon(
    layerId: String,
    color: Color,
) {
    when (layerId) {
        "layer_interior" -> InteriorLayerIcon(color = color, modifier = Modifier.size(32.dp))
        "layer_cuerpo" -> WavesIcon(color = color, modifier = Modifier.size(32.dp))
        "layer_conducta" -> InfinityIcon(color = color, modifier = Modifier.size(32.dp))
        "layer_vinculos" -> VinculosLayerIcon(color = color, modifier = Modifier.size(32.dp))
        "layer_proyecto" -> ProjectTriangleIcon(color = color, modifier = Modifier.size(32.dp))
        else -> InfinityIcon(color = color, modifier = Modifier.size(32.dp))
    }
}

private fun DashboardLayerState.layerColor(palette: DashboardPalette): Color =
    when (id) {
        "layer_interior" -> palette.layerInterior
        "layer_cuerpo" -> palette.layerBody
        "layer_conducta" -> palette.layerConduct
        "layer_vinculos" -> palette.layerVinculos
        "layer_proyecto" -> palette.layerProject
        else -> palette.textMuted
    }

package dev.panopt.autonomia.ui.scoring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun ProgressTrack(
    palette: DashboardPalette,
    progress: Float,
    color: Color,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
    ) {
        val radius = size.height / 2f
        drawRoundRect(
            color = mix(palette.textMuted, 0.20f, palette.bgSurface2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = color,
            size = androidx.compose.ui.geometry.Size(
                width = size.width * progress.coerceIn(0f, 1f),
                height = size.height,
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius, radius),
        )
    }
}

internal fun scoreStateColor(
    palette: DashboardPalette,
    state: ScoreState,
): Color =
    when (state) {
        ScoreState.NoData -> palette.textMuted
        ScoreState.Restoration -> palette.risk
        ScoreState.Attention -> palette.colorCardboard
        ScoreState.Motion -> palette.stateMotion
        ScoreState.Plenitude -> palette.layerBody
        ScoreState.Unbreakable -> palette.colorCoral
    }

internal fun layerColor(
    palette: DashboardPalette,
    layerId: String,
): Color =
    when (layerId) {
        "layer_interior" -> palette.layerInterior
        "layer_cuerpo" -> palette.layerBody
        "layer_conducta" -> palette.layerConduct
        "layer_vinculos" -> palette.layerVinculos
        "layer_proyecto" -> palette.layerProject
        else -> palette.colorCoral
    }

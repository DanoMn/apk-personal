package dev.panopt.autonomia.ui.anchors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.InfinityIcon
import dev.panopt.autonomia.ui.dashboard.InteriorLayerIcon
import dev.panopt.autonomia.ui.dashboard.ProjectTriangleIcon
import dev.panopt.autonomia.ui.dashboard.VinculosLayerIcon
import dev.panopt.autonomia.ui.dashboard.WavesIcon
import dev.panopt.autonomia.ui.dashboard.mix

/**
 * Shared container for all config screens.
 * Provides consistent status bar padding, background, and horizontal insets.
 *
 * Usage:
 * ```
 * ConfigScreenContainer(palette) {
 *     // screen content here
 * }
 * ```
 */
@Composable
internal fun ConfigScreenContainer(
    palette: DashboardPalette,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp),
    ) {
        content()
    }
}

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

internal fun isCustomActivityId(activityId: String): Boolean =
    activityId.startsWith("act_custom_") || (!activityId.startsWith("act_") && !activityId.startsWith("sup_"))

@Composable
internal fun ConfirmDeleteActivityDialog(
    activityTitle: String,
    surfaceName: String,
    palette: DashboardPalette,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.bgElevated,
        titleContentColor = palette.colorCardboard,
        textContentColor = palette.textMain,
        title = {
            Text(
                text = "Eliminar actividad",
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            )
        },
        text = {
            Text(
                text = "Se eliminara \"$activityTitle\" de tus $surfaceName y de actividades disponibles.",
                fontFamily = DashboardSans,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = palette.textMuted,
            )
        },
        dismissButton = {
            DialogActionButton(
                text = "Cancelar",
                palette = palette,
                danger = false,
                onClick = onDismiss,
            )
        },
        confirmButton = {
            DialogActionButton(
                text = "Eliminar",
                palette = palette,
                danger = true,
                onClick = onConfirm,
            )
        },
    )
}

@Composable
private fun DialogActionButton(
    text: String,
    palette: DashboardPalette,
    danger: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (danger) mix(palette.risk, 0.22f, palette.bgSurface)
                else palette.bgSurface,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (danger) Color(0xFFF0B0A7) else palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
        )
    }
}

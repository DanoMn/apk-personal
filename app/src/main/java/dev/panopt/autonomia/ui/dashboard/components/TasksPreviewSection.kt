package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardTaskState
import dev.panopt.autonomia.ui.dashboard.CheckIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.InfinityIcon
import dev.panopt.autonomia.ui.dashboard.InteriorLayerIcon
import dev.panopt.autonomia.ui.dashboard.ProjectTriangleIcon
import dev.panopt.autonomia.ui.dashboard.VinculosLayerIcon
import dev.panopt.autonomia.ui.dashboard.WavesIcon
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun TasksPreviewSection(
    palette: DashboardPalette,
    tasks: List<DashboardTaskState>,
    onCompleteTask: (String) -> Unit,
    onOpenTasks: () -> Unit,
) {
    SectionHeader(
        palette = palette.copy(),
        title = "Pendientes",
        note = if (tasks.isEmpty()) "Sin pendientes" else "${tasks.size} abiertos",
        titleColor = palette.textMuted,
        titleSize = 15f,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(mix(palette.bgSurface, 0.6f, palette.bgBase))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (tasks.isEmpty()) {
            Text(
                text = "Sin pendientes",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
            )
        } else {
            tasks.take(4).forEach { task ->
                TaskPreviewRow(
                    task = task,
                    palette = palette,
                    onCompleteTask = onCompleteTask,
                )
            }
            if (tasks.size > 4) {
                Text(
                    text = "+${tasks.size - 4} más",
                    color = palette.textMuted.copy(alpha = 0.6f),
                    fontFamily = DashboardSans,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(start = 28.dp),
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgSurface2)
                .clickable(role = Role.Button, onClick = onOpenTasks),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Abrir pendientes",
                color = palette.colorCardboard,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
            )
        }
    }
}

@Composable
private fun TaskPreviewRow(
    task: DashboardTaskState,
    palette: DashboardPalette,
    onCompleteTask: (String) -> Unit,
) {
    var completedClicked by remember(task.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        TaskLayerIcon(
            layerId = task.layerId,
            palette = palette,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = task.title,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (completedClicked) palette.colorCoral.copy(alpha = 0.2f) else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = if (completedClicked) palette.colorCoral else palette.textMuted.copy(alpha = 0.4f),
                    shape = CircleShape,
                )
                .clickable(role = Role.Checkbox) {
                    completedClicked = true
                    onCompleteTask(task.id)
                },
            contentAlignment = Alignment.Center,
        ) {
            if (completedClicked) {
                CheckIcon(color = palette.colorCoral, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun TaskLayerIcon(
    layerId: String?,
    palette: DashboardPalette,
    modifier: Modifier = Modifier,
) {
    val color = when (layerId) {
        "layer_interior" -> palette.layerInterior
        "layer_cuerpo" -> palette.layerBody
        "layer_conducta" -> palette.layerConduct
        "layer_vinculos" -> palette.layerVinculos
        "layer_proyecto" -> palette.layerProject
        else -> palette.textMuted.copy(alpha = 0.35f)
    }

    when (layerId) {
        "layer_interior" -> InteriorLayerIcon(color = color, modifier = modifier)
        "layer_cuerpo" -> WavesIcon(color = color, modifier = modifier)
        "layer_conducta" -> InfinityIcon(color = color, modifier = modifier)
        "layer_vinculos" -> VinculosLayerIcon(color = color, modifier = modifier)
        "layer_proyecto" -> ProjectTriangleIcon(color = color, modifier = modifier)
        else -> Box(
            modifier = modifier
                .clip(CircleShape)
                .background(color),
        )
    }
}

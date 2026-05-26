package dev.panopt.autonomia.ui.tasks

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.domain.dashboard.DashboardTaskState
import dev.panopt.autonomia.ui.dashboard.CheckIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.InfinityIcon
import dev.panopt.autonomia.ui.dashboard.InteriorLayerIcon
import dev.panopt.autonomia.ui.dashboard.ProjectTriangleIcon
import dev.panopt.autonomia.ui.dashboard.VinculosLayerIcon
import dev.panopt.autonomia.ui.dashboard.WavesIcon
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix

private const val FILTER_ALL = "all"
private const val FILTER_NONE = "none"

@Composable
internal fun TasksScreen(
    pendingTasks: List<DashboardTaskState>,
    completedTasks: List<DashboardTaskState>,
    layers: List<DashboardLayerState>,
    palette: DashboardPalette,
    onCreateTask: (title: String, layerId: String?) -> Unit,
    onCompleteTask: (taskId: String) -> Unit,
    onReactivateTask: (taskId: String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var title by remember { mutableStateOf("") }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var selectedFilterId by remember { mutableStateOf(FILTER_ALL) }

    val filteredPending = pendingTasks.filterByLayer(selectedFilterId)
    val filteredCompleted = completedTasks.filterByLayer(selectedFilterId)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .statusBarsPadding(),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.colorCoral.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    center = Offset(x = size.width * 0.2f, y = size.height * 0.05f),
                    radius = 480.dp.toPx(),
                ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp),
        ) {
            Header(
                pendingCount = pendingTasks.size,
                completedCount = completedTasks.size,
                palette = palette,
                onBack = onBack,
            )

            Spacer(modifier = Modifier.height(16.dp))

            LayerFilterRow(
                layers = layers,
                selectedFilterId = selectedFilterId,
                palette = palette,
                onSelect = { selectedFilterId = it },
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CreateTaskBlock(
                    title = title,
                    selectedLayerId = selectedLayerId,
                    layers = layers,
                    palette = palette,
                    onTitleChange = { title = it.take(120) },
                    onLayerSelected = { layerId ->
                        selectedLayerId = if (selectedLayerId == layerId) null else layerId
                    },
                    onCreate = {
                        val finalTitle = title.trim()
                        if (finalTitle.isNotBlank()) {
                            onCreateTask(finalTitle, selectedLayerId)
                            title = ""
                            selectedLayerId = null
                        }
                    },
                )

                TaskSectionTitle(
                    title = "Abiertos",
                    note = if (filteredPending.isEmpty()) "Sin pendientes" else "${filteredPending.size} pendientes",
                    palette = palette,
                )

                if (filteredPending.isEmpty()) {
                    EmptyState(
                        text = "No hay pendientes abiertos en este filtro.",
                        palette = palette,
                    )
                } else {
                    filteredPending.forEach { task ->
                        PendingTaskRow(
                            task = task,
                            palette = palette,
                            onComplete = { onCompleteTask(task.id) },
                        )
                    }
                }

                TaskSectionTitle(
                    title = "Completados",
                    note = if (filteredCompleted.isEmpty()) "Sin historial" else "${filteredCompleted.size} guardados",
                    palette = palette,
                )

                if (filteredCompleted.isEmpty()) {
                    EmptyState(
                        text = "Lo completado quedará guardado acá.",
                        palette = palette,
                    )
                } else {
                    filteredCompleted.forEach { task ->
                        CompletedTaskRow(
                            task = task,
                            palette = palette,
                            onReactivate = { onReactivateTask(task.id) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Header(
    pendingCount: Int,
    completedCount: Int,
    palette: DashboardPalette,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.bgSurface)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            XIcon(color = palette.textMain)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Pendientes",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 28.sp,
            )
            Text(
                text = "$pendingCount abiertos · $completedCount completados",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun LayerFilterRow(
    layers: List<DashboardLayerState>,
    selectedFilterId: String,
    palette: DashboardPalette,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            label = "Todas",
            selected = selectedFilterId == FILTER_ALL,
            palette = palette,
            onClick = { onSelect(FILTER_ALL) },
        )
        FilterChip(
            label = "Sin capa",
            selected = selectedFilterId == FILTER_NONE,
            palette = palette,
            onClick = { onSelect(FILTER_NONE) },
        )
        layers.forEach { layer ->
            FilterChip(
                label = layer.name,
                selected = selectedFilterId == layer.id,
                palette = palette,
                onClick = { onSelect(layer.id) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) palette.colorCardboard else palette.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) palette.bgBase else palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun CreateTaskBlock(
    title: String,
    selectedLayerId: String?,
    layers: List<DashboardLayerState>,
    palette: DashboardPalette,
    onTitleChange: (String) -> Unit,
    onLayerSelected: (String) -> Unit,
    onCreate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.bgSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicTextField(
            value = title,
            onValueChange = onTitleChange,
            textStyle = TextStyle(
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontSize = 15.sp,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgSurface2)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (title.isEmpty()) {
                        Text(
                            text = "Qué tienes pendiente?",
                            color = palette.textFaint,
                            fontFamily = DashboardSans,
                            fontSize = 15.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            layers.forEach { layer ->
                val isSelected = selectedLayerId == layer.id
                val layerColor = layerColor(layer.id, palette)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) mix(layerColor, 0.15f, palette.bgSurface2) else palette.bgSurface2)
                        .clickable(role = Role.Button) { onLayerSelected(layer.id) },
                    contentAlignment = Alignment.Center,
                ) {
                    TaskLayerIcon(
                        layerId = layer.id,
                        palette = palette,
                        selected = isSelected,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (title.isNotBlank()) palette.colorCardboard else palette.bgSurface2)
                .clickable(role = Role.Button, enabled = title.isNotBlank(), onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Crear pendiente",
                color = if (title.isNotBlank()) palette.bgBase else palette.textFaint,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun TaskSectionTitle(
    title: String,
    note: String,
    palette: DashboardPalette,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
        )
        Text(
            text = note,
            color = palette.textFaint,
            fontFamily = DashboardSans,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun PendingTaskRow(
    task: DashboardTaskState,
    palette: DashboardPalette,
    onComplete: () -> Unit,
) {
    var completedClicked by remember(task.id) { mutableStateOf(false) }

    TaskRowShell(
        task = task,
        palette = palette,
        trailing = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (completedClicked) palette.colorCoral.copy(alpha = 0.2f) else Color.Transparent)
                    .border(
                        width = 1.5.dp,
                        color = if (completedClicked) palette.colorCoral else palette.textMuted.copy(alpha = 0.4f),
                        shape = CircleShape,
                    )
                    .clickable(role = Role.Checkbox) {
                        completedClicked = true
                        onComplete()
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (completedClicked) {
                    CheckIcon(color = palette.colorCoral, modifier = Modifier.size(16.dp))
                }
            }
        },
    )
}

@Composable
private fun CompletedTaskRow(
    task: DashboardTaskState,
    palette: DashboardPalette,
    onReactivate: () -> Unit,
) {
    TaskRowShell(
        task = task,
        palette = palette,
        titleColor = palette.textMuted,
        trailing = {
            Box(
                modifier = Modifier
                    .height(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.bgSurface2)
                    .clickable(role = Role.Button, onClick = onReactivate)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Revivir",
                    color = palette.colorCardboard,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                )
            }
        },
    )
}

@Composable
private fun TaskRowShell(
    task: DashboardTaskState,
    palette: DashboardPalette,
    titleColor: Color = palette.textMain,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TaskLayerIcon(
            layerId = task.layerId,
            palette = palette,
            selected = true,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            color = titleColor,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        trailing()
    }
}

@Composable
private fun EmptyState(
    text: String,
    palette: DashboardPalette,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun TaskLayerIcon(
    layerId: String?,
    palette: DashboardPalette,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) {
        layerColor(layerId, palette)
    } else {
        mix(layerColor(layerId, palette), 0.4f, palette.bgSurface2)
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
                .background(color.copy(alpha = 0.5f)),
        )
    }
}

private fun List<DashboardTaskState>.filterByLayer(filterId: String): List<DashboardTaskState> =
    when (filterId) {
        FILTER_ALL -> this
        FILTER_NONE -> filter { it.layerId == null }
        else -> filter { it.layerId == filterId }
    }

private fun layerColor(layerId: String?, palette: DashboardPalette): Color =
    when (layerId) {
        "layer_interior" -> palette.layerInterior
        "layer_cuerpo" -> palette.layerBody
        "layer_conducta" -> palette.layerConduct
        "layer_vinculos" -> palette.layerVinculos
        "layer_proyecto" -> palette.layerProject
        else -> palette.textMuted
    }

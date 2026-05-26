package dev.panopt.autonomia.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.domain.dashboard.DashboardSleepState
import dev.panopt.autonomia.domain.dashboard.DashboardSobrietyTrackState
import dev.panopt.autonomia.domain.dashboard.DashboardState
import dev.panopt.autonomia.domain.dashboard.DashboardTaskState
import dev.panopt.autonomia.ui.dashboard.components.CheckItem
import dev.panopt.autonomia.ui.dashboard.components.AnchorConfigPanel
import dev.panopt.autonomia.ui.supports.SupportsConfigPanel

internal enum class DashboardSheet {
    EntryMenu,
    Anchor,
    Support,
    Sleep,
    Tasks,
    Activities,
    AnchorConfig,
    SupportsConfig,
    Relapse,
}

@Composable
internal fun DashboardSheetHost(
    sheet: DashboardSheet,
    state: DashboardState,
    palette: DashboardPalette,
    onDismiss: () -> Unit,
    onSwitchSheet: (DashboardSheet) -> Unit,
    onToggleActivity: (String, Boolean) -> Unit,
    onSaveActivityValue: (String, Int) -> Unit,
    onSaveSleep: (String, String, String, String, SleepQuality, String) -> Unit,
    onToggleRelapse: (String, Boolean) -> Unit,
    onCreateActivity: (String, String, Int, Boolean, Int?, Int?) -> Unit,
    onSetFocusSignal: (String) -> Unit,
    onAddAnchor: (String, Int, Int, Int?) -> Unit,
    onRemoveAnchor: (String) -> Unit,
    onCreateTask: (String, String?, Boolean) -> Unit,
    onCompleteTask: (String) -> Unit,
    onNavigateToAnchorConfig: () -> Unit,
    onAddSupport: (String) -> Unit = {},
    onRemoveSupport: (String) -> Unit = {},
    onOpenFullSupportsConfig: () -> Unit = {},
) {
    BackHandler {
        if (sheet == DashboardSheet.AnchorConfig) {
            onSwitchSheet(DashboardSheet.EntryMenu)
        } else {
            onDismiss()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.BottomCenter,
    ) {
        val maxSheetHeight = maxHeight * 0.94f
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxSheetHeight)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(palette.drawer)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp, bottom = 18.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, dragAmount ->
                            if (dragAmount > 20f) {
                                onDismiss()
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 48.dp, height = 4.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                )
            }

            when (sheet) {
                DashboardSheet.EntryMenu -> EntryMenuPanel(
                    palette = palette,
                    onOpenAnchors = { onSwitchSheet(DashboardSheet.AnchorConfig) },
                    onOpenSupports = { onSwitchSheet(DashboardSheet.SupportsConfig) },
                    onOpenTasks = { onSwitchSheet(DashboardSheet.Tasks) },
                    onOpenActivities = { onSwitchSheet(DashboardSheet.Activities) },
                    onOpenRelapse = { onSwitchSheet(DashboardSheet.Relapse) },
                )
                DashboardSheet.Anchor -> AnchorPanel(
                    title = "Registrar ancla",
                    items = state.anchorItems,
                    activityOptions = state.activityOptions.filter {
                        !it.isGoal && it.activityType == "Anchor"
                    },
                    palette = palette,
                    onToggleActivity = onToggleActivity,
                    onSaveActivityValue = onSaveActivityValue,
                    onOpenActivities = {
                        onDismiss()
                        onNavigateToAnchorConfig()
                    },
                )
                DashboardSheet.Support -> AnchorPanel(
                    title = "Cuidado base",
                    items = state.supportItems,
                    activityOptions = state.activityOptions.filter {
                        !it.isGoal && it.activityType == "Support"
                    },
                    palette = palette,
                    onToggleActivity = onToggleActivity,
                    onSaveActivityValue = onSaveActivityValue,
                    onOpenActivities = { onSwitchSheet(DashboardSheet.Activities) },
                )
                DashboardSheet.Sleep -> SleepPanel(
                    sleep = state.sleep,
                    palette = palette,
                    onSave = { plannedSleepAt, plannedWakeAt, sleptAt, wokeAt, quality, note ->
                        onSaveSleep(plannedSleepAt, plannedWakeAt, sleptAt, wokeAt, quality, note)
                        onDismiss()
                    },
                )
                DashboardSheet.Tasks -> TasksPanel(
                    tasks = state.pendingTasks,
                    layers = state.layers,
                    palette = palette,
                    onCreateTask = onCreateTask,
                    onCompleteTask = onCompleteTask,
                )
                DashboardSheet.Activities -> ActivitySettingsPanel(
                    state = state,
                    palette = palette,
                    onToggleActivity = onToggleActivity,
                    onSaveActivityValue = onSaveActivityValue,
                    onCreateActivity = onCreateActivity,
                    onSetFocusSignal = onSetFocusSignal,
                )
                DashboardSheet.AnchorConfig -> AnchorConfigPanel(
                    activityOptions = state.activityOptions,
                    palette = palette,
                    onAddAnchor = onAddAnchor,
                    onOpenFullAnchorConfig = {
                        onDismiss()
                        onNavigateToAnchorConfig()
                    },
                )
                DashboardSheet.SupportsConfig -> SupportsConfigPanel(
                    activityOptions = state.activityOptions,
                    palette = palette,
                    onRemoveSupport = onRemoveSupport,
                    onOpenFullConfig = onOpenFullSupportsConfig,
                )
                DashboardSheet.Relapse -> RelapsePanel(
                    tracks = state.sobrietyTracks,
                    palette = palette,
                    onToggleRelapse = onToggleRelapse,
                )
            }
        }
    }
}

@Composable
private fun AnchorPanel(
    title: String,
    items: List<DashboardCheckItemState>,
    activityOptions: List<DashboardActivityOptionState>,
    palette: DashboardPalette,
    onToggleActivity: (String, Boolean) -> Unit,
    onSaveActivityValue: (String, Int) -> Unit,
    onOpenActivities: () -> Unit,
) {
    SheetTitle(title = title, note = "hechos de hoy", palette = palette)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items.forEach { item ->
            CheckItem(
                palette = palette,
                item = item,
                checked = item.completed,
                onToggle = { onToggleActivity(item.id, !item.completed) },
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        SheetSubtitle(text = "Registrar tiempo", palette = palette)
        activityOptions.filter { it.targetValue > 0 }.forEach { activity ->
            ActivityValueRow(
                activity = activity,
                palette = palette,
                onSaveActivityValue = onSaveActivityValue,
                onToggleActivity = onToggleActivity,
            )
        }

        SheetButton(
            text = "Configurar actividades",
            palette = palette,
            primary = false,
            onClick = onOpenActivities,
        )
    }
}

@Composable
private fun EntryMenuPanel(
    palette: DashboardPalette,
    onOpenAnchors: () -> Unit,
    onOpenSupports: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenRelapse: () -> Unit,
) {
    SheetTitle(title = "Configuración rápida", note = "hechos y registro", palette = palette)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EntryGridCard(
                palette = palette,
                icon = { AnchorIcon(color = it, modifier = Modifier.size(28.dp)) },
                label = "Anclas",
                description = "Ajustar anclas",
                onClick = onOpenAnchors,
                modifier = Modifier.weight(1f),
            )
            EntryGridCard(
                palette = palette,
                icon = { GlassWaterIcon(color = it, modifier = Modifier.size(28.dp)) },
                label = "Cuidado",
                description = "Cuidado base",
                onClick = onOpenSupports,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EntryGridCard(
                palette = palette,
                icon = { ListTodoIcon(color = it, modifier = Modifier.size(28.dp)) },
                label = "Pendientes",
                description = "Tareas abiertas",
                onClick = onOpenTasks,
                modifier = Modifier.weight(1f),
            )
            EntryGridCard(
                palette = palette,
                icon = { BarChartIcon(color = it) },
                label = "Actividades",
                description = "Metas y señales",
                onClick = onOpenActivities,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EntryGridCard(
                palette = palette,
                icon = { FlagIcon(color = it, modifier = Modifier.size(28.dp)) },
                label = "Recaídas",
                description = "Registrar recaída",
                onClick = onOpenRelapse,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EntryGridCard(
    palette: DashboardPalette,
    icon: @Composable (Color) -> Unit,
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(72.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description }
            .padding(8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            icon(palette.textMain)
            Text(
                text = label,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ActivityValueRow(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onSaveActivityValue: (String, Int) -> Unit,
    onToggleActivity: (String, Boolean) -> Unit,
) {
    var valueText by remember(activity.id, activity.actualValue) {
        mutableStateOf(activity.actualValue.coerceAtLeast(activity.targetValue).toString())
    }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = activity.layerName,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.5.sp,
            )
        }
        PanelTextField(
            value = valueText,
            onValueChange = { valueText = it.filter(Char::isDigit).take(4) },
            palette = palette,
            modifier = Modifier
                .size(width = 62.dp, height = 42.dp)
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        onSaveActivityValue(activity.id, valueText.toIntOrNull() ?: activity.targetValue)
                    }
                },
            keyboardType = KeyboardType.Number,
            onDone = {
                onSaveActivityValue(activity.id, valueText.toIntOrNull() ?: activity.targetValue)
                focusManager.clearFocus()
            }
        )
    }
}

@Composable
private fun SleepPanel(
    sleep: DashboardSleepState,
    palette: DashboardPalette,
    onSave: (String, String, String, String, SleepQuality, String) -> Unit,
) {
    var plannedSleepAt by remember(sleep.plannedSleepAt) { mutableStateOf(sleep.plannedSleepAt) }
    var plannedWakeAt by remember(sleep.plannedWakeAt) { mutableStateOf(sleep.plannedWakeAt) }
    var sleptAt by remember(sleep.sleptAt) { mutableStateOf(sleep.sleptAt.ifBlank { "00:00" }) }
    var wokeAt by remember(sleep.wokeAt) { mutableStateOf(sleep.wokeAt.ifBlank { "07:00" }) }
    var quality by remember(sleep.quality) { mutableStateOf(sleep.quality) }
    var note by remember(sleep.note) { mutableStateOf(sleep.note) }

    SheetTitle(title = "Sueno", note = "senal de descanso", palette = palette)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SheetSubtitle(text = "Plan", palette = palette)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelField("Dormir", plannedSleepAt, { plannedSleepAt = it.take(5) }, palette, Modifier.weight(1f))
            PanelField("Despertar", plannedWakeAt, { plannedWakeAt = it.take(5) }, palette, Modifier.weight(1f))
        }
        SheetSubtitle(text = "Registro", palette = palette)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelField("Dormi", sleptAt, { sleptAt = it.take(5) }, palette, Modifier.weight(1f))
            PanelField("Desperte", wokeAt, { wokeAt = it.take(5) }, palette, Modifier.weight(1f))
        }
        SheetSubtitle(text = "Calidad", palette = palette)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QualityChip("Baja", SleepQuality.Low, quality, palette, Modifier.weight(1f)) { quality = it }
            QualityChip("Aceptable", SleepQuality.Acceptable, quality, palette, Modifier.weight(1f)) { quality = it }
            QualityChip("Buena", SleepQuality.Good, quality, palette, Modifier.weight(1f)) { quality = it }
        }
        PanelField("Nota", note, { note = it.take(160) }, palette, Modifier.fillMaxWidth())
        SheetButton(
            text = "Guardar sueno",
            palette = palette,
            primary = true,
            onClick = { onSave(plannedSleepAt, plannedWakeAt, sleptAt, wokeAt, quality, note) },
        )
    }
}

@Composable
private fun TasksPanel(
    tasks: List<DashboardTaskState>,
    layers: List<DashboardLayerState>,
    palette: DashboardPalette,
    onCreateTask: (String, String?, Boolean) -> Unit,
    onCompleteTask: (String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    SheetTitle(title = "Pendientes", note = "${tasks.size} abiertos", palette = palette)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tasks.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSurface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (task.layerId != null) {
                    val layerColor = getLayerColor(task.layerId, palette)
                    Box(
                        modifier = Modifier.size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LayerIcon(layerId = task.layerId, color = layerColor, modifier = Modifier.size(20.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Text(
                    text = task.title,
                    modifier = Modifier.weight(1f),
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                var completedClicked by remember(task.id) { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (completedClicked) palette.colorCoral.copy(alpha = 0.2f) else Color.Transparent)
                        .clickable {
                            completedClicked = true
                            onCompleteTask(task.id)
                        }
                        .border(
                            width = 1.5.dp,
                            color = if (completedClicked) palette.colorCoral else palette.textMuted.copy(alpha = 0.4f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (completedClicked) {
                        CheckIcon(color = palette.colorCoral, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        PanelField("Nuevo pendiente", title, { title = it }, palette, Modifier.fillMaxWidth())
        
        SheetSubtitle(text = "Asociar a capa", palette = palette)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val layerIds = listOf("layer_interior", "layer_cuerpo", "layer_conducta", "layer_vinculos", "layer_proyecto")
            layerIds.forEach { id ->
                val isSelected = selectedLayerId == id
                val baseColor = getLayerColor(id, palette)
                val iconColor = if (isSelected) baseColor else mix(baseColor, 0.4f, palette.bgSurface2)
                val backgroundColor = if (isSelected) mix(baseColor, 0.15f, palette.bgSurface2) else palette.bgSurface2

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(backgroundColor)
                        .clickable {
                            selectedLayerId = if (isSelected) null else id
                        },
                    contentAlignment = Alignment.Center
                ) {
                    LayerIcon(layerId = id, color = iconColor, modifier = Modifier.size(22.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        SheetButton(
            text = "Agregar pendiente",
            palette = palette,
            primary = true,
            onClick = {
                val finalTitle = title.trim()
                if (finalTitle.isNotBlank()) {
                    onCreateTask(finalTitle, selectedLayerId, selectedLayerId != null)
                    title = ""
                    selectedLayerId = null
                }
            },
        )
    }
}

@Composable
private fun LayerIcon(layerId: String, color: Color, modifier: Modifier = Modifier) {
    when (layerId) {
        "layer_interior" -> InteriorLayerIcon(color = color, modifier = modifier)
        "layer_cuerpo" -> WavesIcon(color = color, modifier = modifier)
        "layer_conducta" -> InfinityIcon(color = color, modifier = modifier)
        "layer_vinculos" -> VinculosLayerIcon(color = color, modifier = modifier)
        "layer_proyecto" -> ProjectTriangleIcon(color = color, modifier = modifier)
    }
}

private fun getLayerColor(layerId: String, palette: DashboardPalette): Color {
    return when (layerId) {
        "layer_interior" -> palette.layerInterior
        "layer_cuerpo" -> palette.layerBody
        "layer_conducta" -> palette.layerConduct
        "layer_vinculos" -> palette.layerVinculos
        "layer_proyecto" -> palette.layerProject
        else -> palette.textMuted
    }
}

@Composable
private fun ActivitySettingsPanel(
    state: DashboardState,
    palette: DashboardPalette,
    onToggleActivity: (String, Boolean) -> Unit,
    onSaveActivityValue: (String, Int) -> Unit,
    onCreateActivity: (String, String, Int, Boolean, Int?, Int?) -> Unit,
    onSetFocusSignal: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("20") }
    var layerId by remember(state.layers) { mutableStateOf(state.layers.firstOrNull()?.id.orEmpty()) }
    var isSecondary by remember { mutableStateOf(false) }

    SheetTitle(title = "Actividades", note = "anclas, soportes y senales", palette = palette)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SheetSubtitle(text = "Senal destacada", palette = palette)
        state.activityOptions.filter { !it.isGoal && it.targetValue > 0 }.forEach { activity ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activity.title,
                        color = palette.textMain,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = activity.layerName,
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontSize = 12.5.sp,
                    )
                }
                SheetMiniButton(
                    text = if (activity.isFocusSignal) "Elegida" else "Usar",
                    palette = palette,
                    onClick = { onSetFocusSignal(activity.id) },
                )
            }
        }

        SheetSubtitle(text = "Metas activas", palette = palette)
        val goals = state.activityOptions.filter { it.isGoal && it.targetValue > 0 }
        if (goals.isEmpty()) {
            Text(
                text = "Sin metas configuradas",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
        } else {
            goals.forEach { activity ->
                ActivityValueRow(
                    activity = activity,
                    palette = palette,
                    onSaveActivityValue = onSaveActivityValue,
                    onToggleActivity = onToggleActivity,
                )
            }
        }

        SheetSubtitle(text = "Agregar actividad", palette = palette)
        PanelField("Nombre", name, { name = it }, palette, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            state.layers.forEach { layer ->
                val selected = layer.id == layerId
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) palette.colorCardboard else palette.bgSurface)
                        .clickable { layerId = layer.id },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = layer.name.take(4),
                        color = if (selected) palette.bgBase else palette.textMuted,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PanelField(
                label = "Minutos",
                value = minutes,
                onValueChange = { minutes = it.filter(Char::isDigit).take(4) },
                palette = palette,
                modifier = Modifier.weight(1f),
                keyboardType = KeyboardType.Number,
            )
            Checkbox(checked = isSecondary, onCheckedChange = { isSecondary = it })
            Text(
                text = "Secundaria",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
        }
        SheetButton(
            text = "Agregar actividad",
            palette = palette,
            primary = true,
            onClick = {
                onCreateActivity(
                    name,
                    layerId,
                    minutes.toIntOrNull() ?: 20,
                    isSecondary,
                    if (isSecondary) null else 3,
                    null,
                )
                name = ""
            },
        )
    }
}

@Composable
private fun RelapsePanel(
    tracks: List<DashboardSobrietyTrackState>,
    palette: DashboardPalette,
    onToggleRelapse: (String, Boolean) -> Unit,
) {
    SheetTitle(title = "Recaidas", note = "sobriedad definida", palette = palette)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tracks.forEach { track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSurface)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.label,
                        color = palette.textMain,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp,
                    )
                    Text(
                        text = track.meta,
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontSize = 12.5.sp,
                    )
                }
                SheetMiniButton(
                    text = if (track.isRelapseToday) "Desmarcar" else "Registrar",
                    palette = palette,
                    onClick = { onToggleRelapse(track.id, track.isRelapseToday) },
                )
            }
        }
    }
}

@Composable
private fun SheetTitle(
    title: String,
    note: String,
    palette: DashboardPalette,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            color = palette.colorCardboard,
            fontFamily = DashboardSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 24.sp,
        )
        Text(
            text = note,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 12.5.sp,
        )
    }
}

@Composable
private fun SheetSubtitle(
    text: String,
    palette: DashboardPalette,
) {
    Text(
        text = text,
        color = palette.textMuted,
        fontFamily = DashboardSans,
        fontWeight = FontWeight.Bold,
        fontSize = 12.5.sp,
        modifier = Modifier.padding(top = 4.dp),
    )
}

@Composable
private fun PanelField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    palette: DashboardPalette,
    modifier: Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 5.dp),
        )
        PanelTextField(
            value = value,
            onValueChange = onValueChange,
            palette = palette,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            keyboardType = keyboardType,
        )
    }
}

@Composable
private fun PanelTextField(
    value: String,
    onValueChange: (String) -> Unit,
    palette: DashboardPalette,
    modifier: Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    onDone: (() -> Unit)? = null,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = palette.textMain,
            fontFamily = DashboardSans,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = if (onDone != null) androidx.compose.ui.text.input.ImeAction.Done else androidx.compose.ui.text.input.ImeAction.Default
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onDone = { onDone?.invoke() }
        ),
        singleLine = true,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.bgSurface2)
            .padding(horizontal = 4.dp, vertical = 12.dp),
    )
}

@Composable
private fun QualityChip(
    text: String,
    value: SleepQuality,
    selected: SleepQuality,
    palette: DashboardPalette,
    modifier: Modifier,
    onClick: (SleepQuality) -> Unit,
) {
    val active = value == selected
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) mix(palette.colorCoral, 0.2f, palette.bgSurface2) else palette.bgSurface2)
            .clickable { onClick(value) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (active) Color(0xFFEFAA9C) else palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SheetButton(
    text: String,
    palette: DashboardPalette,
    primary: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) palette.colorCardboard else palette.bgSurface)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) palette.bgBase else palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun SheetMiniButton(
    text: String,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(palette.colorCardboard)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = palette.bgBase,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            maxLines = 1,
        )
    }
}

// -- Supports config panel (bottom sheet) --

@Composable
private fun SupportsConfigPanel(
    activityOptions: List<DashboardActivityOptionState>,
    palette: DashboardPalette,
) {
    val currentSupports = activityOptions.filter { it.activityType == "Support" }

    SheetTitle(title = "Soportes", note = "${currentSupports.size} activos", palette = palette)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (currentSupports.isEmpty()) {
            Text(
                text = "Sin soportes configurados.\nUsa la pantalla de configuración para agregar.",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            currentSupports.forEach { support ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = support.title,
                            color = palette.textMain,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = support.layerName,
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

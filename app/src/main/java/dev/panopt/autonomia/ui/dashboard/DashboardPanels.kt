package dev.panopt.autonomia.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardChecklistItemState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.domain.dashboard.DashboardSleepState
import dev.panopt.autonomia.domain.dashboard.DashboardSobrietyTrackState
import dev.panopt.autonomia.domain.dashboard.DashboardState
import dev.panopt.autonomia.domain.dashboard.DashboardTaskState
import dev.panopt.autonomia.ui.dashboard.components.CheckItem

internal enum class DashboardSheet {
    EntryMenu,
    Checklist,
    SecondaryChecklist,
    Sleep,
    Tasks,
    Activities,
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
    onCreateActivity: (String, String, Int, Boolean, Boolean, Boolean) -> Unit,
    onSetFocusSignal: (String) -> Unit,
    onCreateTask: (String, String?, Boolean) -> Unit,
    onCompleteTask: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.48f))
            .clickable(role = Role.Button, onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 680.dp)
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(palette.drawer)
                .clickable(onClick = {})
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 48.dp, height = 4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .align(Alignment.CenterHorizontally),
            )
            Spacer(modifier = Modifier.height(14.dp))

            when (sheet) {
                DashboardSheet.EntryMenu -> EntryMenuPanel(
                    palette = palette,
                    onOpenChecklist = { onSwitchSheet(DashboardSheet.Checklist) },
                    onOpenSecondaryChecklist = { onSwitchSheet(DashboardSheet.SecondaryChecklist) },
                    onOpenTasks = { onSwitchSheet(DashboardSheet.Tasks) },
                    onOpenActivities = { onSwitchSheet(DashboardSheet.Activities) },
                    onOpenRelapse = { onSwitchSheet(DashboardSheet.Relapse) },
                )
                DashboardSheet.Checklist -> ChecklistPanel(
                    title = "Registrar checklist",
                    items = state.checklistItems,
                    activityOptions = state.activityOptions.filter {
                        !it.isGoal && it.displaySurface == "PrimaryChecklist"
                    },
                    palette = palette,
                    onToggleActivity = onToggleActivity,
                    onSaveActivityValue = onSaveActivityValue,
                    onOpenActivities = { onSwitchSheet(DashboardSheet.Activities) },
                )
                DashboardSheet.SecondaryChecklist -> ChecklistPanel(
                    title = "Checklist secundaria",
                    items = state.secondaryChecklistItems,
                    activityOptions = state.activityOptions.filter {
                        !it.isGoal && it.displaySurface == "SecondaryChecklist"
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
private fun ChecklistPanel(
    title: String,
    items: List<DashboardChecklistItemState>,
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
    onOpenChecklist: () -> Unit,
    onOpenSecondaryChecklist: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenRelapse: () -> Unit,
) {
    SheetTitle(title = "Registrar", note = "hechos y configuracion", palette = palette)

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SheetButton(text = "Checklist principal", palette = palette, primary = true, onClick = onOpenChecklist)
        SheetButton(text = "Checklist secundaria", palette = palette, primary = false, onClick = onOpenSecondaryChecklist)
        SheetButton(text = "Pendientes puntuales", palette = palette, primary = false, onClick = onOpenTasks)
        SheetButton(text = "Actividades, proyectos y goals", palette = palette, primary = false, onClick = onOpenActivities)
        SheetButton(text = "Recaidas de sobriedad", palette = palette, primary = false, onClick = onOpenRelapse)
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
            modifier = Modifier.size(width = 62.dp, height = 42.dp),
            keyboardType = KeyboardType.Number,
        )
        SheetMiniButton(
            text = "Guardar",
            palette = palette,
            onClick = { onSaveActivityValue(activity.id, valueText.toIntOrNull() ?: activity.targetValue) },
        )
        SheetMiniButton(
            text = if (activity.isCompletedToday) "Quitar" else "Meta",
            palette = palette,
            onClick = { onToggleActivity(activity.id, !activity.isCompletedToday) },
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
    var contributesToCore by remember { mutableStateOf(false) }
    var layerId by remember(layers) { mutableStateOf(layers.firstOrNull()?.id.orEmpty()) }
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
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
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
                SheetMiniButton(text = "Listo", palette = palette) { onCompleteTask(task.id) }
            }
        }
        PanelField("Nuevo pendiente", title, { title = it }, palette, Modifier.fillMaxWidth())
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Checkbox(checked = contributesToCore, onCheckedChange = { contributesToCore = it })
            Text(
                text = "Aporta al core",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
        }
        if (contributesToCore) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                layers.forEach { layer ->
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
        }
        SheetButton(
            text = "Agregar pendiente",
            palette = palette,
            primary = true,
            onClick = {
                onCreateTask(title, layerId.takeIf { contributesToCore }, contributesToCore)
                title = ""
            },
        )
    }
}

@Composable
private fun ActivitySettingsPanel(
    state: DashboardState,
    palette: DashboardPalette,
    onToggleActivity: (String, Boolean) -> Unit,
    onSaveActivityValue: (String, Int) -> Unit,
    onCreateActivity: (String, String, Int, Boolean, Boolean, Boolean) -> Unit,
    onSetFocusSignal: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var minutes by remember { mutableStateOf("20") }
    var layerId by remember(state.layers) { mutableStateOf(state.layers.firstOrNull()?.id.orEmpty()) }
    var isSecondary by remember { mutableStateOf(false) }
    var isGoal by remember { mutableStateOf(false) }
    var isMonthlyGoal by remember { mutableStateOf(false) }

    SheetTitle(title = "Actividades", note = "checklist, goals y senales", palette = palette)

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

        SheetSubtitle(text = "Goals activos", palette = palette)
        val goals = state.activityOptions.filter { it.isGoal && it.targetValue > 0 }
        if (goals.isEmpty()) {
            Text(
                text = "Sin goals configurados",
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Checkbox(checked = isGoal, onCheckedChange = { isGoal = it })
            Text(
                text = "Goal",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
            Checkbox(
                checked = isMonthlyGoal,
                onCheckedChange = { isMonthlyGoal = it },
                enabled = isGoal,
            )
            Text(
                text = "Mensual",
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
                onCreateActivity(name, layerId, minutes.toIntOrNull() ?: 20, isSecondary, isGoal, isMonthlyGoal)
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
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = androidx.compose.ui.text.TextStyle(
            color = palette.textMain,
            fontFamily = DashboardSans,
            fontSize = 14.sp,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(palette.bgSurface2)
            .padding(horizontal = 10.dp, vertical = 12.dp),
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

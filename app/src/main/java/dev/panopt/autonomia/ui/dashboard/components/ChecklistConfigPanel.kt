package dev.panopt.autonomia.ui.dashboard.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.ui.checklist.GoalPreset
import dev.panopt.autonomia.ui.checklist.GoalPresetGrid
import dev.panopt.autonomia.ui.checklist.LayerStampSmall
import dev.panopt.autonomia.ui.checklist.TimeWheelPicker
import dev.panopt.autonomia.ui.checklist.layerColor
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.SearchIcon
import dev.panopt.autonomia.ui.dashboard.mix

/**
 * Panel for configuring the primary checklist ("Mis anclas").
 * Shows current anchors, available activities, layer filters, and search.
 */
@Composable
internal fun ChecklistConfigPanel(
    layers: List<DashboardLayerState>,
    activityOptions: List<DashboardActivityOptionState>,
    palette: DashboardPalette,
    onAddToChecklist: (activityId: String, targetValue: Int?, targetCount: Int?, targetPeriod: TargetPeriod?) -> Unit,
    onRemoveFromChecklist: (activityId: String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLayerFilter by remember { mutableStateOf<String?>(null) }
    var isAnchorsExpanded by remember { mutableStateOf(true) }
    var configuringActivity by remember { mutableStateOf<DashboardActivityOptionState?>(null) }

    val currentAnchors = activityOptions.filter { it.activityType == "Anchor" }
    val availableActivities = activityOptions.filter { it.activityType != "Anchor" }
    val filteredActivities = availableActivities.filter { activity ->
        val matchesSearch = searchQuery.isBlank() ||
            activity.title.contains(searchQuery, ignoreCase = true)
        val matchesLayer = selectedLayerFilter == null ||
            activity.layerId == selectedLayerFilter
        matchesSearch && matchesLayer
    }

    // Title
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Mis anclas",
            color = palette.colorCardboard,
            fontFamily = DashboardSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 22.sp,
            lineHeight = 24.sp,
        )
        Text(
            text = "${currentAnchors.size} activas",
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 12.5.sp,
        )
    }

    // Check if we are configuring an activity (show dialog instead of list)
    if (configuringActivity != null) {
        ActivityConfigDialog(
            activity = configuringActivity!!,
            palette = palette,
            onConfirm = { targetValue, targetCount, targetPeriod ->
                onAddToChecklist(configuringActivity!!.id, targetValue, targetCount, targetPeriod)
                configuringActivity = null
            },
            onDismiss = { configuringActivity = null },
        )
    } else {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Collapsible current anchors section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { isAnchorsExpanded = !isAnchorsExpanded }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isAnchorsExpanded) "\u25be Anclas actuales" else "\u25b8 Anclas actuales",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                )
            }

            AnimatedVisibility(
                visible = isAnchorsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (currentAnchors.isEmpty()) {
                        Text(
                            text = "Sin anclas configuradas. Agrega actividades a tu base diaria.",
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        currentAnchors.forEach { anchor ->
                            AnchorRow(
                                activity = anchor,
                                palette = palette,
                                onRemove = { onRemoveFromChecklist(anchor.id) },
                            )
                        }
                    }
                }
            }

            // Available activities header
            Text(
                text = "Anclas disponibles",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(top = 4.dp),
            )

            // Activity list
            if (filteredActivities.isEmpty()) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        "Sin resultados para \"$searchQuery\""
                    } else {
                        "Todas las actividades estan en tu checklist"
                    },
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                filteredActivities.forEach { activity ->
                    ActivityOptionRow(
                        activity = activity,
                        palette = palette,
                        onClick = { configuringActivity = activity },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Bottom pinned search and layer filters
        Column(
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Search field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.bgSurface2)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SearchIcon(color = palette.textMuted, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = TextStyle(
                        color = palette.textMain,
                        fontFamily = DashboardSans,
                        fontSize = 14.sp,
                    ),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar actividad...",
                                    color = palette.textFaint,
                                    fontFamily = DashboardSans,
                                    fontSize = 14.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
            }

            // Layer filter buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                layers.forEach { layer ->
                    val isSelected = selectedLayerFilter == layer.id
                    val color = layerColor(layer.id, palette)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) mix(color, 0.22f, palette.bgSurface)
                                else palette.bgSurface,
                            )
                            .clickable {
                                selectedLayerFilter = if (isSelected) null else layer.id
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            LayerStampSmall(
                                layerId = layer.id,
                                color = if (isSelected) color else palette.textMuted,
                            )
                            Text(
                                text = layer.name,
                                color = if (isSelected) color else palette.textMuted,
                                fontFamily = DashboardSans,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

// -- Private composables --

@Composable
private fun AnchorRow(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onRemove: () -> Unit,
) {
    val color = layerColor(activity.layerId, palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(mix(color, 0.08f, palette.bgSurface))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LayerStampSmall(layerId = activity.layerId, color = color)
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
                text = buildString {
                    append(activity.layerName)
                    if (activity.targetValue > 0) append(" \u00b7 ${activity.targetValue} min")
                },
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.sp,
            )
        }
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(mix(palette.risk, 0.18f, palette.bgSurface))
                .clickable(role = Role.Button, onClick = onRemove)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Quitar",
                color = Color(0xFFF0B0A7),
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ActivityOptionRow(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    val color = layerColor(activity.layerId, palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LayerStampSmall(layerId = activity.layerId, color = color)
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
                fontSize = 12.sp,
            )
        }
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.colorCardboard)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Agregar",
                color = palette.bgBase,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ActivityConfigDialog(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onConfirm: (targetValue: Int?, targetCount: Int?, targetPeriod: TargetPeriod?) -> Unit,
    onDismiss: () -> Unit,
) {
    val hasTime = activity.targetValue > 0
    val initialTotalMinutes = activity.targetValue.coerceAtMost(480)
    var wheelHours by remember { mutableStateOf(initialTotalMinutes / 60) }
    var wheelMinutes by remember { mutableStateOf((initialTotalMinutes % 60) / 5 * 5) }
    val totalMinutes = (wheelHours * 60 + wheelMinutes).coerceIn(0, 480)

    var selectedGoal by remember { mutableStateOf(GoalPreset.None) }
    var customCount by remember { mutableStateOf("5") }
    var customPeriod by remember { mutableStateOf(TargetPeriod.Week) }

    BackHandler(onBack = onDismiss)

    Column(
        modifier = Modifier.heightIn(max = 680.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
        // Header
        Text(
            text = "Agregar a mis anclas",
            color = palette.colorCardboard,
            fontFamily = DashboardSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 19.sp,
        )

        // Activity info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val color = layerColor(activity.layerId, palette)
            LayerStampSmall(layerId = activity.layerId, color = color)
            Column {
                Text(
                    text = activity.title,
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = "Capa: ${activity.layerName}",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 13.sp,
                )
            }
        }

        // Time wheel picker (optional)
        if (hasTime) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tiempo objetivo",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
            )
            TimeWheelPicker(
                hours = wheelHours,
                minutes = wheelMinutes,
                palette = palette,
                onHoursChanged = { wheelHours = it.coerceIn(0, 8) },
                onMinutesChanged = { wheelMinutes = it },
            )
        }

        // Goal selector
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Meta (opcional)",
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
        )
        GoalPresetGrid(
            selectedGoal = selectedGoal,
            palette = palette,
            customCount = customCount,
            customPeriod = customPeriod,
            onGoalSelected = { selectedGoal = it },
            onCustomCountChanged = { customCount = it },
            onCustomPeriodChanged = { customPeriod = it },
        )

        } // End scrollable column

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Cancel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.bgSurface)
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancelar",
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }

            // Save
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.colorCardboard)
                    .clickable(role = Role.Button) {
                        val targetValue = if (hasTime && totalMinutes > 0) totalMinutes else null
                        val (count, period) = if (selectedGoal == GoalPreset.Custom) {
                            (customCount.toIntOrNull() ?: 1) to customPeriod
                        } else {
                            selectedGoal.toCountAndPeriod()
                        }
                        onConfirm(targetValue, count, period)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Guardar ancla",
                    color = palette.bgBase,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

package dev.panopt.autonomia.ui.anchors

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.SearchIcon
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix
import kotlinx.coroutines.delay

/**
 * Full-screen page for configuring anchors ("Mis anclas").
 * Accessible from the navigation drawer. Independent of the dashboard sheet system.
 */
@Composable
internal fun AnchorConfigScreen(
    layers: List<DashboardLayerState>,
    activityOptions: List<DashboardActivityOptionState>,
    palette: DashboardPalette,
    onAddAnchor: (activityId: String, targetValue: Int?, targetCount: Int?, targetPeriod: TargetPeriod?) -> Unit,
    onRemoveAnchor: (activityId: String) -> Unit,
    onDeleteActivity: (activityId: String) -> Unit,
    onCreateActivity: (name: String, layerId: String, targetMinutes: Int, isSecondary: Boolean, isGoal: Boolean, isMonthlyGoal: Boolean, targetCount: Int?, targetPeriod: TargetPeriod?) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var searchQuery by remember { mutableStateOf("") }
    var selectedLayerFilter by remember { mutableStateOf<String?>(null) }
    var isAnchorsExpanded by remember { mutableStateOf(true) }
    var configuringActivity by remember { mutableStateOf<DashboardActivityOptionState?>(null) }
    var isCreatingCustom by remember { mutableStateOf(false) }

    // Auto-collapse anchors when filtering
    var wasAutoCollapsed by remember { mutableStateOf(false) }
    LaunchedEffect(selectedLayerFilter, searchQuery) {
        val isFiltering = selectedLayerFilter != null || searchQuery.isNotBlank()
        if (isFiltering) {
            wasAutoCollapsed = true
            isAnchorsExpanded = false
        } else {
            wasAutoCollapsed = false
        }
    }

    // Flash color when header was auto-collapsed
    val headerFlashColor by animateColorAsState(
        targetValue = if (wasAutoCollapsed) palette.colorCoral else palette.textMuted,
        animationSpec = tween(durationMillis = 800),
        label = "headerFlash",
    )
    LaunchedEffect(wasAutoCollapsed) {
        if (wasAutoCollapsed) {
            kotlinx.coroutines.delay(500)
            wasAutoCollapsed = false
        }
    }

    val currentAnchors = activityOptions.filter { it.isConfigured && it.activityType == "Anchor" }
    val availableActivities = activityOptions.filter { !it.isConfigured && it.activityType == "Anchor" }
    val filteredActivities = availableActivities.filter { activity ->
        val matchesSearch = searchQuery.isBlank() ||
            activity.title.contains(searchQuery, ignoreCase = true)
        val matchesLayer = selectedLayerFilter == null ||
            activity.layerId == selectedLayerFilter
        matchesSearch && matchesLayer
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .statusBarsPadding(),
    ) {
        // Background gradient
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette.colorCoral.copy(alpha = 0.06f),
                        Color.Transparent,
                    ),
                    center = Offset(x = size.width * 0.8f, y = size.height * 0.15f),
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
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Back button
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
                        text = "Mis anclas",
                        color = palette.colorCardboard,
                        fontFamily = DashboardSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                    )
                    Text(
                        text = "Configura tu base diaria",
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontSize = 13.sp,
                    )
                }
                // Anchor count badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(mix(palette.colorCoral, 0.16f, palette.bgSurface))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${currentAnchors.size} activas",
                        color = palette.colorCoral,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                    )
                }
            }

            // Check if configuring
            if (configuringActivity != null) {
                ActivityConfigSection(
                    activity = configuringActivity!!,
                    palette = palette,
                    onConfirm = { targetValue, targetCount, targetPeriod ->
                        onAddAnchor(configuringActivity!!.id, targetValue, targetCount, targetPeriod)
                        configuringActivity = null
                    },
                    onDismiss = { configuringActivity = null },
                )
            } else if (isCreatingCustom) {
                CreateCustomActivitySection(
                    layers = layers,
                    palette = palette,
                    onConfirm = { name, layerId, minutes, isGoal, isMonthlyGoal, goalCount, goalPeriod ->
                        onCreateActivity(name, layerId, minutes, false, isGoal, isMonthlyGoal, goalCount, goalPeriod)
                        isCreatingCustom = false
                    },
                    onDismiss = { isCreatingCustom = false },
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Current anchors collapsible
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isAnchorsExpanded = !isAnchorsExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isAnchorsExpanded) "\u25be Anclas actuales" else "\u25b8 Anclas actuales",
                            color = if (!isAnchorsExpanded && wasAutoCollapsed) headerFlashColor else palette.textMuted,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                        )
                    }

                    AnimatedVisibility(
                        visible = isAnchorsExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (currentAnchors.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(palette.bgSurface)
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Sin anclas configuradas.\nAgrega actividades a tu base diaria.",
                                        color = palette.textMuted,
                                        fontFamily = DashboardSans,
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp,
                                    )
                                }
                            } else {
                                currentAnchors.forEach { anchor ->
                                    AnchorCard(
                                        activity = anchor,
                                        palette = palette,
                                        onRemove = { onRemoveAnchor(anchor.id) },
                                        onDelete = { onDeleteActivity(anchor.id) },
                                    )
                                }
                            }
                        }
                    }

                    // Create custom activity — always visible, outside anchor collapse
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(mix(palette.colorCardboard, 0.14f, palette.bgSurface))
                            .clickable(role = Role.Button) { isCreatingCustom = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "+ Crear actividad personalizada",
                            color = palette.colorCardboard,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }

                    // Header
                    Text(
                        text = "Anclas disponibles",
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp),
                    )

                    // Activity list
                    if (filteredActivities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.bgSurface)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "Sin resultados para \"$searchQuery\""
                                } else {
                                    "Todas las actividades estan en tus anclas"
                                },
                                color = palette.textMuted,
                                fontFamily = DashboardSans,
                                fontSize = 13.5.sp,
                            )
                        }
                    } else {
                        filteredActivities.forEach { activity ->
                            AvailableActivityCard(
                                activity = activity,
                                palette = palette,
                                onClick = { configuringActivity = activity },
                                onDelete = { onDeleteActivity(activity.id) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Bottom pinned search and layer filters
                Column(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(palette.bgSurface)
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SearchIcon(color = palette.textMuted, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = TextStyle(
                                color = palette.textMain,
                                fontFamily = DashboardSans,
                                fontSize = 15.sp,
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
                                            fontSize = 15.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }

                    // Layer filter strip
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
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
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
                                    LayerStamp(
                                        layerId = layer.id,
                                        color = if (isSelected) color else palette.textMuted,
                                        size = 26,
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
    }
}

// -- Cards --

@Composable
private fun AnchorCard(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = layerColor(activity.layerId, palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(mix(color, 0.08f, palette.bgSurface))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LayerStamp(layerId = activity.layerId, color = color, size = 24)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
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
                fontSize = 12.5.sp,
            )
        }
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(mix(palette.risk, 0.18f, palette.bgSurface))
                .clickable(role = Role.Button, onClick = onRemove)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Quitar",
                color = Color(0xFFF0B0A7),
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
            )
        }
        if (activity.id.startsWith("act_custom_") || !activity.id.startsWith("act_")) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.risk.copy(alpha = 0.1f))
                    .clickable(role = Role.Button, onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "X",
                    color = palette.risk,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun AvailableActivityCard(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val color = layerColor(activity.layerId, palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LayerStamp(layerId = activity.layerId, color = color, size = 24)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
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
        Box(
            modifier = Modifier
                .height(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.colorCardboard)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Agregar",
                color = palette.bgBase,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
            )
        }
        if (activity.id.startsWith("act_custom_") || !activity.id.startsWith("act_")) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(palette.risk.copy(alpha = 0.1f))
                    .clickable(role = Role.Button, onClick = onDelete),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "X",
                    color = palette.risk,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

// -- Config section (replaces dialog, inline in full screen) --

@Composable
private fun ActivityConfigSection(
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

    BackHandler(onBack = onDismiss)

    var selectedGoal by remember { mutableStateOf(GoalPreset.None) }
    var customCount by remember { mutableStateOf("5") }
    var customPeriod by remember { mutableStateOf(TargetPeriod.Week) }

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Scrollable content ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Text(
                text = "Agregar a mis anclas",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
            )

            // Activity info card
            ActivityInfoCard(activity = activity, palette = palette)

            Spacer(modifier = Modifier.height(4.dp))

            // Goal preset grid
            Text(
                text = "Meta (opcional)",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
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

            // Time wheel picker (optional)
            if (hasTime) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tiempo objetivo",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                TimeWheelPicker(
                    hours = wheelHours,
                    minutes = wheelMinutes,
                    palette = palette,
                    onHoursChanged = { wheelHours = it.coerceIn(0, 8) },
                    onMinutesChanged = { wheelMinutes = it },
                )
            }

            // Bottom spacing so content doesn't stick to footer
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Fixed footer (pinned, outside scroll) ──────────────────────────
        Row(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Cancel
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSurface)
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancelar",
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            // Save
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
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
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun ActivityInfoCard(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
) {
    val color = layerColor(activity.layerId, palette)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LayerStamp(layerId = activity.layerId, color = color, size = 28)
        Column {
            Text(
                text = activity.title,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            )
            Text(
                text = "Capa: ${activity.layerName}",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.5.sp,
            )
        }
    }
}

// -- Create custom activity section --

@Composable
private fun CreateCustomActivitySection(
    layers: List<DashboardLayerState>,
    palette: DashboardPalette,
    onConfirm: (name: String, layerId: String, targetMinutes: Int, isGoal: Boolean, isMonthlyGoal: Boolean, targetCount: Int?, targetPeriod: TargetPeriod?) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var wheelHours by remember { mutableStateOf(0) }
    var wheelMinutes by remember { mutableStateOf(0) }
    val totalMinutes = wheelHours * 60 + wheelMinutes
    var selectedLayerId by remember { mutableStateOf(layers.firstOrNull()?.id.orEmpty()) }

    var selectedGoal by remember { mutableStateOf(GoalPreset.None) }
    var customCount by remember { mutableStateOf("5") }
    var customPeriod by remember { mutableStateOf(TargetPeriod.Week) }

    BackHandler(onBack = onDismiss)

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Header
            Text(
                text = "Crear actividad",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
            )

            // ── Nombre ──────────────────────────────────────────────────
            Text(
                text = "Nombre",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                textStyle = TextStyle(
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontSize = 15.sp,
                ),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.bgSurface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (name.isEmpty()) {
                            Text(
                                text = "Ej. Leer 30 min, Caminar, Journaling...",
                                color = palette.textFaint,
                                fontFamily = DashboardSans,
                                fontSize = 15.sp,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            // ── Meta (opcional) ─────────────────────────────────────────
            Text(
                text = "Meta (opcional)",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
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

            // ── Tiempo objetivo ─────────────────────────────────────────
            Text(
                text = "Tiempo objetivo",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            TimeWheelPicker(
                hours = wheelHours,
                minutes = wheelMinutes,
                palette = palette,
                onHoursChanged = { wheelHours = it.coerceIn(0, 8) },
                onMinutesChanged = { wheelMinutes = it },
            )

            // ── Capas ───────────────────────────────────────────────────
            Text(
                text = "Capa",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                layers.forEach { layer ->
                    val isSelected = layer.id == selectedLayerId
                    val color = layerColor(layer.id, palette)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) mix(color, 0.22f, palette.bgSurface)
                                else palette.bgSurface,
                            )
                            .clickable { selectedLayerId = layer.id },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            LayerStamp(
                                layerId = layer.id,
                                color = if (isSelected) color else palette.textMuted,
                                size = 22,
                            )
                            Text(
                                text = layer.name.take(5),
                                color = if (isSelected) color else palette.textMuted,
                                fontFamily = DashboardSans,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Fixed footer ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 16.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSurface)
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Cancelar",
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            val isGoal = selectedGoal != GoalPreset.None
            val isMonthlyGoal = selectedGoal.name.endsWith("Month")
            val goalCount: Int?
            val goalPeriod: TargetPeriod?
            if (isGoal && selectedGoal != GoalPreset.Custom) {
                val (c, p) = selectedGoal.toCountAndPeriod()
                goalCount = c
                goalPeriod = p
            } else if (isGoal && selectedGoal == GoalPreset.Custom) {
                goalCount = customCount.toIntOrNull() ?: 1
                goalPeriod = customPeriod
            } else {
                goalCount = null
                goalPeriod = null
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (name.isNotBlank()) palette.colorCardboard
                        else palette.bgSurface2,
                    )
                    .clickable(role = Role.Button, enabled = name.isNotBlank()) {
                        if (name.isNotBlank()) {
                            onConfirm(name, selectedLayerId, totalMinutes, isGoal, isMonthlyGoal, goalCount, goalPeriod)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Crear ancla",
                    color = if (name.isNotBlank()) palette.bgBase else palette.textFaint,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

package dev.panopt.autonomia.ui.tasks

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.border
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
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix

/**
 * Full-screen task manager ("Pendientes").
 * Create, complete, and archive tasks. No targets or recurrence.
 */
@Composable
internal fun TasksScreen(
    pendingTasks: List<DashboardTaskState>,
    layers: List<DashboardLayerState>,
    palette: DashboardPalette,
    onCreateTask: (title: String, layerId: String?, contributesToCore: Boolean) -> Unit,
    onCompleteTask: (taskId: String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var title by remember { mutableStateOf("") }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

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
            // Top bar
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
                        text = if (pendingTasks.isEmpty()) "Todo al dia" else "${pendingTasks.size} pendientes",
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontSize = 13.sp,
                    )
                }
                // New task button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showCreate) palette.colorCardboard else palette.bgSurface)
                        .clickable(role = Role.Button) { showCreate = !showCreate },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "+",
                        color = if (showCreate) palette.bgBase else palette.textMain,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Create section
                if (showCreate) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
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
                                Box {
                                    if (title.isEmpty()) {
                                        Text(
                                            text = "Que tenes pendiente?",
                                            color = palette.textFaint,
                                            fontFamily = DashboardSans,
                                            fontSize = 15.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )

                        // Layer picker
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            layers.forEach { layer ->
                                val isSelected = selectedLayerId == layer.id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) mix(palette.colorCardboard, 0.14f, palette.bgSurface)
                                            else palette.bgSurface,
                                        )
                                        .clickable {
                                            selectedLayerId = if (isSelected) null else layer.id
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = layer.name.take(6),
                                        color = if (isSelected) palette.colorCardboard else palette.textMuted,
                                        fontFamily = DashboardSans,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }

                        // Create button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (title.isNotBlank()) palette.colorCardboard
                                    else palette.bgSurface2,
                                )
                                .clickable(role = Role.Button, enabled = title.isNotBlank()) {
                                    if (title.isNotBlank()) {
                                        onCreateTask(title.trim(), selectedLayerId, selectedLayerId != null)
                                        title = ""
                                        selectedLayerId = null
                                        showCreate = false
                                    }
                                },
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

                // Pending tasks
                if (pendingTasks.isEmpty() && !showCreate) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.bgSurface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No tenés pendientes. Creá una tarea para empezar.",
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                        )
                    }
                } else {
                    pendingTasks.forEach { task ->
                        TaskRow(
                            task = task,
                            palette = palette,
                            onComplete = { onCompleteTask(task.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: DashboardTaskState,
    palette: DashboardPalette,
    onComplete: () -> Unit,
) {
    var completedClicked by remember(task.id) { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            color = palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.5.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (completedClicked) palette.colorCoral.copy(alpha = 0.2f) else Color.Transparent)
                .clickable {
                    completedClicked = true
                    onComplete()
                }
                .border(
                    width = 1.5.dp,
                    color = if (completedClicked) palette.colorCoral else palette.textMuted.copy(alpha = 0.4f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (completedClicked) {
                CheckIcon(color = palette.colorCoral, modifier = Modifier.size(16.dp))
            }
        }
    }
}

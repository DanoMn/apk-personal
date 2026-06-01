package dev.panopt.autonomia.ui.anchors

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.activity.MAX_ANCHOR_SESSION_MINUTES
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif

internal enum class AnchorEditorMode {
    Add,
    Edit,
}

@Composable
internal fun AnchorEditorForm(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    mode: AnchorEditorMode,
    modifier: Modifier = Modifier,
    compactInfo: Boolean = false,
    useNavigationBarPadding: Boolean = true,
    onConfirm: (sessionTargetMinutes: Int, weeklyFrequencyTarget: Int, commitmentDurationMonths: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialTotalMinutes = (activity.sessionTargetMinutes ?: activity.targetValue)
        .coerceIn(5, MAX_ANCHOR_SESSION_MINUTES)
    var wheelHours by remember(activity.id) { mutableIntStateOf(initialTotalMinutes / 60) }
    var wheelMinutes by remember(activity.id) { mutableIntStateOf(initialTotalMinutes % 60) }
    val totalMinutes = (wheelHours * 60 + wheelMinutes).coerceIn(0, MAX_ANCHOR_SESSION_MINUTES)

    var weeklyFrequencyTarget by remember(activity.id) {
        mutableIntStateOf(normalizeWeeklyFrequencyTarget(activity.weeklyFrequencyTarget))
    }
    var commitmentDurationMonths by remember(activity.id) {
        mutableStateOf(activity.commitmentDurationMonths)
    }
    var showCommitmentDurationDialog by remember { mutableStateOf(false) }
    var showTargetError by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = if (mode == AnchorEditorMode.Edit) "Editar ancla" else "Agregar a mis anclas",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = if (compactInfo) 19.sp else 22.sp,
            )

            if (compactInfo) {
                AnchorEditorInfoRow(activity = activity, palette = palette)
            } else {
                AnchorEditorInfoCard(activity = activity, palette = palette)
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tiempo objetivo",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = if (compactInfo) 12.5.sp else 13.sp,
            )
            TimeWheelPicker(
                hours = wheelHours,
                minutes = wheelMinutes,
                palette = palette,
                onHoursChanged = {
                    wheelHours = it.coerceIn(0, MAX_ANCHOR_SESSION_MINUTES / 60)
                    if (wheelHours == MAX_ANCHOR_SESSION_MINUTES / 60) wheelMinutes = 0
                },
                onMinutesChanged = {
                    wheelMinutes = if (wheelHours == MAX_ANCHOR_SESSION_MINUTES / 60) 0 else it
                },
            )

            Text(
                text = "Meta semanal",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = if (compactInfo) 12.5.sp else 13.sp,
            )
            WeeklyFrequencySelector(
                selectedFrequency = weeklyFrequencyTarget,
                palette = palette,
                onFrequencySelected = {
                    weeklyFrequencyTarget = it
                    showTargetError = false
                },
            )
            if (showTargetError) {
                Text(
                    text = "La meta semanal y el tiempo objetivo son obligatorios.",
                    color = palette.risk,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Text(
                text = "Duración del compromiso",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = if (compactInfo) 12.5.sp else 13.sp,
            )
            CommitmentDurationSetting(
                durationMonths = commitmentDurationMonths,
                palette = palette,
                onClick = { showCommitmentDurationDialog = true },
            )

            if (showCommitmentDurationDialog) {
                CommitmentDurationDialog(
                    selectedDurationMonths = commitmentDurationMonths,
                    palette = palette,
                    onDismiss = { showCommitmentDurationDialog = false },
                    onConfirm = {
                        commitmentDurationMonths = it
                        showCommitmentDurationDialog = false
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 16.dp)
                .then(if (useNavigationBarPadding) Modifier.navigationBarsPadding() else Modifier),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compactInfo) 48.dp else 52.dp)
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
                    fontSize = if (compactInfo) 14.sp else 15.sp,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (compactInfo) 48.dp else 52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.colorCardboard)
                    .clickable(role = Role.Button) {
                        val sessionTargetMinutes = totalMinutes.takeIf { it > 0 }
                        if (!isValidAnchorTargetContract(sessionTargetMinutes, weeklyFrequencyTarget)) {
                            showTargetError = true
                            return@clickable
                        }
                        onConfirm(
                            sessionTargetMinutes!!,
                            weeklyFrequencyTarget,
                            commitmentDurationMonths,
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (mode == AnchorEditorMode.Edit) "Guardar cambios" else "Guardar ancla",
                    color = palette.bgBase,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compactInfo) 14.sp else 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AnchorEditorInfoCard(
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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

@Composable
private fun AnchorEditorInfoRow(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
) {
    val color = layerColor(activity.layerId, palette)
    Row(
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
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Capa: ${activity.layerName}",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
        }
    }
}

internal fun anchorConfigurationSummary(activity: DashboardActivityOptionState): String {
    val sessionMinutes = activity.sessionTargetMinutes ?: activity.targetValue
    val weeklyTarget = normalizeWeeklyFrequencyTarget(activity.weeklyFrequencyTarget)
    return "${activity.layerName} - ${sessionMinutes} min - ${weeklyTarget}/sem - " +
        commitmentDurationLabel(activity.commitmentDurationMonths)
}

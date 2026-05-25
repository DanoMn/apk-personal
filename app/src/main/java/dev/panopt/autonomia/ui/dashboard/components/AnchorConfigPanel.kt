package dev.panopt.autonomia.ui.dashboard.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.ui.anchors.AnchorEditorForm
import dev.panopt.autonomia.ui.anchors.AnchorEditorMode
import dev.panopt.autonomia.ui.anchors.LayerStampSmall
import dev.panopt.autonomia.ui.anchors.anchorConfigurationSummary
import dev.panopt.autonomia.ui.anchors.layerColor
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.mix

/**
 * Quick settings panel for configured anchors only.
 * Full catalog management remains in the "Mis anclas" screen.
 */
@Composable
internal fun AnchorConfigPanel(
    activityOptions: List<DashboardActivityOptionState>,
    palette: DashboardPalette,
    onAddAnchor: (activityId: String, sessionTargetMinutes: Int, weeklyFrequencyTarget: Int, commitmentDurationMonths: Int?) -> Unit,
    onOpenFullAnchorConfig: () -> Unit,
) {
    var editingAnchor by remember { mutableStateOf<DashboardActivityOptionState?>(null) }
    val currentAnchors = activityOptions.filter { it.isConfigured && it.activityType == "Anchor" }

    val anchorBeingEdited = editingAnchor
    if (anchorBeingEdited != null) {
        BackHandler(onBack = { editingAnchor = null })
        AnchorEditorForm(
            activity = anchorBeingEdited,
            palette = palette,
            mode = AnchorEditorMode.Edit,
            modifier = Modifier.fillMaxWidth(),
            compactInfo = true,
            useNavigationBarPadding = false,
            onConfirm = { sessionTargetMinutes, weeklyFrequencyTarget, commitmentDurationMonths ->
                onAddAnchor(
                    anchorBeingEdited.id,
                    sessionTargetMinutes,
                    weeklyFrequencyTarget,
                    commitmentDurationMonths,
                )
                editingAnchor = null
            },
            onDismiss = { editingAnchor = null },
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "Anclas",
                    color = palette.colorCardboard,
                    fontFamily = DashboardSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 22.sp,
                    lineHeight = 24.sp,
                )
                Text(
                    text = "Ajustes rapidos de tu base",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 12.5.sp,
                )
            }
            Text(
                text = "${currentAnchors.size} activas",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.5.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (currentAnchors.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgSurface)
                        .padding(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Sin anclas configuradas. Agrega actividades a tu base diaria desde Mis anclas.",
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            } else {
                currentAnchors.forEach { anchor ->
                    QuickAnchorRow(
                        activity = anchor,
                        palette = palette,
                        onEdit = { editingAnchor = anchor },
                    )
                }
            }
        }

            OpenFullAnchorConfigButton(
                palette = palette,
                onClick = onOpenFullAnchorConfig,
            )
        }
    }
}

@Composable
private fun QuickAnchorRow(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onEdit: () -> Unit,
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
                text = anchorConfigurationSummary(activity),
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier = Modifier
                .height(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(mix(palette.colorCardboard, 0.22f, palette.bgSurface))
                .clickable(role = Role.Button, onClick = onEdit)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Ajustar",
                color = palette.colorCardboard,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun OpenFullAnchorConfigButton(
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.colorCardboard)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Abrir Mis anclas",
                color = palette.bgBase,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = ">",
                color = palette.bgBase,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

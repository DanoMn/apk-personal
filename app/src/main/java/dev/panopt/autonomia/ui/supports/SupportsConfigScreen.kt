package dev.panopt.autonomia.ui.supports

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix

/**
 * Full-screen config page for support activities ("Soportes").
 * Thin wrapper that filters activityOptions by activityType="Support".
 * No goals/targets section — supports have no targets.
 */
@Composable
internal fun SupportsConfigScreen(
    layers: List<DashboardLayerState>,
    activityOptions: List<DashboardActivityOptionState>,
    palette: DashboardPalette,
    onAddToSupports: (activityId: String) -> Unit,
    onRemoveFromSupports: (activityId: String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val currentSupports = activityOptions.filter { it.isConfigured && it.activityType == "Support" }
    val availableActivities = activityOptions.filter { !it.isConfigured && it.activityType == "Support" }

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
                        text = "Soportes",
                        color = palette.colorCardboard,
                        fontFamily = DashboardSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        lineHeight = 28.sp,
                    )
                    Text(
                        text = "Cuidado base diario",
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontSize = 13.sp,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Current supports
                Text(
                    text = "Soportes activos",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )

                if (currentSupports.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.bgSurface)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Sin soportes configurados.\nAgrega actividades de cuidado base.",
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontSize = 13.5.sp,
                            lineHeight = 20.sp,
                        )
                    }
                } else {
                    currentSupports.forEach { support ->
                        SupportActiveCard(
                            activity = support,
                            palette = palette,
                            onRemove = { onRemoveFromSupports(support.id) },
                        )
                    }
                }

                // Available to add
                if (availableActivities.isNotEmpty()) {
                    Text(
                        text = "Disponibles para agregar",
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    availableActivities.forEach { activity ->
                        SupportAvailableCard(
                            activity = activity,
                            palette = palette,
                            onAdd = { onAddToSupports(activity.id) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SupportActiveCard(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
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
    }
}

@Composable
private fun SupportAvailableCard(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .clickable(onClick = onAdd)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.title,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
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
                .clickable(role = Role.Button, onClick = onAdd)
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
    }
}

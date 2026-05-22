package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.domain.dashboard.DashboardWeekRowState

@Composable
internal fun WeekSection(
    palette: DashboardPalette,
    rows: List<DashboardWeekRowState>,
) {
    SectionHeader(
        palette = palette,
        title = "Semana",
        note = "consistencia",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(7.68.dp),
    ) {
        rows.forEach { row ->
            WeekRow(
                palette = palette,
                name = row.name,
                score = row.score,
                color = row.layerColor(palette),
                progress = row.progress,
            )
        }
    }
}

@Composable
internal fun WeekRow(
    palette: DashboardPalette,
    name: String,
    score: String,
    color: Color,
    progress: Float,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = name,
            modifier = Modifier.width(80.dp),
            color = palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 16.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(99.dp))
                    .background(color),
            )
        }
        Text(
            text = score,
            modifier = Modifier.width(38.dp),
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 13.sp,
            lineHeight = 15.sp,
            maxLines = 1,
        )
    }
}

private fun DashboardWeekRowState.layerColor(palette: DashboardPalette): Color =
    when (layerId) {
        "layer_interior" -> palette.layerInterior
        "layer_cuerpo" -> palette.layerBody
        "layer_conducta" -> palette.layerConduct
        "layer_vinculos" -> palette.layerVinculos
        "layer_proyecto" -> palette.layerProject
        else -> palette.textMuted
    }

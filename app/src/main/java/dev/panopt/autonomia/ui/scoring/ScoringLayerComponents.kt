package dev.panopt.autonomia.ui.scoring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardScoreLayerReportState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif

@Composable
internal fun ScoreLayerReportCard(
    palette: DashboardPalette,
    layer: DashboardScoreLayerReportState,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(palette.bgSurface)
            .padding(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = layer.name,
                color = palette.textMain,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 19.sp,
                lineHeight = 22.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = layer.scoreLabel,
                color = layerColor(palette, layer.layerId),
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                lineHeight = 18.sp,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        ProgressTrack(
            palette = palette,
            progress = layer.progress,
            color = layerColor(palette, layer.layerId),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            CompactMetric(palette, "Base", layer.baseLabel)
            CompactMetric(palette, "Anclas", layer.anchorLabel)
            CompactMetric(palette, "Soportes", layer.supportLabel)
            CompactMetric(palette, "Superhabit", layer.surplusLabel)
            CompactMetric(palette, "Pendientes", layer.taskMomentumLabel)
            if (layer.sleepLabel != "--") CompactMetric(palette, "Sueno", layer.sleepLabel)
            if (layer.sobrietyLabel != "--") CompactMetric(palette, "Sobriedad", layer.sobrietyLabel)
        }
    }
}

@Composable
private fun CompactMetric(
    palette: DashboardPalette,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 12.5.sp,
            lineHeight = 16.sp,
        )
        Text(
            text = value,
            color = palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.5.sp,
            lineHeight = 16.sp,
        )
    }
}

package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardDailyProgressState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette

private val DashboardSans = FontFamily.SansSerif

@Composable
internal fun DailyProgressCard(
    palette: DashboardPalette,
    progress: DashboardDailyProgressState,
) {
    Spacer(modifier = Modifier.height(11.52.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(15.2.dp),
        verticalArrangement = Arrangement.spacedBy(11.52.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Progreso de hoy",
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.36.sp,
                lineHeight = 20.sp,
            )
            Text(
                text = "${progress.percent}%",
                color = palette.colorCardboard,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.48.sp,
                lineHeight = 20.48.sp,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.progress.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(99.dp))
                    .background(palette.colorCoral),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = progress.pendingLabel,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.16.sp,
                lineHeight = 14.sp,
            )
            Text(
                text = progress.activeLayersLabel,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.16.sp,
                lineHeight = 14.sp,
            )
        }
    }
}

package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.ui.dashboard.ActivityIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.domain.dashboard.DashboardStatusState
import dev.panopt.autonomia.ui.dashboard.mix

private val DashboardSans = FontFamily.SansSerif
private val DashboardSerif = FontFamily.Serif

@Composable
internal fun StatusCard(
    palette: DashboardPalette,
    status: DashboardStatusState,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgSurface)
            .padding(17.6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            StatusLabel(palette = palette, status = status)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = status.headline,
                color = palette.textMain,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 26.4.sp,
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = status.body,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 15.04.sp,
                lineHeight = 22.56.sp,
            )
        }

        ScoreOrbit(
            palette = palette,
            score = status.scoreLabel,
            label = if (status.scoreState == ScoreState.NoData) "sin score" else "base",
            progress = status.progress,
            color = status.scoreState.statusColor(palette),
        )
    }
}

@Composable
internal fun StatusLabel(
    palette: DashboardPalette,
    status: DashboardStatusState,
) {
    val color = status.scoreState.statusColor(palette)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(mix(color, 0.2f, palette.bgSurface))
            .padding(horizontal = 9.92.dp, vertical = 4.8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.4.dp),
    ) {
        ActivityIcon(
            color = color,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = status.title,
            color = color,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.48.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun ScoreOrbit(
    palette: DashboardPalette,
    score: String,
    label: String,
    progress: Float,
    color: Color,
) {
    Box(
        modifier = Modifier.size(104.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 13.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val topLeft = Offset(
                x = size.width / 2f - radius,
                y = size.height / 2f - radius,
            )
            val arcSize = Size(radius * 2f, radius * 2f)

            drawCircle(color = palette.bgElevated)
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = radius,
                style = Stroke(width = strokeWidth),
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
            )
        }

        Column(
            modifier = Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(palette.bgSurface),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = score,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.48.sp,
                lineHeight = 20.48.sp,
            )
            Text(
                text = label,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 10.88.sp,
                lineHeight = 13.sp,
            )
        }
    }
}

private fun ScoreState.statusColor(palette: DashboardPalette): Color =
    when (this) {
        ScoreState.NoData -> palette.textMuted
        ScoreState.Restoration -> palette.risk
        ScoreState.Attention -> palette.colorCardboard
        ScoreState.Motion -> palette.stateMotion
        ScoreState.Plenitude -> palette.layerBody
        ScoreState.Unbreakable -> palette.colorCoral
    }

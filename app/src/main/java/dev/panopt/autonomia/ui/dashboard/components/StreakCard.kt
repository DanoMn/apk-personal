package dev.panopt.autonomia.ui.dashboard.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.GlassWaterIcon
import dev.panopt.autonomia.ui.dashboard.IntimateBoundaryIcon

private val DashboardSans = FontFamily.SansSerif

@Composable
internal fun SobrietySection(palette: DashboardPalette) {
    SectionHeader(
        palette = palette,
        title = "Sobriedad",
        note = "rachas activas",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.2.dp),
    ) {
        StreakCard(
            palette = palette,
            label = "Alcohol",
            days = "18",
            meta = "hoy limpio",
            modifier = Modifier.weight(1f),
        ) { iconColor ->
            GlassWaterIcon(color = iconColor, modifier = Modifier.size(26.dp))
        }
        StreakCard(
            palette = palette,
            label = "Conducta sexual",
            days = "4",
            meta = "activa",
            modifier = Modifier.weight(1f),
        ) { iconColor ->
            IntimateBoundaryIcon(color = iconColor, modifier = Modifier.size(26.dp))
        }
    }
}

@Composable
internal fun StreakCard(
    palette: DashboardPalette,
    label: String,
    days: String,
    meta: String,
    modifier: Modifier,
    icon: @Composable (Color) -> Unit,
) {
    Column(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon(palette.textMain)
            }
            Text(
                text = label,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = days,
                    color = palette.colorCardboard,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    lineHeight = 36.sp,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "días",
                    color = palette.colorCardboard,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = meta,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 14.5.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

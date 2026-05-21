package dev.panopt.autonomia.ui.dashboard.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.InteriorLayerIcon
import dev.panopt.autonomia.ui.dashboard.ProjectTriangleIcon
import dev.panopt.autonomia.ui.dashboard.SleepIcon

private val DashboardSans = FontFamily.SansSerif

@Composable
internal fun SignalsSection(palette: DashboardPalette) {
    SectionHeader(
        palette = palette,
        title = "Señales",
        note = "lectura rápida",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.4.dp),
    ) {
        SignalCard(
            palette = palette,
            label = "Sueño",
            value = "6h 40m",
            meta = "aceptable",
            color = palette.textMuted,
            modifier = Modifier.weight(1f),
        ) { iconColor ->
            SleepIcon(color = iconColor, modifier = Modifier.size(28.dp))
        }
        SignalCard(
            palette = palette,
            label = "Proyecto",
            value = "20m",
            meta = "música",
            color = palette.layerProject,
            modifier = Modifier.weight(1f),
        ) { iconColor ->
            ProjectTriangleIcon(color = iconColor, modifier = Modifier.size(28.dp))
        }
        SignalCard(
            palette = palette,
            label = "Interior",
            value = "12m",
            meta = "elegida",
            color = palette.layerInterior,
            modifier = Modifier.weight(1f),
        ) { iconColor ->
            InteriorLayerIcon(color = iconColor, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
internal fun SignalCard(
    palette: DashboardPalette,
    label: String,
    value: String,
    meta: String,
    color: Color,
    modifier: Modifier,
    icon: @Composable (Color) -> Unit,
) {
    Column(
        modifier = modifier
            .height(116.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .clickable(role = Role.Button, onClick = {})
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon(color)
            }
            Text(
                text = label,
                color = color,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.5.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column {
            Text(
                text = value,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                lineHeight = 24.sp,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = meta,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 14.sp,
                lineHeight = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

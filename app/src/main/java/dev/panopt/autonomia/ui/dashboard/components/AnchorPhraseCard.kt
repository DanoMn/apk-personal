package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.mix

private val DashboardSans = FontFamily.SansSerif
private val DashboardSerif = FontFamily.Serif

@Composable
internal fun AnchorPhraseCard(palette: DashboardPalette) {
    Spacer(modifier = Modifier.height(11.52.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(mix(palette.colorCardboard, 0.12f, palette.bgSurface))
            .padding(17.6.dp),
    ) {
        Text(
            text = "\"Life can only be understood backwards; but it must be lived forwards.\"",
            color = palette.textMain,
            fontFamily = DashboardSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.32.sp,
            lineHeight = 23.66.sp,
        )
        Spacer(modifier = Modifier.height(10.4.dp))
        Text(
            text = "Soren Kierkegaard",
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 12.48.sp,
            lineHeight = 16.sp,
        )
    }
}

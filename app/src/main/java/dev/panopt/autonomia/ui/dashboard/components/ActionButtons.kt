package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.ChecklistIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.FlagIcon
import dev.panopt.autonomia.ui.dashboard.mix

private val DashboardSans = FontFamily.SansSerif

@Composable
internal fun ActionButtons(
    palette: DashboardPalette,
    onChecklistClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(13.6.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.52.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.colorCardboard)
                .clickable(role = Role.Button, onClick = onChecklistClick)
                .semantics { contentDescription = "Registrar checklist" }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ChecklistIcon(
                    color = palette.bgBase,
                    modifier = Modifier.size(24.dp),
                )
                Text(
                    text = "Registrar checklist",
                    color = palette.bgBase,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(mix(palette.risk, 0.22f, palette.bgSurface))
                .clickable(role = Role.Button, onClick = {})
                .semantics { contentDescription = "Abrir protocolo de riesgo" },
            contentAlignment = Alignment.Center,
        ) {
            FlagIcon(color = Color(0xFFF0B0A7), modifier = Modifier.size(28.dp))
        }
    }
}

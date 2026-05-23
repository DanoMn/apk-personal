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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.ChecklistIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.FlagIcon
import dev.panopt.autonomia.ui.dashboard.mix

private val DashboardSans = FontFamily.SansSerif

/**
 * Quick-action buttons row matching the prototype:
 * one primary white button ("Configuración rápida") that opens the EntryMenu bottom sheet,
 * and a red risk button at the side for rapid relapse access.
 *
 * Sleep configuration is accessed from the Signals section, not from here.
 */
@Composable
internal fun ActionButtons(
    palette: DashboardPalette,
    onQuickConfigClick: () -> Unit,
    onRiesgoClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Primary white button: opens entry menu with all config options
        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.colorCardboard)
                .clickable(role = Role.Button, onClick = onQuickConfigClick)
                .semantics { contentDescription = "Configuración rápida" }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                ChecklistIcon(color = palette.bgBase, modifier = Modifier.size(22.dp))
                Text(
                    text = "Configuración rápida",
                    color = palette.bgBase,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }

        // Red risk button: rapid relapse access
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(mix(palette.risk, 0.22f, palette.bgSurface))
                .clickable(role = Role.Button, onClick = onRiesgoClick)
                .semantics { contentDescription = "Abrir protocolo de riesgo" },
            contentAlignment = Alignment.Center,
        ) {
            FlagIcon(
                color = Color(0xFFF0B0A7),
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

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
import dev.panopt.autonomia.domain.dashboard.DashboardSleepState
import dev.panopt.autonomia.ui.dashboard.AnchorIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.FlagIcon
import dev.panopt.autonomia.ui.dashboard.MoonIcon
import dev.panopt.autonomia.ui.dashboard.mix

private val DashboardSans = FontFamily.SansSerif

@Composable
internal fun ActionButtons(
    palette: DashboardPalette,
    sleep: DashboardSleepState,
    isSleepLockActive: Boolean,
    onSleepActionClick: () -> Unit,
    onQuickConfigClick: () -> Unit,
    onRiesgoClick: () -> Unit,
) {
    Spacer(modifier = Modifier.height(12.dp))
    val sleepActionLabel = when {
        !isSleepLockActive -> "Activar bloqueo"
        sleep.isSessionOpen -> "Desperte"
        else -> "Ir a dormir"
    }
    val sleepActionDescription = when {
        !isSleepLockActive -> "Activar bloqueo de sueno"
        sleep.isSessionOpen -> "Registrar despertar"
        else -> "Ir a dormir"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.colorCardboard)
                .clickable(role = Role.Button, onClick = onSleepActionClick)
                .semantics {
                    contentDescription = sleepActionDescription
                }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MoonIcon(color = palette.bgBase, modifier = Modifier.size(21.dp))
                Text(
                    text = sleepActionLabel,
                    color = palette.bgBase,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.colorCardboard)
                .clickable(role = Role.Button, onClick = onQuickConfigClick)
                .semantics { contentDescription = "Configuracion rapida" }
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AnchorIcon(color = palette.bgBase, modifier = Modifier.size(22.dp))
                Text(
                    text = "Configurar",
                    color = palette.bgBase,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }

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

package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.StartupCardState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.mix

private val DashboardSans = FontFamily.SansSerif
private val DashboardSerif = FontFamily.Serif

/**
 * Card de ARRANQUE de cuenta (`scoring-arranque-cuenta`). Hermano de [StatusCard]: misma FORMA (texto
 * + orbe) para coherencia visual, pero componente INDEPENDIENTE — [StatusCard] y [ScoreOrbit] no se
 * tocan. El dashboard elige cuál renderizar según `state.startup != null`.
 *
 * Sin lógica de negocio (state hoisting): recibe el [StartupCardState] ya resuelto por el dominio
 * ([dev.panopt.autonomia.domain.dashboard.buildDashboardState]) y SOLO presenta/anima. El número
 * central sube con [animateIntAsState]; el arco `d/7` se llena con [animateFloatAsState]. El color es
 * cálido propio derivado de los tokens existentes (`colorCoral`/`colorCardboard`) vía [mix] — respeta
 * `docs/frontend/frontend-design.md` (cartón/beige + coral mate), sin inventar paleta.
 */
@Composable
internal fun StartupStatusCard(
    palette: DashboardPalette,
    startup: StartupCardState,
) {
    val animatedCounter by animateIntAsState(
        targetValue = startup.counterPoints,
        label = "startupCounter",
    )
    val animatedProgress by animateFloatAsState(
        targetValue = startup.windowProgress,
        label = "startupArc",
    )
    val warm = startupColor(palette)

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
            StartupLabel(palette = palette, color = warm)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = startup.headline,
                color = palette.textMain,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 26.4.sp,
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = startup.body,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 15.04.sp,
                lineHeight = 22.56.sp,
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = startup.daysRemainingLabel,
                color = warm,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }

        ScoreOrbit(
            palette = palette,
            score = animatedCounter.toString(),
            label = "cargando",
            progress = animatedProgress,
            color = warm,
        )
    }
}

/** Pill cálido de "Arranque" (forma de [StatusLabel] pero independiente; sin enum de estado). */
@Composable
private fun StartupLabel(
    palette: DashboardPalette,
    color: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(mix(color, 0.2f, palette.bgSurface))
            .padding(horizontal = 9.92.dp, vertical = 4.8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.4.dp),
    ) {
        Text(
            text = "Arranque",
            color = color,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.48.sp,
            letterSpacing = 0.5.sp,
            maxLines = 1,
        )
    }
}

/** Color cálido propio del arranque: derivado de `colorCoral`/`colorCardboard` (design §7.3). */
private fun startupColor(palette: DashboardPalette): Color =
    mix(palette.colorCoral, 0.35f, palette.colorCardboard)

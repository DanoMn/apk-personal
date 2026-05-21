package dev.panopt.autonomia.ui.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

internal val DashboardSans = FontFamily.SansSerif
internal val DashboardSerif = FontFamily.Serif

@Composable
internal fun dashboardPalette(isDarkMode: Boolean): DashboardPalette =
    if (isDarkMode) {
        DashboardPalette(
            bgBase = Color(0xFF1F1E1D),
            bgSurface = Color(0xFF2A2927),
            bgSurface2 = Color(0xFF33312E),
            bgElevated = Color(0xFF3A3733),
            drawer = Color(0xFF272522),
            colorCardboard = Color(0xFFE0D8C3),
            colorCardboardSoft = Color(0xFFCFC4AA),
            colorCoral = Color(0xFFE57B65),
            textMain = Color(0xFFEAE5D9),
            textMuted = Color(0xFFA6A097),
            textFaint = Color(0xFF777169),
            layerInterior = Color(0xFF8893A5),
            layerBody = Color(0xFF7F9E83),
            layerConduct = Color(0xFFC49B55),
            layerVinculos = Color(0xFFA38491),
            layerProject = Color(0xFF708A9E),
            stateMotion = Color(0xFF5A8296),
            risk = Color(0xFFD45B51),
        )
    } else {
        DashboardPalette(
            bgBase = Color(0xFFF4F0E7),
            bgSurface = Color(0xFFFFFCF4),
            bgSurface2 = Color(0xFFE9E1D2),
            bgElevated = Color(0xFFE3DACB),
            drawer = Color(0xFFEEE7D9),
            colorCardboard = Color(0xFF4C4638),
            colorCardboardSoft = Color(0xFF6F6654),
            colorCoral = Color(0xFFC76454),
            textMain = Color(0xFF27231F),
            textMuted = Color(0xFF756F66),
            textFaint = Color(0xFFA9A096),
            layerInterior = Color(0xFF6E7B91),
            layerBody = Color(0xFF647F68),
            layerConduct = Color(0xFF967338),
            layerVinculos = Color(0xFF866877),
            layerProject = Color(0xFF587184),
            stateMotion = Color(0xFF5A8296),
            risk = Color(0xFFB85048),
        )
    }

internal data class DashboardPalette(
    val bgBase: Color,
    val bgSurface: Color,
    val bgSurface2: Color,
    val bgElevated: Color,
    val drawer: Color,
    val colorCardboard: Color,
    val colorCardboardSoft: Color,
    val colorCoral: Color,
    val textMain: Color,
    val textMuted: Color,
    val textFaint: Color,
    val layerInterior: Color,
    val layerBody: Color,
    val layerConduct: Color,
    val layerVinculos: Color,
    val layerProject: Color,
    val stateMotion: Color,
    val risk: Color,
)

internal fun mix(
    foreground: Color,
    amount: Float,
    background: Color,
): Color {
    val clamped = amount.coerceIn(0f, 1f)
    val base = 1f - clamped
    return Color(
        red = foreground.red * clamped + background.red * base,
        green = foreground.green * clamped + background.green * base,
        blue = foreground.blue * clamped + background.blue * base,
        alpha = foreground.alpha * clamped + background.alpha * base,
    )
}

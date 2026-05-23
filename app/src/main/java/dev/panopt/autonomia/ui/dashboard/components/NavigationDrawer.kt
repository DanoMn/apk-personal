package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import dev.panopt.autonomia.ui.dashboard.BarChartIcon
import dev.panopt.autonomia.ui.dashboard.AnchorIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif

import dev.panopt.autonomia.ui.dashboard.LayerStackIcon
import dev.panopt.autonomia.ui.dashboard.LayoutGridIcon
import dev.panopt.autonomia.ui.dashboard.ListTodoIcon
import dev.panopt.autonomia.ui.dashboard.LogbookIcon
import dev.panopt.autonomia.ui.dashboard.MoonIcon
import dev.panopt.autonomia.ui.dashboard.ShieldAlertIcon
import dev.panopt.autonomia.ui.dashboard.SlidersIcon
import dev.panopt.autonomia.ui.dashboard.SpiralLogo
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun rememberDrawerWidth(maxWidth: Dp): Dp {
    val density = LocalDensity.current
    return remember(maxWidth, density) {
        with(density) {
            min(maxWidth.toPx() * 0.82f, 340.dp.toPx()).toDp()
        }
    }
}

@Composable
internal fun NavigationDrawer(
    palette: DashboardPalette,
    isDarkMode: Boolean,
    width: Dp,
    modifier: Modifier,
    onClose: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onOpenAnchors: () -> Unit,
    onOpenSupports: () -> Unit = {},
    onOpenTasks: () -> Unit,
    onOpenRelapse: () -> Unit,
    onOpenActivitySettings: () -> Unit,
) {
    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .background(palette.drawer)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(17.6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 19.2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.52.dp),
            ) {
                SpiralLogo(
                    color = palette.colorCardboard,
                    modifier = Modifier.size(38.dp),
                )
                Column {
                    Text(
                        text = "Autonomía sin límites",
                        color = palette.colorCardboard,
                        fontFamily = DashboardSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 17.28.sp,
                        lineHeight = 18.66.sp,
                    )
                    Text(
                        text = "Base diaria",
                        color = palette.textMuted,
                        fontSize = 12.48.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            IconButtonShell(
                palette = palette,
                contentDescription = "Cerrar menú",
                onClick = onClose,
            ) {
                XIcon(color = palette.textMain)
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DrawerLink(
                palette = palette,
                text = "Dashboard",
                active = true,
                onClick = onClose,
            ) {
                LayoutGridIcon(color = it)
            }
            DrawerLink(
                palette = palette,
                text = "Mis anclas",
                onClick = {
                    onClose()
                    onOpenAnchors()
                },
            ) {
                AnchorIcon(color = it)
            }
            DrawerLink(
                palette = palette,
                text = "Soportes",
                onClick = {
                    onClose()
                    onOpenSupports()
                },
            ) {
                // Heart-hand icon for support theme
                AnchorIcon(color = it)
            }
            DrawerLink(
                palette = palette,
                text = "Pendientes",
                onClick = {
                    onClose()
                    onOpenTasks()
                },
            ) {
                ListTodoIcon(color = it)
            }
            DrawerLink(
                palette = palette,
                text = "Recaidas",
                onClick = {
                    onClose()
                    onOpenRelapse()
                },
            ) {
                ShieldAlertIcon(color = it)
            }
            DrawerLink(
                palette = palette,
                text = "Progreso",
                onClick = onClose,
            ) {
                BarChartIcon(color = it)
            }
            DrawerLink(
                palette = palette,
                text = "Registros",
                onClick = onClose,
            ) {
                LogbookIcon(color = it)
            }
            DrawerLink(
                palette = palette,
                text = "Capas y actividades",
                onClick = {
                    onClose()
                    onOpenActivitySettings()
                },
            ) {
                LayerStackIcon(color = it)
            }
            DrawerLink(
                palette = palette,
                text = "Configuración",
                onClick = {
                    onClose()
                    onOpenActivitySettings()
                },
            ) {
                SlidersIcon(color = it)
            }
        }

        ThemeToggleRow(
            palette = palette,
            isDarkMode = isDarkMode,
            onToggle = { onThemeChange(!isDarkMode) },
        )

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(palette.bgSurface)
                .padding(14.4.dp),
        ) {
            Text(
                text = "Estado calculado",
                color = palette.textMain,
                fontWeight = FontWeight.Bold,
                fontSize = 14.4.sp,
                lineHeight = 18.sp,
            )
            Spacer(modifier = Modifier.height(3.2.dp))
            Text(
                text = "Room guarda hechos; el dominio interpreta señales.",
                color = palette.textMuted,
                fontSize = 12.48.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
internal fun ThemeToggleRow(
    palette: DashboardPalette,
    isDarkMode: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        MoonIcon(color = palette.textMain, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Modo oscuro",
                color = palette.textMain,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.6.sp,
                lineHeight = 17.sp,
            )
            Text(
                text = if (isDarkMode) "Activo" else "Inactivo",
                color = palette.textMuted,
                fontSize = 12.2.sp,
                lineHeight = 15.sp,
            )
        }
        ThemeSwitch(
            palette = palette,
            checked = isDarkMode,
        )
    }
}

@Composable
internal fun ThemeSwitch(
    palette: DashboardPalette,
    checked: Boolean,
) {
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (checked) mix(palette.colorCoral, 0.34f, palette.bgSurface2) else palette.bgSurface2)
            .padding(3.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(if (checked) palette.colorCoral else palette.textMuted),
        )
    }
}

@Composable
internal fun DrawerLink(
    palette: DashboardPalette,
    text: String,
    active: Boolean = false,
    onClick: () -> Unit,
    icon: @Composable (Color) -> Unit,
) {
    val contentColor = if (active) Color(0xFFEFAA9C) else palette.textMain
    val background = if (active) mix(palette.colorCoral, 0.18f, palette.drawer) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 11.52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.48.dp),
    ) {
        Box(
            modifier = Modifier.size(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            icon(contentColor)
        }
        Text(
            text = text,
            color = contentColor,
            fontSize = 15.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

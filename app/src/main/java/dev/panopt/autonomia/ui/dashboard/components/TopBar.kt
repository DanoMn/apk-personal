package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.MenuIcon
import dev.panopt.autonomia.ui.dashboard.SearchIcon

private val DashboardSans = FontFamily.SansSerif
private val DashboardSerif = FontFamily.Serif

@Composable
internal fun TopBar(
    palette: DashboardPalette,
    onOpenDrawer: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IconButtonShell(
            palette = palette,
            contentDescription = "Abrir menu",
            onClick = onOpenDrawer,
        ) {
            MenuIcon(color = palette.textMain)
        }

        Text(
            text = "Miercoles 20 de mayo",
            modifier = Modifier.weight(1f),
            color = palette.colorCardboard,
            fontFamily = DashboardSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 22.72.sp,
            lineHeight = 23.86.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        IconButtonShell(
            palette = palette,
            contentDescription = "Buscar",
            onClick = {},
        ) {
            SearchIcon(color = palette.textMain)
        }
    }

    Spacer(modifier = Modifier.height(10.dp))
}

@Composable
internal fun IconButtonShell(
    palette: DashboardPalette,
    contentDescription: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

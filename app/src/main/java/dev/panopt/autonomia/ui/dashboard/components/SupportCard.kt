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
import dev.panopt.autonomia.ui.dashboard.CheckIcon
import dev.panopt.autonomia.ui.dashboard.ChecklistIcon
import dev.panopt.autonomia.ui.dashboard.CircleIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.ListTodoIcon

private val DashboardSans = FontFamily.SansSerif

@Composable
internal fun SupportsSection(palette: DashboardPalette) {
    SectionHeader(
        palette = palette,
        title = "Soportes",
        note = "ligero",
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.2.dp),
    ) {
        SupportCard(
            palette = palette,
            title = "Checklist secundaria",
            value = "2/4",
            copy = "cuidado basico",
            first = "Ducha marcada",
            firstChecked = true,
            second = "Dientes pendiente",
            modifier = Modifier.weight(1f),
        ) { iconColor ->
            ChecklistIcon(color = iconColor, modifier = Modifier.size(24.dp))
        }
        SupportCard(
            palette = palette,
            title = "Pendientes",
            value = "3",
            copy = "tareas abiertas",
            first = "Pagar recibo",
            firstChecked = false,
            second = "Comprar cuerdas",
            modifier = Modifier.weight(1f),
        ) { iconColor ->
            ListTodoIcon(color = iconColor, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
internal fun SupportCard(
    palette: DashboardPalette,
    title: String,
    value: String,
    copy: String,
    first: String,
    firstChecked: Boolean,
    second: String,
    modifier: Modifier,
    icon: @Composable (Color) -> Unit,
) {
    Column(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgSurface)
            .clickable(role = Role.Button, onClick = {})
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = title,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                lineHeight = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Box(
                modifier = Modifier.size(26.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon(palette.textMuted)
            }
        }
        Column {
            Text(
                text = value,
                color = palette.colorCardboard,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 26.sp,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = copy,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 14.sp,
                lineHeight = 16.sp,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            SupportListLine(
                palette = palette,
                text = first,
                checked = firstChecked,
            )
            SupportListLine(
                palette = palette,
                text = second,
                checked = false,
            )
        }
    }
}

@Composable
internal fun SupportListLine(
    palette: DashboardPalette,
    text: String,
    checked: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        if (checked) {
            CheckIcon(color = palette.textMuted, modifier = Modifier.size(16.dp))
        } else {
            CircleIcon(color = palette.textMuted, modifier = Modifier.size(16.dp))
        }
        Text(
            text = text,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 13.5.sp,
            lineHeight = 15.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

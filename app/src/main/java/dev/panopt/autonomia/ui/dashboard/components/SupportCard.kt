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
import dev.panopt.autonomia.ui.dashboard.AnchorIcon
import dev.panopt.autonomia.ui.dashboard.CircleIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.domain.dashboard.DashboardSupportKind
import dev.panopt.autonomia.domain.dashboard.DashboardSupportState
import dev.panopt.autonomia.ui.dashboard.ListTodoIcon

private val DashboardSans = FontFamily.SansSerif

@Composable
internal fun SupportsSection(
    palette: DashboardPalette,
    supports: List<DashboardSupportState>,
    onOpenSupports: () -> Unit,
    onOpenTasks: () -> Unit,
) {
    SectionHeader(
        palette = palette,
        title = "Soportes",
        note = "ligero",
    )

    if (supports.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.2.dp),
    ) {
        supports.forEach { support ->
            SupportCard(
                palette = palette,
                support = support,
                modifier = Modifier.weight(1f),
                onClick = when (support.kind) {
                    DashboardSupportKind.Support -> onOpenSupports
                    DashboardSupportKind.Tasks -> onOpenTasks
                },
            )
        }
    }
}

@Composable
internal fun SupportCard(
    palette: DashboardPalette,
    support: DashboardSupportState,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .height(148.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = support.title,
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
                SupportIcon(kind = support.kind, color = palette.textMuted)
            }
        }
        Column {
            Text(
                text = support.value,
                color = palette.colorCardboard,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 26.sp,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = support.copy,
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
                text = support.first,
                checked = support.firstChecked,
            )
            SupportListLine(
                palette = palette,
                text = support.second,
                checked = support.secondChecked,
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

@Composable
private fun SupportIcon(
    kind: DashboardSupportKind,
    color: Color,
) {
    when (kind) {
        DashboardSupportKind.Support -> AnchorIcon(color = color, modifier = Modifier.size(24.dp))
        DashboardSupportKind.Tasks -> ListTodoIcon(color = color, modifier = Modifier.size(24.dp))
    }
}

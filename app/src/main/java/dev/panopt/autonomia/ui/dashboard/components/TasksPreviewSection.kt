package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardTaskState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun TasksPreviewSection(
    palette: DashboardPalette,
    tasks: List<DashboardTaskState>,
    onOpenTasks: () -> Unit,
) {
    SectionHeader(
        palette = palette.copy(),
        title = "Pendientes",
        note = if (tasks.isEmpty()) "Sin pendientes" else "${tasks.size} abiertos",
        titleColor = palette.textMuted,
        titleSize = 15f,
    )

    if (tasks.isEmpty()) {
        Text(
            text = "Sin pendientes",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(mix(palette.bgSurface, 0.6f, palette.bgBase))
                .clickable(role = Role.Button, onClick = onOpenTasks)
                .padding(16.dp),
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 14.sp,
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(mix(palette.bgSurface, 0.6f, palette.bgBase))
            .clickable(role = Role.Button, onClick = onOpenTasks)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tasks.take(2).forEach { task ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(palette.textMuted.copy(alpha = 0.4f)),
                )
                Text(
                    text = task.title,
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (tasks.size > 2) {
            Text(
                text = "+${tasks.size - 2} más",
                color = palette.textMuted.copy(alpha = 0.6f),
                fontFamily = DashboardSans,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 18.dp),
            )
        }
    }
}

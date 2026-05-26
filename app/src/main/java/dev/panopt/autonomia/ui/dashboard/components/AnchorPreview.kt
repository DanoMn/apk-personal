package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans

@Composable
internal fun AnchorPreviewSection(
    palette: DashboardPalette,
    items: List<DashboardCheckItemState>,
    onToggle: (String, Boolean) -> Unit,
    onOpenActivityInput: (String) -> Unit = {},
) {
    val pendingItems = items.filterNot { it.completed }
    val completedItems = items.filter { it.completed }

    SectionHeader(
        palette = palette,
        title = "Anclas pendientes",
        note = if (pendingItems.size == 1) "1 pendiente" else "${pendingItems.size} pendientes",
    )

    if (items.isEmpty()) {
        Text(
            text = "Sin anclas configuradas. Agregá actividades a tu base diaria.",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(palette.bgSurface)
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
            .animateContentSize(animationSpec = tween(durationMillis = 220))
            .clip(RoundedCornerShape(20.dp))
            .background(palette.bgSurface)
            .padding(5.6.dp),
    ) {
        pendingItems.forEach { item ->
            key(item.id) {
                CheckItem(
                    palette = palette,
                    item = item,
                    checked = false,
                    onToggle = { onToggle(item.id, true) },
                    onLongToggle = { onOpenActivityInput(item.id) },
                )
            }
        }
        if (completedItems.isNotEmpty()) {
            CompletedDivider(palette = palette)
        }
        completedItems.forEach { item ->
            key(item.id) {
                CheckItem(
                    palette = palette,
                    item = item,
                    checked = true,
                    onToggle = { onToggle(item.id, false) },
                    onLongToggle = { onOpenActivityInput(item.id) },
                )
            }
        }
    }
}

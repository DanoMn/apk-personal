package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette

@Composable
internal fun AnchorPreviewSection(
    palette: DashboardPalette,
    items: List<DashboardCheckItemState>,
    onToggle: (String, Boolean) -> Unit,
) {
    val pendingItems = items.filterNot { it.completed }
    val completedItems = items.filter { it.completed }

    SectionHeader(
        palette = palette,
        title = "Anclas pendientes",
        note = if (pendingItems.size == 1) "1 pendiente" else "${pendingItems.size} pendientes",
    )

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
                    onToggle = {
                        onToggle(item.id, true)
                    },
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
                    onToggle = {
                        onToggle(item.id, false)
                    },
                )
            }
        }
    }
}

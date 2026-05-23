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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans

/**
 * Renders individual support items as checkboxes with inverted semantics.
 * Checked means "did NOT do it today" — awareness signal, not completion.
 */
@Composable
internal fun SupportsPreviewSection(
    palette: DashboardPalette,
    items: List<DashboardCheckItemState>,
    onToggle: (String) -> Unit,
    onOpenConfig: () -> Unit,
) {
    // Count "completed (done)" = unchecked items (positive, did it today)
    val doneCount = items.count { !it.completed }
    val total = items.size

    SectionHeader(
        palette = palette,
        title = "Soportes",
        note = if (items.isEmpty()) "Sin soportes" else "$doneCount/$total pendientes",
    )

    if (items.isEmpty()) {
        Text(
            text = "Sin soportes configurados",
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
        items.forEach { item ->
            key(item.id) {
                CheckItem(
                    palette = palette,
                    item = item,
                    checked = item.completed,
                    onToggle = { onToggle(item.id) },
                    isInverted = true,
                )
            }
        }

        // Navigate to full config
        Text(
            text = "editar soportes",
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 7.2.dp, bottom = 2.88.dp),
            color = palette.colorCoral,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.16.sp,
        )
    }
}

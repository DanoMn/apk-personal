package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans

/**
 * Renders the support checklist with inverted semantics.
 * Collapsed by default — expands to show daily check items.
 * Smaller visual weight than AnchorPreviewSection.
 */
@Composable
internal fun SupportsPreviewSection(
    palette: DashboardPalette,
    items: List<DashboardCheckItemState>,
    onToggle: (String) -> Unit,
    onOpenConfig: () -> Unit,
    onResetAll: () -> Unit = {},
    onToggleAll: () -> Unit = {},
    onSaveChecklist: () -> Unit = {},
) {
    val doneCount = items.count { !it.completed }
    val total = items.size
    val hasOmissions = items.any { it.completed }
    var isExpanded by remember { mutableStateOf(false) }
    var saveConfirmed by remember { mutableStateOf(false) }

    // Auto-reset save confirmation after 2 seconds
    LaunchedEffect(saveConfirmed) {
        if (saveConfirmed) {
            delay(2000)
            saveConfirmed = false
        }
    }

    if (items.isEmpty()) return

    // ── Collapsed header row (always visible) ──
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface.copy(alpha = 0.55f))
            .clickable(role = Role.Button) { isExpanded = !isExpanded }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isExpanded) "\u25be" else "\u25b8",
                color = palette.textMuted,
                fontSize = 15.sp,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Soportes",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }
        Text(
            text = "$doneCount/$total hoy",
            color = palette.textMuted.copy(alpha = 0.7f),
            fontFamily = DashboardSans,
            fontSize = 13.sp,
        )
    }

    // ── Expanded content ──
    AnimatedVisibility(
        visible = isExpanded,
        enter = expandVertically(animationSpec = tween(200)),
        exit = shrinkVertically(animationSpec = tween(200)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        ) {
            // Indicator text: inverted semantics explanation
            Text(
                text = "Todo cumplido por defecto. Desmarc\u00e1 solo lo que no hiciste hoy.",
                color = palette.textMuted.copy(alpha = 0.6f),
                fontFamily = DashboardSans,
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            )

            // Checklist container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(220))
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.bgSurface.copy(alpha = 0.55f))
                    .padding(5.6.dp),
            ) {
                items.forEach { item ->
                    key(item.id) {
                        CheckItemSmall(
                            palette = palette,
                            item = item,
                            checked = item.completed,
                            onToggle = { onToggle(item.id) },
                        )
                    }
                }

                // Bottom action row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // "editar soportes" link
                    Text(
                        text = "editar soportes",
                        color = palette.colorCoral,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onOpenConfig() },
                    )
                    // Action buttons row
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Toggle all button
                        Text(
                            text = if (hasOmissions) "Desmarcar todo" else "Marcar todo",
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            modifier = Modifier.clickable { onToggleAll() },
                        )
                        // Save button
                        if (hasOmissions || saveConfirmed) {
                            Text(
                                text = if (saveConfirmed) "Guardado ✓" else "Guardar",
                                color = if (saveConfirmed) palette.bgSurface2 else palette.colorCoral,
                                fontFamily = DashboardSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                modifier = Modifier.clickable {
                                    onSaveChecklist()
                                    saveConfirmed = true
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Smaller variant of CheckItem for Support items.
 * Reduced height, muted colors, amber checkbox (not coral).
 */
@Composable
private fun CheckItemSmall(
    palette: DashboardPalette,
    item: DashboardCheckItemState,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val rowAlpha by animateFloatAsState(
        targetValue = if (checked) 0.55f else 1f,
        animationSpec = tween(180),
        label = "checkItemSmallAlpha",
    )
    val titleColor by animateColorAsState(
        targetValue = if (checked) palette.textMuted else palette.textMain,
        animationSpec = tween(180),
        label = "checkItemSmallColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .alpha(rowAlpha)
            .clip(RoundedCornerShape(12.dp))
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Smaller checkbox, amber color for inverted
        CheckBoxMark(
            palette = palette,
            checked = checked,
            isInverted = true,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                color = titleColor,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                lineHeight = 16.sp,
                textDecoration = if (checked) TextDecoration.LineThrough
                    else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

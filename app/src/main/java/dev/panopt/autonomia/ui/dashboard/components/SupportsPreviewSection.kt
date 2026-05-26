package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.mix

/**
 * Renders the support checklist with inverted semantics (chips layout).
 * All items are checked by default, user unchecks what they omitted.
 * Collapsible layout.
 */
@OptIn(ExperimentalLayoutApi::class)
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

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Separacion explicita de las anclas (reducida para mejor ritmo visual)
        Spacer(modifier = Modifier.height(12.dp))

        // 2. Boton masivo y notorio para desplegar
        val buttonBgColor = if (isExpanded) palette.bgSurface2 else mix(palette.colorCardboard, 0.12f, palette.bgSurface)
        val accentColor = if (isExpanded) palette.textMuted else palette.colorCardboard

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(buttonBgColor)
                .clickable(role = Role.Button) { isExpanded = !isExpanded }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Flecha notoria
                    Text(
                        text = if (isExpanded) "\u25bc" else "\u25ba",
                        color = accentColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                    // Titulo fuerte
                    Text(
                        text = "Soportes Diarios",
                        color = palette.textMain,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                // Conteo rapido
                Text(
                    text = "$doneCount / $total",
                    color = accentColor,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Expanded Content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp) // Separacion del boton masivo
                    .clip(RoundedCornerShape(20.dp))
                    .background(palette.bgSurface.copy(alpha = 0.85f))
                    .padding(16.dp),
            ) {
                // Indicator text
                Text(
                    text = "Todo cumplido por defecto. Desmarc\u00e1 solo lo que no hiciste hoy.",
                    color = palette.textMuted.copy(alpha = 0.8f),
                    fontFamily = DashboardSans,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 14.dp),
                )

                // Chips flow
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items.forEach { item ->
                        key(item.id) {
                            SupportChip(
                                palette = palette,
                                item = item,
                                checked = !item.completed, // inverted: true if DONE (not omitted)
                                onToggle = { onToggle(item.id) },
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Desmarcar todo
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, palette.textFaint, RoundedCornerShape(12.dp))
                            .clickable(role = Role.Button, onClick = onToggleAll),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (hasOmissions) "Restaurar todo" else "Desmarcar todo",
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                        )
                    }
                    // Guardar
                    val isSaved = saveConfirmed
                    val saveBgColor = if (isSaved) mix(palette.colorCoral, 0.15f, palette.bgSurface2) else palette.colorCoral
                    val saveTextColor = if (isSaved) palette.textFaint else palette.bgBase
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(saveBgColor)
                            .clickable(role = Role.Button, enabled = !isSaved) {
                                onSaveChecklist()
                                saveConfirmed = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isSaved) "Guardado \u2713" else "Guardar Registro",
                            color = saveTextColor,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SupportChip(
    palette: DashboardPalette,
    item: DashboardCheckItemState,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val alphaAnim by animateFloatAsState(
        targetValue = if (checked) 1f else 0.45f,
        animationSpec = tween(200),
        label = "chipAlpha"
    )
    val bgColor by animateColorAsState(
        targetValue = if (checked) mix(palette.colorCardboard, 0.15f, palette.bgSurface) else palette.bgSurface2,
        animationSpec = tween(200),
        label = "chipBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (checked) palette.colorCardboard else palette.textMuted,
        animationSpec = tween(200),
        label = "chipText"
    )

    Row(
        modifier = Modifier
            .alpha(alphaAnim)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // Simple dot or checkmark
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (checked) palette.colorCardboard else palette.textFaint),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Text(
                    text = "\u2713", // Checkmark
                    color = palette.bgBase,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            text = item.title,
            color = textColor,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            textDecoration = if (checked) TextDecoration.None else TextDecoration.LineThrough,
        )
    }
}

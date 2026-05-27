package dev.panopt.autonomia.ui.supports

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.ui.anchors.LayerStampSmall
import dev.panopt.autonomia.ui.anchors.layerColor
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans

/**
 * Bottom sheet panel for quick support configuration.
 *
 * Deferred save: removals are not persisted until the panel closes via
 * DisposableEffect. Users can toggle supports off (move to "Agregar"
 * section) and recover them within the same session.
 */
@Composable
internal fun SupportsConfigPanel(
    activityOptions: List<DashboardActivityOptionState>,
    palette: DashboardPalette,
    onRemoveSupport: (String) -> Unit = {},
    onOpenFullConfig: () -> Unit = {},
) {
    val currentSupports = activityOptions.filter {
        it.activityType == "Support" && it.isConfigured
    }

    // Track which items were removed during this session
    val removedIds = remember { mutableStateMapOf<String, Boolean>() }

    // Persist removals when the panel leaves composition
    DisposableEffect(Unit) {
        onDispose {
            removedIds.keys.forEach { id -> onRemoveSupport(id) }
        }
    }

    // "Mis soportes": configured supports minus removed-in-session
    val mySupports = currentSupports.filter { it.id !in removedIds }
    // "Agregar soporte": items that were in current supports but removed this session
    val recoverableItems = currentSupports.filter { it.id in removedIds }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp),
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Soportes",
                color = palette.colorCardboard,
                fontFamily = dev.panopt.autonomia.ui.dashboard.DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 24.sp,
            )
            Text(
                text = "${currentSupports.size} activos",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.5.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
        // Section: "Mis soportes"
        if (mySupports.isEmpty()) {
            Text(
                text = "Sin soportes configurados.\nUsá la pantalla de configuración para agregar.",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            mySupports.forEach { support ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LayerStampSmall(
                        layerId = support.layerId,
                        color = layerColor(support.layerId, palette),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = support.title,
                            color = palette.textMain,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = support.layerName,
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontSize = 12.sp,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                dev.panopt.autonomia.ui.dashboard.mix(
                                    palette.risk, 0.16f, palette.bgSurface,
                                )
                            )
                            .clickable(role = Role.Button) { removedIds[support.id] = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Quitar",
                            color = palette.risk,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }

        // Section: "Agregar soporte" (recoverable items)
        if (recoverableItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Agregar soporte",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            recoverableItems.forEach { support ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.bgSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    LayerStampSmall(
                        layerId = support.layerId,
                        color = layerColor(support.layerId, palette),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = support.title,
                            color = palette.textMain,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = support.layerName,
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontSize = 12.sp,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(palette.colorCardboard)
                            .clickable(role = Role.Button) { removedIds.remove(support.id) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Agregar",
                            color = palette.bgBase,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
        } // Inner Column ends here

        // "Ver catálogo completo" button
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.colorCardboard)
                .clickable(role = Role.Button, onClick = onOpenFullConfig),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Ver catálogo completo",
                color = palette.bgBase,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            )
        }
    }
}

package dev.panopt.autonomia.ui.supports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.ui.anchors.ConfirmDeleteActivityDialog
import dev.panopt.autonomia.ui.anchors.ConfigScreenContainer
import dev.panopt.autonomia.ui.anchors.LayerStamp
import dev.panopt.autonomia.ui.anchors.isCustomActivityId
import dev.panopt.autonomia.ui.anchors.layerColor
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun SupportsConfigScreen(
    layers: List<DashboardLayerState>,
    supportItems: List<DashboardCheckItemState>,
    supportOptions: List<DashboardActivityOptionState>,
    palette: DashboardPalette,
    onAddSupport: (String) -> Unit,
    onCreateSupport: (String, String) -> Unit,
    onRemoveSupport: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var isCreatingCustom by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var selectedLayerId by remember { mutableStateOf(layers.firstOrNull()?.id.orEmpty()) }
    var selectedLayerFilter by remember { mutableStateOf<String?>(null) }
    var supportPendingDeletion by remember { mutableStateOf<DashboardCheckItemState?>(null) }

    // Filter only applies to catalog options (not configured supports)
    val filteredOptions = if (selectedLayerFilter == null) {
        supportOptions
    } else {
        supportOptions.filter { it.layerId == selectedLayerFilter }
    }

    ConfigScreenContainer(palette = palette) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.bgSurface)
                    .clickable(role = Role.Button, onClick = onBack),
                contentAlignment = Alignment.Center,
            ) { XIcon(color = palette.textMain) }
            Spacer(modifier = Modifier.width(14.dp))
            Text("Soportes", color = palette.colorCardboard, fontFamily = DashboardSerif, fontWeight = FontWeight.Medium, fontSize = 24.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("${supportItems.size} activos", color = palette.colorCoral, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        }

        if (isCreatingCustom) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            text = "Nuevo soporte",
                            color = palette.colorCardboard,
                            fontFamily = DashboardSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 23.sp,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = "Nombre",
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                        )
                        BasicTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            textStyle = TextStyle(
                                color = palette.textMain,
                                fontFamily = DashboardSans,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp,
                                textAlign = TextAlign.Center,
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(palette.bgSurface2)
                                .padding(horizontal = 16.dp, vertical = 17.dp),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (customName.isBlank()) {
                                        Text(
                                            text = "Ej. Tender la cama",
                                            color = palette.textFaint,
                                            fontFamily = DashboardSans,
                                            fontSize = 16.sp,
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                        Text(
                            text = "Capa",
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            layers.forEach { layer ->
                                val selected = layer.id == selectedLayerId
                                val color = layerColor(layer.id, palette)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(74.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(
                                            if (selected) mix(color, 0.22f, palette.bgSurface)
                                            else palette.bgSurface,
                                        )
                                        .clickable(role = Role.Button) { selectedLayerId = layer.id },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.padding(horizontal = 4.dp),
                                    ) {
                                        LayerStamp(
                                            layerId = layer.id,
                                            color = if (selected) color else palette.textMuted,
                                            size = 26,
                                        )
                                        Text(
                                            text = layer.name,
                                            color = if (selected) color else palette.textMuted,
                                            fontFamily = DashboardSans,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 10.5.sp,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                val canCreate = customName.isNotBlank() && selectedLayerId.isNotBlank()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(top = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.bgSurface)
                            .clickable(role = Role.Button, onClick = { isCreatingCustom = false }),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Cancelar", color = palette.textMain, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (canCreate) palette.colorCardboard else palette.bgSurface2)
                            .clickable(role = Role.Button, enabled = canCreate) {
                                onCreateSupport(customName.trim(), selectedLayerId)
                                customName = ""
                                isCreatingCustom = false
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Agregar",
                            color = if (canCreate) palette.bgBase else palette.textFaint,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Soportes actuales", color = palette.textMuted, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (supportItems.isEmpty()) {
                    Text("Sin soportes configurados.", color = palette.textMuted, fontFamily = DashboardSans, fontSize = 14.sp)
                } else {
                    supportItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.bgSurface).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            LayerStamp(
                                layerId = item.layerId,
                                color = layerColor(item.layerId, palette),
                                size = 22,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.title, color = palette.textMain, fontFamily = DashboardSans, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                                Text(item.layerName, color = palette.textMuted, fontFamily = DashboardSans, fontSize = 12.sp)
                            }
                            if (isCustomActivityId(item.id)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(mix(palette.risk, 0.16f, palette.bgSurface))
                                        .clickable(
                                            role = Role.Button,
                                            onClick = { supportPendingDeletion = item },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "X",
                                        color = palette.risk,
                                        fontFamily = DashboardSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                            Box(modifier = Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(mix(palette.risk, 0.16f, palette.bgSurface)).clickable(role = Role.Button, onClick = { onRemoveSupport(item.id) }).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                Text("Quitar", color = palette.risk, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Agregar soporte", color = palette.textMuted, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (filteredOptions.isEmpty()) {
                    Text(
                        if (selectedLayerFilter != null) "No quedan soportes en esta capa."
                        else "No quedan soportes del catálogo para agregar.",
                        color = palette.textMuted, fontFamily = DashboardSans, fontSize = 14.sp,
                    )
                } else {
                    filteredOptions.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.bgSurface).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            LayerStamp(
                                layerId = option.layerId,
                                color = layerColor(option.layerId, palette),
                                size = 22,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(option.title, color = palette.textMain, fontFamily = DashboardSans, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                                Text(option.layerName, color = palette.textMuted, fontFamily = DashboardSans, fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(palette.colorCardboard).clickable(role = Role.Button, onClick = { onAddSupport(option.id) }).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                                Text("Agregar", color = palette.bgBase, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Create button — pinned above filter chips, inside else (list view only)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.colorCardboard)
                    .clickable(role = Role.Button, onClick = { isCreatingCustom = true }),
                contentAlignment = Alignment.Center,
            ) {
                Text("+ Crear soporte personalizado", color = palette.bgBase, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // Bottom pinned layer filter chips — list view only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                layers.forEach { layer ->
                    val isSelected = selectedLayerFilter == layer.id
                    val color = layerColor(layer.id, palette)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) mix(color, 0.22f, palette.bgSurface)
                                else palette.bgSurface,
                            )
                            .clickable(role = Role.Button) {
                                selectedLayerFilter = if (isSelected) null else layer.id
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            LayerStamp(
                                layerId = layer.id,
                                color = if (isSelected) color else palette.textMuted,
                                size = 20,
                            )
                            Text(
                                text = layer.name,
                                color = if (isSelected) color else palette.textMuted,
                                fontFamily = DashboardSans,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 9.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        supportPendingDeletion?.let { item ->
            ConfirmDeleteActivityDialog(
                activityTitle = item.title,
                surfaceName = "soportes",
                palette = palette,
                onDismiss = { supportPendingDeletion = null },
                onConfirm = {
                    onDeleteActivity(item.id)
                    supportPendingDeletion = null
                },
            )
        }
    }
}

package dev.panopt.autonomia.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.domain.onboarding.OnboardingAnchorsRule
import dev.panopt.autonomia.ui.dashboard.DashboardPalette

/**
 * Bloque Anclas del onboarding (slice 2). El usuario elige 3 anclas en 3 capas
 * distintas — del catálogo o creando propias. NO se piden targets (toman defaults,
 * se afinan después). "Continuar" se habilita solo cuando se cumple la compuerta del
 * motor ([OnboardingAnchorsRule]).
 */
@Composable
internal fun OnboardingAnchorsStep(
    palette: DashboardPalette,
    layers: List<DashboardLayerState>,
    options: List<DashboardActivityOptionState>,
    onAddAnchor: (activityId: String) -> Unit,
    onCreateAnchor: (name: String, layerId: String) -> Unit,
    onRemoveAnchor: (activityId: String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val configuredAnchors = options.filter {
        it.isConfigured && it.activityType == ActivitySurface.Anchor.name
    }
    val coveredLayers = OnboardingAnchorsRule.distinctLayersWithAnchor(configuredAnchors.map { it.layerId })
    val canAdvance = OnboardingAnchorsRule.canAdvance(configuredAnchors.map { it.layerId })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Tus anclas",
            color = palette.textMain,
            fontSize = 28.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Un ancla es una práctica pequeña que te sostiene. No tiene que ser grande: " +
                "una página leída, un vaso de agua, una caminata. Elige al menos tres, en tres " +
                "áreas distintas de tu vida.",
            color = palette.textMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$coveredLayers de ${OnboardingAnchorsRule.minLayers} áreas con un ancla",
            color = if (canAdvance) palette.colorCoral else palette.textFaint,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(20.dp))

        layers.forEach { layer ->
            LayerSection(
                palette = palette,
                layer = layer,
                anchorsInLayer = configuredAnchors.filter { it.layerId == layer.id },
                availableInLayer = options.filter { it.layerId == layer.id && !it.isConfigured },
                onAddAnchor = onAddAnchor,
                onRemoveAnchor = onRemoveAnchor,
            )
            Spacer(Modifier.height(16.dp))
        }

        CreateOwnAnchor(palette = palette, layers = layers, onCreateAnchor = onCreateAnchor)
        Spacer(Modifier.height(28.dp))

        OnboardingPrimaryButton(
            palette = palette,
            label = "Continuar",
            enabled = canAdvance,
            onClick = onContinue,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Volver",
            color = palette.textFaint,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onBack)
                .padding(vertical = 12.dp),
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LayerSection(
    palette: DashboardPalette,
    layer: DashboardLayerState,
    anchorsInLayer: List<DashboardActivityOptionState>,
    availableInLayer: List<DashboardActivityOptionState>,
    onAddAnchor: (String) -> Unit,
    onRemoveAnchor: (String) -> Unit,
) {
    Text(
        text = layer.name,
        color = palette.textMain,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    anchorsInLayer.forEach { anchor ->
        AnchorRow(
            palette = palette,
            label = anchor.title,
            actionLabel = "Quitar",
            selected = true,
            onClick = { onRemoveAnchor(anchor.id) },
        )
    }
    availableInLayer.forEach { option ->
        AnchorRow(
            palette = palette,
            label = option.title,
            actionLabel = "Agregar",
            selected = false,
            onClick = { onAddAnchor(option.id) },
        )
    }
}

@Composable
private fun AnchorRow(
    palette: DashboardPalette,
    label: String,
    actionLabel: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.colorCardboardSoft else palette.bgSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            color = palette.textMain,
            fontSize = 15.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = actionLabel,
            color = if (selected) palette.textFaint else palette.colorCoral,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun CreateOwnAnchor(
    palette: DashboardPalette,
    layers: List<DashboardLayerState>,
    onCreateAnchor: (name: String, layerId: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedLayerId by remember { mutableStateOf(layers.firstOrNull()?.id) }

    Text(
        text = "Crear un ancla propia",
        color = palette.textMain,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.bgSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = TextStyle(color = palette.textMain, fontSize = 15.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(palette.colorCoral),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (name.isEmpty()) {
                    Text("Nombre del ancla", color = palette.textFaint, fontSize = 15.sp)
                }
                inner()
            },
        )
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        layers.forEach { layer ->
            val isSelected = layer.id == selectedLayerId
            Text(
                text = layer.name,
                color = if (isSelected) palette.bgBase else palette.textMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) palette.colorCoral else palette.bgSurface)
                    .clickable { selectedLayerId = layer.id }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    val canCreate = name.isNotBlank() && selectedLayerId != null
    Text(
        text = "Agregar ancla propia",
        color = if (canCreate) palette.colorCoral else palette.textFaint,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = canCreate) {
                selectedLayerId?.let { layerId ->
                    onCreateAnchor(name.trim(), layerId)
                    name = ""
                }
            }
            .padding(vertical = 8.dp),
    )
}

package dev.panopt.autonomia.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.domain.onboarding.OnboardingAnchorsRule
import dev.panopt.autonomia.ui.anchors.AnchorConfigScreen
import dev.panopt.autonomia.ui.dashboard.DashboardPalette

/**
 * Bloque Anclas del onboarding. NO reinventa el selector: reusa la pantalla real
 * [AnchorConfigScreen] (buscador, filtros por capa, editor de targets, crear custom) y le inyecta,
 * por dos slots la guía propia del onboarding:
 *  - `subHeader`: banner compacto de progreso ("N/3") bajo el header (siempre visible en este paso).
 *  - `listFooter`: el botón "Continuar" al fondo (más fácil de alcanzar), solo al cumplir la
 *    compuerta ([OnboardingAnchorsRule]).
 * El fondo (buscador + filtros) queda intacto. Una sola fuente de verdad para el UI de anclas.
 */
@Composable
internal fun OnboardingAnchorsStep(
    palette: DashboardPalette,
    layers: List<DashboardLayerState>,
    options: List<DashboardActivityOptionState>,
    onAddAnchor: (activityId: String, sessionTargetMinutes: Int, weeklyFrequencyTarget: Int, commitmentDurationMonths: Int?) -> Unit,
    onRemoveAnchor: (activityId: String) -> Unit,
    onCreateActivity: (name: String, layerId: String, sessionTargetMinutes: Int, isSecondary: Boolean, weeklyFrequencyTarget: Int, commitmentDurationMonths: Int?) -> Unit,
    onDeleteActivity: (activityId: String) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val configuredAnchorLayers = options
        .filter { it.isConfigured && it.activityType == ActivitySurface.Anchor.name }
        .map { it.layerId }
    val coveredLayers = OnboardingAnchorsRule.distinctLayersWithAnchor(configuredAnchorLayers)
    val canAdvance = OnboardingAnchorsRule.canAdvance(configuredAnchorLayers)

    AnchorConfigScreen(
        layers = layers,
        activityOptions = options,
        palette = palette,
        onAddAnchor = onAddAnchor,
        onRemoveAnchor = onRemoveAnchor,
        onDeleteActivity = onDeleteActivity,
        onCreateActivity = onCreateActivity,
        onBack = onBack,
        subHeader = {
            OnboardingAnchorsBanner(
                palette = palette,
                coveredLayers = coveredLayers,
                canAdvance = canAdvance,
            )
        },
        listFooter = {
            // Solo al abrir la compuerta: el CTA vive al fondo, fácil de alcanzar.
            if (canAdvance) {
                OnboardingPrimaryButton(
                    palette = palette,
                    label = "Continuar",
                    enabled = true,
                    onClick = onContinue,
                )
            }
        },
    )
}

/**
 * Banner de guía bajo el header: una sola fila, compacta. Muestra el progreso de cobertura ("N/3")
 * y un texto que pasa de instructivo a positivo al completar. No empuja ni tapa el buscador/filtros
 * del fondo; el CTA "Continuar" vive aparte, en el `listFooter`.
 */
@Composable
private fun OnboardingAnchorsBanner(
    palette: DashboardPalette,
    coveredLayers: Int,
    canAdvance: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.bgSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(palette.colorCoral.copy(alpha = 0.16f))
                .padding(horizontal = 9.dp, vertical = 5.dp),
        ) {
            Text(
                text = "$coveredLayers/${OnboardingAnchorsRule.minLayers}",
                color = palette.colorCoral,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = if (canAdvance) {
                "Tus ${OnboardingAnchorsRule.minLayers} áreas tienen un ancla"
            } else {
                "Sumá anclas en ${OnboardingAnchorsRule.minLayers} áreas distintas para continuar"
            },
            color = palette.textMuted,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

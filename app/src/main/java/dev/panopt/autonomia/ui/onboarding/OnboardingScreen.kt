package dev.panopt.autonomia.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.domain.dashboard.DashboardLayerState
import dev.panopt.autonomia.domain.onboarding.OnboardingState
import dev.panopt.autonomia.domain.onboarding.OnboardingStep
import dev.panopt.autonomia.ui.dashboard.DashboardPalette

/**
 * Esqueleto del onboarding de introducción (slice 1). El Bloque 0 (Bienvenida) y el
 * Bloque 4 (Cierre) traen su copy canónico; los bloques intermedios son placeholders
 * navegables que se implementan en los slices 2-4. No impone las compuertas del motor.
 */
@Composable
internal fun OnboardingScreen(
    state: OnboardingState,
    palette: DashboardPalette,
    onAdvance: () -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    layers: List<DashboardLayerState> = emptyList(),
    anchorOptions: List<DashboardActivityOptionState> = emptyList(),
    onAddAnchor: (activityId: String) -> Unit = {},
    onCreateAnchor: (name: String, layerId: String) -> Unit = { _, _ -> },
    onRemoveAnchor: (activityId: String) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        when (state.currentStep) {
            OnboardingStep.Welcome -> OnboardingBlock(
                palette = palette,
                title = "Autonomía sin límites",
                body = "Toda vida descansa sobre cimientos que no siempre vemos: el descanso, el " +
                    "cuerpo, el orden de los días, los otros, aquello que construimos. Cuando uno " +
                    "cede, lo demás empieza a inclinarse.\n\n" +
                    "Esta app no pretende medir esa abstracción inalcanzable que llamamos " +
                    "felicidad, sino algo más sencillo y más noble: saber si tus cimientos siguen " +
                    "en pie.\n\n" +
                    "A ese conjunto de cimientos lo llamamos tu base. Vamos a reconocerla juntos, " +
                    "sin apuro y sin exigir perfección.",
                primaryLabel = "Empecemos",
                onPrimary = onAdvance,
            )

            OnboardingStep.Closing -> OnboardingBlock(
                palette = palette,
                title = "Tus cimientos están en pie",
                body = "Esto es el comienzo de un viaje, no un examen. Podrás ajustar todo cuando " +
                    "quieras.\n\n" +
                    "Y si algún día el rigor decae, la app no te condena: solo te recuerda, con la " +
                    "calma de un adulto funcional, que es momento de volver a la base, de volver al " +
                    "cuerpo, y recomenzar.",
                primaryLabel = "Entrar",
                onPrimary = onComplete,
                onBack = onBack,
            )

            OnboardingStep.Anchors -> OnboardingAnchorsStep(
                palette = palette,
                layers = layers,
                options = anchorOptions,
                onAddAnchor = onAddAnchor,
                onCreateAnchor = onCreateAnchor,
                onRemoveAnchor = onRemoveAnchor,
                onContinue = onAdvance,
                onBack = onBack,
            )

            else -> OnboardingBlock(
                palette = palette,
                title = placeholderTitle(state.currentStep),
                body = "Esta sección se construye en el siguiente paso del desarrollo.",
                primaryLabel = "Continuar",
                onPrimary = onAdvance,
                onBack = onBack,
            )
        }
    }
}

private fun placeholderTitle(step: OnboardingStep): String =
    when (step) {
        OnboardingStep.Intention -> "¿Qué te trae aquí?"
        OnboardingStep.Anchors -> "Tus anclas"
        OnboardingStep.Sleep -> "El descanso primero"
        OnboardingStep.Sobriety -> "Cuidar algo que te cuesta"
        else -> ""
    }

@Composable
private fun OnboardingBlock(
    palette: DashboardPalette,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            color = palette.textMain,
            fontSize = 30.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 36.sp,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = body,
            color = palette.textMuted,
            fontSize = 16.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(36.dp))
        OnboardingPrimaryButton(palette = palette, label = primaryLabel, onClick = onPrimary)
        if (onBack != null) {
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
        }
    }
}

@Composable
internal fun OnboardingPrimaryButton(
    palette: DashboardPalette,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (enabled) palette.bgBase else palette.textFaint,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) palette.colorCoral else palette.bgSurface)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 16.dp),
    )
}

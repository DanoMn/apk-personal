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
import dev.panopt.autonomia.domain.dashboard.DashboardSleepState
import dev.panopt.autonomia.domain.onboarding.OnboardingIntention
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
    onAddAnchor: (activityId: String, sessionTargetMinutes: Int, weeklyFrequencyTarget: Int, commitmentDurationMonths: Int?) -> Unit = { _, _, _, _ -> },
    onRemoveAnchor: (activityId: String) -> Unit = {},
    onCreateActivity: (name: String, layerId: String, sessionTargetMinutes: Int, isSecondary: Boolean, weeklyFrequencyTarget: Int, commitmentDurationMonths: Int?) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteActivity: (activityId: String) -> Unit = {},
    // Bloque Intención (slice 4)
    intention: OnboardingIntention? = null,
    onSelectIntention: (OnboardingIntention) -> Unit = {},
    // Bloque Sobriedad (slice 4)
    onCreateSobrietyTrack: (name: String) -> Unit = {},
    onSkipSobriety: () -> Unit = {},
    // Bloque Sueño (slice 3)
    sleepState: DashboardSleepState = DashboardSleepState(),
    isAutoModeEnabled: Boolean = false,
    sleepUsageStatsRequested: Boolean = false,
    sleepUsageStatsSkipped: Boolean = false,
    sleepWindDownConsent: Boolean? = null,
    onActivateTelemetry: (onPermissionRequired: () -> Unit) -> Unit = {},
    onSkipTelemetry: () -> Unit = {},
    onWindDownConsent: (Boolean) -> Unit = {},
    onSleepContinue: (sleepAt: String, wakeAt: String) -> Unit = { _, _ -> },
) {
    // El Bloque Anclas reusa AnchorConfigScreen (pantalla real, autónoma a pantalla completa). Va
    // FUERA del Box con padding del onboarding para no romper su layout propio (top bar, buscador
    // y filtros pineados ya manejan sus márgenes).
    if (state.currentStep == OnboardingStep.Anchors) {
        OnboardingAnchorsStep(
            palette = palette,
            layers = layers,
            options = anchorOptions,
            onAddAnchor = onAddAnchor,
            onRemoveAnchor = onRemoveAnchor,
            onCreateActivity = onCreateActivity,
            onDeleteActivity = onDeleteActivity,
            onContinue = onAdvance,
            onBack = onBack,
        )
        return
    }

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

            // Anclas se renderiza a pantalla completa antes del Box (ver arriba).
            OnboardingStep.Anchors -> Unit

            OnboardingStep.Intention -> OnboardingIntentionStep(
                palette = palette,
                currentIntention = intention,
                onSelectAndContinue = onSelectIntention,
                onBack = onBack,
            )

            OnboardingStep.Sleep -> OnboardingSleepStep(
                initialSleepAt = sleepState.targetSleepAt,
                initialWakeAt = sleepState.targetWakeAt,
                usageStatsRequested = sleepUsageStatsRequested,
                usageStatsSkipped = sleepUsageStatsSkipped,
                windDownConsent = sleepWindDownConsent,
                isAutoModeEnabled = isAutoModeEnabled,
                onActivateTelemetry = onActivateTelemetry,
                onSkipTelemetry = onSkipTelemetry,
                onWindDownConsent = onWindDownConsent,
                onContinue = onSleepContinue,
                onBack = onBack,
                palette = palette,
            )

            OnboardingStep.Sobriety -> OnboardingSobrietyStep(
                palette = palette,
                onCreateTrackAndContinue = onCreateSobrietyTrack,
                onSkipSobriety = onSkipSobriety,
                onBack = onBack,
            )
        }
    }
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

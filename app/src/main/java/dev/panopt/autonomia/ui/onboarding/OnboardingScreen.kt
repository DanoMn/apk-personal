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
    // El Bloque Bienvenida es una secuencia editorial autónoma (pager de 4 momentos, animaciones,
    // sellos de capa). Va a pantalla completa, fuera del Box con padding del onboarding.
    if (state.currentStep == OnboardingStep.Welcome) {
        OnboardingWelcomeStep(palette = palette, onContinue = onAdvance)
        return
    }

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

    // El Cierre es el momento de logro sereno (sellos de capas cubiertas encendiéndose). Pantalla
    // completa, autónoma, fuera del Box con padding del onboarding.
    if (state.currentStep == OnboardingStep.Closing) {
        OnboardingClosingStep(
            palette = palette,
            anchorOptions = anchorOptions,
            onComplete = onComplete,
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
            // Bienvenida se renderiza a pantalla completa antes del Box (ver arriba).
            OnboardingStep.Welcome -> Unit

            // Cierre se renderiza a pantalla completa antes del Box (ver arriba).
            OnboardingStep.Closing -> Unit

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

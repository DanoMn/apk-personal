package dev.panopt.autonomia.ui.onboarding

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.panopt.autonomia.domain.onboarding.OnboardingSleepRule
import dev.panopt.autonomia.domain.onboarding.WindowFeedback
import dev.panopt.autonomia.platform.telemetry.TelemetryPermission
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.sleep.AutoModeCard
import dev.panopt.autonomia.ui.sleep.DurationRow
import dev.panopt.autonomia.ui.sleep.TimeField
import dev.panopt.autonomia.ui.sleep.filterTimeInput

/**
 * Bloque Sueño del onboarding (slice 3). El usuario confirma o ajusta la ventana de
 * sueño (targetSleepAt / targetWakeAt); la duración se deriva en tiempo real. También
 * ofrece (sin bloquear el avance) el permiso UsageStats para detección automática y
 * captura el consentimiento explícito para el recordatorio de descanso.
 *
 * "Continuar" se habilita solo cuando la ventana es válida según
 * [OnboardingSleepRule.canAdvance] (≥ 5 horas, compuerta 2 del motor).
 *
 * Los pickers son estado local sembrado de [initialSleepAt]/[initialWakeAt]. Al
 * retroceder y volver, los valores persisten (el seed viene de Room vía
 * [DashboardSleepState] — S3-D3 retroceso voluntario).
 *
 * Tono: adulto funcional compasivo (AGENTS.md). Español neutro (no voseo).
 */
@Composable
internal fun OnboardingSleepStep(
    initialSleepAt: String,
    initialWakeAt: String,
    usageStatsRequested: Boolean,
    usageStatsSkipped: Boolean,
    windDownConsent: Boolean?,
    isAutoModeEnabled: Boolean,
    onActivateTelemetry: (onPermissionRequired: () -> Unit) -> Unit,
    onSkipTelemetry: () -> Unit,
    onWindDownConsent: (Boolean) -> Unit,
    onContinue: (sleepAt: String, wakeAt: String) -> Unit,
    onBack: () -> Unit,
    palette: DashboardPalette,
) {
    val context = LocalContext.current

    // Estado local de los pickers — sembrado de lo ya persistido en Room.
    var sleepAt by remember(initialSleepAt) { mutableStateOf(initialSleepAt) }
    var wakeAt by remember(initialWakeAt) { mutableStateOf(initialWakeAt) }

    var showPermissionPrompt by remember { mutableStateOf(false) }

    // Lanzador para abrir la pantalla de Ajustes del sistema (Usage access).
    // El result code NO trae el estado del permiso — se re-chequea en ON_RESUME.
    val usageAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* resultado ignorado; re-check en ON_RESUME */ }

    val appDetailsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { /* resultado ignorado; re-check en ON_RESUME */ }

    // Re-evaluación del permiso al volver de Ajustes (ON_RESUME).
    // Si el usuario concedió el permiso, oculta el prompt.
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResume by rememberUpdatedState(
        newValue = {
            if (TelemetryPermission.isGranted(context)) {
                showPermissionPrompt = false
            }
        },
    )
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) currentOnResume()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Regla de dominio puro — Compose solo renderiza el resultado.
    val canAdvance = OnboardingSleepRule.canAdvance(sleepAt, wakeAt)
    val derivedMinutes = OnboardingSleepRule.derivedWindowMinutes(sleepAt, wakeAt)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // ── Encabezado ──────────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "El descanso primero",
                color = palette.textMain,
                fontSize = 30.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 36.sp,
            )
            Text(
                text = "Elige la ventana de descanso que te funcione. " +
                    "No pedimos el número de horas — se deriva de tu ventana.",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 15.sp,
                lineHeight = 22.sp,
            )
        }

        // ── Pickers de hora ─────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.bgSurface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TimeField(
                    label = "Dormir",
                    value = sleepAt,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onValueChange = { sleepAt = it.filterTimeInput() },
                )
                TimeField(
                    label = "Despertar",
                    value = wakeAt,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onValueChange = { wakeAt = it.filterTimeInput() },
                )
            }

            DurationRow(minutes = derivedMinutes, palette = palette)
        }

        // ── Mensaje de ventana inválida (tono neutral, sin "error") ─────────
        // El dominio decide QUÉ está mal; acá solo mapeamos a texto (tono AGENTS.md).
        val feedbackText = when (OnboardingSleepRule.windowFeedback(sleepAt, wakeAt)) {
            WindowFeedback.NONE -> null
            WindowFeedback.INVALID_FORMAT -> "Revisa la hora: usa el formato HH:mm."
            WindowFeedback.TOO_SHORT -> "La ventana mínima es de 5 horas."
        }
        feedbackText?.let {
            Text(
                text = it,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
        }

        // ── Oferta de telemetría (salteable) ────────────────────────────────
        if (!usageStatsSkipped && !isAutoModeEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Detección automática",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Text(
                    text = "Puedes activar el acceso de uso para leer tu descanso sin anotar nada.",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
                AutoModeCard(
                    isEnabled = isAutoModeEnabled,
                    showPermissionPrompt = showPermissionPrompt,
                    palette = palette,
                    onToggle = { wantEnabled ->
                        if (wantEnabled) {
                            showPermissionPrompt = false
                            onActivateTelemetry { showPermissionPrompt = true }
                        }
                    },
                    onOpenUsageAccess = {
                        usageAccessLauncher.launch(TelemetryPermission.settingsIntent())
                    },
                    onOpenAppDetails = {
                        appDetailsLauncher.launch(TelemetryPermission.appDetailsSettingsIntent(context))
                    },
                )
                // Botón "Más tarde"
                if (!usageStatsRequested) {
                    Text(
                        text = "Más tarde",
                        color = palette.textFaint,
                        fontFamily = DashboardSans,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClick = onSkipTelemetry)
                            .padding(vertical = 10.dp),
                    )
                }
            }
        }

        // ── Consentimiento wind-down ─────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(palette.bgSurface)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "¿Quieres que te avise cuando se acerque tu hora de descanso?",
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WindDownConsentButton(
                    label = "Sí",
                    selected = windDownConsent == true,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = { onWindDownConsent(true) },
                )
                WindDownConsentButton(
                    label = "No",
                    selected = windDownConsent == false,
                    palette = palette,
                    modifier = Modifier.weight(1f),
                    onClick = { onWindDownConsent(false) },
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Botón Continuar ──────────────────────────────────────────────────
        OnboardingPrimaryButton(
            palette = palette,
            label = "Continuar",
            enabled = canAdvance,
            onClick = { onContinue(sleepAt, wakeAt) },
        )

        // ── Botón Volver ─────────────────────────────────────────────────────
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

@Composable
private fun WindDownConsentButton(
    label: String,
    selected: Boolean,
    palette: DashboardPalette,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (selected) palette.bgBase else palette.textMuted,
        fontFamily = DashboardSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) palette.colorCoral else palette.bgSurface2)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    )
}

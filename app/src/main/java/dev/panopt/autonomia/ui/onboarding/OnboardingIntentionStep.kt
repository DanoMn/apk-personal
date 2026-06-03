package dev.panopt.autonomia.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.onboarding.OnboardingIntention
import dev.panopt.autonomia.domain.onboarding.OnboardingIntentionRule
import dev.panopt.autonomia.ui.dashboard.DashboardPalette

/**
 * Bloque Intención del onboarding (slice 4, Bloque 0.5).
 * Pregunta "¿Qué te trae aquí?" y ofrece dos rutas sin etiquetar al usuario.
 * La elección se persiste en prefs y ramifica la secuencia restante.
 */
@Composable
internal fun OnboardingIntentionStep(
    palette: DashboardPalette,
    /** Intención actualmente persistida; null si el usuario todavía no eligió. */
    currentIntention: OnboardingIntention?,
    /**
     * Callback invocado cuando el usuario confirma su elección.
     * El caller DEBE persistir la intención ANTES de avanzar el paso.
     */
    onSelectAndContinue: (OnboardingIntention) -> Unit,
    onBack: () -> Unit,
) {
    // Estado local sembrado desde la pref persistida para que la selección
    // previa se muestre si el usuario regresó a este bloque.
    var selected by remember(currentIntention) { mutableStateOf(currentIntention) }

    val canAdvance = OnboardingIntentionRule.canAdvance(selected)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        // Encabezado en serif — copy canónico v3
        Text(
            text = "¿Qué te trae aquí?",
            color = palette.textMain,
            fontSize = 28.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 34.sp,
        )
        Spacer(Modifier.height(10.dp))
        // Aviso suave — sin presión
        Text(
            text = "No hay respuesta correcta. Podrás cambiarla cuando quieras.",
            color = palette.textMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(28.dp))

        // Tarjeta: ruta estándar
        IntentionCard(
            label = "Quiero ordenar mi día a día",
            description = "Anclas, rutinas y estructura para sostener lo cotidiano.",
            selected = selected == OnboardingIntention.STANDARD,
            palette = palette,
            onClick = { selected = OnboardingIntention.STANDARD },
        )
        Spacer(Modifier.height(14.dp))

        // Tarjeta: ruta sobriedad
        IntentionCard(
            label = "Quiero cuidarme de algo que me cuesta",
            description = "Para quienes también quieren llevar un registro de Sobriedad.",
            selected = selected == OnboardingIntention.PROTECTION,
            palette = palette,
            onClick = { selected = OnboardingIntention.PROTECTION },
        )
        Spacer(Modifier.height(36.dp))

        OnboardingPrimaryButton(
            palette = palette,
            label = "Continuar",
            enabled = canAdvance,
            onClick = { onSelectAndContinue(selected!!) },
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
    }
}

@Composable
private fun IntentionCard(
    label: String,
    description: String,
    selected: Boolean,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) palette.colorCoral.copy(alpha = 0.15f) else palette.bgSurface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) palette.colorCoral else palette.bgSurface2,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(
            text = label,
            color = if (selected) palette.colorCoral else palette.textMain,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = description,
            color = palette.textMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

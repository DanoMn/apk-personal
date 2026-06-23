package dev.panopt.autonomia.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import dev.panopt.autonomia.ActivitySurface
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif

/**
 * Bloque Cierre del onboarding: el momento de logro sereno (dirección Híbrido). Los sellos de las
 * capas que el usuario realmente cubrió con anclas se ENCIENDEN uno a uno —un amanecer lento, sin
 * XP ni confeti— y recién después aparece "Tus cimientos están en pie". Cierra el círculo con la
 * Bienvenida, donde estos mismos sellos se presentaron. Dignidad, no premio.
 */
@Composable
internal fun OnboardingClosingStep(
    palette: DashboardPalette,
    anchorOptions: List<DashboardActivityOptionState>,
    onComplete: () -> Unit,
    onBack: () -> Unit,
) {
    val coveredLayerIds = anchorOptions
        .filter { it.isConfigured && it.activityType == ActivitySurface.Anchor.name }
        .map { it.layerId }
        .toSet()
    // En orden canónico, solo las capas que el usuario cubrió.
    val coveredSeals = onboardingLayerSeals(palette).filter { it.layerId in coveredLayerIds }

    // Pasos de cascada: un sello por capa cubierta + título + cuerpo/CTA.
    val reveal = rememberStagger(
        active = true,
        steps = coveredSeals.size + 2,
        startDelayMs = 350,
        stepDelayMs = 480,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Los sellos de tus capas, encendiéndose en secuencia con un pulso de luz.
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            coveredSeals.forEachIndexed { i, seal ->
                ClosingSeal(seal = seal, lit = reveal >= i + 1)
            }
        }
        Spacer(Modifier.height(20.dp))
        // Los nombres de las capas que cubriste, debajo de sus sellos.
        Beat(reveal >= coveredSeals.size + 1) {
            Text(
                text = coveredSeals.joinToString("   ·   ") { it.name.substringAfter(' ') },
                color = palette.textFaint,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(24.dp))
        Beat(reveal >= coveredSeals.size + 1) {
            Text(
                text = "Tus cimientos están en pie.",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 30.sp,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        Beat(reveal >= coveredSeals.size + 2) {
            Text(
                text = "Esto es el comienzo de un viaje, no un examen. Podrás ajustar todo cuando quieras.",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )
        }
        Spacer(Modifier.height(40.dp))
        Beat(reveal >= coveredSeals.size + 2) {
            OnboardingPrimaryButton(palette = palette, label = "Entrar", onClick = onComplete)
        }
        Spacer(Modifier.height(8.dp))
        Beat(reveal >= coveredSeals.size + 2) {
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

/**
 * Encendido de un sello: nace pequeño (scale + fade) y al prenderse emite un pulso de luz —un
 * anillo en su color que se expande y se desvanece, como una lámpara que enciende—. Sin estridencia.
 */
@Composable
private fun ClosingSeal(seal: OnboardingLayerSeal, lit: Boolean) {
    val appear by animateFloatAsState(
        targetValue = if (lit) 1f else 0f,
        animationSpec = tween(650, easing = LinearOutSlowInEasing),
        label = "sealAppear",
    )
    val ping = remember { Animatable(0f) }
    LaunchedEffect(lit) {
        if (lit) {
            ping.snapTo(0f)
            ping.animateTo(1f, tween(820, easing = LinearOutSlowInEasing))
        }
    }
    Box(contentAlignment = Alignment.Center) {
        // El pulso de luz que se expande desde el sello al encenderse.
        Canvas(modifier = Modifier.size(52.dp)) {
            val p = ping.value
            if (p > 0f && p < 1f) {
                drawCircle(
                    color = seal.color.copy(alpha = 0.45f * (1f - p)),
                    radius = size.minDimension * (0.34f + 0.42f * p),
                )
            }
        }
        Box(
            modifier = Modifier.graphicsLayer {
                scaleX = lerp(0.4f, 1f, appear)
                scaleY = lerp(0.4f, 1f, appear)
                alpha = appear
            },
        ) {
            LayerSeal(layerId = seal.layerId, color = seal.color, size = 52.dp)
        }
    }
}

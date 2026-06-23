package dev.panopt.autonomia.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Bloque Bienvenida del onboarding, rediseñado como una secuencia editorial que respira en
 * lugar de un muro de texto estático. Es un [HorizontalPager] de 4 momentos:
 *
 *  0. Apertura  — la línea-base se dibuja sola + título; voz íntima ("quiero contarte algo").
 *  1. La idea   — explica qué son las capas/cimientos, en cascada y con tono compasivo.
 *  2. Las cinco — los sellos de capa se encienden en sus colores reales (constelación).
 *  3. El puente — "vamos a reconocer la tuya" + CTA Empecemos (avanza a Intención).
 *
 * Puramente UI: no toca dominio ni reglas. Las capas YA no son 5 pantallas tediosas; la idea
 * vive en el momento 1 y las cinco aparecen juntas en el momento 2.
 */
@Composable
internal fun OnboardingWelcomeStep(
    palette: DashboardPalette,
    onContinue: () -> Unit,
) {
    val pageCount = 4
    val pagerState = rememberPagerState(pageCount = { pageCount })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .statusBarsPadding(),
    ) {
        // Barra superior: Volver (navega el pager hacia atrás) / Saltar (mínima fricción).
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val onFirst = pagerState.currentPage == 0
            Text(
                text = if (onFirst) "" else "Volver",
                color = palette.textFaint,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !onFirst) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
            Text(
                text = "Saltar",
                color = palette.textFaint,
                fontSize = 14.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onContinue)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val active = pagerState.currentPage == page
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val distance = abs(pageOffset).coerceIn(0f, 1f)
                        val nearness = 1f - distance
                        // Escala suave (la página entrante crece) + alpha con piso (nunca llega a 0,
                        // así no se "vacía" la pantalla a mitad del gesto) + parallax leve = profundidad.
                        val scale = lerp(0.90f, 1f, nearness)
                        scaleX = scale
                        scaleY = scale
                        alpha = lerp(0.30f, 1f, nearness)
                        translationX = size.width * pageOffset * 0.08f
                    }
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                when (page) {
                    0 -> WelcomeOpening(palette, active)
                    1 -> WelcomeIdea(palette, active)
                    2 -> WelcomeLayers(palette, active)
                    else -> WelcomeBridge(palette, active, onContinue)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            PagerDots(palette = palette, count = pageCount, current = pagerState.currentPage)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Momento 0 — Apertura
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeOpening(palette: DashboardPalette, active: Boolean) {
    val reveal = rememberStagger(active, steps = 2, startDelayMs = 1450, stepDelayMs = 650)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // El logo de la app —la espiral— nace lentamente, dibujándose trazo a trazo.
        AppSpiral(
            palette = palette,
            active = active,
            modifier = Modifier.size(184.dp),
        )
        Spacer(Modifier.height(40.dp))
        Beat(reveal >= 1) {
            Text(
                text = "Autonomía sin límites",
                color = palette.colorCardboard,
                fontSize = 34.sp,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 40.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        Beat(reveal >= 2) {
            Text(
                text = "Antes de empezar, quiero contarte algo.",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )
        }
    }
}

/**
 * El logo de la app —la espiral hacia el centro— dibujándose trazo a trazo (PathMeasure sobre el
 * pathData real de `ic_spiral_foreground`). "Nace" desde afuera hacia el núcleo, sin apuro.
 */
@Composable
private fun AppSpiral(palette: DashboardPalette, active: Boolean, modifier: Modifier) {
    val fullPath = remember { PathParser().parsePathString(SPIRAL_PATH_DATA).toPath() }
    val measure = remember { PathMeasure().apply { setPath(fullPath, false) } }
    val totalLength = remember { measure.length }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(active) {
        if (active) {
            progress.snapTo(0f)
            delay(150)
            progress.animateTo(1f, tween(1800, easing = FastOutSlowInEasing))
        } else {
            progress.snapTo(0f)
        }
    }
    val strokeColor = palette.colorCardboard
    Canvas(modifier = modifier) {
        val drawn = Path()
        measure.getSegment(0f, totalLength * progress.value, drawn, true)
        // El viewport del vector es 108x108; escalamos desde el origen para que el centro (54,54)
        // caiga en el centro del Canvas.
        val scale = size.minDimension / 108f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawPath(
                path = drawn,
                color = strokeColor,
                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

private const val SPIRAL_PATH_DATA =
    "M78,54C78,40.7 67.3,30 54,30C40.7,30 30,40.7 30,54C30,67.3 40.7,78 54,78C65.6,78 74,69.6 74,58.6" +
        "C74,47.7 65.9,38 55,38C45.1,38 38,45.1 38,54C38,63.1 44.9,70 54,70C61.3,70 66,65.3 66,58.2" +
        "C66,51.1 61.1,46 54,46C49.1,46 46,49.1 46,54C46,58.7 49.3,62 54,62C57.1,62 59.4,59.7 59.4,56.9" +
        "C59.4,53.9 57.4,52 54,52"

// ─────────────────────────────────────────────────────────────────────────────
// Momento 1 — La idea (qué son las capas)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeIdea(palette: DashboardPalette, active: Boolean) {
    val reveal = rememberStagger(active, steps = 5, startDelayMs = 250, stepDelayMs = 520)
    Column {
        Beat(reveal >= 1) { IdeaLine("Creo que ninguna vida se sostiene sola.", palette, emphasis = true) }
        Spacer(Modifier.height(14.dp))
        Beat(reveal >= 2) {
            IdeaLine(
                "Se apoya en cosas simples: dormir, mover el cuerpo, no aislarse, cuidar lo que uno construye.",
                palette,
            )
        }
        Spacer(Modifier.height(14.dp))
        Beat(reveal >= 3) {
            IdeaLine("A eso lo llamo tus capas. Son los cimientos que te mantienen en pie.", palette)
        }
        Spacer(Modifier.height(14.dp))
        Beat(reveal >= 4) {
            IdeaLine("Cuando una se descuida, casi no se nota. Pero si varias ceden, viene la caída.", palette)
        }
        Spacer(Modifier.height(14.dp))
        Beat(reveal >= 5) {
            IdeaLine("No estás roto cuando eso pasa. Es solo una señal: toca volver a la base.", palette, emphasis = true)
        }
    }
}

@Composable
private fun IdeaLine(text: String, palette: DashboardPalette, emphasis: Boolean = false) {
    Text(
        text = text,
        color = if (emphasis) palette.textMain else palette.textMuted,
        fontFamily = if (emphasis) DashboardSerif else DashboardSans,
        fontSize = if (emphasis) 19.sp else 16.sp,
        lineHeight = if (emphasis) 26.sp else 23.sp,
        fontWeight = if (emphasis) FontWeight.Medium else FontWeight.Normal,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Momento 2 — Las cinco capas (constelación que se enciende)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeLayers(palette: DashboardPalette, active: Boolean) {
    val layers = onboardingLayerSeals(palette)
    val reveal = rememberStagger(active, steps = layers.size + 1, startDelayMs = 200, stepDelayMs = 360)

    Column {
        Beat(reveal >= 1) {
            Text(
                text = "Estas son las cinco.",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
            )
        }
        Spacer(Modifier.height(18.dp))
        layers.forEachIndexed { i, layer ->
            Beat(reveal >= i + 2) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 7.dp),
                ) {
                    LayerSeal(layerId = layer.layerId, color = layer.color, size = 44.dp)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(
                            text = layer.name,
                            color = palette.textMain,
                            fontFamily = DashboardSerif,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                        )
                        Text(
                            text = layer.phrase,
                            color = palette.textMuted,
                            fontFamily = DashboardSans,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Momento 3 — El puente a la acción
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeBridge(palette: DashboardPalette, active: Boolean, onContinue: () -> Unit) {
    val reveal = rememberStagger(active, steps = 2, startDelayMs = 220, stepDelayMs = 620)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Beat(reveal >= 1) {
            Text(
                text = "A ese conjunto lo llamamos tu base.",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 26.sp,
                textAlign = TextAlign.Center,
                lineHeight = 32.sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        Beat(reveal >= 2) {
            Text(
                text = "Vamos a reconocer la tuya. Sin apuro, sin exigir perfección.",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
            )
        }
        Spacer(Modifier.height(36.dp))
        Beat(reveal >= 2) {
            OnboardingPrimaryButton(palette = palette, label = "Empecemos", onClick = onContinue)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers compartidos
// ─────────────────────────────────────────────────────────────────────────────

/** Aparición de un "beat": fade + leve subida + escala sutil. El alma del ritmo editorial. */
@Composable
internal fun Beat(visible: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(750, easing = LinearOutSlowInEasing)) +
            slideInVertically(tween(750, easing = LinearOutSlowInEasing)) { it / 14 } +
            scaleIn(tween(750, easing = LinearOutSlowInEasing), initialScale = 0.985f),
    ) { content() }
}

/**
 * Devuelve cuántos elementos deben estar revelados, avanzando en cascada cuando la página se
 * vuelve [active]. Reinicia a 0 al salir para que la animación se repita si el usuario vuelve.
 */
@Composable
internal fun rememberStagger(active: Boolean, steps: Int, startDelayMs: Long, stepDelayMs: Long): Int {
    var revealed by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        if (active) {
            revealed = 0
            delay(startDelayMs)
            for (i in 1..steps) {
                revealed = i
                delay(stepDelayMs)
            }
        } else {
            revealed = 0
        }
    }
    return revealed
}

@Composable
private fun PagerDots(palette: DashboardPalette, count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until count) {
            val isActive = i == current
            val dotWidth by animateDpAsState(
                targetValue = if (isActive) 28.dp else 9.dp,
                animationSpec = tween(300),
                label = "dotWidth",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .height(9.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(
                        if (isActive) palette.colorCoral
                        else palette.colorCardboardSoft.copy(alpha = 0.45f),
                    ),
            )
        }
    }
}

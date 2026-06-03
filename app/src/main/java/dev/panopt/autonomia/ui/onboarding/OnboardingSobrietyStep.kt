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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette

/**
 * Bloque Sobriedad del onboarding (slice 4, Bloque 3).
 * Aparece SOLO en la ruta PROTECTION — el dominio ([OnboardingFlow]) garantiza
 * que este bloque nunca se muestre en ruta STANDARD.
 *
 * Ofrece de forma opcional crear un track de Sobriedad inicial:
 * - "Sí, agregar" → formulario mínimo con nombre → crea track y avanza.
 * - "Ahora no" → avanza sin crear nada.
 * El avance no bloquea en ninguno de los dos casos.
 */
@Composable
internal fun OnboardingSobrietyStep(
    palette: DashboardPalette,
    onCreateTrackAndContinue: (name: String) -> Unit,
    onSkipSobriety: () -> Unit,
    onBack: () -> Unit,
) {
    var showForm by remember { mutableStateOf(false) }
    var trackName by remember { mutableStateOf("") }
    // Mensaje neutral cuando se intenta confirmar con nombre vacío
    var showEmptyHint by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
    ) {
        // Encabezado en serif — copy canónico v3
        Text(
            text = "Cuidar algo que te cuesta",
            color = palette.textMain,
            fontSize = 28.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 34.sp,
        )
        Spacer(Modifier.height(16.dp))

        // Cuerpo literario — copy v3 §4 Bloque 3, tono sin culpa
        Text(
            text = "Hay cosas que queremos dejar de hacer, o hacer menos. No porque alguien " +
                "nos lo pida, sino porque nosotros elegimos cuidarnos.",
            color = palette.textMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "El registro de Sobriedad no es un juez. Es un testigo. Marca los días " +
                "tranquilos, y cuando vienen las tormentas, recuerda que la tormenta pasa.",
            color = palette.textMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(12.dp))
        // Frase canónica literal — exigida por spec
        Text(
            text = "Una recaída no es un fracaso. Es una señal, no una condena.",
            color = palette.textMain,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp,
        )
        Spacer(Modifier.height(28.dp))

        if (!showForm) {
            // Pregunta — dos acciones
            Text(
                text = "¿Quieres llevar el registro de algo que estás cuidando?",
                color = palette.textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 23.sp,
            )
            Spacer(Modifier.height(16.dp))

            // "Sí, agregar" — revela el formulario
            OnboardingPrimaryButton(
                palette = palette,
                label = "Sí, agregar",
                onClick = { showForm = true },
            )
            Spacer(Modifier.height(12.dp))

            // "Ahora no" — avanza sin crear nada
            Text(
                text = "Ahora no",
                color = palette.textFaint,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onSkipSobriety)
                    .padding(vertical = 12.dp),
            )
        } else {
            // Formulario mínimo para nombrar el track
            Text(
                text = "¿Qué estás cuidando?",
                color = palette.textMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.bgSurface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                BasicTextField(
                    value = trackName,
                    onValueChange = {
                        trackName = it
                        showEmptyHint = false
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = palette.textMain, fontSize = 15.sp),
                    cursorBrush = SolidColor(palette.colorCoral),
                    modifier = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (trackName.isEmpty()) {
                            Text(
                                text = "Por ejemplo: Tabaco, Alcohol...",
                                color = palette.textFaint,
                                fontSize = 15.sp,
                            )
                        }
                        inner()
                    },
                )
            }

            // Hint neutral si el usuario intenta confirmar con nombre vacío
            if (showEmptyHint) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Escribí un nombre para continuar.",
                    color = palette.textMuted,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(16.dp))

            // Confirmar — valida que no esté vacío
            OnboardingPrimaryButton(
                palette = palette,
                label = "Agregar y continuar",
                onClick = {
                    if (trackName.isBlank()) {
                        showEmptyHint = true
                    } else {
                        onCreateTrackAndContinue(trackName)
                    }
                },
            )
            Spacer(Modifier.height(12.dp))

            // Cancelar formulario — vuelve a la pregunta
            Text(
                text = "Cancelar",
                color = palette.textFaint,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { showForm = false; trackName = "" }
                    .padding(vertical = 12.dp),
            )
        }

        Spacer(Modifier.height(20.dp))
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

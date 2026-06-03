package dev.panopt.autonomia.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.sleep.SleepPolicy
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans

/**
 * Campo de texto para ingresar una hora en formato HH:mm.
 *
 * Extraído de [SleepConfigScreen] para reuso en [OnboardingSleepStep].
 */
@Composable
internal fun TimeField(
    label: String,
    value: String,
    palette: DashboardPalette,
    modifier: Modifier,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgSurface2)
                .padding(horizontal = 10.dp, vertical = 15.dp),
        )
    }
}

/**
 * Fila que muestra la duración derivada de la ventana de sueño (en horas/minutos),
 * o "--" si los horarios no son parseables.
 *
 * Extraído de [SleepConfigScreen] para reuso en [OnboardingSleepStep].
 */
@Composable
internal fun DurationRow(
    minutes: Int?,
    palette: DashboardPalette,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Duracion objetivo",
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 13.sp,
        )
        Text(
            text = minutes?.let(SleepPolicy::formatDuration) ?: "--",
            color = palette.colorCardboard,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

/**
 * Filtra la entrada del usuario para un campo de hora HH:mm:
 * solo dígitos y ':', máximo 5 caracteres.
 */
internal fun String.filterTimeInput(): String =
    filter { it.isDigit() || it == ':' }.take(5)

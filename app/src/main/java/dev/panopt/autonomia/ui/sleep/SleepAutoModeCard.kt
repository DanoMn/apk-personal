package dev.panopt.autonomia.ui.sleep

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans

/**
 * Card para el modo de detección automática de sueño por telemetría (UsageStats).
 *
 * Extraído de [SleepConfigScreen] para reuso en [OnboardingSleepStep].
 *
 * - Cuando OFF: toggle + explicación. El usuario puede activar.
 * - Cuando ON: toggle + estado activo. El usuario puede desactivar.
 * - [showPermissionPrompt]: el permiso de acceso a uso de apps no está concedido.
 *   Se muestra un prompt compasivo en dos pasos (sin crash, sin fallo silencioso).
 *   En Android 13+ instalado desde fuente no confiable (e.g. `adb install`) la
 *   pantalla de Ajustes tiene el toggle bloqueado: el paso 1 cubre ese escape hatch.
 *
 * Tono: compasivo, adulto funcional (AGENTS.md).
 */
@Composable
internal fun AutoModeCard(
    isEnabled: Boolean,
    showPermissionPrompt: Boolean,
    palette: DashboardPalette,
    onToggle: (Boolean) -> Unit,
    onOpenUsageAccess: () -> Unit = {},
    onOpenAppDetails: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.bgSurface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Deteccion automatica",
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                )
                Text(
                    text = if (isEnabled) {
                        "El dispositivo infiere tu descanso en segundo plano."
                    } else {
                        "El telefono lee tu actividad nocturna para inferir el sueno."
                    },
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = palette.bgBase,
                    checkedTrackColor = palette.colorCardboard,
                    uncheckedThumbColor = palette.textMuted,
                    uncheckedTrackColor = palette.bgSurface2,
                ),
            )
        }

        if (showPermissionPrompt) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Falta el permiso de acceso a uso de apps.",
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    text = "Esta es una senal, no una condena. La app nunca comparte esos datos. Son dos pasos:",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
                PermissionStep(
                    number = "1",
                    instruction = "Si el interruptor de Acceso de uso aparece bloqueado, abri la info de la app y, en el menu ⋮, elegi \"Permitir ajustes restringidos\".",
                    actionLabel = "Abrir info de la app",
                    palette = palette,
                    onAction = onOpenAppDetails,
                )
                PermissionStep(
                    number = "2",
                    instruction = "Despues entra a Acceso de uso y concedelo a Vocal.",
                    actionLabel = "Ir a Acceso de uso",
                    palette = palette,
                    onAction = onOpenUsageAccess,
                )
            }
        }
    }
}

/**
 * Un paso numerado dentro del prompt de permiso de uso de apps: una instrucción
 * calmada y un botón de acción que abre la pantalla de ajustes del sistema correspondiente.
 */
@Composable
internal fun PermissionStep(
    number: String,
    instruction: String,
    actionLabel: String,
    palette: DashboardPalette,
    onAction: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "$number ·",
                color = palette.colorCardboard,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
            )
            Text(
                text = instruction,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.weight(1f),
            )
        }
        Box(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.colorCardboard)
                .clickable(role = Role.Button, onClick = onAction)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = actionLabel,
                color = palette.bgBase,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 12.5.sp,
            )
        }
    }
}

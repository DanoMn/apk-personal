package dev.panopt.autonomia.ui.sleep

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardSleepState
import dev.panopt.autonomia.domain.sleep.SleepConfigValidation
import dev.panopt.autonomia.domain.sleep.SleepPolicy
import dev.panopt.autonomia.ui.anchors.ConfigScreenContainer
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun SleepConfigScreen(
    sleep: DashboardSleepState,
    isSleepLockActive: Boolean,
    isAutoModeEnabled: Boolean = false,
    palette: DashboardPalette,
    onRequestSleepLockPermission: () -> Unit,
    onToggleAutoMode: (enabled: Boolean, onPermissionRequired: () -> Unit) -> Unit = { _, _ -> },
    onOpenTelemetrySettings: () -> Unit = {},
    onSave: (targetSleepAt: String, targetWakeAt: String, digitalWindDownMinutes: Int) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var targetSleepAt by remember(sleep.targetSleepAt) { mutableStateOf(sleep.targetSleepAt) }
    var targetWakeAt by remember(sleep.targetWakeAt) { mutableStateOf(sleep.targetWakeAt) }
    var windDownMinutes by remember(sleep.digitalWindDownMinutes) {
        mutableStateOf(sleep.digitalWindDownMinutes)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var showPermissionPrompt by remember { mutableStateOf(false) }

    val targetMinutes = SleepPolicy.minutesBetween(targetSleepAt, targetWakeAt)
    val validation = SleepPolicy.validateConfig(
        targetSleepAt = targetSleepAt,
        targetWakeAt = targetWakeAt,
        digitalWindDownMinutes = windDownMinutes,
    )

    ConfigScreenContainer(palette = palette) {
        Header(palette = palette, onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                        value = targetSleepAt,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onValueChange = {
                            targetSleepAt = it.filterTimeInput()
                            error = null
                        },
                    )
                    TimeField(
                        label = "Despertar",
                        value = targetWakeAt,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onValueChange = {
                            targetWakeAt = it.filterTimeInput()
                            error = null
                        },
                    )
                }

                DurationRow(
                    minutes = targetMinutes,
                    palette = palette,
                )
            }

            SleepLockStatusCard(
                isSleepLockActive = isSleepLockActive,
                palette = palette,
                onRequestSleepLockPermission = onRequestSleepLockPermission,
            )

            AutoModeCard(
                isEnabled = isAutoModeEnabled,
                showPermissionPrompt = showPermissionPrompt,
                palette = palette,
                onToggle = { wantEnabled ->
                    showPermissionPrompt = false
                    onToggleAutoMode(wantEnabled) { showPermissionPrompt = true }
                },
                onOpenSettings = {
                    showPermissionPrompt = false
                    onOpenTelemetrySettings()
                },
            )

            Text(
                text = "Descanso digital",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            )
            WindDownChips(
                selectedMinutes = windDownMinutes,
                palette = palette,
                onSelect = {
                    windDownMinutes = it
                    error = null
                },
            )

            error?.let {
                Text(
                    text = it,
                    color = palette.risk,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }

        SaveButton(
            enabled = validation is SleepConfigValidation.Valid,
            palette = palette,
            onClick = {
                when (validation) {
                    is SleepConfigValidation.Valid -> {
                        onSave(targetSleepAt.trim(), targetWakeAt.trim(), windDownMinutes)
                        onBack()
                    }
                    is SleepConfigValidation.Invalid -> {
                        error = validation.message
                    }
                }
            },
        )
    }
}

/**
 * Card for the automatic telemetry-based sleep detection mode (design §7, WU-7).
 *
 * - When OFF: toggle + explanation. User can activate.
 * - When ON: toggle + active status. User can deactivate.
 * - [showPermissionPrompt]: usage-access permission is required. Show compassionate prompt
 *   with a direct link to the system settings screen (no crash, no silent fail).
 *
 * Tono: compasivo, adulto funcional (AGENTS.md).
 */
@Composable
private fun AutoModeCard(
    isEnabled: Boolean,
    showPermissionPrompt: Boolean,
    palette: DashboardPalette,
    onToggle: (Boolean) -> Unit,
    onOpenSettings: () -> Unit = {},
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
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Falta el permiso de acceso a uso de apps.",
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    text = "Esta es una senal, no una condena. Podes concederlo en Ajustes del sistema. La app nunca comparte esos datos.",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.colorCardboard)
                        .clickable(role = Role.Button, onClick = onOpenSettings)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Ir a Ajustes",
                        color = palette.bgBase,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SleepLockStatusCard(
    isSleepLockActive: Boolean,
    palette: DashboardPalette,
    onRequestSleepLockPermission: () -> Unit,
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
                    text = if (isSleepLockActive) "Bloqueo activo" else "Bloqueo pendiente",
                    color = palette.textMain,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                )
                Text(
                    text = if (isSleepLockActive) {
                        "Ir a dormir bloqueara la pantalla."
                    } else {
                        "Activalo antes de usar Ir a dormir."
                    },
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                )
            }
            if (!isSleepLockActive) {
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.colorCardboard)
                        .clickable(role = Role.Button, onClick = onRequestSleepLockPermission)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Activar",
                        color = palette.bgBase,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(
    palette: DashboardPalette,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.bgSurface)
                .clickable(role = Role.Button, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            XIcon(color = palette.textMain)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Sueno",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 28.sp,
            )
            Text(
                text = "Configuracion nocturna",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun TimeField(
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

@Composable
private fun DurationRow(
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

@Composable
private fun WindDownChips(
    selectedMinutes: Int,
    palette: DashboardPalette,
    onSelect: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SleepPolicy.allowedDigitalWindDownMinutes.sorted().chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { minutes ->
                    WindDownChip(
                        minutes = minutes,
                        selected = selectedMinutes == minutes,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(minutes) },
                    )
                }
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WindDownChip(
    minutes: Int,
    selected: Boolean,
    palette: DashboardPalette,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) mix(palette.colorCoral, 0.22f, palette.bgSurface)
                else palette.bgSurface,
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (minutes == 0) "Omitir" else "${minutes} min",
            color = if (selected) Color(0xFFEFAA9C) else palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SaveButton(
    enabled: Boolean,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(top = 12.dp, bottom = 12.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) palette.colorCardboard else palette.bgSurface2)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Guardar",
            color = if (enabled) palette.bgBase else palette.textFaint,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
    }
}

private fun String.filterTimeInput(): String =
    filter { it.isDigit() || it == ':' }.take(5)

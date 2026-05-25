package dev.panopt.autonomia.ui.anchors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun WeeklyFrequencySelector(
    selectedFrequency: Int,
    palette: DashboardPalette,
    onFrequencySelected: (Int) -> Unit,
) {
    val rows = weeklyFrequencyPresets.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowPresets.forEach { preset ->
                    val frequency = weeklyFrequencyTargetFromPreset(preset)
                    val isSelected = selectedFrequency == frequency
                    FrequencyButton(
                        frequency = frequency,
                        isSelected = isSelected,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onFrequencySelected(frequency) },
                    )
                }
                repeat(3 - rowPresets.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FrequencyButton(
    frequency: Int,
    isSelected: Boolean,
    palette: DashboardPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) mix(palette.colorCoral, 0.18f, palette.bgSurface)
                else palette.bgSurface,
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = frequency.toString(),
                color = if (isSelected) Color(0xFFEFAA9C) else palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "/sem",
                color = if (isSelected) Color(0xFFEFAA9C) else palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
internal fun CommitmentDurationSummaryButton(
    durationMonths: Int?,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(shape)
            .background(mix(palette.colorCardboard, 0.12f, palette.bgSurface))
            .border(1.dp, mix(palette.colorCardboard, 0.34f, palette.bgSurface), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = commitmentDurationLabel(durationMonths),
                color = palette.colorCardboard,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .background(palette.colorCardboard)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Configurar",
                    color = palette.bgBase,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
internal fun CommitmentDurationSetting(
    durationMonths: Int?,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    CommitmentDurationSummaryButton(
        durationMonths = durationMonths,
        palette = palette,
        onClick = onClick,
    )
}

@Composable
internal fun CommitmentDurationGuidance(
    palette: DashboardPalette,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Te recomendamos dejarlo como indefinido si esta ancla representa una base que aporta estabilidad a tu vida general.",
        color = palette.textMuted,
        fontFamily = DashboardSans,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        modifier = modifier,
    )
}

@Composable
internal fun CommitmentDurationDialog(
    selectedDurationMonths: Int?,
    palette: DashboardPalette,
    onDismiss: () -> Unit,
    onConfirm: (Int?) -> Unit,
) {
    val quickMonths = commitmentDurationPresets
        .filter { it != CommitmentDurationPreset.Custom }
        .mapNotNull { it.months }
        .toSet()
    var draftPreset by remember(selectedDurationMonths) {
        mutableStateOf(
            commitmentDurationPresets.firstOrNull { preset ->
                preset != CommitmentDurationPreset.Custom && preset.months == selectedDurationMonths
            } ?: CommitmentDurationPreset.Custom,
        )
    }
    var draftCustomMonths by remember(selectedDurationMonths) {
        mutableStateOf(
            selectedDurationMonths
                ?.takeIf { it !in quickMonths }
                ?.toString()
                .orEmpty(),
        )
    }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.bgElevated,
        titleContentColor = palette.colorCardboard,
        textContentColor = palette.textMain,
        title = {
            Text(
                text = "¿Cuántos meses quieres sostener esta ancla?",
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CommitmentDurationGuidance(palette = palette)

                DurationPresetGrid(
                    selectedPreset = draftPreset,
                    palette = palette,
                    onPresetSelected = {
                        draftPreset = it
                        showError = false
                    },
                )

                if (draftPreset == CommitmentDurationPreset.Custom) {
                    BasicTextField(
                        value = draftCustomMonths,
                        onValueChange = {
                            draftCustomMonths = it.filter { char -> char.isDigit() }.take(3)
                            showError = false
                        },
                        textStyle = TextStyle(
                            color = palette.textMain,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center,
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.bgSurface2)
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (draftCustomMonths.isBlank()) {
                                    Text(
                                        text = "Meses",
                                        color = palette.textFaint,
                                        fontFamily = DashboardSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                    if (showError) {
                        Text(
                            text = "Elige una duración entre 1 y 120 meses.",
                            color = palette.risk,
                            fontFamily = DashboardSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        },
        dismissButton = {
            DurationDialogActionButton(
                text = "Cancelar",
                palette = palette,
                filled = false,
                onClick = onDismiss,
            )
        },
        confirmButton = {
            DurationDialogActionButton(
                text = "Aceptar",
                palette = palette,
                filled = true,
                onClick = {
                    if (draftPreset == CommitmentDurationPreset.Custom) {
                        val customMonths = normalizeCustomCommitmentMonths(draftCustomMonths)
                        if (customMonths == null) {
                            showError = true
                            return@DurationDialogActionButton
                        }
                        onConfirm(customMonths)
                    } else {
                        onConfirm(draftPreset.months)
                    }
                },
            )
        },
    )
}

@Composable
private fun DurationPresetGrid(
    selectedPreset: CommitmentDurationPreset,
    palette: DashboardPalette,
    onPresetSelected: (CommitmentDurationPreset) -> Unit,
) {
    val rows = commitmentDurationPresets.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowPresets.forEach { preset ->
                    DurationPresetButton(
                        preset = preset,
                        isSelected = selectedPreset == preset,
                        palette = palette,
                        modifier = Modifier.weight(1f),
                        onClick = { onPresetSelected(preset) },
                    )
                }
                repeat(2 - rowPresets.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DurationPresetButton(
    preset: CommitmentDurationPreset,
    isSelected: Boolean,
    palette: DashboardPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) mix(palette.colorCoral, 0.18f, palette.bgSurface)
                else palette.bgSurface,
            )
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = preset.label,
            color = if (isSelected) Color(0xFFEFAA9C) else palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.5.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DurationDialogActionButton(
    text: String,
    palette: DashboardPalette,
    filled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (filled) palette.colorCardboard else palette.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (filled) palette.bgBase else palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
        )
    }
}

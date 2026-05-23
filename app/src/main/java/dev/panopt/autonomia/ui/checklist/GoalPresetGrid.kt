package dev.panopt.autonomia.ui.checklist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.TargetPeriod
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.mix

/** Weekly goal presets displayed in the grid. */
internal val weeklyPresets = listOf(
    GoalPreset.TwoPerWeek,
    GoalPreset.ThreePerWeek,
    GoalPreset.FourPerWeek,
    GoalPreset.FivePerWeek,
    GoalPreset.SixPerWeek,
    GoalPreset.SevenPerWeek,
)

/** Monthly goal presets displayed in the grid. */
internal val monthlyPresets = listOf(
    GoalPreset.TwoPerMonth,
    GoalPreset.ThreePerMonth,
    GoalPreset.FourPerMonth,
    GoalPreset.SixPerMonth,
    GoalPreset.EightPerMonth,
    GoalPreset.TenPerMonth,
)

private enum class GoalTab { Weekly, Monthly }

/**
 * Grid-based goal preset selector with Semanal/Mensual toggle.
 *
 * Layout:
 * - "Sin meta" — full-width button
 * - "Semanal" | "Mensual" — toggle row
 * - Animated grid for the active tab (2×3)
 * - "Personalizada" — full-width button
 * - When Custom is selected: inline count + Week/Month toggle
 */
@Composable
internal fun GoalPresetGrid(
    selectedGoal: GoalPreset,
    palette: DashboardPalette,
    customCount: String,
    customPeriod: TargetPeriod,
    onGoalSelected: (GoalPreset) -> Unit,
    onCustomCountChanged: (String) -> Unit,
    onCustomPeriodChanged: (TargetPeriod) -> Unit,
) {
    val isWeekly = selectedGoal.name.endsWith("Week")
    val isMonthly = selectedGoal.name.endsWith("Month")
    val isPreset = isWeekly || isMonthly
    var activeTab by remember {
        mutableStateOf(if (isMonthly) GoalTab.Monthly else GoalTab.Weekly)
    }

    // Sync tab only when selectedGoal changes from outside (not from user toggle)
    LaunchedEffect(selectedGoal) {
        if (isMonthly) activeTab = GoalTab.Monthly
        else if (isWeekly) activeTab = GoalTab.Weekly
        // None and Custom: don't change the tab
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // "Sin meta" — full width
        PresetFullWidthButton(
            label = "Sin meta",
            isSelected = selectedGoal == GoalPreset.None,
            palette = palette,
            onClick = { onGoalSelected(GoalPreset.None) },
        )

        // Semanal / Mensual toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ToggleTab(
                label = "Semanal",
                isActive = activeTab == GoalTab.Weekly,
                palette = palette,
                modifier = Modifier.weight(1f),
                onClick = {
                    activeTab = GoalTab.Weekly
                    // Clear selection if current goal is a monthly preset
                    if (isMonthly) onGoalSelected(GoalPreset.None)
                },
            )
            ToggleTab(
                label = "Mensual",
                isActive = activeTab == GoalTab.Monthly,
                palette = palette,
                modifier = Modifier.weight(1f),
                onClick = {
                    activeTab = GoalTab.Monthly
                    // Clear selection if current goal is a weekly preset
                    if (isWeekly) onGoalSelected(GoalPreset.None)
                },
            )
        }

        // Grid (animated)
        AnimatedVisibility(
            visible = true,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            val presets = if (activeTab == GoalTab.Weekly) weeklyPresets else monthlyPresets
            PresetGrid(
                presets = presets,
                selectedGoal = selectedGoal,
                palette = palette,
                columns = 3,
                onGoalSelected = onGoalSelected,
            )
        }

        // "Personalizada" — full width
        PresetFullWidthButton(
            label = "Personalizada",
            isSelected = selectedGoal == GoalPreset.Custom,
            palette = palette,
            onClick = { onGoalSelected(GoalPreset.Custom) },
        )

        // Inline custom count + period
        if (selectedGoal == GoalPreset.Custom) {
            CustomCountRow(
                count = customCount,
                period = customPeriod,
                palette = palette,
                onCountChanged = onCustomCountChanged,
                onPeriodChanged = onCustomPeriodChanged,
            )
        }
    }
}

// ── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun ToggleTab(
    label: String,
    isActive: Boolean,
    palette: DashboardPalette,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isActive) mix(palette.colorCoral, 0.18f, palette.bgSurface)
                else palette.bgSurface,
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isActive) Color(0xFFEFAA9C) else palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun PresetFullWidthButton(
    label: String,
    isSelected: Boolean,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) mix(palette.colorCoral, 0.18f, palette.bgSurface)
                else palette.bgSurface,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            color = if (isSelected) Color(0xFFEFAA9C) else palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 15.sp,
        )
    }
}

@Composable
private fun PresetGrid(
    presets: List<GoalPreset>,
    selectedGoal: GoalPreset,
    palette: DashboardPalette,
    columns: Int,
    onGoalSelected: (GoalPreset) -> Unit,
) {
    val rows = presets.chunked(columns)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowPresets.forEach { preset ->
                    val isSelected = selectedGoal == preset
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) mix(palette.colorCoral, 0.18f, palette.bgSurface)
                                else palette.bgSurface,
                            )
                            .clickable { onGoalSelected(preset) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = countFromPreset(preset),
                                color = if (isSelected) Color(0xFFEFAA9C) else palette.textMain,
                                fontFamily = DashboardSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = frequencyLabel(preset),
                                color = if (isSelected) Color(0xFFEFAA9C) else palette.textMuted,
                                fontFamily = DashboardSans,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
                // Fill remaining space if last row has fewer items
                repeat(columns - rowPresets.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CustomCountRow(
    count: String,
    period: TargetPeriod,
    palette: DashboardPalette,
    onCountChanged: (String) -> Unit,
    onPeriodChanged: (TargetPeriod) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicTextField(
            value = count,
            onValueChange = { onCountChanged(it.filter { c -> c.isDigit() }.take(3)) },
            textStyle = TextStyle(
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontSize = 16.sp,
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgSurface2)
                .padding(12.dp),
        )

        Row(
            modifier = Modifier
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgSurface2),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PeriodToggleButton(
                label = "Semana",
                isSelected = period == TargetPeriod.Week,
                palette = palette,
                onClick = { onPeriodChanged(TargetPeriod.Week) },
            )
            PeriodToggleButton(
                label = "Mes",
                isSelected = period == TargetPeriod.Month,
                palette = palette,
                onClick = { onPeriodChanged(TargetPeriod.Month) },
            )
        }
    }
}

@Composable
private fun PeriodToggleButton(
    label: String,
    isSelected: Boolean,
    palette: DashboardPalette,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) palette.colorCoral.copy(alpha = 0.2f)
                else Color.Transparent,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isSelected) palette.colorCoral else palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 14.sp,
        )
    }
}

// ── Pure helpers ─────────────────────────────────────────────────────────────

internal fun countFromPreset(preset: GoalPreset): String =
    preset.toCountAndPeriod().first?.toString() ?: "—"

internal fun frequencyLabel(preset: GoalPreset): String = when (preset) {
    GoalPreset.TwoPerWeek, GoalPreset.ThreePerWeek, GoalPreset.FourPerWeek,
    GoalPreset.FivePerWeek, GoalPreset.SixPerWeek, GoalPreset.SevenPerWeek -> "/sem"
    GoalPreset.TwoPerMonth, GoalPreset.ThreePerMonth, GoalPreset.FourPerMonth,
    GoalPreset.SixPerMonth, GoalPreset.EightPerMonth, GoalPreset.TenPerMonth -> "/mes"
    else -> ""
}

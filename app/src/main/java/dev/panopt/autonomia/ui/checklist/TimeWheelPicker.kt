package dev.panopt.autonomia.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.mix
import kotlin.math.abs

// ── Pure helpers ─────────────────────────────────────────────────────────────

/** Hours displayed on the wheel: 0..8 inclusive. */
internal val wheelHours: List<Int> = (0..8).toList()

/** Minutes displayed on the wheel: 0, 5, 10, …, 55. */
internal val wheelMinutes: List<Int> = (0..55 step 5).toList()

/**
 * Formats an integer wheel value into a two-digit string for display.
 */
internal fun formatWheelValue(value: Int): String =
    value.toString().padStart(2, '0')

private val ItemHeight = 48
private val WheelContainerHeight = 180

// ── Composable ───────────────────────────────────────────────────────────────

/**
 * Wheel-style time picker with snap-scrolling columns for hours and minutes.
 * Visible labels show "h" and "m" above each column.
 */
@Composable
internal fun TimeWheelPicker(
    hours: Int,
    minutes: Int,
    palette: DashboardPalette,
    onHoursChanged: (Int) -> Unit,
    onMinutesChanged: (Int) -> Unit,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelContainerHeight.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.bgSurface)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Hours column
            WheelColumn(
                values = wheelHours,
                currentValue = hours,
                palette = palette,
                enabled = enabled,
                onValueSnapped = onHoursChanged,
                modifier = Modifier.weight(1f),
            )

            // Visible separator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Text(
                    text = "h",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }

            // Minutes column
            WheelColumn(
                values = wheelMinutes,
                currentValue = minutes,
                palette = palette,
                enabled = enabled,
                onValueSnapped = onMinutesChanged,
                modifier = Modifier.weight(1f),
            )

            // Visible separator
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                Text(
                    text = "m",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
            }
        }
    }
}

// ── WheelColumn ──────────────────────────────────────────────────────────────

/** Padding needed above/below the list so the first and last items can reach centre. */
private val WheelPad = (WheelContainerHeight - ItemHeight) / 2

@Composable
private fun WheelColumn(
    values: List<Int>,
    currentValue: Int,
    palette: DashboardPalette,
    enabled: Boolean,
    onValueSnapped: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialIndex = values.indexOf(currentValue).coerceAtLeast(0)
    val density = LocalDensity.current
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex,
        initialFirstVisibleItemScrollOffset = with(density) { WheelPad.dp.roundToPx() },
    )
    val snapBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Detect settled centre item
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            if (layoutInfo.visibleItemsInfo.isEmpty()) return@derivedStateOf -1

            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset / 2
            layoutInfo.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index ?: -1
        }
    }

    // Notify parent when centre item changes
    if (enabled && centerIndex in values.indices) {
        val newValue = values[centerIndex]
        if (newValue != currentValue) {
            LaunchedEffect(newValue) {
                onValueSnapped(newValue)
            }
        }
    }

    // Centre highlight drawn behind the list
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            mix(palette.colorCoral, 0.06f, Color.Transparent),
                            mix(palette.colorCoral, 0.10f, Color.Transparent),
                            mix(palette.colorCoral, 0.06f, Color.Transparent),
                            Color.Transparent,
                        ),
                    ),
                ),
        )

        LazyColumn(
            state = listState,
            flingBehavior = snapBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = WheelPad.dp),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
        ) {
            itemsIndexed(values) { index, value ->
                val distanceToCenter = abs(index - (centerIndex.coerceIn(values.indices)))
                val isCenter = distanceToCenter == 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(ItemHeight.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = formatWheelValue(value),
                        color = when {
                            isCenter -> palette.colorCardboard
                            distanceToCenter <= 2 -> palette.textMuted
                            else -> palette.textFaint
                        },
                        fontFamily = DashboardSans,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        fontSize = if (isCenter) 28.sp else 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .scale(if (isCenter) 1.1f else 0.85f)
                            .alpha(
                                when {
                                    isCenter -> 1f
                                    distanceToCenter <= 1 -> 0.6f
                                    else -> 0.35f
                                },
                            ),
                    )
                }
            }
        }
    }
}

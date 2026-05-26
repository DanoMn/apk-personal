package dev.panopt.autonomia.ui.dashboard.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import dev.panopt.autonomia.ui.dashboard.DashboardIconKind
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.Icon
import dev.panopt.autonomia.ui.dashboard.color
import dev.panopt.autonomia.ui.dashboard.iconKind

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CheckItem(
    palette: DashboardPalette,
    item: DashboardCheckItemState,
    checked: Boolean,
    onToggle: () -> Unit,
    onLongToggle: () -> Unit = {},
    isInverted: Boolean = false,
) {
    val iconKind = item.iconKind()
    val layerColor = iconKind.color(palette)
    val rowAlpha by animateFloatAsState(
        targetValue = if (checked) 0.58f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "checkItemAlpha",
    )
    val titleColor by animateColorAsState(
        targetValue = if (checked) palette.textMuted else palette.textMain,
        animationSpec = tween(durationMillis = 180),
        label = "checkItemTitleColor",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .alpha(rowAlpha)
            .animateContentSize(animationSpec = tween(durationMillis = 180))
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                role = Role.Checkbox,
                onLongClick = { onLongToggle() },
                onClick = { onToggle() },
            )
            .padding(11.52.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CheckBoxMark(
            palette = palette,
            checked = checked,
            isInverted = isInverted,
            modifier = Modifier.size(23.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = item.title,
                color = titleColor,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.36.sp,
                lineHeight = 17.66.sp,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.padding(top = 3.52.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.12.dp),
            ) {
                iconKind.Icon(color = layerColor, modifier = Modifier.size(14.dp))
                Text(
                    text = item.layerName,
                    color = layerColor,
                    fontFamily = DashboardSans,
                    fontSize = 12.16.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = item.value,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 12.48.sp,
            lineHeight = 16.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CheckBoxMark(
    palette: DashboardPalette,
    checked: Boolean,
    modifier: Modifier,
    isInverted: Boolean = false,
) {
    val checkedColor = if (isInverted) Color(0xFFB0A090) else palette.colorCoral
    val boxColor by animateColorAsState(
        targetValue = if (checked) checkedColor else palette.textMuted,
        animationSpec = tween(durationMillis = 180),
        label = "checkBoxColor",
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = 160),
        label = "checkBoxAlpha",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (checked) 1f else 0.55f,
        animationSpec = tween(durationMillis = 160),
        label = "checkBoxScale",
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 2.dp.toPx()
        val halfStroke = strokeWidth / 2f
        drawRoundRect(
            color = boxColor,
            topLeft = Offset(halfStroke, halfStroke),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
            style = if (checked) Fill else Stroke(width = strokeWidth),
        )
        if (checkAlpha > 0f) {
            val path = Path().apply {
                moveTo(size.width * 0.35f, size.height * 0.50f)
                lineTo(size.width * 0.47f, size.height * 0.66f)
                lineTo(size.width * 0.70f, size.height * 0.34f)
            }
            withTransform({
                scale(checkScale, checkScale, pivot = Offset(size.width / 2f, size.height / 2f))
            }) {
                drawPath(
                    path = path,
                    color = palette.bgBase.copy(alpha = checkAlpha),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Composable
internal fun CompletedDivider(palette: DashboardPalette) {
    Text(
        text = "Completados",
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = 7.2.dp, bottom = 2.88.dp),
        color = palette.textFaint,
        fontFamily = DashboardSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.16.sp,
        letterSpacing = 0.49.sp,
        lineHeight = 14.sp,
    )
}

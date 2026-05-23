package dev.panopt.autonomia.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import dev.panopt.autonomia.domain.dashboard.DashboardCheckItemState
import kotlin.math.min

// ── Layer icons ──────────────────────────────────────────────────────────────

@Composable
internal fun InteriorLayerIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val outer = Path().apply {
                moveTo(12f, 2f)
                lineTo(22f, 12f)
                lineTo(12f, 22f)
                lineTo(2f, 12f)
                close()
            }
            val inner = Path().apply {
                moveTo(12f, 6.5f)
                lineTo(17.5f, 12f)
                lineTo(12f, 17.5f)
                lineTo(6.5f, 12f)
                close()
            }
            drawPath(
                path = outer,
                color = color,
                style = Stroke(width = 1.35f, cap = StrokeCap.Square, join = StrokeJoin.Miter),
            )
            drawPath(path = inner, color = color, style = Fill)
        }
    }
}

@Composable
internal fun VinculosLayerIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val outer = Path().apply {
                moveTo(12f, 2f)
                lineTo(21f, 7.5f)
                lineTo(21f, 16.5f)
                lineTo(12f, 22f)
                lineTo(3f, 16.5f)
                lineTo(3f, 7.5f)
                close()
            }
            val inner = Path().apply {
                moveTo(12f, 6.5f)
                lineTo(17f, 9.5f)
                lineTo(17f, 14.5f)
                lineTo(12f, 17.5f)
                lineTo(7f, 14.5f)
                lineTo(7f, 9.5f)
                close()
            }
            drawPath(
                path = outer,
                color = color,
                style = Stroke(width = 1.35f, cap = StrokeCap.Square, join = StrokeJoin.Miter),
            )
            drawPath(path = inner, color = color, style = Fill)
        }
    }
}

@Composable
internal fun ProjectTriangleIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val triangle = Path().apply {
                moveTo(12f, 2f)
                lineTo(23f, 21f)
                lineTo(1f, 21f)
                close()
            }
            drawPath(path = triangle, color = color, style = Fill)
        }
    }
}

@Composable
internal fun WavesIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    MultiPathStrokeIcon(
        color = color,
        modifier = modifier,
        pathData = listOf(
            "M2 6c.6.5 1.2 1 2.5 1C7 7 7 5 9.5 5c2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1",
            "M2 12c.6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1",
            "M2 18c.6.5 1.2 1 2.5 1 2.5 0 2.5-2 5-2 2.6 0 2.4 2 5 2 2.5 0 2.5-2 5-2 1.3 0 1.9.5 2.5 1",
        ),
    )
}

@Composable
internal fun InfinityIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val path = Path().apply {
                moveTo(18.2f, 7.6f)
                cubicTo(20.4f, 7.6f, 22f, 9.3f, 22f, 12f)
                cubicTo(22f, 14.7f, 20.4f, 16.4f, 18.2f, 16.4f)
                cubicTo(15.5f, 16.4f, 13.8f, 13.7f, 12f, 12f)
                cubicTo(10.2f, 10.3f, 8.5f, 7.6f, 5.8f, 7.6f)
                cubicTo(3.6f, 7.6f, 2f, 9.3f, 2f, 12f)
                cubicTo(2f, 14.7f, 3.6f, 16.4f, 5.8f, 16.4f)
                cubicTo(8.5f, 16.4f, 10.2f, 13.7f, 12f, 12f)
                cubicTo(13.8f, 10.3f, 15.5f, 7.6f, 18.2f, 7.6f)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

// ── Signal icons ─────────────────────────────────────────────────────────────

@Composable
internal fun SleepIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    val moon = remember {
        PathParser().parsePathString(
            "M18.25 15.35c-1.08.66-2.36 1.04-3.72 1.04-4.05 0-7.34-3.28-7.34-7.34 0-1.74.6-3.34 1.61-4.59-3.28.92-5.68 3.92-5.68 7.48 0 4.3 3.48 7.78 7.78 7.78 3.32 0 6.15-2.08 7.25-5.01.04-.12.08-.24.1-.36z",
        ).toPath()
    }
    val star = remember {
        PathParser().parsePathString(
            "M18.15 4.25l1.02 1.78 1.78 1.02-1.78 1.02-1.02 1.78-1.02-1.78-1.78-1.02 1.78-1.02 1.02-1.78z",
        ).toPath()
    }

    Canvas(modifier = modifier) {
        drawPathInViewport(moon, viewport = 24f, color = color, style = Fill)
        drawPathInViewport(
            path = star,
            viewport = 24f,
            color = color,
            style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
internal fun GlassWaterIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val stroke = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val glass = Path().apply {
                moveTo(5.4f, 3.4f)
                lineTo(18.6f, 3.4f)
                lineTo(17.1f, 18.7f)
                cubicTo(16.95f, 20.2f, 15.8f, 21.2f, 14.3f, 21.2f)
                lineTo(9.7f, 21.2f)
                cubicTo(8.2f, 21.2f, 7.05f, 20.2f, 6.9f, 18.7f)
                close()
            }
            val water = Path().apply {
                moveTo(7.1f, 11.2f)
                cubicTo(8.2f, 10.55f, 9.3f, 10.55f, 10.55f, 11.15f)
                cubicTo(11.8f, 11.75f, 13.05f, 11.78f, 14.25f, 11.16f)
                cubicTo(15.45f, 10.55f, 16.45f, 10.55f, 17.45f, 11.08f)
            }
            drawPath(glass, color, style = stroke)
            drawPath(water, color, style = stroke)
        }
    }
}

@Composable
internal fun IntimateBoundaryIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    val outer = remember {
        PathParser().parsePathString(
            "M12 3.5l6.5 2.4v5.7c0 4.2-2.45 7.15-6.5 8.9-4.05-1.75-6.5-4.7-6.5-8.9V5.9L12 3.5z",
        ).toPath()
    }
    val inner = remember {
        PathParser().parsePathString("M12 8.1l3.7 3.7-3.7 3.7-3.7-3.7L12 8.1z").toPath()
    }
    val center = remember {
        PathParser().parsePathString("M12 10.55l1.25 1.25L12 13.05l-1.25-1.25L12 10.55z").toPath()
    }

    Canvas(modifier = modifier) {
        drawPathInViewport(outer, 24f, color, Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPathInViewport(inner, 24f, color, Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPathInViewport(center, 24f, color, Fill)
    }
}

@Composable
internal fun NoPhoneBedIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    MultiPathStrokeIcon(
        color = color,
        modifier = modifier,
        pathData = listOf(
            "M4.4 15.2V8.9",
            "M4.4 13.2h8.1c1.25 0 2.25.98 2.25 2.2v1.55",
            "M4.4 16.95h15.2",
            "M4.4 16.95v2.4M19.6 16.95v2.4",
            "M15.85 5.05h2.9c.72 0 1.3.58 1.3 1.3v6.2c0 .72-.58 1.3-1.3 1.3h-3.2c-.72 0-1.3-.58-1.3-1.3v-3",
            "M15.15 4.15l5.7 10.55",
        ),
    )
}

// ── UI icons ─────────────────────────────────────────────────────────────────

@Composable
internal fun MenuIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val stroke = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawLine(color, Offset(4f, 12f), Offset(20f, 12f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(color, Offset(4f, 6f), Offset(20f, 6f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(color, Offset(4f, 18f), Offset(20f, 18f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun SearchIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            drawCircle(
                color = color,
                radius = 8f,
                center = Offset(11f, 11f),
                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawLine(
                color = color,
                start = Offset(16.65f, 16.65f),
                end = Offset(21f, 21f),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun XIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            drawLine(color, Offset(18f, 6f), Offset(6f, 18f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(6f, 6f), Offset(18f, 18f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun ActivityIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val path = Path().apply {
                moveTo(22f, 12f)
                lineTo(18f, 12f)
                lineTo(15f, 21f)
                lineTo(9f, 3f)
                lineTo(6f, 12f)
                lineTo(2f, 12f)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
internal fun FlagIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            withTransform({ translate(left = -1.35f, top = 0f) }) {
                val poleStroke = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawLine(
                    color = color,
                    start = Offset(7.1f, 4.2f),
                    end = Offset(7.1f, 20.6f),
                    strokeWidth = poleStroke.width,
                    cap = StrokeCap.Round,
                )

                val flag = Path().apply {
                    moveTo(8.0f, 5.0f)
                    cubicTo(10.1f, 3.95f, 12.2f, 4.15f, 14.2f, 4.85f)
                    cubicTo(16.15f, 5.52f, 17.95f, 5.45f, 19.6f, 4.55f)
                    lineTo(19.6f, 13.0f)
                    cubicTo(17.82f, 13.98f, 15.92f, 14.08f, 13.95f, 13.42f)
                    cubicTo(11.85f, 12.72f, 9.95f, 12.55f, 8.0f, 13.62f)
                    close()
                }
                drawPath(path = flag, color = color.copy(alpha = 0.24f), style = Fill)
                drawPath(
                    path = flag,
                    color = color,
                    style = Stroke(width = 1.45f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        }
    }
}

@Composable
internal fun CheckIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            drawLine(color, Offset(20f, 6f), Offset(9f, 17f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(9f, 17f), Offset(4f, 12f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun CircleIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = color,
            radius = size.minDimension / 2f - 1.5f,
            style = Stroke(width = 1.5f),
        )
    }
}

@Composable
internal fun MoonIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    val path = remember {
        PathParser().parsePathString(
            "M21 12.8A8.4 8.4 0 1 1 11.2 3a6.5 6.5 0 0 0 9.8 9.8z",
        ).toPath()
    }

    Canvas(modifier = modifier) {
        drawPathInViewport(
            path = path,
            viewport = 24f,
            color = color,
            style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
internal fun AnchorIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val stroke = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawRoundRect(color, Offset(4f, 5f), Size(3.3f, 3.3f), CornerRadius(0.9f, 0.9f))
            drawLine(color, Offset(10f, 6.65f), Offset(19.5f, 6.65f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawRoundRect(color, Offset(4f, 10.35f), Size(3.3f, 3.3f), CornerRadius(0.9f, 0.9f))
            drawLine(color, Offset(10f, 12f), Offset(18f, 12f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawRoundRect(color, Offset(4f, 15.7f), Size(3.3f, 3.3f), CornerRadius(0.9f, 0.9f))
            drawLine(color, Offset(10f, 17.35f), Offset(16.5f, 17.35f), strokeWidth = stroke.width, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun ListTodoIcon(
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
) {
    Canvas(modifier = modifier) {
        drawIconViewport {
            val stroke = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawLine(color, Offset(13f, 5f), Offset(21f, 5f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(color, Offset(13f, 12f), Offset(21f, 12f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawLine(color, Offset(13f, 19f), Offset(21f, 19f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            drawCheckmark(color, 3f, 5f)
            drawCheckmark(color, 3f, 12f)
            drawCheckmark(color, 3f, 19f)
        }
    }
}

// ── Drawer icons ─────────────────────────────────────────────────────────────

@Composable
internal fun LayoutGridIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawIconViewport {
            val stroke = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val radius = CornerRadius(1f, 1f)
            drawRoundRect(color, Offset(3f, 3f), Size(7f, 7f), radius, style = stroke)
            drawRoundRect(color, Offset(14f, 3f), Size(7f, 7f), radius, style = stroke)
            drawRoundRect(color, Offset(14f, 14f), Size(7f, 7f), radius, style = stroke)
            drawRoundRect(color, Offset(3f, 14f), Size(7f, 7f), radius, style = stroke)
        }
    }
}

@Composable
internal fun ShieldAlertIcon(color: Color) {
    PathStrokeIcon(
        pathData = "M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67 0C7.5 20.5 4 18 4 13V5a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 2.8 17 4 19 4a1 1 0 0 1 1 1v8z",
        color = color,
        modifier = Modifier.size(22.dp),
    )
    Canvas(modifier = Modifier.size(22.dp)) {
        drawIconViewport {
            drawLine(color, Offset(12f, 8f), Offset(12f, 12f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(12f, 16f), Offset(12.01f, 16f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun BarChartIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawIconViewport {
            drawLine(color, Offset(3f, 3f), Offset(3f, 21f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(3f, 21f), Offset(21f, 21f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(18f, 17f), Offset(18f, 9f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(13f, 17f), Offset(13f, 5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(8f, 17f), Offset(8f, 14f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        }
    }
}

@Composable
internal fun SlidersIcon(color: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        drawIconViewport {
            drawLine(color, Offset(21f, 4f), Offset(14f, 4f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(10f, 4f), Offset(3f, 4f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(21f, 12f), Offset(12f, 12f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(8f, 12f), Offset(3f, 12f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(21f, 20f), Offset(16f, 20f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawLine(color, Offset(12f, 20f), Offset(3f, 20f), strokeWidth = 1.5f, cap = StrokeCap.Round)
            drawCircle(color, radius = 2f, center = Offset(12f, 4f), style = Stroke(width = 1.5f))
            drawCircle(color, radius = 2f, center = Offset(10f, 12f), style = Stroke(width = 1.5f))
            drawCircle(color, radius = 2f, center = Offset(14f, 20f), style = Stroke(width = 1.5f))
        }
    }
}

@Composable
internal fun LogbookIcon(color: Color) {
    MultiPathStrokeIcon(
        color = color,
        pathData = listOf(
            "M7 4.5h8.6c1 0 1.9.85 1.9 1.9v13.1H8.2A2.7 2.7 0 0 1 5.5 16.8V6A1.5 1.5 0 0 1 7 4.5z",
            "M8.2 19.5a2.2 2.2 0 0 1 0-4.4h9.3",
            "M9.2 8.2h5.1M9.2 11h3.8",
        ),
    )
}

@Composable
internal fun LayerStackIcon(color: Color) {
    MultiPathStrokeIcon(
        color = color,
        pathData = listOf(
            "M12 4l8 4.2-8 4.2-8-4.2L12 4z",
            "M4 12l8 4.2 8-4.2",
            "M4 15.8l8 4.2 8-4.2",
        ),
    )
}

// ── Logo ─────────────────────────────────────────────────────────────────────

@Composable
internal fun SpiralLogo(
    color: Color,
    modifier: Modifier = Modifier.size(38.dp),
) {
    val path = remember {
        PathParser().parsePathString(SPIRAL_PATH).toPath()
    }

    Canvas(modifier = modifier) {
        val iconSize = min(size.width, size.height)
        val scale = iconSize / 64f
        val dx = (size.width - iconSize) / 2f
        val dy = (size.height - iconSize) / 2f

        withTransform({
            translate(dx, dy)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 4f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round,
                ),
            )
        }
    }
}

internal const val SPIRAL_PATH =
    "M56 32C56 18.7 45.3 8 32 8C18.7 8 8 18.7 8 32C8 45.3 18.7 56 32 56C43.6 56 52 47.6 52 36.6C52 25.7 43.9 16 33 16C23.1 16 16 23.1 16 32C16 41.1 22.9 48 32 48C39.3 48 44 43.3 44 36.2C44 29.1 39.1 24 32 24C27.1 24 24 27.1 24 32C24 36.7 27.3 40 32 40C35.1 40 37.4 37.7 37.4 34.9C37.4 31.9 35.4 30 32 30"

// ── Drawing utilities ────────────────────────────────────────────────────────

@Composable
internal fun PathStrokeIcon(
    pathData: String,
    color: Color,
    modifier: Modifier = Modifier.size(22.dp),
    viewport: Float = 24f,
) {
    val path = remember(pathData) {
        PathParser().parsePathString(pathData).toPath()
    }

    Canvas(modifier = modifier) {
        drawPathInViewport(
            path = path,
            viewport = viewport,
            color = color,
            style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
internal fun MultiPathStrokeIcon(
    color: Color,
    pathData: List<String>,
    modifier: Modifier = Modifier.size(22.dp),
) {
    val paths = remember(pathData) {
        pathData.map { PathParser().parsePathString(it).toPath() }
    }

    Canvas(modifier = modifier) {
        paths.forEach { path ->
            drawPathInViewport(
                path = path,
                viewport = 24f,
                color = color,
                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

internal fun DrawScope.drawPathInViewport(
    path: Path,
    viewport: Float,
    color: Color,
    style: DrawStyle,
) {
    val iconSize = min(size.width, size.height)
    val scale = iconSize / viewport
    val dx = (size.width - iconSize) / 2f
    val dy = (size.height - iconSize) / 2f

    withTransform({
        translate(dx, dy)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        drawPath(
            path = path,
            color = color,
            style = style,
        )
    }
}

internal fun DrawScope.drawIconViewport(block: DrawScope.() -> Unit) {
    val iconSize = min(size.width, size.height)
    val scale = iconSize / 24f
    val dx = (size.width - iconSize) / 2f
    val dy = (size.height - iconSize) / 2f

    withTransform({
        translate(dx, dy)
        scale(scale, scale, pivot = Offset.Zero)
    }) {
        block()
    }
}

internal fun DrawScope.drawCheckmark(
    color: Color,
    startX: Float,
    centerY: Float,
) {
    val path = Path().apply {
        moveTo(startX, centerY)
        lineTo(startX + 2f, centerY + 2f)
        lineTo(startX + 6f, centerY - 2f)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

// -- Layer icon kind mapping --

internal enum class DashboardIconKind {
    Interior,
    Body,
    Project,
    Conduct,
}

internal fun DashboardIconKind.color(palette: DashboardPalette): Color =
    when (this) {
        DashboardIconKind.Interior -> palette.layerInterior
        DashboardIconKind.Body -> palette.layerBody
        DashboardIconKind.Project -> palette.layerProject
        DashboardIconKind.Conduct -> palette.layerConduct
    }

@Composable
internal fun DashboardIconKind.Icon(
    color: Color,
    modifier: Modifier,
) {
    when (this) {
        DashboardIconKind.Interior -> InteriorLayerIcon(color = color, modifier = modifier)
        DashboardIconKind.Body -> WavesIcon(color = color, modifier = modifier)
        DashboardIconKind.Project -> ProjectTriangleIcon(color = color, modifier = modifier)
        DashboardIconKind.Conduct -> NoPhoneBedIcon(color = color, modifier = modifier)
    }
}

internal fun DashboardCheckItemState.iconKind(): DashboardIconKind =
    when (layerId) {
        "layer_interior" -> DashboardIconKind.Interior
        "layer_cuerpo" -> DashboardIconKind.Body
        "layer_proyecto" -> DashboardIconKind.Project
        "layer_conducta" -> DashboardIconKind.Conduct
        else -> DashboardIconKind.Conduct
    }

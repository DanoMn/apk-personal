package dev.panopt.autonomia.ui.dashboard.components

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardDimensionStatus
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.domain.dashboard.DashboardSobrietyTrackState
import dev.panopt.autonomia.ui.dashboard.GlassWaterIcon
import dev.panopt.autonomia.ui.dashboard.IntimateBoundaryIcon

private val DashboardSans = FontFamily.SansSerif

@Composable
internal fun SobrietySection(
    palette: DashboardPalette,
    tracks: List<DashboardSobrietyTrackState>,
    onToggleClean: (String, Boolean) -> Unit,
    onOpenConfig: () -> Unit,
) {
    SectionHeader(
        palette = palette,
        title = "Sobriedad",
        note = if (tracks.isEmpty()) "sin rachas activas" else "rachas activas",
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(11.2.dp),
    ) {
        if (tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.bgSurface)
                    .clickable(role = Role.Button, onClick = onOpenConfig)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Activa solo las rachas que quieres cuidar.",
                    color = palette.textMuted,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp,
                )
            }
            return@Column
        }

        tracks.chunked(2).forEach { rowTracks ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(11.2.dp),
            ) {
                rowTracks.forEach { track ->
                    StreakCard(
                        palette = palette,
                        track = track,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onToggleClean(track.id, track.isMarkedCleanToday)
                        },
                    )
                }
                if (rowTracks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgSurface)
                .clickable(role = Role.Button, onClick = onOpenConfig),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Abrir sobriedad",
                color = palette.colorCardboard,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
            )
        }
    }
}

@Composable
internal fun StreakCard(
    palette: DashboardPalette,
    track: DashboardSobrietyTrackState,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val accent = track.statusColor(palette)

    Column(
        modifier = modifier
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                SobrietyIcon(trackId = track.id, color = accent)
            }
            Text(
                text = track.label,
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                lineHeight = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = track.days.toString(),
                    color = palette.colorCardboard,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    lineHeight = 36.sp,
                    modifier = Modifier.alignByBaseline(),
                )
                Text(
                    text = "dias",
                    color = palette.colorCardboard,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.alignByBaseline(),
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = track.meta,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 14.5.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun SobrietyIcon(
    trackId: String,
    color: Color,
) {
    when (trackId) {
        "trk_sexual" -> IntimateBoundaryIcon(color = color, modifier = Modifier.size(26.dp))
        else -> GlassWaterIcon(color = color, modifier = Modifier.size(26.dp))
    }
}

private fun DashboardSobrietyTrackState.statusColor(palette: DashboardPalette): Color =
    when (status) {
        DashboardDimensionStatus.Stable -> palette.layerBody
        DashboardDimensionStatus.Motion -> palette.stateMotion
        DashboardDimensionStatus.Attention -> palette.colorCardboard
        DashboardDimensionStatus.Restoration -> palette.risk
        DashboardDimensionStatus.Unknown -> palette.textMuted
    }

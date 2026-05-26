package dev.panopt.autonomia.ui.sobriety

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardDimensionStatus
import dev.panopt.autonomia.domain.dashboard.DashboardSobrietyTrackState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.GlassWaterIcon
import dev.panopt.autonomia.ui.dashboard.IntimateBoundaryIcon
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun SobrietyConfigScreen(
    tracks: List<DashboardSobrietyTrackState>,
    palette: DashboardPalette,
    onToggleClean: (String, Boolean) -> Unit,
    onToggleRelapse: (String, Boolean) -> Unit,
    onSetTrackActive: (String, Boolean) -> Unit,
    onAddTrack: (String) -> Unit,
    onRemoveTrack: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var isAdding by remember { mutableStateOf(false) }
    var newTrackName by remember { mutableStateOf("") }

    val activeTracks = tracks.filter { it.active }
    val inactiveTracks = tracks.filter { !it.active }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .statusBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp),
    ) {
        Header(
            activeCount = activeTracks.size,
            palette = palette,
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle(
                title = "Rachas activas",
                note = if (activeTracks.isEmpty()) "ninguna activa" else "${activeTracks.size} activas",
                palette = palette,
            )

            if (activeTracks.isEmpty()) {
                EmptyState(
                    text = "Activa solo las rachas que quieres cuidar. Si no aplica, no pesa.",
                    palette = palette,
                )
            } else {
                activeTracks.forEach { track ->
                    ActiveTrackRow(
                        track = track,
                        palette = palette,
                        onToggleClean = onToggleClean,
                        onToggleRelapse = onToggleRelapse,
                        onPause = { onSetTrackActive(track.id, false) },
                        onRemove = if (track.isCustom) {
                            { onRemoveTrack(track.id) }
                        } else {
                            null
                        },
                    )
                }
            }

            SectionTitle(
                title = "Para activar",
                note = if (inactiveTracks.isEmpty()) "sin pendientes" else "${inactiveTracks.size} disponibles",
                palette = palette,
            )

            if (inactiveTracks.isEmpty()) {
                EmptyState(
                    text = "No hay rachas inactivas.",
                    palette = palette,
                )
            } else {
                inactiveTracks.forEach { track ->
                    InactiveTrackRow(
                        track = track,
                        palette = palette,
                        onActivate = { onSetTrackActive(track.id, true) },
                        onRemove = if (track.isCustom) {
                            { onRemoveTrack(track.id) }
                        } else {
                            null
                        },
                    )
                }
            }

            SectionTitle(
                title = "Personalizada",
                note = "opcional",
                palette = palette,
            )

            if (isAdding) {
                AddTrackRow(
                    value = newTrackName,
                    palette = palette,
                    onValueChange = { newTrackName = it.take(80) },
                    onCancel = {
                        newTrackName = ""
                        isAdding = false
                    },
                    onAdd = {
                        val finalName = newTrackName.trim()
                        if (finalName.isNotBlank()) {
                            onAddTrack(finalName)
                            newTrackName = ""
                            isAdding = false
                        }
                    },
                )
            } else {
                FullWidthButton(
                    text = "Agregar racha",
                    palette = palette,
                    primary = true,
                    onClick = { isAdding = true },
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Header(
    activeCount: Int,
    palette: DashboardPalette,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 18.dp),
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
                text = "Sobriedad",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 28.sp,
            )
            Text(
                text = "$activeCount rachas activas",
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    note: String,
    palette: DashboardPalette,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = title,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
        )
        Text(
            text = note,
            color = palette.textFaint,
            fontFamily = DashboardSans,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ActiveTrackRow(
    track: DashboardSobrietyTrackState,
    palette: DashboardPalette,
    onToggleClean: (String, Boolean) -> Unit,
    onToggleRelapse: (String, Boolean) -> Unit,
    onPause: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    TrackShell(
        track = track,
        palette = palette,
        trailing = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End,
            ) {
                ActionPill(
                    text = if (track.isMarkedCleanToday) "Desmarcar" else "Limpio",
                    palette = palette,
                    primary = true,
                    onClick = { onToggleClean(track.id, track.isMarkedCleanToday) },
                )
                ActionPill(
                    text = if (track.isRelapseToday) "Desmarcar" else "Recaida",
                    palette = palette,
                    danger = true,
                    onClick = { onToggleRelapse(track.id, track.isRelapseToday) },
                )
                ActionPill(
                    text = "Pausar",
                    palette = palette,
                    onClick = onPause,
                )
                if (onRemove != null) {
                    ActionPill(
                        text = "Eliminar",
                        palette = palette,
                        onClick = onRemove,
                    )
                }
            }
        },
    )
}

@Composable
private fun InactiveTrackRow(
    track: DashboardSobrietyTrackState,
    palette: DashboardPalette,
    onActivate: () -> Unit,
    onRemove: (() -> Unit)?,
) {
    TrackShell(
        track = track,
        palette = palette,
        muted = true,
        trailing = {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End,
            ) {
                ActionPill(
                    text = "Activar",
                    palette = palette,
                    primary = true,
                    onClick = onActivate,
                )
                if (onRemove != null) {
                    ActionPill(
                        text = "Eliminar",
                        palette = palette,
                        onClick = onRemove,
                    )
                }
            }
        },
    )
}

@Composable
private fun TrackShell(
    track: DashboardSobrietyTrackState,
    palette: DashboardPalette,
    muted: Boolean = false,
    trailing: @Composable () -> Unit,
) {
    val accent = track.statusColor(palette)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(30.dp),
            contentAlignment = Alignment.Center,
        ) {
            SobrietyIcon(
                trackId = track.id,
                color = if (muted) palette.textMuted.copy(alpha = 0.45f) else accent,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.label,
                color = if (muted) palette.textMuted else palette.textMain,
                fontFamily = DashboardSans,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.subtitle(),
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 12.5.sp,
                lineHeight = 16.sp,
            )
            if (track.active) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = track.days.toString(),
                        color = palette.colorCardboard,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
                        lineHeight = 26.sp,
                    )
                    Text(
                        text = "dias",
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                    Text(
                        text = track.meta,
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
        }
        trailing()
    }
}

@Composable
private fun AddTrackRow(
    value: String,
    palette: DashboardPalette,
    onValueChange: (String) -> Unit,
    onCancel: () -> Unit,
    onAdd: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                color = palette.textMain,
                fontFamily = DashboardSans,
                fontSize = 15.sp,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.bgSurface2)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = "Nombre de la racha",
                            color = palette.textFaint,
                            fontFamily = DashboardSans,
                            fontSize = 15.sp,
                        )
                    }
                    innerTextField()
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FullWidthButton(
                text = "Cancelar",
                palette = palette,
                primary = false,
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            )
            FullWidthButton(
                text = "Agregar",
                palette = palette,
                primary = true,
                onClick = onAdd,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun EmptyState(
    text: String,
    palette: DashboardPalette,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.bgSurface)
            .padding(16.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            color = palette.textMuted,
            fontFamily = DashboardSans,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
        )
    }
}

@Composable
private fun FullWidthButton(
    text: String,
    palette: DashboardPalette,
    primary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (primary) palette.colorCardboard else palette.bgSurface2)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (primary) palette.bgBase else palette.textMain,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
        )
    }
}

@Composable
private fun ActionPill(
    text: String,
    palette: DashboardPalette,
    primary: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val background = when {
        primary -> palette.colorCardboard
        danger -> mix(palette.risk, 0.18f, palette.bgSurface2)
        else -> palette.bgSurface2
    }
    val textColor = when {
        primary -> palette.bgBase
        danger -> palette.risk
        else -> palette.textMuted
    }

    Box(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = textColor,
            fontFamily = DashboardSans,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun SobrietyIcon(
    trackId: String,
    color: Color,
) {
    when (trackId) {
        "trk_sexual" -> IntimateBoundaryIcon(color = color, modifier = Modifier.size(24.dp))
        else -> GlassWaterIcon(color = color, modifier = Modifier.size(24.dp))
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

private fun DashboardSobrietyTrackState.subtitle(): String =
    buildString {
        append(if (isCustom) "personalizada" else "preset")
        append(" - ")
        append(if (severity == "Critical") "critica" else "moderada")
        if (!active) append(" - inactiva")
    }

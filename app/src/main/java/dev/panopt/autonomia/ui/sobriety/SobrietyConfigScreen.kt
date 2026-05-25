package dev.panopt.autonomia.ui.sobriety

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.domain.dashboard.DashboardSobrietyTrackState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.mix

@Composable
internal fun SobrietyConfigScreen(
    tracks: List<DashboardSobrietyTrackState>,
    palette: DashboardPalette,
    onToggleClean: (String, Boolean) -> Unit,
    onToggleRelapse: (String, Boolean) -> Unit,
    onAddTrack: (String) -> Unit,
    onRemoveTrack: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    var isAdding by remember { mutableStateOf(false) }
    var newTrackName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .padding(horizontal = 18.dp)
            .padding(top = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(palette.bgSurface).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) {
                XIcon(color = palette.textMain)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text("Sobriedad", color = palette.colorCardboard, fontFamily = DashboardSerif, fontWeight = FontWeight.Medium, fontSize = 24.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text("${tracks.size} activos", color = palette.colorCoral, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Rachas activas", color = palette.textMuted, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 13.sp)

            tracks.forEach { track ->
                Row(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.bgSurface).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.label, color = palette.textMain, fontFamily = DashboardSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(track.days.toString(), color = palette.colorCardboard, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                            Text("días", color = palette.textMuted, fontFamily = DashboardSans, fontSize = 14.sp)
                        }
                        Text(track.meta, color = palette.textMuted, fontFamily = DashboardSans, fontSize = 13.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(modifier = Modifier.height(34.dp).clip(RoundedCornerShape(8.dp)).background(palette.colorCardboard).clickable(role = Role.Button, onClick = { onToggleClean(track.id, track.isMarkedCleanToday) }).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text(if (track.isMarkedCleanToday) "Desmarcar" else "Limpio", color = palette.bgBase, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Box(modifier = Modifier.height(34.dp).clip(RoundedCornerShape(8.dp)).background(mix(palette.risk, 0.2f, palette.bgSurface)).clickable(role = Role.Button, onClick = { onToggleRelapse(track.id, track.isRelapseToday) }).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text(if (track.isRelapseToday) "Desmarcar" else "Recaída", color = palette.risk, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (isAdding) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = newTrackName,
                        onValueChange = { newTrackName = it },
                        textStyle = androidx.compose.ui.text.TextStyle(color = palette.textMain, fontFamily = DashboardSans, fontSize = 14.sp),
                        singleLine = true,
                        modifier = Modifier.weight(1f).height(44.dp).clip(RoundedCornerShape(10.dp)).background(palette.bgSurface2).padding(horizontal = 12.dp, vertical = 11.dp),
                    )
                    Box(modifier = Modifier.height(44.dp).clip(RoundedCornerShape(10.dp)).background(palette.colorCardboard).clickable(role = Role.Button) {
                        if (newTrackName.isNotBlank()) { onAddTrack(newTrackName.trim()); newTrackName = ""; isAdding = false }
                    }.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
                        Text("Agregar", color = palette.bgBase, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp)).background(palette.colorCardboard).clickable(role = Role.Button, onClick = { isAdding = true }), contentAlignment = Alignment.Center) {
                    Text("+ Agregar track personalizado", color = palette.bgBase, fontFamily = DashboardSans, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

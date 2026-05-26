package dev.panopt.autonomia.ui.dashboard.components

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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.panopt.autonomia.domain.dashboard.DashboardActivityOptionState
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.anchors.TimeWheelPicker

private val DashboardSans = FontFamily.SansSerif
private val DashboardSerif = FontFamily.Serif

@Composable
internal fun ActivityValueInputDialog(
    activity: DashboardActivityOptionState,
    palette: DashboardPalette,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var totalMinutes by remember { 
        mutableStateOf(activity.actualValue.coerceAtLeast(activity.targetValue)) 
    }
    var hours by remember { mutableStateOf(totalMinutes / 60) }
    var minutes by remember { mutableStateOf(totalMinutes % 60) }

    LaunchedEffect(hours, minutes) {
        totalMinutes = hours * 60 + minutes
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.drawer)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Tiempo dedicado",
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = activity.title,
                color = palette.textMuted,
                fontFamily = DashboardSans,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                TimeWheelPicker(
                    hours = hours,
                    minutes = minutes,
                    palette = palette,
                    onHoursChanged = { hours = it },
                    onMinutesChanged = { minutes = it },
                )
            }
            
            Spacer(modifier = Modifier.height(36.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.bgSurface2)
                        .clickable(role = Role.Button, onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancelar",
                        color = palette.textMuted,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.colorCardboard)
                        .clickable(role = Role.Button) {
                            onSave(totalMinutes)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Registrar",
                        color = palette.bgBase,
                        fontFamily = DashboardSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

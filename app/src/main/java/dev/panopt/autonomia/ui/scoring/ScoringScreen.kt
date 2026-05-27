package dev.panopt.autonomia.ui.scoring

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.panopt.autonomia.ScoreState
import dev.panopt.autonomia.domain.dashboard.DashboardState
import dev.panopt.autonomia.ui.dashboard.ActivityIcon
import dev.panopt.autonomia.ui.dashboard.DashboardPalette
import dev.panopt.autonomia.ui.dashboard.DashboardSans
import dev.panopt.autonomia.ui.dashboard.DashboardSerif
import dev.panopt.autonomia.ui.dashboard.XIcon
import dev.panopt.autonomia.ui.dashboard.components.IconButtonShell
import dev.panopt.autonomia.ui.dashboard.components.ScoreOrbit

@Composable
internal fun ScoringScreen(
    state: DashboardState,
    palette: DashboardPalette,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val report = state.scoreReport
    val scoreState = state.status.scoreState
    val accent = scoreStateColor(palette, scoreState)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconButtonShell(
                palette = palette,
                contentDescription = "Volver",
                onClick = onBack,
            ) {
                XIcon(color = palette.textMain)
            }
            Text(
                text = "Estado Base",
                modifier = Modifier.weight(1f),
                color = palette.colorCardboard,
                fontFamily = DashboardSerif,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                lineHeight = 27.sp,
            )
            ActivityIcon(color = accent)
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.bgSurface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = report.stateTitle,
                    color = accent,
                    fontFamily = DashboardSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                )
                Spacer(modifier = Modifier.height(9.dp))
                Text(
                    text = report.headline,
                    color = palette.textMain,
                    fontFamily = DashboardSerif,
                    fontWeight = FontWeight.Medium,
                    fontSize = 26.sp,
                    lineHeight = 29.sp,
                )
            }
            ScoreOrbit(
                palette = palette,
                score = report.scoreLabel,
                label = if (scoreState == ScoreState.NoData) "sin score" else "base",
                progress = report.progress,
                color = accent,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        ReportSectionTitle(palette = palette, title = "Lectura semanal")
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ScoreMetricRow(palette, "Base semanal", report.weeklyBaseLabel)
            ScoreMetricRow(palette, "Score semanal", report.weeklyScoreLabel)
            ScoreMetricRow(palette, "Promedio de capas", report.averageLayerLabel)
            ScoreMetricRow(palette, "Capa mas baja", report.worstLayerLabel)
            ScoreMetricRow(palette, "Estabilidad", report.stabilityLabel)
        }

        Spacer(modifier = Modifier.height(20.dp))
        ReportSectionTitle(palette = palette, title = "Razones")
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            report.reasons.forEach { reason ->
                ReasonRow(palette = palette, text = reason)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        ReportSectionTitle(palette = palette, title = "Capas")
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            report.layers.forEach { layer ->
                ScoreLayerReportCard(palette = palette, layer = layer)
            }
        }
    }
}

package dev.panopt.autonomia.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.panopt.autonomia.ui.dashboard.components.ActionButtons
import dev.panopt.autonomia.ui.dashboard.components.AnchorPhraseCard
import dev.panopt.autonomia.ui.dashboard.components.ChecklistPreviewSection
import dev.panopt.autonomia.ui.dashboard.components.DailyProgressCard
import dev.panopt.autonomia.ui.dashboard.components.LayersSection
import dev.panopt.autonomia.ui.dashboard.components.NavigationDrawer
import dev.panopt.autonomia.ui.dashboard.components.SignalsSection
import dev.panopt.autonomia.ui.dashboard.components.SobrietySection
import dev.panopt.autonomia.ui.dashboard.components.StatusCard
import dev.panopt.autonomia.ui.dashboard.components.SupportsSection
import dev.panopt.autonomia.ui.dashboard.components.TopBar
import dev.panopt.autonomia.ui.dashboard.components.WeekSection
import dev.panopt.autonomia.ui.dashboard.components.rememberDrawerWidth
import kotlinx.coroutines.launch

@Composable
fun DashboardScreen(
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
) {
    val palette = dashboardPalette(isDarkMode)
    var isDrawerOpen by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = isDrawerOpen) {
        isDrawerOpen = false
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.bgBase),
    ) {
        DashboardBackground(palette = palette)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 32.dp),
        ) {
            TopBar(
                palette = palette,
                onOpenDrawer = { isDrawerOpen = true },
            )
            StatusCard(palette = palette)
            DailyProgressCard(palette = palette)
            AnchorPhraseCard(palette = palette)
            ActionButtons(
                palette = palette,
                onChecklistClick = {
                    coroutineScope.launch {
                        scrollState.animateScrollTo((scrollState.maxValue * 0.55f).toInt())
                    }
                },
            )
            LayersSection(palette = palette)
            SignalsSection(palette = palette)
            SobrietySection(palette = palette)
            ChecklistPreviewSection(palette = palette)
            SupportsSection(palette = palette)
            WeekSection(palette = palette)
        }

        if (isDrawerOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        role = Role.Button,
                        onClick = { isDrawerOpen = false },
                    )
                    .semantics { contentDescription = "Cerrar menú" },
            )
        }

        val drawerWidth = rememberDrawerWidth(maxWidth = maxWidth)
        val drawerOffset by animateDpAsState(
            targetValue = if (isDrawerOpen) 0.dp else drawerWidth * -1.05f,
            animationSpec = tween(durationMillis = 250),
            label = "drawerOffset",
        )

        NavigationDrawer(
            palette = palette,
            isDarkMode = isDarkMode,
            width = drawerWidth,
            modifier = Modifier.offset(x = drawerOffset),
            onClose = { isDrawerOpen = false },
            onThemeChange = onThemeChange,
        )
    }
}

@Composable
private fun DashboardBackground(palette: DashboardPalette) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    palette.colorCoral.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = Offset(x = size.width * 0.2f, y = 0f),
                radius = 512.dp.toPx(),
            ),
        )
    }
}

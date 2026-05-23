package dev.panopt.autonomia.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import dev.panopt.autonomia.TargetPeriod
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.panopt.autonomia.SleepQuality
import dev.panopt.autonomia.domain.dashboard.DashboardState
import dev.panopt.autonomia.ui.dashboard.components.ActionButtons
import dev.panopt.autonomia.ui.dashboard.components.AnchorPhraseCard
import dev.panopt.autonomia.ui.dashboard.components.ChecklistPreviewSection
import dev.panopt.autonomia.ui.dashboard.components.DailyProgressCard
import dev.panopt.autonomia.ui.dashboard.components.LayersSection
import dev.panopt.autonomia.ui.dashboard.components.NavigationDrawer
import dev.panopt.autonomia.ui.dashboard.components.SignalsSection
import dev.panopt.autonomia.ui.dashboard.components.SobrietySection
import dev.panopt.autonomia.ui.dashboard.components.StatusCard
import dev.panopt.autonomia.ui.dashboard.components.SupportsPreviewSection
import dev.panopt.autonomia.ui.dashboard.components.SupportsSection
import dev.panopt.autonomia.ui.dashboard.components.TopBar
import dev.panopt.autonomia.ui.dashboard.components.WeekSection
import dev.panopt.autonomia.ui.dashboard.components.rememberDrawerWidth

@Composable
internal fun DashboardScreen(
    state: DashboardState,
    isDarkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onToggleActivity: (String, Boolean) -> Unit,
    onToggleAbstinenceClean: (String, Boolean) -> Unit,
    onSaveActivityValue: (String, Int) -> Unit,
    onSaveSleep: (String, String, String, String, SleepQuality, String) -> Unit,
    onToggleAbstinenceRelapse: (String, Boolean) -> Unit,
    onCreateActivity: (String, String, Int, Boolean, Boolean, Boolean) -> Unit,
    onSetFocusSignal: (String) -> Unit,
    onCreateTask: (String, String?, Boolean) -> Unit,
    onCompleteTask: (String) -> Unit,
    onAddToChecklist: (String, Int?, Int?, TargetPeriod?) -> Unit,
    onRemoveFromChecklist: (String) -> Unit,
    onNavigateToChecklistConfig: () -> Unit,
    onToggleSupport: (String) -> Unit = {},
    onNavigateToSupportsConfig: () -> Unit = {},
) {
    val palette = dashboardPalette(isDarkMode)
    var isDrawerOpen by remember { mutableStateOf(false) }
    var activeSheet by remember { mutableStateOf<DashboardSheet?>(null) }

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
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 32.dp),
        ) {
            TopBar(
                palette = palette,
                onOpenDrawer = { isDrawerOpen = true },
            )
            StatusCard(palette = palette, status = state.status)
            DailyProgressCard(palette = palette, progress = state.dailyProgress)
            AnchorPhraseCard(palette = palette, phrase = state.anchorPhrase)
            ActionButtons(
                palette = palette,
                onQuickConfigClick = { activeSheet = DashboardSheet.EntryMenu },
                onRiesgoClick = { activeSheet = DashboardSheet.Relapse },
            )
            LayersSection(palette = palette, layers = state.layers)
            SignalsSection(
                palette = palette,
                signals = state.signals,
                onSleepClick = { activeSheet = DashboardSheet.Sleep },
                onSignalSettingsClick = { activeSheet = DashboardSheet.Activities },
            )
            SobrietySection(
                palette = palette,
                tracks = state.sobrietyTracks,
                onToggleClean = onToggleAbstinenceClean,
            )
            ChecklistPreviewSection(
                palette = palette,
                items = state.checklistItems,
                onToggle = onToggleActivity,
            )
            SupportsPreviewSection(
                palette = palette,
                items = state.secondaryChecklistItems,
                onToggle = onToggleSupport,
                onOpenConfig = onNavigateToSupportsConfig,
            )
            SupportsSection(
                palette = palette,
                supports = state.supports,
                onOpenSecondaryChecklist = { activeSheet = DashboardSheet.SecondaryChecklist },
                onOpenTasks = { activeSheet = DashboardSheet.Tasks },
            )
            WeekSection(palette = palette, rows = state.weekRows)
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
            onOpenChecklist = onNavigateToChecklistConfig,
            onOpenSupports = onNavigateToSupportsConfig,
            onOpenTasks = { activeSheet = DashboardSheet.Tasks },
            onOpenRelapse = { activeSheet = DashboardSheet.Relapse },
            onOpenActivitySettings = { activeSheet = DashboardSheet.Activities },
        )

        activeSheet?.let { sheet ->
            DashboardSheetHost(
                sheet = sheet,
                state = state,
                palette = palette,
                onDismiss = { activeSheet = null },
                onSwitchSheet = { activeSheet = it },
                onToggleActivity = onToggleActivity,
                onSaveActivityValue = onSaveActivityValue,
                onSaveSleep = onSaveSleep,
                onToggleRelapse = onToggleAbstinenceRelapse,
                onCreateActivity = onCreateActivity,
                onSetFocusSignal = onSetFocusSignal,
                onAddToChecklist = onAddToChecklist,
                onRemoveFromChecklist = onRemoveFromChecklist,
                onCreateTask = onCreateTask,
                onCompleteTask = onCompleteTask,
                onNavigateToChecklistConfig = onNavigateToChecklistConfig,
            )
        }
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

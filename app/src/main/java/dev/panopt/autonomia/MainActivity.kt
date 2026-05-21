package dev.panopt.autonomia

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import dev.panopt.autonomia.ui.dashboard.DashboardScreen
import dev.panopt.autonomia.ui.dashboard.dashboardPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val prefs = remember {
                getSharedPreferences("autonomia_prefs", Context.MODE_PRIVATE)
            }
            var isDarkMode by rememberSaveable {
                mutableStateOf(prefs.getBoolean("dark_mode", true))
            }
            val palette = dashboardPalette(isDarkMode)

            SideEffect {
                applySystemBars(isDarkMode = isDarkMode, bgColor = palette.bgBase.toArgb())
            }

            DashboardScreen(
                isDarkMode = isDarkMode,
                onThemeChange = { enabled ->
                    prefs.edit().putBoolean("dark_mode", enabled).apply()
                    isDarkMode = enabled
                },
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun applySystemBars(isDarkMode: Boolean, bgColor: Int) {
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        window.decorView.systemUiVisibility = if (isDarkMode) {
            window.decorView.systemUiVisibility and
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        } else {
            window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
}

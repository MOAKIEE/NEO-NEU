package edu.neu.campus.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

@Stable
enum class AppThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemeManager {
    var currentMode by mutableStateOf(AppThemeMode.SYSTEM)
        private set

    fun setMode(mode: AppThemeMode) {
        currentMode = mode
    }

    fun toColorSchemeMode(): ColorSchemeMode = when (currentMode) {
        AppThemeMode.SYSTEM -> ColorSchemeMode.System
        AppThemeMode.LIGHT -> ColorSchemeMode.Light
        AppThemeMode.DARK -> ColorSchemeMode.Dark
    }
}

@Composable
fun CampusTheme(
    mode: AppThemeMode = ThemeManager.currentMode,
    content: @Composable () -> Unit
) {
    val controller = ThemeController(
        colorSchemeMode = when (mode) {
            AppThemeMode.SYSTEM -> ColorSchemeMode.System
            AppThemeMode.LIGHT -> ColorSchemeMode.Light
            AppThemeMode.DARK -> ColorSchemeMode.Dark
        }
    )
    MiuixTheme(controller = controller) {
        content()
    }
}

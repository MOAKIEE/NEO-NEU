package edu.neu.campus.ui.theme

import android.content.Context
import android.content.SharedPreferences
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
    private const val PREFS_NAME = "neo_neu_theme"
    private const val KEY_MODE = "mode"
    private var prefs: SharedPreferences? = null

    var currentMode by mutableStateOf(AppThemeMode.SYSTEM)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val storage = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = storage
        currentMode = runCatching {
            AppThemeMode.valueOf(storage.getString(KEY_MODE, AppThemeMode.SYSTEM.name).orEmpty())
        }.getOrDefault(AppThemeMode.SYSTEM)
    }

    fun setMode(mode: AppThemeMode) {
        currentMode = mode
        prefs?.edit()?.putString(KEY_MODE, mode.name)?.apply()
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

package edu.neu.campus.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Stable
enum class AppThemeMode(val displayName: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色模式"),
    DARK("深色模式")
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

private val CampusLightMiuixColors = lightColorScheme(
    primary = Color(0xFF2458C6),
    onPrimary = Color.White,
    primaryVariant = Color(0xFF173A79),
    primaryContainer = Color(0xFFE8EFFF),
    onPrimaryContainer = Color(0xFF2458C6),
    background = Color(0xFFF4F6FA),
    onBackground = Color(0xFF182337),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF182337),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceSecondary = Color(0xFF56647A),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEAF0F7),
    outline = Color(0xFFD9E1EC),
    dividerLine = Color(0xFFD9E1EC),
    error = Color(0xFFB3263E),
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE)
)

private val CampusDarkMiuixColors = darkColorScheme(
    primary = Color(0xFFA9C4FF),
    onPrimary = Color(0xFF10141D),
    primaryVariant = Color(0xFF203F73),
    primaryContainer = Color(0xFF23375D),
    onPrimaryContainer = Color(0xFFA9C4FF),
    background = Color(0xFF10141D),
    onBackground = Color(0xFFEDF2FA),
    surface = Color(0xFF1B2230),
    onSurface = Color(0xFFEDF2FA),
    surfaceVariant = Color(0xFF1B2230),
    onSurfaceSecondary = Color(0xFFB0BDCF),
    surfaceContainer = Color(0xFF1B2230),
    surfaceContainerHigh = Color(0xFF252F40),
    outline = Color(0xFF3C4960),
    dividerLine = Color(0xFF3C4960),
    error = Color(0xFFFFACB8),
    onError = Color(0xFF10141D),
    errorContainer = Color(0xFF452C3A)
)

@Composable
fun CampusTheme(
    mode: AppThemeMode = ThemeManager.currentMode,
    content: @Composable () -> Unit
) {
    val isDark = when (mode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }
    val campusColors = if (isDark) DarkCampusColors else LightCampusColors

    val controller = ThemeController(
        colorSchemeMode = when (mode) {
            AppThemeMode.SYSTEM -> ColorSchemeMode.System
            AppThemeMode.LIGHT -> ColorSchemeMode.Light
            AppThemeMode.DARK -> ColorSchemeMode.Dark
        },
        lightColors = CampusLightMiuixColors,
        darkColors = CampusDarkMiuixColors,
        isDark = isDark
    )

    CompositionLocalProvider(LocalCampusColors provides campusColors) {
        MiuixTheme(controller = controller) {
            content()
        }
    }
}

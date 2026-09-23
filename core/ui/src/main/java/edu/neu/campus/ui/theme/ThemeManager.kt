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

/**
 * 外观偏好管理：持久化系统/浅色/深色选择，并统一提供给 Miuix 与项目设计令牌。
 */
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

/**
 * 浅色 Miuix 配色：完整覆盖语义槽位，避免上游默认蓝色与项目品牌色混杂。
 */
private val CampusLightMiuixColors = lightColorScheme(
    primary = LightCampusColors.brand,
    onPrimary = LightCampusColors.onBrand,
    primaryVariant = LightCampusColors.heroSurface,
    onPrimaryVariant = Color(0xFFC6D8FF),
    error = LightCampusColors.error,
    onError = Color.White,
    errorContainer = LightCampusColors.errorContainer,
    onErrorContainer = LightCampusColors.onErrorContainer,
    disabledPrimary = Color(0xFFC2D4F7),
    disabledOnPrimary = Color(0xFFF2F6FF),
    disabledPrimaryButton = Color(0xFFC2D4F7),
    disabledOnPrimaryButton = Color(0xFFFFFFFF),
    disabledPrimarySlider = Color(0xFFB8CBF2),
    primaryContainer = LightCampusColors.brandContainer,
    onPrimaryContainer = LightCampusColors.onBrandContainer,
    secondary = LightCampusColors.surfaceMuted,
    onSecondary = LightCampusColors.textPrimary,
    secondaryVariant = LightCampusColors.surfaceSunken,
    onSecondaryVariant = LightCampusColors.textPrimary,
    disabledSecondary = LightCampusColors.surfaceSunken,
    disabledOnSecondary = LightCampusColors.textDisabled,
    disabledSecondaryVariant = LightCampusColors.surfaceSunken,
    disabledOnSecondaryVariant = LightCampusColors.textDisabled,
    secondaryContainer = LightCampusColors.surfaceMuted,
    onSecondaryContainer = LightCampusColors.textSecondary,
    secondaryContainerVariant = LightCampusColors.surfaceSunken,
    onSecondaryContainerVariant = LightCampusColors.textTertiary,
    tertiaryContainer = LightCampusColors.brandContainer,
    onTertiaryContainer = LightCampusColors.brand,
    tertiaryContainerVariant = LightCampusColors.surfaceMuted,
    background = LightCampusColors.background,
    onBackground = LightCampusColors.textPrimary,
    onBackgroundVariant = LightCampusColors.textTertiary,
    surface = LightCampusColors.surface,
    onSurface = LightCampusColors.textPrimary,
    surfaceVariant = LightCampusColors.surface,
    onSurfaceSecondary = LightCampusColors.textSecondary,
    onSurfaceVariantSummary = LightCampusColors.textSecondary,
    onSurfaceVariantActions = LightCampusColors.textTertiary,
    disabledOnSurface = LightCampusColors.textDisabled,
    surfaceContainer = LightCampusColors.surface,
    onSurfaceContainer = LightCampusColors.textPrimary,
    onSurfaceContainerVariant = LightCampusColors.textSecondary,
    surfaceContainerHigh = LightCampusColors.surfaceMuted,
    onSurfaceContainerHigh = LightCampusColors.textTertiary,
    surfaceContainerHighest = LightCampusColors.surfaceSunken,
    onSurfaceContainerHighest = LightCampusColors.textPrimary,
    outline = LightCampusColors.outline,
    dividerLine = LightCampusColors.divider,
    windowDimming = LightCampusColors.scrim,
    sliderKeyPoint = Color(0x4D2A5FD6),
    sliderKeyPointForeground = LightCampusColors.brand,
    sliderBackground = Color(0x0F16233A)
)

/**
 * 深色 Miuix 配色：以分层深灰蓝表面替代纯黑，降低夜间对比疲劳。
 */
private val CampusDarkMiuixColors = darkColorScheme(
    primary = DarkCampusColors.brand,
    onPrimary = DarkCampusColors.onBrand,
    primaryVariant = DarkCampusColors.heroSurface,
    onPrimaryVariant = Color(0xFFD6E4FF),
    error = DarkCampusColors.error,
    onError = Color(0xFF3F0E14),
    errorContainer = DarkCampusColors.errorContainer,
    onErrorContainer = DarkCampusColors.onErrorContainer,
    disabledPrimary = Color(0xFF2B3D5F),
    disabledOnPrimary = Color(0xFF6C7C97),
    disabledPrimaryButton = Color(0xFF2B3D5F),
    disabledOnPrimaryButton = Color(0xFF6C7C97),
    disabledPrimarySlider = Color(0xFF44587C),
    primaryContainer = DarkCampusColors.brandContainer,
    onPrimaryContainer = DarkCampusColors.onBrandContainer,
    secondary = DarkCampusColors.surfaceMuted,
    onSecondary = DarkCampusColors.textPrimary,
    secondaryVariant = DarkCampusColors.surfaceElevated,
    onSecondaryVariant = DarkCampusColors.textPrimary,
    disabledSecondary = DarkCampusColors.surfaceMuted,
    disabledOnSecondary = DarkCampusColors.textDisabled,
    disabledSecondaryVariant = DarkCampusColors.surfaceMuted,
    disabledOnSecondaryVariant = DarkCampusColors.textDisabled,
    secondaryContainer = DarkCampusColors.surfaceMuted,
    onSecondaryContainer = DarkCampusColors.textSecondary,
    secondaryContainerVariant = DarkCampusColors.surfaceElevated,
    onSecondaryContainerVariant = DarkCampusColors.textTertiary,
    tertiaryContainer = DarkCampusColors.brandContainer,
    onTertiaryContainer = DarkCampusColors.brand,
    tertiaryContainerVariant = DarkCampusColors.surfaceMuted,
    background = DarkCampusColors.background,
    onBackground = DarkCampusColors.textPrimary,
    onBackgroundVariant = DarkCampusColors.textTertiary,
    surface = DarkCampusColors.surface,
    onSurface = DarkCampusColors.textPrimary,
    surfaceVariant = DarkCampusColors.surface,
    onSurfaceSecondary = DarkCampusColors.textSecondary,
    onSurfaceVariantSummary = DarkCampusColors.textSecondary,
    onSurfaceVariantActions = DarkCampusColors.textTertiary,
    disabledOnSurface = DarkCampusColors.textDisabled,
    surfaceContainer = DarkCampusColors.surface,
    onSurfaceContainer = DarkCampusColors.textPrimary,
    onSurfaceContainerVariant = DarkCampusColors.textSecondary,
    surfaceContainerHigh = DarkCampusColors.surfaceMuted,
    onSurfaceContainerHigh = DarkCampusColors.textTertiary,
    surfaceContainerHighest = DarkCampusColors.surfaceElevated,
    onSurfaceContainerHighest = DarkCampusColors.textPrimary,
    outline = DarkCampusColors.outline,
    dividerLine = DarkCampusColors.divider,
    windowDimming = DarkCampusColors.scrim,
    sliderKeyPoint = Color(0x4DA8C4FF),
    sliderKeyPointForeground = DarkCampusColors.brand,
    sliderBackground = Color(0x26FFFFFF)
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

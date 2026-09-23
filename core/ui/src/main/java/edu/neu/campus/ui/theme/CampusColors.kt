package edu.neu.campus.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * NEO NEU 统一设计令牌 (Design Tokens)。
 * 严格遵照 docs/07-UI视觉与布局重设计.md 第 2 节规范：
 * - 校园蓝品牌色与独立深色表面层级
 * - 功能辅色及稳定的六组课程映射色彩
 * - 语义化状态色彩与文本对比度保证
 */
@Stable
class CampusColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val brand: Color,
    val brandContainer: Color,
    val heroSurface: Color,
    val onHero: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    // 功能辅色 (Foreground / Container)
    val timetableForeground: Color,
    val timetableContainer: Color,
    val gradeForeground: Color,
    val gradeContainer: Color,
    val examForeground: Color,
    val examContainer: Color,
    val cardForeground: Color,
    val cardContainer: Color,
    val networkForeground: Color,
    val networkContainer: Color,
    val messageForeground: Color,
    val messageContainer: Color,
    val neutralForeground: Color,
    val neutralContainer: Color
) {
    val cardLight: Color get() = cardContainer
    val cardText: Color get() = cardForeground
    val networkLight: Color get() = networkContainer
    val networkText: Color get() = networkForeground
    val examLight: Color get() = examContainer
    val examText: Color get() = examForeground
    val gradeText: Color get() = gradeForeground
    val messageLight: Color get() = messageContainer
    val messageText: Color get() = messageForeground

    /**
     * 根据课程标题或组合标识，返回稳定的 (Foreground, Container) 颜色对。
     * 跨周保持一致，深浅模式自动适配，保证文字可读性与边框识别。
     */
    fun courseColor(courseKey: String): Pair<Color, Color> {
        val palette = listOf(
            timetableForeground to timetableContainer, // 蓝
            gradeForeground to gradeContainer,         // 紫
            examForeground to examContainer,           // 橙
            cardForeground to cardContainer,           // 绿
            networkForeground to networkContainer,     // 青
            messageForeground to messageContainer      // 玫瑰
        )
        val hash = (courseKey.hashCode() and 0x7fffffff)
        return palette[hash % palette.size]
    }
}

val LightCampusColors = CampusColors(
    isDark = false,
    background = Color(0xFFF4F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEAF0F7),
    textPrimary = Color(0xFF182337),
    textSecondary = Color(0xFF56647A),
    outline = Color(0xFFD9E1EC),
    brand = Color(0xFF2458C6),
    brandContainer = Color(0xFFE8EFFF),
    heroSurface = Color(0xFF173A79),
    onHero = Color(0xFFFFFFFF),
    success = Color(0xFF167252),
    warning = Color(0xFF85510D),
    error = Color(0xFFB3263E),

    timetableForeground = Color(0xFF2458C6),
    timetableContainer = Color(0xFFE8EFFF),
    gradeForeground = Color(0xFF6550A1),
    gradeContainer = Color(0xFFF0EBFA),
    examForeground = Color(0xFF985019),
    examContainer = Color(0xFFFFF0DF),
    cardForeground = Color(0xFF167252),
    cardContainer = Color(0xFFE4F5ED),
    networkForeground = Color(0xFF126F83),
    networkContainer = Color(0xFFE3F3F7),
    messageForeground = Color(0xFFA63F64),
    messageContainer = Color(0xFFFBE9F0),
    neutralForeground = Color(0xFF56647A),
    neutralContainer = Color(0xFFEAF0F7)
)

val DarkCampusColors = CampusColors(
    isDark = true,
    background = Color(0xFF10141D),
    surface = Color(0xFF1B2230),
    surfaceMuted = Color(0xFF252F40),
    textPrimary = Color(0xFFEDF2FA),
    textSecondary = Color(0xFFB0BDCF),
    outline = Color(0xFF3C4960),
    brand = Color(0xFFA9C4FF),
    brandContainer = Color(0xFF23375D),
    heroSurface = Color(0xFF203F73),
    onHero = Color(0xFFFFFFFF),
    success = Color(0xFF7ADAB5),
    warning = Color(0xFFF4C47B),
    error = Color(0xFFFFACB8),

    timetableForeground = Color(0xFFA9C4FF),
    timetableContainer = Color(0xFF23375D),
    gradeForeground = Color(0xFFCEBDF9),
    gradeContainer = Color(0xFF352B4B),
    examForeground = Color(0xFFF3C18E),
    examContainer = Color(0xFF453221),
    cardForeground = Color(0xFF91DBC0),
    cardContainer = Color(0xFF203D34),
    networkForeground = Color(0xFF93D5E4),
    networkContainer = Color(0xFF223C46),
    messageForeground = Color(0xFFEEADC5),
    messageContainer = Color(0xFF452C3A),
    neutralForeground = Color(0xFFB0BDCF),
    neutralContainer = Color(0xFF252F40)
)

val LocalCampusColors = staticCompositionLocalOf { LightCampusColors }

object CampusTheme {
    val colors: CampusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCampusColors.current
}

package edu.neu.campus.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * NEO NEU 统一设计令牌 (Design Tokens)。
 *
 * 设计约定：
 * - 品牌色保持校园蓝识别度，同时提高明度层次以适配浅色与深色表面
 * - 表面分为「底层背景 / 卡片 / 高亮卡片 / 凹陷容器」四级，避免纯黑纯白生硬对比
 * - 课程色板固定为八组稳定的 (前景, 容器) 色对，跨周、跨页面保持一致
 * - 语义色同时提供前景与容器，便于制作浅色状态卡
 * - 文本分为主 / 次 / 三级 / 禁用四档，保证层级与对比度
 */
@Stable
class CampusColors(
    val isDark: Boolean,

    // 表面层级
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val surfaceSunken: Color,

    // 文本层级
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textDisabled: Color,

    // 描边
    val outline: Color,
    val outlineVariant: Color,
    val divider: Color,

    // 品牌
    val brand: Color,
    val onBrand: Color,
    val brandContainer: Color,
    val onBrandContainer: Color,
    val brandBorder: Color,
    val brandPressed: Color,

    // 品牌渐变主视觉
    val heroStart: Color,
    val heroEnd: Color,
    val heroSurface: Color,
    val onHero: Color,
    val heroOverlay: Color,

    // 语义状态
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val error: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val info: Color,

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
    val neutralContainer: Color,

    // 蒙层
    val scrim: Color
) {
    // ---- 兼容旧字段名 ----
    val cardLight: Color get() = cardContainer
    val cardText: Color get() = cardForeground
    val networkLight: Color get() = networkContainer
    val networkText: Color get() = networkForeground
    val examLight: Color get() = examContainer
    val examText: Color get() = examForeground
    val gradeText: Color get() = gradeForeground
    val messageLight: Color get() = messageContainer
    val messageText: Color get() = messageForeground

    /** 品牌渐变，用于首页主课程卡与身份卡。 */
    val heroGradient: Brush
        get() = Brush.linearGradient(listOf(heroStart, heroEnd))

    /** 卡片默认阴影色，深色模式依赖表面层级而非阴影。 */
    val shadow: Color
        get() = if (isDark) Color.Transparent else Color(0x14243A6B)

    /** 八组稳定课程色对。 */
    private val coursePalette: List<Pair<Color, Color>>
        get() = listOf(
            timetableForeground to timetableContainer, // 蓝
            gradeForeground to gradeContainer,         // 紫
            examForeground to examContainer,           // 橙
            cardForeground to cardContainer,           // 绿
            networkForeground to networkContainer,     // 青
            messageForeground to messageContainer,     // 玫
            oliveForeground to oliveContainer,         // 橄榄
            brickForeground to brickContainer          // 栗
        )

    val oliveForeground: Color
        get() = if (isDark) Color(0xFFC6D89B) else Color(0xFF4F6B1F)
    val oliveContainer: Color
        get() = if (isDark) Color(0xFF333D20) else Color(0xFFEDF3DC)
    val brickForeground: Color
        get() = if (isDark) Color(0xFFF0B79C) else Color(0xFF93412A)
    val brickContainer: Color
        get() = if (isDark) Color(0xFF452A21) else Color(0xFFFCEAE2)

    /**
     * 根据课程标题或组合标识，返回稳定的 (Foreground, Container) 颜色对。
     * 跨周保持一致，深浅模式自动适配，保证文字可读性与边框识别。
     */
    fun courseColor(courseKey: String): Pair<Color, Color> {
        val palette = coursePalette
        val hash = (courseKey.hashCode() and 0x7fffffff)
        return palette[hash % palette.size]
    }

    /** 与课程色对应的强调色（用于左侧色条、顶部细线）。 */
    fun courseAccent(courseKey: String): Color = courseColor(courseKey).first
}

val LightCampusColors = CampusColors(
    isDark = false,

    background = Color(0xFFF5F6FA),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceMuted = Color(0xFFEFF2F8),
    surfaceSunken = Color(0xFFE8ECF4),

    textPrimary = Color(0xFF161D2E),
    textSecondary = Color(0xFF5A6579),
    textTertiary = Color(0xFF8B95A9),
    textDisabled = Color(0xFFB3BBCB),

    outline = Color(0xFFDDE3EE),
    outlineVariant = Color(0xFFE9EDF5),
    divider = Color(0xFFEDF0F7),

    brand = Color(0xFF2A5FD6),
    onBrand = Color(0xFFFFFFFF),
    brandContainer = Color(0xFFE8EFFF),
    onBrandContainer = Color(0xFF1C46A5),
    brandBorder = Color(0xFFC6D8FF),
    brandPressed = Color(0xFF1B4AB4),

    heroStart = Color(0xFF2E5FD8),
    heroEnd = Color(0xFF1B3E92),
    heroSurface = Color(0xFF1F4189),
    onHero = Color(0xFFFFFFFF),
    heroOverlay = Color(0x1FFFFFFF),

    success = Color(0xFF11795A),
    successContainer = Color(0xFFE2F4EC),
    onSuccessContainer = Color(0xFF0B5740),
    warning = Color(0xFF8A5309),
    warningContainer = Color(0xFFFDF0DA),
    onWarningContainer = Color(0xFF6B3F06),
    error = Color(0xFFB82433),
    errorContainer = Color(0xFFFCE8EB),
    onErrorContainer = Color(0xFF8C1A27),
    info = Color(0xFF2A5FD6),

    timetableForeground = Color(0xFF2A5FD6),
    timetableContainer = Color(0xFFE7EEFF),
    gradeForeground = Color(0xFF6A4CBB),
    gradeContainer = Color(0xFFEFE9FC),
    examForeground = Color(0xFFA05512),
    examContainer = Color(0xFFFFEFDB),
    cardForeground = Color(0xFF10785A),
    cardContainer = Color(0xFFE0F4EC),
    networkForeground = Color(0xFF0C6D88),
    networkContainer = Color(0xFFDDF1F8),
    messageForeground = Color(0xFFA93465),
    messageContainer = Color(0xFFFBE7F0),
    neutralForeground = Color(0xFF5A6579),
    neutralContainer = Color(0xFFEFF2F8),

    scrim = Color(0x66000000)
)

val DarkCampusColors = CampusColors(
    isDark = true,

    background = Color(0xFF0F131B),
    surface = Color(0xFF181E2A),
    surfaceElevated = Color(0xFF1F2634),
    surfaceMuted = Color(0xFF232B3A),
    surfaceSunken = Color(0xFF131822),

    textPrimary = Color(0xFFEFF3FB),
    textSecondary = Color(0xFFAAB5C8),
    textTertiary = Color(0xFF7E889C),
    textDisabled = Color(0xFF5C6678),

    outline = Color(0xFF39435A),
    outlineVariant = Color(0xFF2A3346),
    divider = Color(0xFF262E3E),

    brand = Color(0xFFA8C4FF),
    onBrand = Color(0xFF0D2246),
    brandContainer = Color(0xFF1E2F52),
    onBrandContainer = Color(0xFFC6D8FF),
    brandBorder = Color(0xFF33507F),
    brandPressed = Color(0xFFBFD4FF),

    heroStart = Color(0xFF2A4F9E),
    heroEnd = Color(0xFF172F63),
    heroSurface = Color(0xFF203F73),
    onHero = Color(0xFFFFFFFF),
    heroOverlay = Color(0x24FFFFFF),

    success = Color(0xFF6FD6AE),
    successContainer = Color(0xFF1B3A30),
    onSuccessContainer = Color(0xFF9BE6C7),
    warning = Color(0xFFF2C079),
    warningContainer = Color(0xFF3D2E17),
    onWarningContainer = Color(0xFFF7D9A6),
    error = Color(0xFFFFA9B5),
    errorContainer = Color(0xFF3F1E24),
    onErrorContainer = Color(0xFFFFC7CE),
    info = Color(0xFFA8C4FF),

    timetableForeground = Color(0xFFA8C4FF),
    timetableContainer = Color(0xFF1E2F52),
    gradeForeground = Color(0xFFCBBBF8),
    gradeContainer = Color(0xFF2E2743),
    examForeground = Color(0xFFF1BF8A),
    examContainer = Color(0xFF3B2C1B),
    cardForeground = Color(0xFF8CDCC0),
    cardContainer = Color(0xFF15332B),
    networkForeground = Color(0xFF8FD3E6),
    networkContainer = Color(0xFF12303A),
    messageForeground = Color(0xFFF0AAC5),
    messageContainer = Color(0xFF3A2230),
    neutralForeground = Color(0xFFAAB5C8),
    neutralContainer = Color(0xFF232B3A),

    scrim = Color(0x99000000)
)

val LocalCampusColors = staticCompositionLocalOf { LightCampusColors }

object CampusTheme {
    val colors: CampusColors
        @Composable
        @ReadOnlyComposable
        get() = LocalCampusColors.current
}

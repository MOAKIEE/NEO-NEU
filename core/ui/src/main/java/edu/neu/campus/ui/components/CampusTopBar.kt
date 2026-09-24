package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * 统一的 NEO NEU 标题栏组件。
 *
 * 约定：
 * - 背景与页面背景同色，避免状态栏区域与标题栏之间出现横向色带断层
 * - 传入 [scrollBehavior] 后大标题会随内容滚动收起为小标题（Miuix 折叠规范）
 * - 大标题左缘与正文统一落在 [CampusSpacing.screenHorizontal]；Miuix 默认 26dp 会比卡片多缩进 6dp
 * - 导航/操作图标按钮为 40dp 触控区、图标 24dp，两侧各留 8dp，因此外边距取 12dp，
 *   让图标的可见边缘同样落在 20dp 参考线上
 *
 * 接入折叠时三处必须成对出现，缺一会失效：
 * ```
 * val pageScrollBehavior = MiuixScrollBehavior()
 * Column(
 *     modifier = Modifier
 *         .fillMaxSize()
 *         .background(colors.background)
 *         .nestedScroll(pageScrollBehavior.nestedScrollConnection)
 * ) {
 *     CampusTopBar(title = "标题", scrollBehavior = pageScrollBehavior)
 *     // 正文中的 LazyColumn / verticalScroll 会驱动大标题收起
 * }
 * ```
 */
@Composable
fun CampusTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    scrollBehavior: ScrollBehavior? = null,
    // 父布局已应用系统栏边距时关闭，避免重复顶部留白。默认保留既有页面行为。
    defaultWindowInsetsPadding: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = CampusTheme.colors
    TopAppBar(
        modifier = modifier,
        title = title,
        color = colors.background,
        titleColor = colors.textPrimary,
        largeTitle = title,
        largeTitleColor = colors.textPrimary,
        subtitle = subtitle,
        subtitleColor = colors.textSecondary,
        defaultWindowInsetsPadding = defaultWindowInsetsPadding,
        titlePadding = CampusSpacing.screenHorizontal,
        navigationIconPadding = CampusSpacing.sm,
        actionIconPadding = CampusSpacing.sm,
        navigationIcon = if (onBack != null) {
            {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Back,
                        contentDescription = "返回",
                        tint = colors.textPrimary
                    )
                }
            }
        } else {
            {}
        },
        actions = actions ?: {},
        scrollBehavior = scrollBehavior
    )
}

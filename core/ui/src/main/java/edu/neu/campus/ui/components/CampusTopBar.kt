package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

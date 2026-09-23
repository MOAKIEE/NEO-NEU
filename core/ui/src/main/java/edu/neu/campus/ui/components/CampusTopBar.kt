package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

/**
 * 统一的 NEO NEU 标题栏组件。
 *
 * 遵循 Miuix 大标题规范：滚动时标题收起为小标题，返回按钮带按压缩放的触控反馈。
 */
@Composable
fun CampusTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    subtitle: String = "",
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = CampusTheme.colors
    TopAppBar(
        modifier = modifier,
        title = title,
        color = colors.surface,
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
        actions = actions ?: {}
    )
}

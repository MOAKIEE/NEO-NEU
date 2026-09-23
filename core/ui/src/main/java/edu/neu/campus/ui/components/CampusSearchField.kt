package edu.neu.campus.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Search

/** 图标与输入框边缘之间的留白，对齐 Miuix 搜索控件的视觉密度。 */
private val IconStartPadding = 14.dp
private val IconEndPadding = 6.dp

/**
 * 项目统一搜索输入框。
 *
 * 需要封装的两个 Miuix 细节：
 * 1. [TextField] 的 `leadingIcon` / `trailingIcon` 槽位自身没有内边距，直接放图标会紧贴输入框边缘。
 * 2. `useLabelAsPlaceholder = true` 时，Miuix 在「内容为空」状态下仍把占位文字画在输入起点，
 *    而光标也在同一位置，两者会重叠成一团。这里在「聚焦且为空」时清空 label，
 *    让占位文字在获得焦点后隐藏（与 Material 搜索框一致），只留下光标。
 */
@Composable
fun CampusSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "搜索",
    enabled: Boolean = true
) {
    val colors = CampusTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val hidePlaceholder = focused && value.isEmpty()

    TextField(
        value = value,
        onValueChange = onValueChange,
        label = if (hidePlaceholder) "" else placeholder,
        useLabelAsPlaceholder = true,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = MiuixIcons.Regular.Search,
                contentDescription = "搜索",
                tint = colors.textTertiary,
                modifier = Modifier.padding(start = IconStartPadding, end = IconEndPadding)
            )
        },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.padding(start = IconEndPadding, end = IconStartPadding)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Close,
                        contentDescription = "清除",
                        tint = colors.textSecondary
                    )
                }
            }
        }
    )
}

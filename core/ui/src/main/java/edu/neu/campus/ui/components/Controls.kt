package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.SwitchDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close

/** 按钮最小高度：比 Miuix 默认的 40dp 略高，满足触控尺寸且与列表行高协调。 */
private val ButtonMinHeight = 44.dp

/**
 * 项目统一文字按钮。
 *
 * Miuix [Button] 默认使用 17sp 标题字号与 16dp 圆角，放进卡片、横幅和弹窗时显得偏大、
 * 圆角也与卡片不一致。这里统一为 15sp 中等字重、[CampusShapes.small] 圆角、44dp 最小高度，
 * 并保留 Miuix 的按压反馈与禁用配色。主操作传 `primary = true`，次要操作使用默认的浅色底。
 */
@Composable
fun CampusButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        cornerRadius = CampusShapes.small,
        minHeight = ButtonMinHeight,
        colors = if (primary) ButtonDefaults.buttonColorsPrimary() else ButtonDefaults.buttonColors(),
        insideMargin = PaddingValues(horizontal = CampusSpacing.lg, vertical = CampusSpacing.xs)
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 底部抽屉标题栏的关闭按钮。
 *
 * 按 Miuix 抽屉的惯例放在标题左侧（`startAction`），用图标代替灰底「关闭」文字按钮，
 * 标题两侧因此不会一边空、一边挤着一个胶囊按钮。
 */
@Composable
fun CampusSheetCloseAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = MiuixIcons.Regular.Close,
            contentDescription = "关闭",
            tint = CampusTheme.colors.textPrimary
        )
    }
}

/**
 * 项目统一开关。
 *
 * 直接复用 Miuix [Switch]，保留其拖拽、回弹与触觉反馈动效，
 * 仅覆盖轨道与滑块颜色以匹配项目语义色，避免各处自行绘制开关。
 */
@Composable
fun CampusSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val switchColors = SwitchDefaults.switchColors(
        checkedThumbColor = colors.onBrand,
        // 深色模式下 surface/surfaceSunken 与卡片底色几乎相同，关闭态改用 outline 轨道 + 浅色滑块保证可辨认。
        uncheckedThumbColor = if (colors.isDark) colors.textSecondary else colors.surface,
        disabledCheckedThumbColor = colors.textDisabled,
        disabledUncheckedThumbColor = colors.textDisabled,
        checkedTrackColor = colors.brand,
        uncheckedTrackColor = colors.outline,
        disabledCheckedTrackColor = colors.brandContainer,
        disabledUncheckedTrackColor = colors.surfaceMuted
    )
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = switchColors,
        enabled = onCheckedChange != null
    )
}

/**
 * 项目统一复选框。
 *
 * 复用 Miuix [Checkbox] 的勾选过渡与触觉反馈，颜色对齐品牌语义色。
 */
@Composable
fun CampusCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val checkboxColors = CheckboxDefaults.checkboxColors(
        checkedForegroundColor = colors.onBrand,
        uncheckedForegroundColor = colors.surface,
        disabledCheckedForegroundColor = colors.textDisabled,
        disabledUncheckedForegroundColor = colors.textDisabled,
        checkedBackgroundColor = colors.brand,
        uncheckedBackgroundColor = colors.outline,
        disabledCheckedBackgroundColor = colors.brandContainer,
        disabledUncheckedBackgroundColor = colors.surfaceMuted
    )
    Checkbox(
        state = if (checked) ToggleableState.On else ToggleableState.Off,
        onClick = onCheckedChange?.let { callback -> { callback(!checked) } },
        modifier = modifier,
        colors = checkboxColors,
        enabled = onCheckedChange != null
    )
}

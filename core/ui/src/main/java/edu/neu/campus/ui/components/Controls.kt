package edu.neu.campus.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.state.ToggleableState
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.CheckboxDefaults
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.SwitchDefaults

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
        uncheckedThumbColor = colors.surface,
        disabledCheckedThumbColor = colors.textDisabled,
        disabledUncheckedThumbColor = colors.textDisabled,
        checkedTrackColor = colors.brand,
        uncheckedTrackColor = colors.surfaceSunken,
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
        uncheckedBackgroundColor = colors.surfaceSunken,
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

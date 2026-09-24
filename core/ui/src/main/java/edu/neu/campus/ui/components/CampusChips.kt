package edu.neu.campus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowUpDown

/**
 * 顶部筛选胶囊 (CampusFilterChip)。
 *
 * 用于学期、校区、排序等单一维度的切换；[active] 为真时使用品牌浅底与描边强调当前已被改动。
 */
@Composable
fun CampusFilterChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    trailingText: String? = null
) {
    val colors = CampusTheme.colors
    Row(
        modifier = modifier
            .tapScale(onClick = onClick, pressedScale = 0.95f, clipShape = RoundedCornerShape(CampusShapes.pill))
            .background(if (active) colors.brandContainer else colors.surface)
            .border(
                width = 1.dp,
                color = if (active) colors.brandBorder else colors.outlineVariant,
                shape = RoundedCornerShape(CampusShapes.pill)
            )
            .padding(horizontal = CampusSpacing.sm + 2.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (active) colors.brand else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailingText != null) {
            Text(
                text = trailingText,
                fontSize = 13.sp,
                color = colors.brand
            )
        }
        CampusDropdownArrow(tint = if (active) colors.brand else colors.textTertiary)
    }
}

/**
 * 下拉指示箭头。
 *
 * 沿用 Miuix 下拉菜单的上下箭头字形（Miuix 的 `ExpandMore` 是「展开全屏」的四角图标，不能当下拉箭头），
 * 按 13sp 胶囊文字把 Miuix 默认 10×16dp 等比缩小。
 */
@Composable
fun CampusDropdownArrow(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = MiuixIcons.Basic.ArrowUpDown,
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(width = 8.dp, height = 13.dp)
    )
}

/**
 * 列表或模块为空时的居中单行提示：抽屉选择列表为空时避免只剩标题栏，
 * 首页模块的数据由别处（如主卡）说明错误原因时，用它占位而不重复整张状态面板。
 */
@Composable
fun CampusEmptyHint(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = CampusSpacing.xl),
        fontSize = 14.sp,
        color = CampusTheme.colors.textSecondary,
        textAlign = TextAlign.Center
    )
}

/**
 * 底部抽屉内的选择行 (CampusSelectionRow)。
 *
 * 选中项使用品牌浅底 + 描边，未选中使用普通表面，避免大面积白底堆叠。
 */
@Composable
fun CampusSelectionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    val colors = CampusTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tapScale(onClick = onClick, pressedScale = 0.98f, clipShape = RoundedCornerShape(CampusShapes.medium))
            .background(if (selected) colors.brandContainer else colors.surface)
            .border(
                width = 1.dp,
                color = if (selected) colors.brandBorder else colors.outlineVariant,
                shape = RoundedCornerShape(CampusShapes.medium)
            )
            .padding(CampusSpacing.sm + 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.brand else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 12.sp,
                color = if (selected) colors.brand else colors.textSecondary
            )
        }
    }
}

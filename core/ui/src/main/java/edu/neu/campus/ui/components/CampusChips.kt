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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ExpandMore

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
            .clip(RoundedCornerShape(CampusShapes.pill))
            .background(if (active) colors.brandContainer else colors.surface)
            .border(
                width = 1.dp,
                color = if (active) colors.brandBorder else colors.outlineVariant,
                shape = RoundedCornerShape(CampusShapes.pill)
            )
            .tapScale(onClick = onClick, pressedScale = 0.95f)
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
        Icon(
            imageVector = MiuixIcons.Regular.ExpandMore,
            contentDescription = null,
            tint = if (active) colors.brand else colors.textTertiary,
            modifier = Modifier.size(15.dp)
        )
    }
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
            .clip(RoundedCornerShape(CampusShapes.medium))
            .background(if (selected) colors.brandContainer else colors.surface)
            .border(
                width = 1.dp,
                color = if (selected) colors.brandBorder else colors.outlineVariant,
                shape = RoundedCornerShape(CampusShapes.medium)
            )
            .tapScale(onClick = onClick, pressedScale = 0.98f)
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

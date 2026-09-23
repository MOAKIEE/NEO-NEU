package edu.neu.campus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/**
 * NEO NEU 标准卡片 (CampusCard)。
 *
 * 基于 Miuix [Card] 的方圆形表面；传入 [onClick] 时启用 Miuix 的下沉按压反馈，
 * 让点击有明确的物理触感。所有页面卡片都应通过本组件构造，避免各自硬编码圆角与配色。
 */
@Composable
fun CampusCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    cornerRadius: Dp = CampusShapes.large,
    containerColor: Color = CampusTheme.colors.surface,
    contentColor: Color = CampusTheme.colors.textPrimary,
    contentPadding: PaddingValues = PaddingValues(CampusSpacing.md),
    showIndication: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = cornerRadius,
        insideMargin = contentPadding,
        colors = CardDefaults.defaultColors(color = containerColor, contentColor = contentColor),
        pressFeedbackType = if (onClick != null) PressFeedbackType.Sink else PressFeedbackType.None,
        showIndication = showIndication && onClick != null,
        onClick = onClick,
        content = content
    )
}

/**
 * 功能色方形图标底座，统一首页快捷入口、查询工具面板与列表项的视觉。
 */
@Composable
fun CampusIconBadge(
    icon: ImageVector,
    tint: Color,
    container: Color,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    cornerRadius: Dp = CampusShapes.small
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/** 小尺寸状态标签，用于「已排考」「官方网页」等元信息。 */
@Composable
fun CampusPill(
    text: String,
    modifier: Modifier = Modifier,
    contentColor: Color = CampusTheme.colors.textSecondary,
    containerColor: Color = CampusTheme.colors.surfaceMuted
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(CampusShapes.extraSmall - 4.dp))
            .background(containerColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

/** 数值统计块，用于成绩/余额等摘要卡。 */
@Composable
fun CampusStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = CampusTheme.colors.textPrimary
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = CampusTheme.colors.textSecondary
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

/** 带左侧强调色条的列表项容器，用于课表列表与冲突课程选择。 */
@Composable
fun CampusAccentRow(
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .tapScale(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 4.dp, height = 36.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        content()
    }
}

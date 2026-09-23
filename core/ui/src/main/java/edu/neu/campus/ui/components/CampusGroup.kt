package edu.neu.campus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text

/**
 * NEO NEU 统一分组容器 (CampusGroup)。
 *
 * 约定：
 * - 承载列表与字段信息，避免列表项反复套小卡
 * - 默认大圆角表面 + 1px 细描边，内部由调用方插入 [CampusGroupDivider] 分隔
 * - 传入 [onClick] 时整体可点击并带按压缩放
 */
@Composable
fun CampusGroup(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CampusShapes.large,
    containerColor: Color = CampusTheme.colors.surface,
    contentPadding: Dp = CampusSpacing.md,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .tapScale(onClick = onClick, pressedScale = 0.985f, clipShape = RoundedCornerShape(cornerRadius))
            .background(containerColor)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(cornerRadius))
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * 带有外部标题和右侧操作按钮的标准区块包装。
 *
 * 标题位于卡片外部，保持 Miuix 的「小标题 + 分组」信息层级。
 */
@Composable
fun CampusSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = CampusTheme.colors
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs + 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (actionText != null && onActionClick != null) {
                Box(
                    modifier = Modifier
                        .tapScale(onClick = onActionClick, pressedScale = 0.94f, clipShape = RoundedCornerShape(CampusShapes.pill))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = actionText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.brand
                    )
                }
            }
        }
        content()
    }
}

/**
 * 分组内的项目分割线，默认向左侧文字起点缩进。
 */
@Composable
fun CampusGroupDivider(
    modifier: Modifier = Modifier,
    startIndent: Dp = 0.dp
) {
    val colors = CampusTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(1.dp)
            .background(colors.divider)
    )
}

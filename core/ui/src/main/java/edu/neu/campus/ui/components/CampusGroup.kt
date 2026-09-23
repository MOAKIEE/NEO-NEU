package edu.neu.campus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text

/**
 * NEO NEU B 类 Surface 统一分组容器 (CampusGroup)。
 * 严格遵照 docs/07-UI视觉与布局重设计.md 第 2.3 节规范：
 * - 承载列表与字段信息，避免列表项反复套小卡
 * - 20dp 圆角，背景为 Surface，标题在卡片外部独立呈现
 * - 内部列表项之间支持平滑的 1dp 分隔线与缩进
 */
@Composable
fun CampusGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = CampusTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            content = content
        )
    }
}

/**
 * 带有外部标题和右侧操作按钮的标准区块包装。
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
        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
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
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(onClick = onActionClick)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = actionText,
                        fontSize = 13.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
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
    startIndent: androidx.compose.ui.unit.Dp = 0.dp
) {
    val colors = CampusTheme.colors
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = startIndent)
            .height(1.dp)
            .background(colors.outline.copy(alpha = 0.6f))
    )
}

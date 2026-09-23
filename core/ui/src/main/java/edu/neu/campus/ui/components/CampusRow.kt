package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight

/**
 * 通用列表项 (CampusRow)。
 *
 * 用于设置项、搜索结果、服务目录等「左图标 + 主副标题 + 右侧内容」结构。
 * 传入 [onClick] 时整行可点击并按压缩放，右箭头随之轻微位移。
 */
@Composable
fun CampusRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    trailingColor: Color = CampusTheme.colors.textSecondary,
    trailingContent: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val colors = CampusTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .tapScale(onClick = onClick, enabled = enabled, pressedScale = 0.985f)
            .padding(vertical = CampusSpacing.xs + 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
    ) {
        if (leading != null) {
            leading()
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) colors.textPrimary else colors.textDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (trailingText != null) {
            Text(
                text = trailingText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = trailingColor,
                maxLines = 1
            )
        }

        if (trailingContent != null) {
            trailingContent()
        }

        if (showChevron) {
            Icon(
                imageVector = MiuixIcons.Basic.ArrowRight,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * 带角标的小标题行，用于分组内部的次级信息。
 */
@Composable
fun CampusRowDividerText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = CampusTheme.colors.textSecondary,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = CampusSpacing.xxs)
    )
}

package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardColors
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text

/**
 * 通用信息卡 (QueryCard)。
 *
 * 保留 Miuix [Card] 的方圆形涂层，统一标题层级、内外间距与右侧动作样式；
 * 传入 [onClick] 时整卡可点击，按压有下沉反馈。
 */
@Composable
fun QueryCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    colors: CardColors = CardDefaults.defaultColors(),
    insideMargin: PaddingValues = PaddingValues(CampusSpacing.md),
    content: @Composable ColumnScope.() -> Unit
) {
    val campusColors = CampusTheme.colors
    Card(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = CampusShapes.large,
        insideMargin = insideMargin,
        colors = colors,
        onClick = onClick,
        content = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (title != null || actionText != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = CampusSpacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title.orEmpty(),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = campusColors.textPrimary
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = campusColors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                        if (actionText != null && onActionClick != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(CampusShapes.pill))
                                    .tapScale(onClick = onActionClick, pressedScale = 0.94f)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = actionText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = campusColors.brand
                                )
                            }
                        }
                    }
                }
                content()
            }
        }
    )
}

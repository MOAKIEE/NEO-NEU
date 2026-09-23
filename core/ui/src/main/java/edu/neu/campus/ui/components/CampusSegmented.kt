package edu.neu.campus.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text
import java.util.Locale

/**
 * 分段选择控件 (CampusSegmentedControl)。
 *
 * 相对 Miuix TabRow 更紧凑，用于设置页的外观模式、筛选等 2—4 项互斥选择。
 * 选中块使用弹簧位移动画，文字颜色同步渐变。
 */
@Composable
fun CampusSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 42.dp
) {
    if (options.isEmpty()) return
    val colors = CampusTheme.colors
    val count = options.size
    val safeIndex = selectedIndex.coerceIn(0, count - 1)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(CampusShapes.small))
            .background(colors.surfaceMuted)
            .padding(4.dp)
    ) {
        val itemWidth = maxWidth / count
        val targetOffset by animateDpAsState(
            targetValue = itemWidth * safeIndex,
            animationSpec = CampusMotion.springSmooth(),
            label = "segmentOffset"
        )

        Box(
            modifier = Modifier
                .offset(x = targetOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(CampusShapes.extraSmall - 2.dp))
                .background(colors.surface)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            options.forEachIndexed { index, label ->
                val selected = index == safeIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .tapScale(onClick = { onSelect(index) }, pressedScale = 0.94f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) colors.brand else colors.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * 数值滚动文本 (AnimatedNumber)。
 *
 * 用于余额等需要「数字从旧值滚到新值」的场景；[target] 为 null 时展示 [fallback]，
 * 不进行动画，避免把未同步渲染成 0。
 */
@Composable
fun AnimatedNumber(
    target: Float?,
    modifier: Modifier = Modifier,
    fallback: String = "未同步",
    decimals: Int = 2,
    prefix: String = "",
    suffix: String = "",
    fontSize: TextUnit = 22.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    color: Color = CampusTheme.colors.textPrimary
) {
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(target) {
        if (target == null) {
            animatable.snapTo(0f)
        } else {
            animatable.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 780, easing = CampusMotion.Easing.smooth)
            )
        }
    }

    val text = when {
        target == null -> fallback
        decimals <= 0 -> prefix + String.format(Locale.US, "%.0f", animatable.value) + suffix
        else -> prefix + String.format(Locale.US, "%.${decimals}f", animatable.value) + suffix
    }

    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color
    )
}

package edu.neu.campus.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.delay

/**
 * 可点击并按压缩放的通用外壳。
 *
 * 行为约定：
 * - 按下时轻微缩小（默认 0.97），松开使用回弹曲线复位
 * - 指示效果沿用 Miuix 提供的 [LocalIndication]，不额外叠加 Material 涟漪
 */
@Composable
fun Modifier.tapScale(
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = CampusMotion.springBouncy(),
        label = "tapScale"
    )
    val indication = LocalIndication.current
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = indication,
                    enabled = enabled,
                    onClick = onClick
                )
            } else {
                Modifier
            }
        )
}

/**
 * 手指按下的整体缩放包装（内容自绘，适合整块卡片）。
 */
@Composable
fun PressScaleBox(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    pressedScale: Float = 0.97f,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier.tapScale(onClick = onClick, enabled = enabled, pressedScale = pressedScale), content = content)
}

/**
 * 交错淡入上移出场效果。
 *
 * 用于列表首屏挂载时逐项出现；索引越大延迟越久，但设有上限避免尾部等待过长。
 *
 * @param index 项目在列表中的序号，用于计算延迟
 * @param key 数据标识变化时重新播放动画
 */
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    key: Any? = Unit,
    offsetY: Dp = 14.dp,
    content: @Composable () -> Unit
) {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        delay(CampusMotion.staggerDelay(index).toLong())
        visible = true
    }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = CampusMotion.enter(),
        label = "staggeredAppear"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * offsetY.toPx()
        }
    ) {
        content()
    }
}

/**
 * 二级页面统一入场：挂载时轻微上移淡入，让「进入新页面」有方向感。
 *
 * 仅作用于首次进入，不干扰页面内部的滚动与列表动画。
 */
@Composable
fun CampusPageEnter(
    modifier: Modifier = Modifier,
    key: Any? = Unit,
    offsetY: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    var visible by remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) { visible = true }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = CampusMotion.enter(),
        label = "pageEnter"
    )
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress
            translationY = (1f - progress) * offsetY.toPx()
        }
    ) {
        content()
    }
}

/**
 * 骨架屏微光画刷，用于加载占位。
 */
@Composable
fun rememberShimmerBrush(): Brush {
    val colors = CampusTheme.colors
    val base = colors.surfaceMuted
    val highlight = colors.surfaceSunken.copy(alpha = 0.9f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(progress * 900f - 500f, 0f),
        end = Offset(progress * 900f, 260f)
    )
}

/**
 * 单块骨架占位。
 */
@Composable
fun ShimmerLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    cornerRadius: Dp = 8.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(rememberShimmerBrush())
    )
}

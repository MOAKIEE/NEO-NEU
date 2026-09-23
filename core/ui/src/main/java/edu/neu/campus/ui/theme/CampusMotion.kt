package edu.neu.campus.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Easing as ComposeEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 全局动效令牌。
 *
 * 约定：
 * - 位移与淡入统一使用 [CampusEasing.standard]，进出场使用强调曲线
 * - 触达类反馈（按压、勾选）使用轻微回弹 [springBouncy]
 * - 时长控制在 120—420ms，避免长动画拖慢高频操作
 */
object CampusMotion {

    object Duration {
        /** 瞬时反馈：颜色、透明度。 */
        const val instant = 120
        /** 短动画：按压、指示器。 */
        const val short = 180
        /** 标准动画：卡片入场、Tab 切换。 */
        const val medium = 260
        /** 强调动画：页面转场、Hero 展开。 */
        const val long = 360
        /** 布局尺寸动画。 */
        const val layout = 300
    }

    object Easing {
        /** 标准缓动，可用于绝大多数属性动画。 */
        val standard: ComposeEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

        /** 强调减速：元素进入屏幕。 */
        val emphasizedDecelerate: ComposeEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

        /** 强调加速：元素离开屏幕。 */
        val emphasizedAccelerate: ComposeEasing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

        /** 平滑双向。 */
        val smooth: ComposeEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    }

    /** 默认补间动画：标准缓动 + 标准时长。 */
    fun <T> standard(
        durationMillis: Int = Duration.medium
    ): FiniteAnimationSpec<T> = tween(durationMillis = durationMillis, easing = Easing.standard)

    /** 进场补间：减速曲线，略长。 */
    fun <T> enter(
        durationMillis: Int = Duration.long
    ): FiniteAnimationSpec<T> = tween(durationMillis = durationMillis, easing = Easing.emphasizedDecelerate)

    /** 退场补间：加速曲线，略短。 */
    fun <T> exit(
        durationMillis: Int = Duration.short
    ): FiniteAnimationSpec<T> = tween(durationMillis = durationMillis, easing = Easing.emphasizedAccelerate)

    /** 触达回弹：用于按压恢复与勾选。 */
    fun <T> springBouncy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** 沉稳弹簧：用于尺寸与位置收敛。 */
    fun <T> springSmooth(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** 交错入场的单项延迟，按索引递增但设有上限。 */
    fun staggerDelay(index: Int, stepMillis: Int = 40, maxMillis: Int = 240): Int =
        (index * stepMillis).coerceAtMost(maxMillis)
}

/** 统一圆角令牌。 */
object CampusShapes {
    // 圆角尺寸
    val extraSmall: Dp = 10.dp
    val small: Dp = 14.dp
    val medium: Dp = 18.dp
    val large: Dp = 22.dp
    val extraLarge: Dp = 28.dp
    val pill: Dp = 999.dp

    // 常用形状
    val extraSmallShape = RoundedCornerShape(extraSmall)
    val smallShape = RoundedCornerShape(small)
    val mediumShape = RoundedCornerShape(medium)
    val largeShape = RoundedCornerShape(large)
    val extraLargeShape = RoundedCornerShape(extraLarge)
    val pillShape = RoundedCornerShape(pill)
}

/** 统一间距令牌，避免页面各自写魔法数字。 */
object CampusSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    /** 页面左右安全留白。 */
    val screenHorizontal: Dp = 20.dp

    /** 页面顶部留白。 */
    val screenTop: Dp = 16.dp

    /** 滚动内容底部补白，避免被底栏遮挡。 */
    val screenBottom: Dp = 28.dp
}

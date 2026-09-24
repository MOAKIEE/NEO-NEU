package edu.neu.campus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.contract.QueryError
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

private enum class HeroState { Error, Loading, TermPending, NoData, Schedule }

/**
 * 首页主课程卡 (HeroCourseCard)。
 *
 * 设计约定：
 * - 品牌渐变主视觉 + 低透明度装饰线条，构成首页唯一的强视觉焦点
 * - 状态之间使用上滑淡入转场；正在上课时展示呼吸圆点
 * - 整卡可按压反馈，多门冲突时引导进入课程列表
 */
@Composable
fun HeroCourseCard(
    courses: List<CourseOccurrence>,
    nowTimeStr: String,
    isLoading: Boolean,
    error: QueryError?,
    isStale: Boolean = false,
    hasConfirmedTerm: Boolean = true,
    hasData: Boolean = false,
    onClickCourse: (CourseOccurrence) -> Unit,
    onConflictClick: (List<CourseOccurrence>) -> Unit,
    onGotoTimetable: () -> Unit,
    // 为 null 时不显示重试/同步按钮：例如登录失效时重试必然失败，登录入口由页面横幅提供。
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val focus = courseFocus(courses, nowTimeStr, !isStale)

    val state = when {
        !hasData && error != null -> HeroState.Error
        isLoading && !hasData -> HeroState.Loading
        !hasConfirmedTerm -> HeroState.TermPending
        !hasData -> HeroState.NoData
        else -> HeroState.Schedule
    }

    val isLive = state == HeroState.Schedule && focus.label == "正在上课" && focus.courses.isNotEmpty()

    // 状态标签不能沿用课程焦点文案：同步失败时课程焦点为空，会误报「今天的课程已结束」。
    val statusLabel = when (state) {
        HeroState.Error -> "课程同步失败"
        HeroState.Loading -> "正在同步"
        HeroState.TermPending -> "教学周待确认"
        HeroState.NoData -> "尚未同步"
        HeroState.Schedule -> focus.label
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 176.dp)
            .clip(RoundedCornerShape(CampusShapes.extraLarge))
            .background(colors.heroGradient)
    ) {
        HeroDecoration()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CampusSpacing.lg, vertical = CampusSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
        ) {
            // 顶部：状态标签 + 当前时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isLive) {
                        PulsingDot(color = Color(0xFF7BE0A8))
                    }
                    Text(
                        text = statusLabel,
                        color = colors.onHero,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = nowTimeStr,
                    color = colors.onHero.copy(alpha = 0.72f),
                    fontSize = 13.sp
                )
            }

            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    (fadeIn(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)) +
                        slideInVertically(
                            animationSpec = tween(
                                CampusMotion.Duration.long,
                                easing = CampusMotion.Easing.emphasizedDecelerate
                            )
                        ) { full -> full / 6 })
                        .togetherWith(fadeOut(tween(CampusMotion.Duration.instant)))
                },
                label = "heroState"
            ) { current ->
                when (current) {
                    HeroState.Error -> HeroMessage(
                        title = "课程暂不可用",
                        description = error?.message.orEmpty(),
                        primaryText = "重试",
                        onPrimary = onRetry
                    )

                    HeroState.Loading -> HeroMessage(
                        title = "正在同步今日课程…",
                        description = "首次加载需要片刻，已缓存内容会优先展示",
                        primaryText = null,
                        onPrimary = null
                    )

                    // 顶部状态标签已写明「教学周待确认」，标题改说结果，避免同一张卡上重复两次。
                    HeroState.TermPending -> HeroMessage(
                        title = "今日课程暂无法确定",
                        description = "学校尚未返回当前教学周，暂不推断今日课程",
                        primaryText = "查看课表",
                        onPrimary = onGotoTimetable
                    )

                    HeroState.NoData -> HeroMessage(
                        title = "尚未获取今日课程",
                        description = "点击同步读取本学期课表",
                        primaryText = "同步课程",
                        onPrimary = onRetry
                    )

                    HeroState.Schedule -> HeroScheduleContent(
                        focus = focus,
                        isStale = isStale,
                        error = error,
                        onClickCourse = onClickCourse,
                        onConflictClick = onConflictClick,
                        onGotoTimetable = onGotoTimetable,
                        onRetry = onRetry
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroScheduleContent(
    focus: CourseFocus,
    isStale: Boolean,
    error: QueryError?,
    onClickCourse: (CourseOccurrence) -> Unit,
    onConflictClick: (List<CourseOccurrence>) -> Unit,
    onGotoTimetable: () -> Unit,
    onRetry: (() -> Unit)?
) {
    val colors = CampusTheme.colors
    val course = focus.courses.firstOrNull()

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (course == null) {
            Text(
                text = "今天没有已安排课程",
                color = colors.onHero,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "可以提前查看本周课表安排",
                color = colors.onHero.copy(alpha = 0.75f),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            HeroButton(text = "查看本周课表", primary = true, onClick = onGotoTimetable)
        } else {
            Text(
                text = if (focus.conflict) "同时有 ${focus.courses.size} 项安排" else course.title,
                color = colors.onHero,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (hasCourseTime(course)) {
                    "${course.beginTime} — ${course.endTime} · 第 ${course.beginSection}—${course.endSection} 节"
                } else {
                    "第 ${course.beginSection}—${course.endSection} 节 · 时间未提供"
                },
                color = colors.onHero.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
            Text(
                text = course.place ?: "地点未提供",
                color = colors.onHero.copy(alpha = 0.72f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroButton(
                    text = if (focus.courses.size > 1) "查看课程列表" else "查看课程详情",
                    primary = true,
                    onClick = {
                        if (focus.courses.size > 1) onConflictClick(focus.courses) else onClickCourse(course)
                    }
                )
                HeroButton(text = "本周课表", primary = false, onClick = onGotoTimetable)
            }

            if (isStale) {
                Text(
                    text = "上次同步课程，可能已变化",
                    color = colors.onHero.copy(alpha = 0.68f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (error != null) {
                Text(
                    text = error.message,
                    color = colors.onHero.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
                if (onRetry != null) {
                    HeroButton(text = "重试", primary = false, onClick = onRetry)
                }
            }
        }
    }
}

@Composable
private fun HeroMessage(
    title: String,
    description: String,
    primaryText: String?,
    onPrimary: (() -> Unit)?
) {
    val colors = CampusTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            color = colors.onHero,
            fontSize = 22.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (description.isNotBlank()) {
            Text(
                text = description,
                color = colors.onHero.copy(alpha = 0.75f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (primaryText != null && onPrimary != null) {
            Spacer(modifier = Modifier.height(4.dp))
            HeroButton(text = primaryText, primary = true, onClick = onPrimary)
        }
    }
}

@Composable
private fun HeroButton(
    text: String,
    primary: Boolean,
    onClick: () -> Unit
) {
    val colors = CampusTheme.colors
    if (primary) {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                color = colors.onHero,
                contentColor = colors.heroEnd
            )
        ) {
            Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                color = colors.heroOverlay,
                contentColor = colors.onHero
            )
        ) {
            Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** 呼吸圆点：正在上课的实时指示。 */
@Composable
private fun PulsingDot(color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = CampusMotion.Easing.smooth),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = 1.35f - scale * 0.4f
            }
            .clip(CircleShape)
            .background(color)
    )
}

/** 主视觉装饰：低透明度折线与同心圆，随卡片一起被裁剪。 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.HeroDecoration() {
    val colors = CampusTheme.colors
    Canvas(
        modifier = Modifier
            .size(190.dp)
            .align(Alignment.TopEnd)
            .offset(x = 28.dp, y = (-24).dp)
    ) {
        val stroke = colors.onHero.copy(alpha = 0.10f)
        val path = Path().apply {
            moveTo(size.width * 0.34f, size.height * 0.92f)
            lineTo(size.width * 0.34f, size.height * 0.12f)
            lineTo(size.width * 0.92f, size.height * 0.92f)
            lineTo(size.width * 0.92f, size.height * 0.12f)
        }
        drawPath(path, stroke, style = Stroke(2.5.dp.toPx()))

        drawCircle(
            color = colors.onHero.copy(alpha = 0.06f),
            radius = size.minDimension * 0.46f,
            center = Offset(size.width * 0.86f, size.height * 0.16f),
            style = Stroke(2.dp.toPx())
        )
        drawCircle(
            color = colors.onHero.copy(alpha = 0.05f),
            radius = size.minDimension * 0.30f,
            center = Offset(size.width * 0.12f, size.height * 0.95f),
            style = Stroke(2.dp.toPx())
        )
    }
}

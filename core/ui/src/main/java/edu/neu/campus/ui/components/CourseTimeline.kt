package edu.neu.campus.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text

/**
 * 今日课程时间轴组件 (CourseTimeline)。
 *
 * 组件约定：
 * - 集中放置在一个表面分组中，替代重复卡片嵌套
 * - 时间列约 54dp、标记列 18dp，其余空间承载课程信息
 * - 标记点区分状态：已结束 (空心)、进行中 (放大实心并呼吸)、待上课 (实心)
 * - 行随首屏逐条淡入，正在上课的行使用品牌浅底强调
 * - 默认最多展示 4 项，超过时展示「查看全部 N 项」
 */
@Composable
fun CourseTimeline(
    courses: List<CourseOccurrence>,
    nowTimeStr: String,
    trusted: Boolean = true,
    onCourseClick: (CourseOccurrence) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CampusShapes.large))
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(CampusShapes.large))
            .padding(CampusSpacing.md)
    ) {
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = CampusSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "今天没有已安排课程",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        } else {
            val displayCourses = courses.take(4)
            Column(modifier = Modifier.fillMaxWidth()) {
                displayCourses.forEachIndexed { index, course ->
                    val isLast = index == displayCourses.lastIndex
                    val statusText = courseStatus(course, nowTimeStr, trusted)
                    val isEnded = statusText == "已结束"
                    val isOngoing = statusText == "正在上课"

                    val statusColor by animateColorAsState(
                        targetValue = when {
                            isOngoing -> colors.success
                            isEnded -> colors.textTertiary
                            else -> colors.brand
                        },
                        animationSpec = CampusMotion.standard(),
                        label = "timelineStatusColor"
                    )

                    val rowBg by animateColorAsState(
                        targetValue = if (isOngoing) colors.brandContainer.copy(alpha = 0.55f) else Color.Transparent,
                        animationSpec = CampusMotion.standard(),
                        label = "timelineRowBg"
                    )

                    StaggeredAppear(index = index, key = courses.size) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tapScale(onClick = { onCourseClick(course) }, pressedScale = 0.985f, clipShape = RoundedCornerShape(CampusShapes.extraSmall))
                                .background(rowBg)
                                .padding(vertical = 7.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. 时间列
                            Column(
                                modifier = Modifier.width(54.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                val begin = course.beginTime
                                val end = course.endTime
                                if (!begin.isNullOrBlank() && !end.isNullOrBlank()) {
                                    Text(
                                        text = begin,
                                        fontSize = 13.sp,
                                        fontWeight = if (isOngoing) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isOngoing) colors.brand else colors.textPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = end,
                                        fontSize = 11.sp,
                                        color = colors.textTertiary,
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Text(
                                        text = "第${course.beginSection}节",
                                        fontSize = 12.sp,
                                        fontWeight = if (isOngoing) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isOngoing) colors.brand else colors.textPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "至${course.endSection}节",
                                        fontSize = 11.sp,
                                        color = colors.textTertiary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            // 2. 标记列
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(46.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!isLast) {
                                    Box(
                                        modifier = Modifier
                                            .width(1.5.dp)
                                            .fillMaxHeight()
                                            .offset(y = 22.dp)
                                            .background(colors.outline)
                                    )
                                }

                                val dotSize by animateDpAsState(
                                    targetValue = if (isOngoing) 12.dp else 9.dp,
                                    animationSpec = CampusMotion.springBouncy(),
                                    label = "timelineDot"
                                )

                                if (isEnded) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(colors.surface)
                                            .border(1.5.dp, colors.textTertiary.copy(alpha = 0.6f), CircleShape)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(dotSize)
                                            .clip(CircleShape)
                                            .background(if (isOngoing) colors.brand else colors.brand.copy(alpha = 0.75f))
                                    )
                                    if (isOngoing) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(colors.brand.copy(alpha = 0.14f))
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // 3. 课程主体信息
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = course.title,
                                    fontSize = 15.sp,
                                    fontWeight = if (isOngoing) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isEnded) colors.textSecondary else colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "第 ${course.beginSection}-${course.endSection} 节 · ${course.place ?: "教室待定"}",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 4. 状态标签
                            Text(
                                text = statusText,
                                fontSize = 12.sp,
                                fontWeight = if (isOngoing) FontWeight.SemiBold else FontWeight.Normal,
                                color = statusColor,
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                            )
                        }
                    }
                }

                if (courses.size > 4) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .tapScale(onClick = onSeeAllClick, pressedScale = 0.97f, clipShape = RoundedCornerShape(CampusShapes.extraSmall))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "查看全部 ${courses.size} 项课程安排",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.brand
                        )
                    }
                }
            }
        }
    }
}

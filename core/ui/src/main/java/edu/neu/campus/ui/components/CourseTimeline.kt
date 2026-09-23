package edu.neu.campus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text

/**
 * 今日课程时间轴组件 (CourseTimeline)。
 * 严格遵照 docs/07-UI视觉与布局重设计.md 第 4.2 节规范：
 * - 集中放置在一个 Surface 分组中，替代重复卡片嵌套
 * - 时间列约 52dp、标记列 12dp、其余空间承载课程信息
 * - 标记点区分状态：已结束 (○)、进行中/待上课 (●)，垂直连线串联
 * - 当前正在进行的课程使用品牌色指示与微浅底色强调
 * - 默认最多展示 4 项，超过时展示“查看全部 N 项 ›”
 */
@Composable
fun CourseTimeline(
    courses: List<CourseOccurrence>,
    nowTimeStr: String,
    onCourseClick: (CourseOccurrence) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surface)
            .padding(16.dp)
    ) {
        if (courses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "今天没有已安排课程安排",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
        } else {
            val displayCourses = courses.take(4)
            Column(modifier = Modifier.fillMaxWidth()) {
                displayCourses.forEachIndexed { index, course ->
                    val isLast = index == displayCourses.lastIndex

                    val b = course.beginTime
                    val e = course.endTime
                    val isEnded = e != null && e < nowTimeStr
                    val isOngoing = b != null && e != null && nowTimeStr >= b && nowTimeStr <= e

                    val statusText = when {
                        isOngoing -> "正在上课"
                        isEnded -> "已结束"
                        else -> "待上课"
                    }

                    val statusColor = when {
                        isOngoing -> colors.success
                        isEnded -> colors.textSecondary.copy(alpha = 0.6f)
                        else -> colors.brand
                    }

                    val rowBg = if (isOngoing) colors.brandContainer.copy(alpha = 0.35f) else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(rowBg)
                            .clickable { onCourseClick(course) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. 左侧时间列 (约 52dp)
                        Column(
                            modifier = Modifier.width(52.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (!b.isNullOrBlank() && !e.isNullOrBlank()) {
                                Text(
                                    text = b,
                                    fontSize = 12.sp,
                                    fontWeight = if (isOngoing) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOngoing) colors.brand else colors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = e,
                                    fontSize = 10.sp,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                Text(
                                    text = "第${course.beginSection}节",
                                    fontSize = 11.sp,
                                    fontWeight = if (isOngoing) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isOngoing) colors.brand else colors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "至${course.endSection}节",
                                    fontSize = 10.sp,
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // 2. 标记列 (12dp，轴线与圆点)
                        Box(
                            modifier = Modifier
                                .width(18.dp)
                                .height(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // 纵向连接轴线
                            if (!isLast) {
                                Box(
                                    modifier = Modifier
                                        .width(1.5.dp)
                                        .fillMaxHeight()
                                        .offset(y = 20.dp)
                                        .background(colors.outline)
                                )
                            }

                            // 节点圆点
                            if (isEnded) {
                                // 空心圆圈
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(colors.surface)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(colors.textSecondary.copy(alpha = 0.5f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .align(Alignment.Center)
                                            .clip(CircleShape)
                                            .background(colors.surface)
                                    )
                                }
                            } else {
                                // 实心圆点
                                Box(
                                    modifier = Modifier
                                        .size(if (isOngoing) 11.dp else 9.dp)
                                        .clip(CircleShape)
                                        .background(if (isOngoing) colors.brand else colors.brand.copy(alpha = 0.7f))
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // 3. 课程主体信息列
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = course.title,
                                fontSize = 15.sp,
                                fontWeight = if (isOngoing) FontWeight.Bold else FontWeight.Medium,
                                color = if (isEnded) colors.textPrimary.copy(alpha = 0.75f) else colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            val placeText = course.place ?: "教室待定"
                            Text(
                                text = "第 ${course.beginSection}-${course.endSection} 节 · $placeText",
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

                // 超过 4 门排课时提供“查看全部”入口
                if (courses.size > 4) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onSeeAllClick)
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "查看全部 ${courses.size} 项课程安排 ›",
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

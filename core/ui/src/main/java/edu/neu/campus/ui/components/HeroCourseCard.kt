package edu.neu.campus.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.contract.QueryError
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text

/**
 * 首页 A 类品牌主课程卡 (HeroCourseCard)。
 * 严格遵照 docs/07-UI视觉与布局重设计.md 第 2 节与第 4 节规范：
 * - 最小高度 168dp，内边距 20dp，圆角 24dp
 * - HeroSurface 品牌深蓝背景 (#173A79 -> #2458C6 轻渐变)，右上角抽象几何 N 装饰线 (8% 透明度)
 * - 完整呈现主课程状态、名称 (22sp Semibold，至多两行)、节次时间与地点
 * - 覆盖全部 9 种数据状态（正在上课、下一节课、今日结束、无课、缺少可信时间、周次待确认、冲突多项、旧缓存、加载失败）
 */
@Composable
fun HeroCourseCard(
    courses: List<CourseOccurrence>,
    nowTimeStr: String,
    isLoading: Boolean,
    error: QueryError?,
    isStale: Boolean = false,
    hasConfirmedTerm: Boolean = true,
    onClickCourse: (CourseOccurrence) -> Unit,
    onConflictClick: (List<CourseOccurrence>) -> Unit,
    onGotoTimetable: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val bgBrush = if (!colors.isDark) {
        Brush.linearGradient(
            colors = listOf(colors.heroSurface, Color(0xFF2458C6)),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    } else {
        Brush.linearGradient(
            colors = listOf(colors.heroSurface, Color(0xFF1B3560)),
            start = Offset.Zero,
            end = Offset.Infinite
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 168.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(bgBrush)
    ) {
        // 右上角抽象几何 N 与书页线条 (8% 透明度)
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.TopEnd)
        ) {
            val strokeWidth = 2.dp.toPx()
            val lineColor = Color.White.copy(alpha = 0.08f)

            // 抽象折线 N
            val path = Path().apply {
                moveTo(size.width * 0.45f, size.height * 0.95f)
                lineTo(size.width * 0.45f, size.height * 0.15f)
                lineTo(size.width * 0.85f, size.height * 0.85f)
                lineTo(size.width * 0.85f, size.height * 0.05f)
            }
            drawPath(path, color = lineColor, style = Stroke(width = strokeWidth))

            // 装饰书页框线
            drawRoundRect(
                color = lineColor,
                topLeft = Offset(size.width * 0.25f, size.height * 0.35f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.65f, size.height * 0.55f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
        }

        // 内容区
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            when {
                isLoading && courses.isEmpty() -> {
                    // 1. 首次加载且无缓存
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 128.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        InfiniteProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "正在同步今日课程…",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                error != null && courses.isEmpty() -> {
                    // 2. 无缓存且失败
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 128.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "排课数据暂未更新",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = error.message.ifBlank { "查询遇到异常，请检查网络后重试" },
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.82f),
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable(onClick = onRetry)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "重新同步",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                !hasConfirmedTerm -> {
                    // 3. 学期/周次待确认
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 128.dp)
                            .clickable(onClick = onGotoTimetable),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "教学周待确认",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Text(
                            text = "请前往课表选择当前学期",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "同步学校日历校对学期周次",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                        Spacer(modifier = Modifier.weight(1f, fill = false))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "选择学期 ›",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                courses.isEmpty() -> {
                    // 4. 成功获取且当日无排课
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 128.dp)
                            .clickable(onClick = onGotoTimetable),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.16f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "今日安排",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "今天没有已安排课程",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "好好休息，或查看本周其他日期的排课与实践课",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "查看本周课表 ›",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                    }
                }

                else -> {
                    // 5. 有课程排课：判断当前进行、下一节或今日已结束
                    val ongoingCourses = courses.filter {
                        val b = it.beginTime
                        val e = it.endTime
                        b != null && e != null && nowTimeStr >= b && nowTimeStr <= e
                    }
                    val upcomingCourses = courses.filter {
                        val b = it.beginTime
                        b != null && b > nowTimeStr
                    }
                    val hasTimeInfo = courses.any { !it.beginTime.isNullOrBlank() }

                    val isOngoing = ongoingCourses.isNotEmpty()
                    val targetCourses = when {
                        isOngoing -> ongoingCourses
                        upcomingCourses.isNotEmpty() -> {
                            val nextSection = upcomingCourses.first().beginSection
                            upcomingCourses.filter { it.beginSection == nextSection }
                        }
                        !hasTimeInfo -> courses
                        else -> emptyList() // 当天全部已结束
                    }

                    if (targetCourses.isEmpty()) {
                        // 当天排课已全部结束
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 128.dp)
                                .clickable(onClick = onGotoTimetable),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.White.copy(alpha = 0.16f))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "今日日程",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "今天的课程已结束",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "共完成 ${courses.size} 门课程 · 点击查看完整课表",
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.82f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "完整课表 ›",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    } else if (targetCourses.size > 1) {
                        // 同时有多项课程冲突
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 128.dp)
                                .clickable { onConflictClick(targetCourses) },
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                val statusLabel = if (isOngoing) "正在上课" else "下一节课"
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFFFB74D).copy(alpha = 0.25f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "$statusLabel · 冲突排课",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFFFE082)
                                        )
                                    }
                                    val first = targetCourses.first()
                                    val timeSpan = if (!first.beginTime.isNullOrBlank() && !first.endTime.isNullOrBlank()) {
                                        "${first.beginTime}—${first.endTime}"
                                    } else "第 ${first.beginSection}—${first.endSection} 节"
                                    Text(
                                        text = timeSpan,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }

                                Text(
                                    text = "同时有 ${targetCourses.size} 项课程安排",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Text(
                                    text = targetCourses.joinToString(" / ") { it.title },
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "点击选择查看详情",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.75f)
                                )
                                Text(
                                    text = "选择课程 ›",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    } else {
                        // 单门主要课程展示
                        val course = targetCourses.first()
                        val statusLabel = when {
                            !hasTimeInfo -> "今日课程"
                            isOngoing -> "正在上课"
                            else -> "下一节课"
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 128.dp)
                                .clickable { onClickCourse(course) },
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                // 顶部状态标签与时间
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isOngoing) Color(0xFF66BB6A).copy(alpha = 0.28f)
                                                    else Color.White.copy(alpha = 0.16f)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOngoing) Color(0xFFA5D6A7) else Color.White
                                            )
                                        }

                                        if (isStale) {
                                            Text(
                                                text = "上次同步课程",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }

                                    // 时间或节次
                                    val timeSpan = if (!course.beginTime.isNullOrBlank() && !course.endTime.isNullOrBlank()) {
                                        "${course.beginTime}—${course.endTime}"
                                    } else {
                                        "第 ${course.beginSection}—${course.endSection} 节"
                                    }
                                    Text(
                                        text = timeSpan,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }

                                // 课程名称 (22sp, Semibold, 允许两行)
                                Text(
                                    text = course.title,
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )

                                // 地点与校区
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    val placeText = course.place ?: "教室待公布"
                                    Text(
                                        text = "第 ${course.beginSection}—${course.endSection} 节 · $placeText",
                                        fontSize = 13.sp,
                                        color = Color.White.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // 底部箭头提示
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "查看详情",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "查看详情",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package edu.neu.campus.ui.timetable

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.components.rememberSchoolClock
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text
import java.text.SimpleDateFormat
import java.util.*

/** Individual section rows prevent adjacent single-section courses becoming false conflicts. */
fun coursesAtSection(courses: List<CourseOccurrence>, day: Int, section: Int) =
    courses.filter { it.dayOfWeek == day && section in it.beginSection..it.endSection }

/** Connected overlapping section ranges; adjacent lessons stay separate. */
fun courseBlocksForDay(courses: List<CourseOccurrence>, day: Int): List<List<CourseOccurrence>> {
    val blocks = mutableListOf<MutableList<CourseOccurrence>>()
    for (course in courses.filter { it.dayOfWeek == day }.sortedBy { it.beginSection }) {
        val previous = blocks.lastOrNull()
        if (previous != null && course.beginSection <= previous.maxOf { it.endSection }) previous.add(course)
        else blocks.add(mutableListOf(course))
    }
    return blocks
}

private val AxisWidth = 46.dp
private val DayWidth = 88.dp
private val SectionHeight = 96.dp

/**
 * 课表网格 (TimetableGrid)。
 *
 * 视觉约定：
 * - 表头使用表面色，今日列以品牌浅底 + 圆角高亮，日期文字同色强调
 * - 左侧节次轴独立宽度，与课程区共享同一纵向滚动
 * - 课程块为功能色容器 + 左侧强调色条，按压有缩放反馈
 * - 课块在首次挂载时按列交错淡入
 */
@Composable
fun TimetableGrid(
    courses: List<CourseOccurrence>, currentWeek: TeachingWeek?,
    sections: List<Section> = emptyList(),
    onCourseClick: (CourseOccurrence) -> Unit,
    onConflictClick: (List<CourseOccurrence>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val schoolClock = rememberSchoolClock()
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai"); isLenient = false
    }
    val today = formatter.format(schoolClock.time)
    val dates = remember(currentWeek?.startDate) {
        runCatching {
            val first = formatter.parse(currentWeek?.startDate.orEmpty()) ?: return@runCatching emptyList<String>()
            val calendar = Calendar.getInstance(formatter.timeZone).apply { time = first }
            (1..7).map { formatter.format(calendar.time).also { calendar.add(Calendar.DATE, 1) } }
        }.getOrDefault(emptyList())
    }
    val numbers = (sections.mapNotNull { it.code.toIntOrNull() } +
        courses.flatMap { (it.beginSection..it.endSection).toList() }).distinct().sorted()
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()

    if (numbers.isEmpty()) {
        Text(
            text = "暂无可显示的节次信息",
            modifier = modifier.padding(20.dp),
            color = colors.textSecondary,
            fontSize = 14.sp
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
    ) {
        // 表头
        Row(modifier = Modifier.background(colors.surface)) {
            Box(
                modifier = Modifier
                    .width(AxisWidth)
                    .height(58.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "节次",
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontal)
            ) {
                (1..7).forEach { day ->
                    val date = dates.getOrNull(day - 1)
                    val isToday = date == today
                    val headerColor by animateColorAsState(
                        targetValue = if (isToday) colors.brandContainer else Color.Transparent,
                        animationSpec = tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.standard),
                        label = "dayHeaderColor"
                    )
                    Box(
                        modifier = Modifier
                            .width(DayWidth)
                            .heightIn(min = 58.dp)
                            .padding(horizontal = 3.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(CampusShapes.extraSmall))
                            .background(headerColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "周${dayOfWeekText(day)}",
                                fontSize = 13.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                color = if (isToday) colors.brand else colors.textPrimary
                            )
                            Text(
                                text = date?.takeLast(5) ?: "日期待确认",
                                fontSize = 11.sp,
                                color = if (isToday) colors.brand else colors.textTertiary
                            )
                        }
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider)
        )

        // 网格主体
        Row(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(vertical)
        ) {
            // 节次轴
            Column(modifier = Modifier.width(AxisWidth)) {
                numbers.forEach { number ->
                    Box(
                        modifier = Modifier
                            .height(SectionHeight)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number.toString(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textTertiary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(horizontal)
            ) {
                (1..7).forEach { day ->
                    val date = dates.getOrNull(day - 1)
                    val isToday = date == today
                    val columnBg by animateColorAsState(
                        targetValue = if (isToday) colors.brandContainer.copy(alpha = 0.28f) else Color.Transparent,
                        animationSpec = tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.standard),
                        label = "dayColumnColor"
                    )
                    Box(
                        modifier = Modifier
                            .width(DayWidth)
                            .height((SectionHeight.value * numbers.size).dp)
                            .background(columnBg)
                    ) {
                        courseBlocksForDay(courses, day).forEachIndexed { blockIndex, matched ->
                            val firstSection = matched.minOf { it.beginSection }
                            val lastSection = matched.maxOf { it.endSection }
                            val startRow = numbers.indexOf(firstSection)
                            val rowCount = numbers.indexOf(lastSection) - startRow + 1
                            val course = matched.first()
                            val (fg, bg) = colors.courseColor(course.sourceId ?: "${course.campusId}:${course.title}")

                            StaggeredAppear(index = day + blockIndex, key = currentWeek?.number) {
                                Box(
                                    modifier = Modifier
                                        .offset(y = (SectionHeight * startRow))
                                        .height(SectionHeight * rowCount)
                                        .fillMaxWidth()
                                        .padding(3.dp)
                                        .tapScale(onClick = {
                                                if (matched.size > 1) onConflictClick(matched) else onCourseClick(course)
                                            },
                                            pressedScale = 0.96f, clipShape = RoundedCornerShape(CampusShapes.small))
                                        .background(bg)
                                        .border(
                                            width = 1.dp,
                                            color = fg.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(CampusShapes.small)
                                        )
                                ) {
                                    Row(modifier = Modifier.fillMaxSize()) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .fillMaxHeight()
                                                .background(fg)
                                        )
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 6.dp, vertical = 5.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            if (matched.size > 1) {
                                                Text(
                                                    text = "${matched.size} 项安排",
                                                    color = fg,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else {
                                                Text(
                                                    text = course.title,
                                                    color = fg,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = course.place ?: "地点未提供",
                                                    color = fg.copy(alpha = 0.82f),
                                                    fontSize = 11.sp,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
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
        }
    }
}

fun dayOfWeekText(day: Int): String = listOf("一", "二", "三", "四", "五", "六", "日").getOrNull(day - 1) ?: day.toString()

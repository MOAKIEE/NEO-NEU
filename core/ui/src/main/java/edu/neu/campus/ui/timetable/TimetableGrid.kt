package edu.neu.campus.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import edu.neu.campus.contract.TeachingWeek
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text
import java.text.SimpleDateFormat
import java.util.*

private val SectionPairs = listOf(
    Pair(1, 2) to "08:00\n09:40",
    Pair(3, 4) to "10:00\n11:40",
    Pair(5, 6) to "14:00\n15:40",
    Pair(7, 8) to "16:00\n17:40",
    Pair(9, 10) to "18:30\n20:10",
    Pair(11, 12) to "20:20\n22:00"
)

/**
 * 课表网格组件 (TimetableGrid)。
 * 严格遵照 docs/07-UI视觉与布局重设计.md 第 5 节规范：
 * - 顶部固定日期与星期行，今日高亮
 * - 左侧节次坐标轴（1..12 节，规范 44dp 宽度）
 * - 单日列宽最小 64dp，支持日期与网格横向联动滚动，避免窄屏硬挤
 * - 6 组稳定课程配色，同门课跨周一致，深浅模式自动适配
 * - 12dp 圆角课程块，细色条指示，冲突课程集合卡片提示
 */
@Composable
fun TimetableGrid(
    courses: List<CourseOccurrence>,
    currentWeek: TeachingWeek?,
    onCourseClick: (CourseOccurrence) -> Unit,
    onConflictClick: (List<CourseOccurrence>) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val todayDayOfWeek = rememberTodayDayOfWeek()
    val scrollStateV = rememberScrollState()
    val scrollStateH = rememberScrollState()

    val dayColumnWidth = 66.dp
    val sectionRowHeight = 76.dp
    val sectionHeaderWidth = 44.dp

    // 计算当周各天的日期数值（如 21, 22...）
    val weekDates = remember(currentWeek?.startDate) {
        calculateWeekDates(currentWeek?.startDate)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        // 1. 顶部固定星期与日期行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(vertical = 6.dp)
        ) {
            // 左上角节次轴留白
            Box(
                modifier = Modifier
                    .width(sectionHeaderWidth)
                    .height(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "节次",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }

            // 横向滚动的星期与日期栏
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollStateH)
            ) {
                for (day in 1..7) {
                    val isToday = day == todayDayOfWeek
                    val dateNumStr = weekDates.getOrNull(day - 1) ?: ""

                    Column(
                        modifier = Modifier
                            .width(dayColumnWidth)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isToday) colors.brandContainer else Color.Transparent)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "周${dayOfWeekText(day)}",
                            fontSize = 12.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday) colors.brand else colors.textPrimary
                        )
                        if (dateNumStr.isNotBlank()) {
                            Text(
                                text = dateNumStr,
                                fontSize = 11.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                color = if (isToday) colors.brand else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // 分割线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.outline.copy(alpha = 0.5f))
        )

        // 2. 网格主体（节次纵向滚动，日期横向联动滚动）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollStateV)
        ) {
            // 左侧固定节次与时间轴
            Column(
                modifier = Modifier
                    .width(sectionHeaderWidth)
                    .background(colors.surface.copy(alpha = 0.6f))
            ) {
                SectionPairs.forEach { (pair, timeStr) ->
                    Box(
                        modifier = Modifier
                            .height(sectionRowHeight)
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${pair.first}-${pair.second}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = timeStr,
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            // 右侧课程格子区域
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollStateH)
            ) {
                for (day in 1..7) {
                    Column(modifier = Modifier.width(dayColumnWidth)) {
                        SectionPairs.forEach { (pair, _) ->
                            // 查找在该天、该节次区间的课程
                            val matched = courses.filter { c ->
                                c.dayOfWeek == day &&
                                        ((c.beginSection in pair.first..pair.second) ||
                                                (c.endSection in pair.first..pair.second) ||
                                                (c.beginSection <= pair.first && c.endSection >= pair.second))
                            }

                            Box(
                                modifier = Modifier
                                    .height(sectionRowHeight)
                                    .width(dayColumnWidth)
                                    .padding(3.dp)
                            ) {
                                when {
                                    matched.size == 1 -> {
                                        val course = matched[0]
                                        // 采用稳定 6 色课程色映射
                                        val (fgColor, bgColor) = colors.courseColor(course.title)

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(bgColor)
                                                .clickable { onCourseClick(course) }
                                        ) {
                                            // 左侧色条指示
                                            Box(
                                                modifier = Modifier
                                                    .width(3.5.dp)
                                                    .fillMaxHeight()
                                                    .background(fgColor)
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(start = 6.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = course.title,
                                                    fontSize = 12.sp,
                                                    lineHeight = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = fgColor,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = course.place ?: "",
                                                    fontSize = 10.sp,
                                                    lineHeight = 12.sp,
                                                    color = fgColor.copy(alpha = 0.85f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    matched.size > 1 -> {
                                        // 冲突排课：集合卡片展示
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(colors.warning.copy(alpha = 0.18f))
                                                .clickable { onConflictClick(matched) }
                                                .padding(4.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${matched.size} 门冲突",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.warning
                                            )
                                            Text(
                                                text = "点击查看",
                                                fontSize = 9.sp,
                                                color = colors.warning
                                            )
                                        }
                                    }

                                    else -> {
                                        // 空白节次槽位
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colors.surface.copy(alpha = 0.35f))
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

private fun calculateWeekDates(startDateStr: String?): List<String> {
    if (startDateStr.isNullOrBlank()) return emptyList()
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val date = sdf.parse(startDateStr) ?: return emptyList()
        val cal = Calendar.getInstance()
        cal.time = date
        (0..6).map {
            val d = cal.get(Calendar.DAY_OF_MONTH)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            d.toString()
        }
    }.getOrDefault(emptyList())
}

private fun rememberTodayDayOfWeek(): Int {
    val cal = Calendar.getInstance()
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 1
        Calendar.TUESDAY -> 2
        Calendar.WEDNESDAY -> 3
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 5
        Calendar.SATURDAY -> 6
        Calendar.SUNDAY -> 7
        else -> 1
    }
}

fun dayOfWeekText(day: Int): String = when (day) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> day.toString()
}

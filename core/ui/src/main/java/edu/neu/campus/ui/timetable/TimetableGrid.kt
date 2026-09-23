package edu.neu.campus.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import edu.neu.campus.contract.TeachingWeek
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Calendar

private val SectionPairs = listOf(
    Pair(1, 2) to "08:00\n09:40",
    Pair(3, 4) to "10:00\n11:40",
    Pair(5, 6) to "14:00\n15:40",
    Pair(7, 8) to "16:00\n17:40",
    Pair(9, 10) to "18:30\n20:10",
    Pair(11, 12) to "20:20\n22:00"
)

private val CoursePalette = listOf(
    Color(0xFFE8F0FE) to Color(0xFF1967D2),
    Color(0xFFFEF7E0) to Color(0xFFB06000),
    Color(0xFFE6F4EA) to Color(0xFF137333),
    Color(0xFFFCE8E6) to Color(0xFFC5221F),
    Color(0xFFF3E8FD) to Color(0xFF7627BB),
    Color(0xFFE0F2F1) to Color(0xFF00796B),
    Color(0xFFFFF3E0) to Color(0xFFE65100),
    Color(0xFFEDE7F6) to Color(0xFF512DA8)
)

private fun colorForCourse(title: String): Pair<Color, Color> {
    val index = kotlin.math.abs(title.hashCode()) % CoursePalette.size
    return CoursePalette[index]
}

@Composable
fun TimetableGrid(
    courses: List<CourseOccurrence>,
    currentWeek: TeachingWeek?,
    onCourseClick: (CourseOccurrence) -> Unit,
    onConflictClick: (List<CourseOccurrence>) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayDayOfWeek = rememberTodayDayOfWeek()
    val scrollStateV = rememberScrollState()
    val scrollStateH = rememberScrollState()

    val dayColumnWidth = 64.dp
    val sectionRowHeight = 72.dp
    val sectionHeaderWidth = 42.dp

    Column(modifier = modifier.fillMaxWidth()) {
        // 1. 顶部固定星期与日期行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Spacer(modifier = Modifier.width(sectionHeaderWidth))
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollStateH)
            ) {
                for (day in 1..7) {
                    val isToday = day == todayDayOfWeek
                    Column(
                        modifier = Modifier
                            .width(dayColumnWidth)
                            .padding(horizontal = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isToday) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "周${dayOfWeekText(day)}",
                            fontSize = 13.sp,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 2. 网格主体（节次纵向滚动，日期横向联动滚动）
        Row(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollStateV)
        ) {
            // 左侧固定节次与起止时间
            Column(modifier = Modifier.width(sectionHeaderWidth)) {
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
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = timeStr,
                                fontSize = 9.sp,
                                lineHeight = 11.sp,
                                textAlign = TextAlign.Center,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
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
                                    .padding(2.dp)
                            ) {
                                when {
                                    matched.size == 1 -> {
                                        val course = matched[0]
                                        val (bgColor, textColor) = colorForCourse(course.title)
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bgColor)
                                                .clickable { onCourseClick(course) }
                                                .padding(4.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = course.title,
                                                fontSize = 11.sp,
                                                lineHeight = 13.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = textColor,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = course.place ?: "",
                                                fontSize = 10.sp,
                                                color = textColor.copy(alpha = 0.85f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    matched.size > 1 -> {
                                        // 冲突处理：集合卡片展示
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFFFEBEE))
                                                .clickable { onConflictClick(matched) }
                                                .padding(4.dp),
                                            verticalArrangement = Arrangement.Center,
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "${matched.size} 门冲突",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFC62828)
                                            )
                                            Text(
                                                text = "点击查看",
                                                fontSize = 9.sp,
                                                color = Color(0xFFC62828)
                                            )
                                        }
                                    }
                                    else -> {
                                        // 空白节次槽位
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.25f))
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

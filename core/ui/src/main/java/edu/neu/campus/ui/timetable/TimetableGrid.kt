package edu.neu.campus.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.rememberSchoolClock
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
        Text("暂无可显示的节次信息", modifier = modifier.padding(20.dp))
        return
    }
    Column(modifier.fillMaxSize()) {
        Row(Modifier.background(colors.surface)) {
            Box(Modifier.width(52.dp).height(56.dp), contentAlignment = Alignment.Center) { Text("节次") }
            Row(Modifier.weight(1f).horizontalScroll(horizontal)) {
                (1..7).forEach { day ->
                    val date = dates.getOrNull(day - 1)
                    Column(Modifier.width(84.dp).heightIn(min = 56.dp)
                        .background(if (date == today) colors.brandContainer else Color.Transparent).padding(6.dp)) {
                        Text("周${dayOfWeekText(day)}")
                        Text(date?.takeLast(5) ?: "日期待确认", fontSize = 12.sp)
                    }
                }
            }
        }
        Row(Modifier.weight(1f).verticalScroll(vertical)) {
            Column(Modifier.width(52.dp)) {
                numbers.forEach { number ->
                    Box(Modifier.height(96.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(number.toString(), fontSize = 14.sp)
                    }
                }
            }
            Row(Modifier.weight(1f).horizontalScroll(horizontal)) {
                (1..7).forEach { day ->
                    Box(Modifier.width(84.dp).height((96 * numbers.size).dp)) {
                        courseBlocksForDay(courses, day).forEach { matched ->
                            val firstSection = matched.minOf { it.beginSection }
                            val lastSection = matched.maxOf { it.endSection }
                            val startRow = numbers.indexOf(firstSection)
                            val rowCount = numbers.indexOf(lastSection) - startRow + 1
                            val course = matched.first()
                            val (fg, bg) = colors.courseColor(course.sourceId ?: "${course.campusId}:${course.title}")
                            Column(Modifier.offset(y = (96 * startRow).dp)
                                .height((96 * rowCount).dp).fillMaxWidth().padding(3.dp)
                                .clip(RoundedCornerShape(12.dp)).background(bg)
                                .clickable {
                                    if (matched.size > 1) onConflictClick(matched) else onCourseClick(course)
                                }.padding(6.dp)) {
                                if (matched.size > 1) Text("${matched.size} 项安排", color = fg, fontWeight = FontWeight.Bold)
                                else {
                                    Text(course.title, color = fg, fontSize = 13.sp, maxLines = 3)
                                    Text(course.place ?: "地点未提供", color = fg, fontSize = 12.sp, maxLines = 2)
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

package edu.neu.campus.ui.timetable

import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.contract.Section
import edu.neu.campus.contract.TeachingWeek
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * 课表的纯数据模型：日期推算与课块排布都在这里完成，Compose 层只负责绘制。
 *
 * 星期统一用 1=周一 … 7=周日（与教务 `dayOfWeek` 一致）。学校可把一周起点配置为周日
 * （官方前端 `firstDayOfWeek`），所以列顺序与日期必须按教学周的真实日期推算，
 * 不能把 `startDate` 固定当作周一。
 */
data class WeekDay(val dayOfWeek: Int, val date: String?, val month: Int?, val dayOfMonth: Int?)

data class SectionSlot(val number: Int, val startTime: String?, val endTime: String?)

/** 同一列里区间相连或重叠的课程合并为一个课块；多于一门即为冲突。 */
data class TimetableBlock(val column: Int, val startRow: Int, val rowSpan: Int, val courses: List<CourseOccurrence>) {
    val isConflict: Boolean get() = courses.size > 1
}

data class TimetableLayout(val days: List<WeekDay>, val sections: List<SectionSlot>, val blocks: List<TimetableBlock>)

private val schoolTimeZone: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

private fun dateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
    timeZone = schoolTimeZone; isLenient = false
}

private fun parseSchoolDate(value: String?): Calendar? {
    if (value.isNullOrBlank()) return null
    val parsed = runCatching { dateFormat().parse(value.trim().take(10)) }.getOrNull() ?: return null
    return Calendar.getInstance(schoolTimeZone).apply { time = parsed }
}

/** 1=周一 … 7=周日。 */
fun isoDayOfWeek(calendar: Calendar): Int = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1

fun schoolDateString(calendar: Calendar): String = dateFormat().format(calendar.time)

private fun daysBetween(start: Calendar, end: Calendar): Long =
    Math.round((end.timeInMillis - start.timeInMillis) / 86_400_000.0)

/**
 * 从整学期教学周推断一周从星期几开始：取「恰好 7 天」的教学周里最常见的起始星期。
 * 首尾周可能不满 7 天（开学日在周中），不参与推断。没有可用日期时按周一处理。
 */
fun inferFirstDayOfWeek(weeks: List<TeachingWeek>): Int {
    val starts = weeks.mapNotNull { week ->
        val start = parseSchoolDate(week.startDate) ?: return@mapNotNull null
        val end = parseSchoolDate(week.endDate) ?: return@mapNotNull null
        if (daysBetween(start, end) == 6L) isoDayOfWeek(start) else null
    }
    return starts.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: 1
}

/**
 * 教学周的七列：从 [firstDayOfWeek] 开始排列，日期取包含该周起止日期的那个自然周。
 * 教学周缺日期时只给出星期，不猜日期。
 */
fun weekDaysOf(week: TeachingWeek?, firstDayOfWeek: Int = 1): List<WeekDay> {
    val first = firstDayOfWeek.coerceIn(1, 7)
    val anchor = parseSchoolDate(week?.startDate) ?: parseSchoolDate(week?.endDate)
        ?: return (0 until 7).map { WeekDay((first - 1 + it) % 7 + 1, null, null, null) }
    anchor.add(Calendar.DATE, -((isoDayOfWeek(anchor) - first + 7) % 7))
    return (0 until 7).map {
        WeekDay(
            dayOfWeek = isoDayOfWeek(anchor),
            date = schoolDateString(anchor),
            month = anchor.get(Calendar.MONTH) + 1,
            dayOfMonth = anchor.get(Calendar.DAY_OF_MONTH)
        ).also { anchor.add(Calendar.DATE, 1) }
    }
}

/** Individual section rows prevent adjacent single-section courses becoming false conflicts. */
fun coursesAtSection(courses: List<CourseOccurrence>, day: Int, section: Int) =
    courses.filter { it.dayOfWeek == day && section in it.beginSection..it.endSection }

/** Connected overlapping section ranges; adjacent lessons stay separate. */
fun courseBlocksForDay(courses: List<CourseOccurrence>, day: Int): List<List<CourseOccurrence>> {
    val blocks = mutableListOf<MutableList<CourseOccurrence>>()
    var blockEnd = Int.MIN_VALUE
    for (course in courses.filter { it.dayOfWeek == day }.sortedBy { it.beginSection }) {
        val previous = blocks.lastOrNull()
        if (previous != null && course.beginSection <= blockEnd) {
            previous.add(course)
            blockEnd = maxOf(blockEnd, course.endSection)
        } else {
            blocks.add(mutableListOf(course))
            blockEnd = course.endSection
        }
    }
    return blocks
}

/**
 * 节次轴：学校节次列表与课程实际占用节次的并集。节次时间优先取学校节次，
 * 缺失时用恰好从该节开始／在该节结束的课程时间补齐。
 */
fun sectionSlotsOf(sections: List<Section>, courses: List<CourseOccurrence>): List<SectionSlot> {
    val byNumber = sections.mapNotNull { s -> s.code.toIntOrNull()?.let { it to s } }.toMap()
    val numbers = (byNumber.keys + courses.flatMap { (it.beginSection..it.endSection).toList() })
        .filter { it > 0 }.distinct().sorted()
    return numbers.map { n ->
        val section = byNumber[n]
        SectionSlot(
            number = n,
            startTime = section?.startTime?.takeIf { it.isNotBlank() }
                ?: courses.firstOrNull { it.beginSection == n && !it.beginTime.isNullOrBlank() }?.beginTime,
            endTime = section?.endTime?.takeIf { it.isNotBlank() }
                ?: courses.firstOrNull { it.endSection == n && !it.endTime.isNullOrBlank() }?.endTime
        )
    }
}

fun buildTimetableLayout(
    courses: List<CourseOccurrence>,
    sections: List<Section>,
    days: List<WeekDay>
): TimetableLayout {
    val slots = sectionSlotsOf(sections, courses)
    val rowOf = slots.withIndex().associate { (index, slot) -> slot.number to index }
    val blocks = days.flatMapIndexed { column, day ->
        courseBlocksForDay(courses, day.dayOfWeek).mapNotNull { matched ->
            val startRow = rowOf[matched.minOf { it.beginSection }] ?: return@mapNotNull null
            val endRow = rowOf[matched.maxOf { it.endSection }] ?: return@mapNotNull null
            TimetableBlock(column, startRow, endRow - startRow + 1, matched)
        }
    }
    return TimetableLayout(days, slots, blocks)
}

/** 课程配色键：优先教学班标识，跨周、跨页面保持同一门课同色。 */
fun CourseOccurrence.colorKey(): String = sourceId ?: "$campusId:$title"

fun dayOfWeekText(day: Int): String = listOf("一", "二", "三", "四", "五", "六", "日").getOrNull(day - 1) ?: day.toString()

/** 「2026-09-21」→「09/21」，格式不符时原样返回。 */
fun shortDate(value: String?): String? = value?.trim()?.let {
    if (Regex("^\\d{4}-\\d{2}-\\d{2}.*").matches(it)) it.substring(5, 10).replace('-', '/') else it
}

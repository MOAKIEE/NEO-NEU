package edu.neu.campus.ui.components

import edu.neu.campus.contract.CourseOccurrence

/** Only validated clock times may drive a live course status. End times are exclusive. */
fun clockMinutes(value: String?): Int? {
    val match = Regex("^(\\d{1,2}):(\\d{2})$").matchEntire(value.orEmpty()) ?: return null
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    return if (hour in 0..23 && minute in 0..59) hour * 60 + minute else null
}

fun hasCourseTime(course: CourseOccurrence): Boolean {
    val start = clockMinutes(course.beginTime) ?: return false
    val end = clockMinutes(course.endTime) ?: return false
    return end > start
}

fun courseStatus(course: CourseOccurrence, now: String, trusted: Boolean): String {
    val minute = clockMinutes(now)
    if (!trusted || !hasCourseTime(course) || minute == null) return "时间待确认"
    return when {
        minute < clockMinutes(course.beginTime)!! -> "待上课"
        minute < clockMinutes(course.endTime)!! -> "正在上课"
        else -> "已结束"
    }
}

data class CourseFocus(val label: String, val courses: List<CourseOccurrence>, val conflict: Boolean = false)

fun courseFocus(courses: List<CourseOccurrence>, now: String, trusted: Boolean): CourseFocus {
    if (!trusted || courses.any { !hasCourseTime(it) } || clockMinutes(now) == null) {
        return CourseFocus("今日课程 · 时间待确认", courses)
    }
    val ongoing = courses.filter { courseStatus(it, now, true) == "正在上课" }
    if (ongoing.isNotEmpty()) return CourseFocus("正在上课", ongoing, ongoing.size > 1)
    val upcoming = courses.filter { courseStatus(it, now, true) == "待上课" }
    val next = upcoming.minOfOrNull { clockMinutes(it.beginTime)!! }
    val focus = upcoming.filter { clockMinutes(it.beginTime) == next }
    return if (focus.isEmpty()) CourseFocus("今天的课程已结束", emptyList())
    else CourseFocus("下一节课", focus, focus.size > 1)
}

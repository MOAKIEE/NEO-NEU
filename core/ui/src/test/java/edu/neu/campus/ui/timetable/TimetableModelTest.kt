package edu.neu.campus.ui.timetable

import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.contract.Section
import edu.neu.campus.contract.TeachingWeek
import org.junit.Assert.*
import org.junit.Test

class TimetableModelTest {
    private fun course(day: Int, first: Int, last: Int = first, title: String = "Course $day-$first") =
        CourseOccurrence(null, "campus", title, day, first, last, null, null, null, null, null)

    @Test fun mondayStartWeekKeepsMondayFirst() {
        // 2026-09-21 是周一。
        val days = weekDaysOf(TeachingWeek(4, "2026-09-21", "2026-09-27", true))
        assertEquals((1..7).toList(), days.map { it.dayOfWeek })
        assertEquals("2026-09-21", days.first().date)
        assertEquals("2026-09-27", days.last().date)
        assertEquals(21, days.first().dayOfMonth)
    }

    @Test fun sundayStartWeekPutsSundayFirstWithMatchingDates() {
        // 学校把一周起点配置为周日：2026-09-20 是周日。
        val weeks = listOf(
            TeachingWeek(3, "2026-09-13", "2026-09-19", false),
            TeachingWeek(4, "2026-09-20", "2026-09-26", true)
        )
        val first = inferFirstDayOfWeek(weeks)
        assertEquals(7, first)
        val days = weekDaysOf(weeks[1], first)
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), days.map { it.dayOfWeek })
        assertEquals("2026-09-20", days[0].date)
        assertEquals("2026-09-21", days[1].date)
        // 旧实现把起始日当周一，周一列会显示 09-20。
        assertEquals("2026-09-21", days.first { it.dayOfWeek == 1 }.date)
    }

    @Test fun partialFirstWeekStillAlignsToNaturalWeek() {
        // 开学日为周三 2026-09-02，教学周只剩 5 天；列仍从周一 08-31 开始。
        val weeks = listOf(
            TeachingWeek(1, "2026-09-02", "2026-09-06", false),
            TeachingWeek(2, "2026-09-07", "2026-09-13", false)
        )
        val days = weekDaysOf(weeks[0], inferFirstDayOfWeek(weeks))
        assertEquals(1, days.first().dayOfWeek)
        assertEquals("2026-08-31", days.first().date)
        assertEquals("2026-09-02", days.first { it.dayOfWeek == 3 }.date)
    }

    @Test fun missingDatesKeepWeekdaysWithoutGuessing() {
        val days = weekDaysOf(TeachingWeek(1, null, null, false))
        assertEquals((1..7).toList(), days.map { it.dayOfWeek })
        assertTrue(days.all { it.date == null })
        assertEquals(1, inferFirstDayOfWeek(emptyList()))
    }

    @Test fun layoutPlacesBlocksInWeekColumns() {
        val days = weekDaysOf(TeachingWeek(4, "2026-09-20", "2026-09-26", true), 7)
        val monday = course(1, 3, 4)
        val sunday = course(7, 1, 2)
        val layout = buildTimetableLayout(listOf(monday, sunday), emptyList(), days)
        assertEquals(listOf(1, 2, 3, 4), layout.sections.map { it.number })
        val mondayBlock = layout.blocks.single { it.courses == listOf(monday) }
        assertEquals(1, mondayBlock.column)
        assertEquals(2, mondayBlock.startRow)
        assertEquals(2, mondayBlock.rowSpan)
        assertEquals(0, layout.blocks.single { it.courses == listOf(sunday) }.column)
    }

    @Test fun overlappingCoursesMergeIntoConflictBlock() {
        val long = course(2, 1, 4, "Long")
        val inner = course(2, 2, 2, "Inner")
        val tail = course(2, 4, 5, "Tail")
        val layout = buildTimetableLayout(listOf(long, inner, tail), emptyList(), weekDaysOf(null))
        val block = layout.blocks.single()
        assertTrue(block.isConflict)
        assertEquals(3, block.courses.size)
        assertEquals(5, block.rowSpan)
    }

    @Test fun sectionTimesFallBackToCourseTimes() {
        val timed = CourseOccurrence("id", "c", "T", 1, 1, 2, "08:00", "09:35", null, null, null)
        val slots = sectionSlotsOf(listOf(Section("1", null, null, null, null), Section("3", null, null, "10:00", "10:45")), listOf(timed))
        assertEquals(listOf(1, 2, 3), slots.map { it.number })
        assertEquals("08:00", slots[0].startTime)
        assertEquals("09:35", slots[1].endTime)
        assertEquals("10:00", slots[2].startTime)
    }

    @Test fun colorKeyPrefersTeachingClass() {
        assertEquals("tc1", CourseOccurrence("tc1", "c", "T", 1, 1, 1, null, null, null, null, null).colorKey())
        assertEquals("c:T", CourseOccurrence(null, "c", "T", 1, 1, 1, null, null, null, null, null).colorKey())
    }

    @Test fun shortDateFormatsSchoolDates() {
        assertEquals("09/21", shortDate("2026-09-21"))
        assertEquals("09/21", shortDate("2026-09-21 00:00:00"))
        assertNull(shortDate(null))
    }
}

package edu.neu.campus.ui.components

import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.ui.timetable.coursesAtSection
import edu.neu.campus.ui.timetable.courseBlocksForDay
import org.junit.Assert.*
import org.junit.Test

class CoursePresentationTest {
    private fun course(start: String? = "08:00", end: String? = "08:45", first: Int = 1, last: Int = first) =
        CourseOccurrence(null, "campus", "Test course", 1, first, last, start, end, null, null, null)

    @Test fun clockRejectsMissingOrInvalidTimes() {
        listOf(null, "", "24:00", "08:60", "tomorrow", "08:00:00").forEach { assertNull(clockMinutes(it)) }
        assertEquals(480, clockMinutes("8:00"))
    }
    @Test fun endBoundaryIsExclusive() {
        assertEquals("正在上课", courseStatus(course(), "08:44", true))
        assertEquals("已结束", courseStatus(course(), "08:45", true))
    }
    @Test fun missingTimeDoesNotBecomeConflictOrFinished() {
        val courses = listOf(course(null, null), course(null, null, 3))
        val focus = courseFocus(courses, "23:00", true)
        assertFalse(focus.conflict)
        assertEquals(courses, focus.courses)
        assertTrue(focus.label.contains("待确认"))
    }
    @Test fun partialTimePreventsClaimingAllCoursesFinished() {
        val focus = courseFocus(listOf(course(), course("15:00", null, 3)), "23:00", true)
        assertEquals(2, focus.courses.size)
        assertTrue(focus.label.contains("待确认"))
    }
    @Test fun staleDataNeverClaimsOngoing() {
        assertEquals("时间待确认", courseStatus(course(), "08:30", false))
        assertTrue(courseFocus(listOf(course()), "08:30", false).label.contains("待确认"))
    }
    @Test fun nextCourseUsesTimeRatherThanSectionOrder() {
        val late = course("11:00", "11:45", 1)
        val early = course("09:00", "09:45", 3)
        assertEquals(listOf(early), courseFocus(listOf(late, early), "08:00", true).courses)
    }
    @Test fun adjacentSectionsAreNotConflicts() {
        val first = course(first = 1)
        val second = course(first = 2)
        assertEquals(listOf(first), coursesAtSection(listOf(first, second), 1, 1))
        assertEquals(listOf(second), coursesAtSection(listOf(first, second), 1, 2))
    }
    @Test fun actualOverlapAndLateSectionsRemainAccessible() {
        val long = course(first = 12, last = 14)
        val overlap = course(first = 13)
        assertEquals(2, coursesAtSection(listOf(long, overlap), 1, 13).size)
        assertEquals(listOf(long), coursesAtSection(listOf(long, overlap), 1, 14))
        assertTrue(coursesAtSection(listOf(long), 2, 13).isEmpty())
    }
    @Test fun multiSectionCourseIsOneBlock() {
        val lesson = course(first = 1, last = 4)
        assertEquals(listOf(listOf(lesson)), courseBlocksForDay(listOf(lesson), 1))
    }
    @Test fun blockGroupingSeparatesAdjacentAndCombinesOverlap() {
        val first = course(first = 1, last = 2)
        val second = course(first = 3, last = 4)
        assertEquals(2, courseBlocksForDay(listOf(first, second), 1).size)
        val bridge = course(first = 2, last = 3)
        assertEquals(1, courseBlocksForDay(listOf(first, second, bridge), 1).size)
    }

}

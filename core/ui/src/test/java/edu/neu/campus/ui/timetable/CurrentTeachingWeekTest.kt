package edu.neu.campus.ui.timetable

import edu.neu.campus.contract.TeachingWeek
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CurrentTeachingWeekTest {
    private val previous = TeachingWeek(4, "2026-09-21", "2026-09-27", true)
    private val next = TeachingWeek(5, "2026-09-28", "2026-10-04", false)

    @Test fun weekBoundaryRejectsOldFlagUntilSchoolConfirmsTheNewWeek() {
        val cached = listOf(previous, next)
        assertEquals(previous, currentTeachingWeek(cached, "2026-09-27", null))
        assertNull(currentTeachingWeek(cached, "2026-09-28", null))
        val refreshed = listOf(previous.copy(isCurrent = false), next.copy(isCurrent = true))
        assertEquals(5, currentTeachingWeek(refreshed, "2026-09-28", null)?.number)
    }

    @Test fun offlineCacheWithinTheSameWeekRemainsUsable() {
        assertEquals(previous, currentTeachingWeek(listOf(previous), "2026-09-25", null))
    }

    @Test fun sundayStartUsesSchoolDatesInsteadOfAssumingMonday() {
        val week = TeachingWeek(4, "2026-09-20", "2026-09-26", true)
        assertEquals(week, currentTeachingWeek(listOf(week), "2026-09-26", null))
        assertNull(currentTeachingWeek(listOf(week), "2026-09-27", null))
    }

    @Test fun missingDatesOnlyTrustAFlagVerifiedOnTheSameSchoolDate() {
        val week = TeachingWeek(4, null, null, true)
        val verified = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(2026, Calendar.SEPTEMBER, 20, 16, 5)
        }.timeInMillis
        assertEquals("2026-09-21", schoolDateAt(verified))
        assertEquals(week, currentTeachingWeek(listOf(week), "2026-09-21", verified))
        assertNull(currentTeachingWeek(listOf(week), "2026-09-22", verified))
        assertNull(currentTeachingWeek(listOf(week), "2026-09-21", null))
    }

    @Test fun ambiguousOrMissingCurrentFlagsDoNotInventACurrentWeek() {
        assertNull(currentTeachingWeek(listOf(previous, next.copy(isCurrent = true)), "2026-09-28", null))
        assertNull(currentTeachingWeek(listOf(next), "2026-09-28", null))
        assertNull(currentTeachingWeek(emptyList(), "2026-09-28", null))
    }

    @Test fun invalidOrPartialRangesCannotConfirmAnOutOfRangeDate() {
        assertNull(currentTeachingWeek(listOf(previous.copy(startDate = "invalid")), "2026-09-21", null))
        assertNull(currentTeachingWeek(listOf(previous.copy(startDate = null)), "2026-09-28", null))
        assertNull(currentTeachingWeek(listOf(previous), "2026-09-20", null))
    }
}

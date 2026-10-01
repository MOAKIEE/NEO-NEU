package edu.neu.campus.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekCacheRetentionTest {
    private val term = "2026-2027-1"

    @Test fun keepsNewestWeeksAndExpiresOlderOnes() {
        val stored = listOf(
            "table:$term:9", "table:$term:8", "table:$term:7", "table:$term:6", "table:$term:5"
        )
        assertEquals(listOf("table:$term:6", "table:$term:5"), WeekCacheRetention.expired(stored, term, 3))
    }

    @Test fun wholeTermPayloadNeverExpiresEvenWhenWholeTermListIsLong() {
        val stored = listOf("table:$term:4", "table:$term:null", "table:$term:3", "table:$term:2")
        val expired = WeekCacheRetention.expired(stored, term, 1)
        assertEquals(listOf("table:$term:3", "table:$term:2"), expired)
        assertTrue(expired.none { it == "table:$term:null" })
    }

    @Test fun otherTermsAndKeysAreNotEligible() {
        val stored = listOf(
            "table:$term:4", "table:2026-2027-10:4", "table:2025-2026-2:4",
            "table:$term:null", "terms", "campuses:$term", "weeks:$term", "table:$term-extra:4"
        )
        assertTrue(WeekCacheRetention.expired(stored, term, 1).isEmpty())
    }

    @Test fun nothingExpiresWithinBudget() {
        assertTrue(WeekCacheRetention.expired(listOf("table:$term:2", "table:$term:1"), term, 6).isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroRetentionIsRejected() {
        WeekCacheRetention.expired(listOf("table:$term:1"), term, 0)
    }
}

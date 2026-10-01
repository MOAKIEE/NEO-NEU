package edu.neu.campus.database

/**
 * Decides which stored timetable payloads exceed the offline retention budget.
 *
 * Only one term's per-week keys are eligible. The whole-term payload (`table:<term>:null`) feeds the
 * 作息 view and is never expired, and the trailing separator keeps a term id from matching another term
 * whose id merely starts with the same text.
 */
internal object WeekCacheRetention {
    const val WholeTermWeek = "null"

    /** @param storedNewestFirst one account scope's keys, most recently saved first. */
    fun expired(storedNewestFirst: List<String>, termId: String, keep: Int): List<String> {
        require(keep > 0) { "Retention must keep at least one week" }
        val prefix = "table:$termId:"
        val wholeTermKey = prefix + WholeTermWeek
        return storedNewestFirst
            .filter { it.startsWith(prefix) && it != wholeTermKey }
            .drop(keep)
    }
}

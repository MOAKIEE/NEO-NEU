package edu.neu.campus.contract

import org.junit.Assert.*
import org.junit.Test

class SnapshotTransitionsTest {
    @Test fun failedRefreshRetainsLastSuccessfulValueAndTime() {
        val ready = SnapshotTransitions.succeeded(listOf("cached"), 123L)
        val loading = SnapshotTransitions.loading(ready)
        val failed = SnapshotTransitions.failed(loading, QueryError(QueryErrorKind.AUTH_REQUIRED, "认证过期", true))
        assertEquals(listOf("cached"), loading.data)
        assertEquals(listOf("cached"), failed.data)
        assertEquals(123L, failed.lastSuccessEpochMillis)
        assertNotNull(failed.lastAttemptEpochMillis)
        assertTrue(failed.isStale)
        assertEquals(QueryPhase.FAILED, failed.phase)
    }

    @Test fun verifiedEmptyResultIsDataButFailureWithoutCacheIsNot() {
        val empty = SnapshotTransitions.succeeded(emptyList<String>(), 123L)
        val failed = SnapshotTransitions.failed(QuerySnapshot<List<String>>(), QueryError(QueryErrorKind.NETWORK, "网络失败", true))
        assertNotNull(empty.data)
        assertEquals(123L, empty.lastAttemptEpochMillis)
        assertNull(failed.data)
    }
}

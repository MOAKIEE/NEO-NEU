package edu.neu.campus.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisibleEventGateTest {
    @Test fun navigationAndResumeShareRoundButRealReentryAndManualRun() {
        val gate = VisibleEventGate()
        assertTrue(gate.shouldRun("scope|today", SyncReason.COLD_START, 1000))
        assertFalse(gate.shouldRun("scope|today", SyncReason.FOREGROUND, 1100))
        assertTrue(gate.shouldRun("scope|grades", SyncReason.PAGE_ENTER, 1200))
        assertTrue(gate.shouldRun("scope|today", SyncReason.PAGE_ENTER, 1300))
        assertTrue(gate.shouldRun("scope|today", SyncReason.MANUAL, 1301))
        assertTrue(gate.shouldRun("scope|today", SyncReason.FOREGROUND, 2302))
    }
}

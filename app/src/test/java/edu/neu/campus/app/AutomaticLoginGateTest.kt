package edu.neu.campus.app

import edu.neu.campus.contract.Domain
import org.junit.Assert.*
import org.junit.Test

class AutomaticLoginGateTest {
    @Test fun repeatedEventsShareOneAttemptUntilCooldownExpires() {
        val gate = AutomaticLoginGate()
        assertTrue(gate.begin("scope", Domain.PORTAL, 0))
        assertFalse(gate.begin("scope", Domain.PORTAL, 59_999))
        assertTrue(gate.begin("scope", Domain.PORTAL, 60_000))
    }

    @Test fun interactiveFailureWaitsForUserEvenAfterCooldown() {
        val gate = AutomaticLoginGate()
        gate.begin("scope", Domain.PORTAL, 0)
        gate.pause("scope", Domain.PORTAL)
        assertFalse(gate.begin("scope", Domain.PORTAL, 600_000))
        assertTrue(gate.begin("scope", Domain.ACADEMIC, 600_000))
    }

    @Test fun accountChangeResetsRecoveryButOldResultCannotPauseNewAccount() {
        val gate = AutomaticLoginGate()
        gate.begin("old", Domain.PORTAL, 0)
        gate.pause("old", Domain.PORTAL)
        assertTrue(gate.begin("new", Domain.PORTAL, 1))
        gate.pause("old", Domain.PORTAL)
        assertTrue(gate.begin("new", Domain.PORTAL, 60_001))
    }
}

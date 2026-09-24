package edu.neu.campus.app

import edu.neu.campus.contract.Domain
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCoordinatorTest {
    @Test fun visibleRecoveryAndOriginalReplayAreLimitedToOne() {
        val gate = AuthCoordinator()
        assertTrue(gate.begin(Domain.ACADEMIC))
        assertFalse(gate.begin(Domain.ACADEMIC))
        assertTrue(gate.suppressResume())
        assertTrue(gate.complete(targetVerified = true))
        assertFalse(gate.complete(targetVerified = true))
        assertFalse(gate.begin(Domain.PORTAL))
        gate.replayFinished()
        assertFalse(gate.suppressResume())
        assertTrue(gate.begin(Domain.PORTAL))
        assertFalse(gate.complete(targetVerified = false))
    }
}

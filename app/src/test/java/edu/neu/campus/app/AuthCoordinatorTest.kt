package edu.neu.campus.app

import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthCoordinatorTest {
    @Test fun portalSessionRoutesAcademicRecoveryToAcademic() {
        val session = SessionState("scope", DomainStatus.READY, DomainStatus.EXPIRED)
        assertEquals(Domain.ACADEMIC, preferredLoginDomain(session, academicPage = false))
        assertEquals(Domain.ACADEMIC, preferredLoginDomain(
            session.copy(academic = DomainStatus.UNREACHABLE), academicPage = false
        ))
    }

    @Test fun expiredPortalRoutesRecoveryToPortal() {
        val session = SessionState("scope", DomainStatus.EXPIRED, DomainStatus.EXPIRED)
        assertEquals(Domain.PORTAL, preferredLoginDomain(session, academicPage = true))
    }

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

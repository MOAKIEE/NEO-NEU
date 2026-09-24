package edu.neu.campus.authweb

import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.SessionState
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginFlowTest {
    private fun state(portal: DomainStatus, academic: DomainStatus) =
        SessionState("scope", portal, academic)

    @Test fun portalLoginOpensAcademicOnce() {
        val result = state(DomainStatus.READY, DomainStatus.EXPIRED)
        assertEquals(LoginStep.OPEN_ACADEMIC, nextLoginStep(Domain.PORTAL, result, 0, false, true))
        assertEquals(LoginStep.WAIT, nextLoginStep(Domain.PORTAL, result, 1, true, true))
        assertEquals(LoginStep.FINISH, nextLoginStep(Domain.PORTAL, result, 1, true, false))
    }

    @Test fun bothDomainsReadyReturnAutomatically() {
        val result = state(DomainStatus.READY, DomainStatus.READY)
        assertEquals(LoginStep.FINISH, nextLoginStep(Domain.PORTAL, result, 1, true, true))
    }

    @Test fun academicRecoveryOnlyRequiresAcademic() {
        val result = state(DomainStatus.EXPIRED, DomainStatus.READY)
        assertEquals(LoginStep.FINISH, nextLoginStep(Domain.ACADEMIC, result, 1, false, true))
    }

    @Test fun unauthenticatedPortalDoesNotHandoff() {
        val result = state(DomainStatus.EXPIRED, DomainStatus.EXPIRED)
        assertEquals(LoginStep.WAIT, nextLoginStep(Domain.PORTAL, result, 0, false, true))
    }
}

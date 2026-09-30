package edu.neu.campus.authweb

import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.SessionState
import edu.neu.campus.session.SavedLoginStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginFlowTest {
    @Test fun savedEntryAttemptsOnceDespiteRecompositionAndPauseResumeChanges() {
        val gate = SavedLoginEntryGate()
        assertTrue(gate.claim(SavedLoginStatus.ENABLED, false, false, false))
        assertFalse(gate.claim(SavedLoginStatus.PAUSED, false, false, false))
        assertFalse(gate.claim(SavedLoginStatus.ENABLED, false, false, false))
    }

    @Test fun pausedRejectedEditedAndChallengeEntriesDoNotSubmitAutomatically() {
        assertFalse(SavedLoginEntryGate().claim(SavedLoginStatus.PAUSED, false, false, false))
        assertFalse(SavedLoginEntryGate().claim(SavedLoginStatus.ENABLED, false, false, true))
        assertFalse(SavedLoginEntryGate().claim(SavedLoginStatus.ENABLED, true, false, false))
        assertFalse(SavedLoginEntryGate().claim(SavedLoginStatus.ENABLED, false, true, false))
        val gate = SavedLoginEntryGate()
        assertFalse(gate.claim(SavedLoginStatus.NONE, false, false, false))
        assertFalse(gate.claim(SavedLoginStatus.ENABLED, false, false, false))
    }
    @Test fun transientUnknownPageDoesNotInterruptDelayedRedirectOrDomInitialization() {
        val gate = UnknownLoginPageGate()
        assertFalse(gate.ready("unsupported", 1, 0))
        assertFalse(gate.ready("null", 1, 1_000))
        assertFalse(gate.ready("loading", 2, 1_200))
        assertFalse(gate.ready("unsupported", 2, 1_600))
        assertFalse(gate.ready("form", 2, 1_800))
        assertFalse(gate.ready("unsupported", 2, 2_000))
        assertFalse(gate.ready("unsupported", 2, 3_499))
        assertTrue(gate.ready("unsupported", 2, 3_500))
        assertFalse(gate.ready("unsupported", 3, 3_600))
    }

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

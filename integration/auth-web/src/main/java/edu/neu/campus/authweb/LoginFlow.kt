package edu.neu.campus.authweb

import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.SessionState

internal enum class LoginStep { WAIT, OPEN_ACADEMIC, FINISH }

internal fun nextLoginStep(
    target: Domain,
    state: SessionState,
    selectedSite: Int,
    attemptedAcademicHandoff: Boolean,
    automatic: Boolean
): LoginStep {
    val portalReady = state.portal == DomainStatus.READY
    val academicReady = state.academic == DomainStatus.READY
    if (target == Domain.PORTAL && portalReady && !academicReady &&
        selectedSite == 0 && !attemptedAcademicHandoff) return LoginStep.OPEN_ACADEMIC
    val targetReady = if (target == Domain.PORTAL) portalReady else academicReady
    return if (targetReady && (!automatic || target == Domain.ACADEMIC || academicReady)) {
        LoginStep.FINISH
    } else LoginStep.WAIT
}

package edu.neu.campus.authweb

import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.SessionState
import edu.neu.campus.session.SavedLoginStatus

/** Decide once per entry, before status changes from our own attempt can retrigger login. */
internal class SavedLoginEntryGate {
    private var considered = false

    fun claim(status: SavedLoginStatus, edited: Boolean, hasChallenge: Boolean, rejected: Boolean): Boolean {
        if (considered) return false
        considered = true
        return status == SavedLoginStatus.ENABLED && !edited && !hasChallenge && !rejected
    }
}

internal enum class LoginStep { WAIT, OPEN_ACADEMIC, FINISH }

/** A second visit to CAS after leaving it may select a different account. */
internal class InteractiveLoginGate {
    private var visitedCas = false
    private var leftCas = false

    fun requiresFreshSession(host: String?): Boolean {
        if (host == "pass.neu.edu.cn") {
            val restart = visitedCas && leftCas
            visitedCas = true
            leftCas = false
            return restart
        }
        if (visitedCas) leftCas = true
        return false
    }
}

/** onPageFinished can precede a scripted SSO redirect or late DOM initialization. */
internal class UnknownLoginPageGate {
    private var navigation: Int? = null
    private var since = 0L

    fun ready(state: String, revision: Int, now: Long): Boolean {
        if (state != "unsupported" && state != "null") {
            navigation = null
            return false
        }
        if (navigation != revision) {
            navigation = revision
            since = now
        }
        return now - since >= 1_500
    }
}

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

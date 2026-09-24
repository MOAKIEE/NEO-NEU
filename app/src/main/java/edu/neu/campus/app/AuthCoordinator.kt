package edu.neu.campus.app

import edu.neu.campus.contract.Domain

/** Tracks one visible recovery attempt and permits one replay after its target domain verifies. */
class AuthCoordinator {
    private var recovering: Domain? = null
    private var replayPending = false

    @Synchronized fun begin(domain: Domain): Boolean {
        if (recovering != null || replayPending) return false
        recovering = domain
        return true
    }

    @Synchronized fun complete(targetVerified: Boolean): Boolean {
        if (recovering == null) return false
        recovering = null
        replayPending = targetVerified
        return targetVerified
    }

    @Synchronized fun replayFinished() { replayPending = false }

    @Synchronized fun suppressResume(): Boolean = recovering != null || replayPending

    @Synchronized fun pendingDomainName(): String? = recovering?.name

    @Synchronized fun restorePending(name: String?) {
        if (recovering == null && !name.isNullOrBlank()) {
            recovering = runCatching { Domain.valueOf(name) }.getOrNull()
        }
    }
}

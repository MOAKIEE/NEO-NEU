package edu.neu.campus.network

import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.contract.SessionState
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

object SessionProbe {
    private val verifyLock = Mutex()
    private var sharedSession: LocalSession? = null
    private var sharedHttp: SchoolHttp? = null

    @Synchronized private fun clientFor(session: LocalSession): SchoolHttp {
        if (sharedSession !== session || sharedHttp == null) {
            sharedSession = session
            sharedHttp = SchoolHttp(session)
        }
        return sharedHttp!!
    }

    /** Service routing needs the personal portal, without waiting for an unrelated academic probe. */
    suspend fun verifyPortal(session: LocalSession, http: SchoolHttp = clientFor(session)): SessionState = verifyLock.withLock {
        val scope = session.state.value.accountScope ?: return@withLock session.state.value
        var account: String? = null
        val status = probe {
            val root = JSONObject(http.execute(SchoolCall.PORTAL_INFO))
            when (root.optInt("e", -1)) {
                0 -> {
                    account = root.optJSONObject("d")?.optJSONObject("info")?.optString("xgh")?.takeIf { it.isNotBlank() }
                    if (root.optJSONObject("d") != null) DomainStatus.READY else DomainStatus.UNREACHABLE
                }
                10013 -> DomainStatus.EXPIRED
                else -> DomainStatus.UNREACHABLE
            }
        }
        if (session.state.value.accountScope == scope) {
            session.markIfScope(scope, Domain.PORTAL, status)
            if (status == DomainStatus.READY) session.confirmSavedAccount(scope, account)
        }
        session.state.value
    }

    suspend fun verify(session: LocalSession, http: SchoolHttp = clientFor(session)): SessionState = verifyLock.withLock {
        val scope = session.state.value.accountScope ?: return@withLock session.state.value
        var portalAccount: String? = null
        val (portal, academic) = coroutineScope {
            val portal = async {
                probe {
                    val root = JSONObject(http.execute(SchoolCall.PORTAL_INFO))
                    when (root.optInt("e", -1)) {
                        0 -> {
                            portalAccount = root.optJSONObject("d")?.optJSONObject("info")
                                ?.optString("xgh")?.takeIf { it.isNotBlank() }
                            if (root.optJSONObject("d") != null) DomainStatus.READY else DomainStatus.UNREACHABLE
                        }
                        10013 -> DomainStatus.EXPIRED
                        else -> DomainStatus.UNREACHABLE
                    }
                }
            }
            val academic = async {
                probe {
                    val root = JSONObject(http.execute(SchoolCall.CURRENT_TERM,
                        mapOf("CSDM" to "SYS", "ZCSDM" to "DQXNXQDM", "SFSY" to "1")))
                    if (root.optString("code") == "0" && root.optJSONObject("datas") != null) {
                        DomainStatus.READY
                    } else DomainStatus.UNREACHABLE
                }
            }
            portal.await() to academic.await()
        }
        if (session.state.value.accountScope != scope) return@withLock session.state.value
        session.completeVerification(scope, portal, academic)
        // Reuse this probe's account field, including after a cancelled attempt or process restart.
        // Missing/mismatched fields cannot silently re-enable a saved password.
        if (portal == DomainStatus.READY) session.confirmSavedAccount(scope, portalAccount)
        session.state.value
    }

    private suspend fun probe(block: suspend () -> DomainStatus): DomainStatus = try {
        block()
    } catch (error: SchoolHttpException) {
        when (error.safeError.kind) {
            QueryErrorKind.AUTH_REQUIRED -> DomainStatus.EXPIRED
            else -> DomainStatus.UNREACHABLE
        }
    } catch (error: CancellationException) {
        throw error
    } catch (_: Exception) { DomainStatus.UNREACHABLE }

}

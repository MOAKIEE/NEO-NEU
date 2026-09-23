package edu.neu.campus.network

import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.contract.SessionState
import edu.neu.campus.session.LocalSession
import org.json.JSONObject

object SessionProbe {
    suspend fun verify(session: LocalSession, http: SchoolHttp = SchoolHttp(session)): SessionState {
        if (session.state.value.accountScope == null) return session.state.value
        val portal = probe {
            val root = JSONObject(http.execute(SchoolCall.PORTAL_INFO))
            root.optInt("e", -1) == 0 && root.optJSONObject("d") != null
        }
        session.mark(Domain.PORTAL, portal)
        val academic = probe {
            val root = JSONObject(http.execute(SchoolCall.CURRENT_TERM,
                mapOf("CSDM" to "SYS", "ZCSDM" to "DQXNXQDM", "SFSY" to "1")))
            root.optString("code") == "0" && root.optJSONObject("datas") != null
        }
        session.mark(Domain.ACADEMIC, academic)
        return session.state.value
    }

    private suspend fun probe(block: suspend () -> Boolean): DomainStatus = try {
        if (block()) DomainStatus.READY else DomainStatus.UNREACHABLE
    } catch (error: SchoolHttpException) {
        when (error.safeError.kind) {
            QueryErrorKind.AUTH_REQUIRED -> DomainStatus.EXPIRED
            else -> DomainStatus.UNREACHABLE
        }
    } catch (_: Exception) { DomainStatus.UNREACHABLE }
}

package edu.neu.campus.session

import android.content.Context
import android.webkit.CookieManager
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.SessionRepository
import edu.neu.campus.contract.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** The scope is a random local partition, never a username or a school token. */
class LocalSession(context: Context) : SessionRepository {
    companion object {
        @Volatile private var instance: LocalSession? = null
        fun get(context: Context): LocalSession = instance ?: synchronized(this) {
            instance ?: LocalSession(context.applicationContext).also { instance = it }
        }
    }
    private val preferences = context.applicationContext.getSharedPreferences("session_v1", Context.MODE_PRIVATE)
    private val cookies = CookieManager.getInstance().apply { setAcceptCookie(true) }
    private val savedScope = preferences.getString("scope", null)
    private val mutableState = MutableStateFlow(
        SessionState(savedScope,
            if (savedScope == null) DomainStatus.SIGNED_OUT else DomainStatus.EXPIRED,
            if (savedScope == null) DomainStatus.SIGNED_OUT else DomainStatus.EXPIRED)
    )
    override val state: StateFlow<SessionState> = mutableState
    private val scopeListeners = mutableListOf<(String?) -> Unit>()
    @Synchronized fun addScopeListener(listener: (String?) -> Unit) { scopeListeners += listener }

    @Synchronized fun beginLogin() {
        val scope = UUID.randomUUID().toString()
        preferences.edit().putString("scope", scope).apply()
        mutableState.value = SessionState(scope, DomainStatus.AUTHENTICATING, DomainStatus.AUTHENTICATING)
        scopeListeners.forEach { it(scope) }
    }

    @Synchronized fun mark(domain: Domain, status: DomainStatus) {
        val old = mutableState.value
        mutableState.value = when (domain) {
            Domain.PORTAL -> old.copy(portal = status)
            Domain.ACADEMIC -> old.copy(academic = status)
        }
    }

    fun cookieHeader(url: String): String? = cookies.getCookie(url)?.takeIf { it.isNotBlank() }
    fun acceptSetCookie(url: String, value: String) {
        cookies.setCookie(url, value)
        cookies.flush()
    }

    override suspend fun signOut() {
        suspendCancellableCoroutine<Unit> { continuation ->
            cookies.removeAllCookies { if (continuation.isActive) continuation.resume(Unit) }
        }
        cookies.flush()
        preferences.edit().remove("scope").apply()
        mutableState.value = SessionState(null, DomainStatus.SIGNED_OUT, DomainStatus.SIGNED_OUT)
        synchronized(this) { scopeListeners.forEach { it(null) } }
    }
}

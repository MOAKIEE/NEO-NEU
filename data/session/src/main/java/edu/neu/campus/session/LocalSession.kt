package edu.neu.campus.session

import android.content.Context
import android.webkit.CookieManager
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.SessionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/** The scope is a random local partition, never a username or a school token. */
class LocalSession(context: Context) {
    companion object {
        @Volatile private var instance: LocalSession? = null
        fun get(context: Context): LocalSession = instance ?: synchronized(this) {
            instance ?: LocalSession(context.applicationContext).also { instance = it }
        }
    }
    private val preferences = context.applicationContext.getSharedPreferences("session_v1", Context.MODE_PRIVATE)
    private val cookies = CookieManager.getInstance().apply { setAcceptCookie(true) }
    private val cookieWrites = Mutex()
    private val savedScope = preferences.getString("scope", null)
    private val mutableState = MutableStateFlow(
        SessionState(savedScope,
            if (savedScope == null) DomainStatus.SIGNED_OUT else DomainStatus.UNVERIFIED,
            if (savedScope == null) DomainStatus.SIGNED_OUT else DomainStatus.UNVERIFIED)
    )
    val state: StateFlow<SessionState> = mutableState
    private val scopeListeners = mutableListOf<(String?) -> Unit>()
    @Synchronized fun addScopeListener(listener: (String?) -> Unit) { scopeListeners += listener }

    suspend fun beginLogin() = cookieWrites.withLock {
        val hadScope = state.value.accountScope != null
        if (!hadScope) {
            // A fresh login cannot inherit cookies left by an interrupted sign-out.
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    cookies.removeAllCookies { if (continuation.isActive) continuation.resume(Unit) }
                }
                cookies.flush()
            }
        }
        // Rotating the local scope isolates old cached data if the official page switches accounts.
        // Existing CAS and business cookies remain available for SSO during recovery.
        synchronized(this) {
            val scope = UUID.randomUUID().toString()
            preferences.edit().putString("scope", scope).apply()
            mutableState.value = SessionState(scope, DomainStatus.AUTHENTICATING, DomainStatus.AUTHENTICATING)
            scopeListeners.forEach { it(scope) }
        }
    }

    @Synchronized fun mark(domain: Domain, status: DomainStatus) {
        val old = mutableState.value
        mutableState.value = when (domain) {
            Domain.PORTAL -> old.copy(portal = status)
            Domain.ACADEMIC -> old.copy(academic = status)
        }
    }

    @Synchronized fun markIfScope(scope: String, domain: Domain, status: DomainStatus) {
        if (mutableState.value.accountScope == scope) mark(domain, status)
    }

    /** Publish both probe results together, unless the scope changed while probing. */
    @Synchronized fun completeVerification(
        scope: String,
        portal: DomainStatus,
        academic: DomainStatus
    ): SessionState {
        val old = mutableState.value
        if (old.accountScope != scope) return old
        return old.copy(portal = portal, academic = academic).also { mutableState.value = it }
    }

    suspend fun cookieHeader(url: String): String? = cookieWrites.withLock {
        withContext(Dispatchers.Main.immediate) { cookies.getCookie(url)?.takeIf { it.isNotBlank() } }
    }

    /** The callback precedes flush, and the scope is checked under the same write lock as logout. */
    suspend fun acceptSetCookies(scope: String, url: String, values: List<String>) = cookieWrites.withLock {
        if (values.isEmpty() || state.value.accountScope != scope) return@withLock
        withContext(Dispatchers.Main.immediate) {
            writeCookiesInOrder(values, write = { value ->
                if (state.value.accountScope != scope) throw CancellationException("Account scope changed")
                suspendCancellableCoroutine<Unit> { continuation ->
                    cookies.setCookie(url, value) { if (continuation.isActive) continuation.resume(Unit) }
                }
            }, flush = { cookies.flush() })
        }
    }

    suspend fun flushCookies() = cookieWrites.withLock {
        withContext(Dispatchers.Main.immediate) { cookies.flush() }
    }

    suspend fun signOut() {
        synchronized(this) {
            preferences.edit().remove("scope").apply()
            mutableState.value = SessionState(null, DomainStatus.SIGNED_OUT, DomainStatus.SIGNED_OUT)
            scopeListeners.forEach { it(null) }
        }
        cookieWrites.withLock {
            if (state.value.accountScope != null) return@withLock
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    cookies.removeAllCookies { if (continuation.isActive) continuation.resume(Unit) }
                }
                cookies.flush()
            }
        }
    }
}

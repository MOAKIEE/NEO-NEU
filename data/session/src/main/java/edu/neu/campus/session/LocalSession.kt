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
import kotlinx.coroutines.NonCancellable
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
    private val savedCredentials = SavedSchoolCredentials(context)
    val savedLoginStatus: StateFlow<SavedLoginStatus> = savedCredentials.status
    private val scopeListeners = mutableListOf<(String?) -> Unit>()
    private var sessionGeneration = 0L
    @Synchronized fun addScopeListener(listener: (String?) -> Unit) { scopeListeners += listener }

    suspend fun beginLogin(keepCredentials: Boolean = false, clearCookies: Boolean = false,
        expectedScope: String? = null, newCredentials: SchoolCredentials? = null): String = cookieWrites.withLock {
        require(!keepCredentials || newCredentials == null)
        if (expectedScope != null && state.value.accountScope != expectedScope) {
            throw CancellationException("Account scope changed")
        }
        val generation = synchronized(this) { sessionGeneration }
        val previousScope = state.value.accountScope
        val credentials = newCredentials ?: if (keepCredentials && previousScope != null) {
            withContext(Dispatchers.IO) { savedCredentials.read(previousScope, includePaused = true) }
        } else null
        val hadScope = state.value.accountScope != null
        if (!hadScope || clearCookies) {
            // A fresh login cannot inherit cookies left by an interrupted sign-out.
            withContext(Dispatchers.Main.immediate) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    cookies.removeAllCookies { if (continuation.isActive) continuation.resume(Unit) }
                }
            }
            withContext(Dispatchers.IO) { cookies.flush() }
        }
        // Rotating the local scope isolates old cached data if the official page switches accounts.
        // Existing CAS and business cookies remain available for SSO during recovery.
        // Once published, the new scope and its encrypted credentials must commit together,
        // even if the owning screen rotates or goes into the background during the IO write.
        withContext(NonCancellable) {
            val nextScope = synchronized(this) {
                if (sessionGeneration != generation) throw CancellationException("Account scope changed")
                sessionGeneration++
                val scope = UUID.randomUUID().toString()
                preferences.edit().putString("scope", scope).apply()
                mutableState.value = SessionState(scope, DomainStatus.AUTHENTICATING, DomainStatus.AUTHENTICATING)
                scopeListeners.forEach { it(scope) }
                scope
            }
            withContext(Dispatchers.IO) {
                if (state.value.accountScope != nextScope) throw CancellationException("Account scope changed")
                if (credentials != null) savedCredentials.save(nextScope, credentials, paused = newCredentials == null)
                else savedCredentials.clear()
            }
            if (state.value.accountScope != nextScope) throw CancellationException("Account scope changed")
            nextScope
        }
    }

    suspend fun saveCredentials(scope: String, credentials: SchoolCredentials): Boolean = cookieWrites.withLock {
        if (state.value.accountScope != scope) return@withLock false
        withContext(Dispatchers.IO) { savedCredentials.save(scope, credentials) }
        state.value.accountScope == scope
    }

    suspend fun readCredentials(scope: String, includePaused: Boolean = false): SchoolCredentials? = cookieWrites.withLock {
        if (state.value.accountScope != scope) return@withLock null
        val credentials = withContext(Dispatchers.IO) { savedCredentials.read(scope, includePaused) }
        credentials.takeIf { state.value.accountScope == scope }
    }

    suspend fun pauseAutomaticLogin(scope: String) = cookieWrites.withLock {
        if (state.value.accountScope == scope) withContext(Dispatchers.IO) { savedCredentials.pause() }
    }

    suspend fun confirmSavedAccount(scope: String, account: String?) = cookieWrites.withLock {
        if (state.value.accountScope != scope) return@withLock
        withContext(Dispatchers.IO) {
            val credentials = savedCredentials.read(scope, includePaused = true) ?: return@withContext
            when {
                account.isNullOrBlank() -> savedCredentials.pause()
                credentials.account == account -> savedCredentials.enable()
                else -> savedCredentials.clear()
            }
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
            }, flush = { withContext(Dispatchers.IO) { cookies.flush() } })
        }
    }

    suspend fun flushCookies() = cookieWrites.withLock {
        withContext(Dispatchers.IO) { cookies.flush() }
    }

    suspend fun signOut() {
        synchronized(this) {
            sessionGeneration++
            preferences.edit().remove("scope").apply()
            mutableState.value = SessionState(null, DomainStatus.SIGNED_OUT, DomainStatus.SIGNED_OUT)
            scopeListeners.forEach { it(null) }
        }
        cookieWrites.withLock {
            if (state.value.accountScope != null) return@withLock
            try {
                withContext(Dispatchers.IO) { savedCredentials.clear() }
            } finally {
                try {
                    withContext(Dispatchers.Main.immediate) {
                        suspendCancellableCoroutine<Unit> { continuation ->
                            cookies.removeAllCookies { if (continuation.isActive) continuation.resume(Unit) }
                        }
                    }
                } finally {
                    withContext(Dispatchers.IO) { cookies.flush() }
                }
            }
        }
    }
}

package edu.neu.campus.authweb

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.FrameLayout
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.network.SessionProbe
import edu.neu.campus.session.LocalSession
import edu.neu.campus.session.SavedLoginStatus
import edu.neu.campus.session.SchoolCredentials
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

sealed interface LoginResult {
    data object Connected : LoginResult
    data class ContinueOnWeb(val token: String) : LoginResult
    data class NeedCredentials(val rejected: Boolean = false) : LoginResult
    data class Failed(val message: String) : LoginResult
}

/** Entry points expose login state without exposing cookies or credentials to business pages. */
object OfficialLogin {
    fun intent(context: Context, domain: Domain = Domain.PORTAL, continuation: String? = null,
        credentialRejected: Boolean = false, silentAttempted: Boolean = false): Intent =
        Intent(context, OfficialLoginActivity::class.java).putExtra("target_domain", domain.name)
            .putExtra("continuation", continuation)
            .putExtra("credential_rejected", credentialRejected)
            .putExtra("silent_attempted", silentAttempted)

    fun savedLoginStatus(context: Context): StateFlow<SavedLoginStatus> = LocalSession.get(context).savedLoginStatus
    suspend fun verifyExisting(context: Context) = SessionProbe.verify(LocalSession.get(context))
    suspend fun recover(activity: Activity, domain: Domain): LoginResult = SchoolLogin.recover(activity, domain)
    fun discardContinuation(token: String) = PendingLogin.discard(token)
}

/** Tokens refer to an in-memory browser only; no password, cookie or URL enters an Intent. */
internal object PendingLogin {
    private val handler = Handler(Looper.getMainLooper())
    private val pending = mutableMapOf<String, LoginBrowser>()

    fun put(browser: LoginBrowser): String {
        pending.values.forEach { it.destroy() }
        pending.clear()
        browser.park()
        val token = UUID.randomUUID().toString()
        pending[token] = browser
        handler.postDelayed({ discard(token) }, 120_000)
        return token
    }

    fun take(token: String?): LoginBrowser? = pending.remove(token)
    fun discard(token: String) { pending.remove(token)?.destroy() }
}

internal object SchoolLogin {
    internal val lock = Mutex()

    suspend fun saveAndConnect(activity: Activity, target: Domain, credentials: SchoolCredentials): LoginResult = lock.withLock {
        val session = LocalSession.get(activity)
        var scope: String? = null
        try {
            val savedScope = session.beginLogin(clearCookies = true, newCredentials = credentials)
            scope = savedScope
            attempt(activity, target, session, savedScope)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            LoginResult.Failed(LoginFailure.STORAGE.message)
        } finally {
            scope?.let { current ->
                if (session.state.value.accountScope == current) {
                    if (session.state.value.portal == DomainStatus.AUTHENTICATING) session.markIfScope(current, Domain.PORTAL, DomainStatus.UNVERIFIED)
                    if (session.state.value.academic == DomainStatus.AUTHENTICATING) session.markIfScope(current, Domain.ACADEMIC, DomainStatus.UNVERIFIED)
                }
            }
        }
    }

    suspend fun recover(activity: Activity, target: Domain): LoginResult = lock.withLock {
        val session = LocalSession.get(activity)
        val scope = session.state.value.accountScope ?: return@withLock LoginResult.NeedCredentials()
        val status = if (target == Domain.PORTAL) session.state.value.portal else session.state.value.academic
        if (status == DomainStatus.READY) return@withLock LoginResult.Connected
        attempt(activity, target, session, scope)
    }

    private suspend fun attempt(activity: Activity, target: Domain, session: LocalSession, scope: String): LoginResult =
        withContext(Dispatchers.Main.immediate) {
            coroutineScope {
                val browser = LoginBrowser(activity, scope)
                browser.attachTo(activity, visible = false)
                val root = activity.findViewById<FrameLayout>(android.R.id.content)
                root.addView(browser.web, FrameLayout.LayoutParams(1, 1))
                var handedOff = false
                var submitted = false
                var lastPageState = "loading"
                val unknownPage = UnknownLoginPageGate()
                var academicHandoff = target == Domain.ACADEMIC
                var portalHandoff = target == Domain.PORTAL
                val watcher = launch {
                    session.state.first { it.accountScope != scope }
                    browser.destroy()
                }
                try {
                    browser.load(if (target == Domain.PORTAL) PORTAL_ENTRY else ACADEMIC_ENTRY)
                    val result = withTimeoutOrNull(30_000) {
                        var lastProbedUrl: String? = null
                        while (!browser.destroyed && session.state.value.accountScope == scope) {
                            browser.failure?.let { return@withTimeoutOrNull LoginResult.Failed(it.message) }
                            if (browser.loading) {
                                lastPageState = "loading"
                                unknownPage.ready("loading", browser.navigationRevision, SystemClock.elapsedRealtime())
                                delay(200); continue
                            }
                            val url = browser.web.url.orEmpty()
                            val host = Uri.parse(url).host
                            if ((host == "personal.neu.edu.cn" || host == "jwxt.neu.edu.cn") && lastProbedUrl != url) {
                                delay(500)
                                if (browser.loading || browser.web.url != url) continue
                                session.flushCookies()
                                val verified = SessionProbe.verify(session)
                                if (verified.accountScope != scope) return@withTimeoutOrNull LoginResult.NeedCredentials()
                                lastProbedUrl = url
                                val targetReady = if (target == Domain.PORTAL) verified.portal == DomainStatus.READY
                                    else verified.academic == DomainStatus.READY
                                if (targetReady) {
                                    if (target == Domain.PORTAL && verified.academic != DomainStatus.READY && !academicHandoff) {
                                        academicHandoff = true
                                        browser.load(ACADEMIC_ENTRY)
                                        continue
                                    }
                                    if (target == Domain.ACADEMIC && verified.portal != DomainStatus.READY && !portalHandoff) {
                                        portalHandoff = true
                                        browser.load(PORTAL_ENTRY)
                                        continue
                                    }
                                    return@withTimeoutOrNull LoginResult.Connected
                                }
                            }
                            val state = browser.inspect(submitted = submitted)
                            lastPageState = state
                            val unknownReady = unknownPage.ready(state, browser.navigationRevision, SystemClock.elapsedRealtime())
                            when (state) {
                                "form", "challenge" -> {
                                    val credentials = session.readCredentials(scope)
                                    if (credentials == null && state == "form") return@withTimeoutOrNull LoginResult.NeedCredentials()
                                    // Persist the stop before submitting: process death cannot blindly retry a rejected password.
                                    if (credentials != null) session.pauseAutomaticLogin(scope)
                                    val filled = browser.inspect(credentials, submit = state == "form", submitted = submitted)
                                    if (filled == "submitted" || filled == "loading" && state == "form" && credentials != null) {
                                        // Navigation can begin synchronously inside the school's click handler.
                                        submitted = true
                                    }
                                    if (filled == "challenge") {
                                        session.pauseAutomaticLogin(scope)
                                        handedOff = true
                                        return@withTimeoutOrNull LoginResult.ContinueOnWeb(PendingLogin.put(browser))
                                    }
                                    if (filled == "unsupported" || filled == "null") {
                                        delay(300)
                                        continue
                                    }
                                    if (filled == "rejected") return@withTimeoutOrNull LoginResult.NeedCredentials(rejected = true)
                                }
                                "rejected" -> {
                                    session.pauseAutomaticLogin(scope)
                                    return@withTimeoutOrNull LoginResult.NeedCredentials(rejected = true)
                                }
                                "unsupported", "null" -> {
                                    // A partial portal connection remains usable if only academic SSO is unavailable.
                                    if (target == Domain.PORTAL && session.state.value.portal == DomainStatus.READY && academicHandoff) {
                                        return@withTimeoutOrNull LoginResult.Connected
                                    }
                                    if (target == Domain.ACADEMIC && session.state.value.academic == DomainStatus.READY && portalHandoff) {
                                        return@withTimeoutOrNull LoginResult.Connected
                                    }
                                    if (!unknownReady) { delay(300); continue }
                                    session.pauseAutomaticLogin(scope)
                                    handedOff = true
                                    return@withTimeoutOrNull LoginResult.ContinueOnWeb(PendingLogin.put(browser))
                                }
                            }
                            delay(300)
                        }
                        LoginResult.NeedCredentials()
                    }
                    result ?: if (session.state.value.accountScope == scope &&
                        (if (target == Domain.PORTAL) session.state.value.portal else session.state.value.academic) == DomainStatus.READY) LoginResult.Connected
                    else if (!browser.destroyed && session.state.value.accountScope == scope &&
                        submitted && lastPageState == "waiting" && !browser.loading && browser.failure == null) {
                        handedOff = true
                        LoginResult.ContinueOnWeb(PendingLogin.put(browser))
                    }
                    else LoginResult.Failed(LoginFailure.TIMEOUT.message)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    LoginResult.Failed(LoginFailure.NETWORK.message)
                } finally {
                    watcher.cancel()
                    if (!handedOff) browser.destroy()
                    // An interrupted initial attempt must not leave both domains AUTHENTICATING indefinitely.
                    if (session.state.value.accountScope == scope) {
                        if (session.state.value.portal == DomainStatus.AUTHENTICATING) session.markIfScope(scope, Domain.PORTAL, DomainStatus.EXPIRED)
                        if (session.state.value.academic == DomainStatus.AUTHENTICATING) session.markIfScope(scope, Domain.ACADEMIC, DomainStatus.EXPIRED)
                    }
                }
            }
        }
}

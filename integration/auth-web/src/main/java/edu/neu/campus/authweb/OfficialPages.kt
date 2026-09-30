package edu.neu.campus.authweb

import android.app.Activity
import android.net.Uri
import android.os.SystemClock
import android.webkit.WebView
import android.widget.FrameLayout
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.network.SchoolCall
import edu.neu.campus.network.SchoolHttp
import edu.neu.campus.network.SessionProbe
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface OfficialPageResult {
    data class Open(val page: PreparedOfficialPage) : OfficialPageResult
    data class Login(val result: LoginResult) : OfficialPageResult
    data class Failed(val message: String) : OfficialPageResult
}

/** UI owns presentation; this adapter owns school routes, sessions and authentication. */
class PreparedOfficialPage internal constructor(private val browser: LoginBrowser, mailboxLogin: Boolean = false) {
    var requiresMailboxLogin: Boolean = mailboxLogin
        private set
    val web: WebView get() = browser.web
    val host: String get() = Uri.parse(web.url.orEmpty()).host.orEmpty()
    val loading: Boolean get() = browser.loading
    val error: String? get() = browser.failure?.message
    private var transferred = false

    fun attach(activity: Activity, onChange: () -> Unit, onStarted: () -> Unit,
        onCommit: () -> Unit, onProgress: (Int) -> Unit, onLogin: (LoginResult) -> Unit) {
        browser.attachTo(activity, visible = true)
        browser.serviceVisible = true
        browser.onChange = onChange
        browser.onStarted = {
            requiresMailboxLogin = false
            onStarted()
            if (!transferred && Uri.parse(web.url.orEmpty()).host == "pass.neu.edu.cn") {
                // Conceal a newly navigated CAS page until cache isolation and FLAG_SECURE are applied.
                browser.attachTo(activity, visible = false)
                transferred = true
                browser.serviceVisible = false
                onLogin(LoginResult.ContinueOnWeb(PendingLogin.put(browser)))
            }
        }
        browser.onCommit = onCommit
        browser.onProgress = onProgress
        browser.onLoaded = {
            if (!transferred && Uri.parse(web.url.orEmpty()).host == "pass.neu.edu.cn") {
                transferred = true
                browser.serviceVisible = false
                onLogin(LoginResult.ContinueOnWeb(PendingLogin.put(browser)))
            }
        }
    }

    fun canGoBack(): Boolean = !transferred && web.canGoBack()
    fun goBack() { if (!transferred) web.goBack() }
    fun stop() { if (!transferred) web.stopLoading() }
    fun close() { if (!transferred) browser.destroy() }
}

object OfficialPages {
    private var http: SchoolHttp? = null
    private fun client(session: LocalSession): SchoolHttp = http ?: SchoolHttp(session).also { http = it }

    /** Native login and service login share a lock to prevent competing CAS submissions. */
    suspend fun prepare(activity: Activity, page: OfficialPage, viaPortal: Boolean = true,
        continuation: String? = null): OfficialPageResult = SchoolLogin.lock.withLock {
        withContext(Dispatchers.Main.immediate) {
            val session = LocalSession.get(activity)
            val scope = session.state.value.accountScope
                ?: return@withContext OfficialPageResult.Login(LoginResult.NeedCredentials())
            coroutineScope {
                val browser = if (continuation == null) LoginBrowser(activity, scope).apply { pageTarget = page }
                    else PendingLogin.take(continuation)
                        ?: return@coroutineScope OfficialPageResult.Failed("学校验证页面已失效，请重新连接。")
                if (browser.scope != scope || browser.pageTarget != page) {
                    browser.destroy()
                    return@coroutineScope OfficialPageResult.Failed("账号状态已变化，请重新连接。")
                }
                browser.serviceVisible = false
                browser.attachTo(activity, visible = false)
                activity.findViewById<FrameLayout>(android.R.id.content).addView(browser.web, FrameLayout.LayoutParams(1, 1))
                var retained = false
                val watcher = launch { session.state.first { it.accountScope != scope }; browser.destroy() }
                try {
                    var plan: PortalPagePlan? = if (viaPortal || page == OfficialPage.PORTAL || continuation != null) null else PortalPagePlan.resolve(page, null)
                    if (continuation == null) browser.load(plan?.authenticationEntry ?: PORTAL_ENTRY)
                    var destinationOpened = false
                    var submitted = browser.automaticSubmitted
                    var submissionAt = 0L
                    val result = withTimeoutOrNull(30_000) {
                        while (!browser.destroyed && session.state.value.accountScope == scope) {
                            browser.failure?.let { return@withTimeoutOrNull OfficialPageResult.Failed(it.message) }
                            if (browser.loading) { delay(200); continue }
                            val url = browser.web.url.orEmpty()
                            val host = Uri.parse(url).host
                            if (host == "personal.neu.edu.cn" && plan == null) {
                                delay(500)
                                if (browser.loading || browser.web.url != url) continue
                                session.flushCookies()
                                val verified = SessionProbe.verifyPortal(session)
                                if (verified.accountScope != scope) break
                                if (browser.loading || browser.web.url != url) continue
                                if (verified.portal != DomainStatus.READY) return@withTimeoutOrNull OfficialPageResult.Failed("个人门户连接未完成，请重试或重新登录。")
                                if (page == OfficialPage.PORTAL) {
                                    browser.web.clearHistory()
                                    retained = true
                                    return@withTimeoutOrNull OfficialPageResult.Open(PreparedOfficialPage(browser))
                                }
                                val catalogue = try {
                                    client(session).execute(SchoolCall.PORTAL_APPS, mapOf("limit" to "999", "type" to "all", "cate_id" to ""))
                                } catch (cancelled: CancellationException) { throw cancelled }
                                catch (_: Exception) { null }
                                if (session.state.value.accountScope != scope) break
                                plan = PortalPagePlan.resolve(page, catalogue)
                                browser.load(plan.authenticationEntry)
                                continue
                            }
                            val activePlan = plan
                            if ((activePlan != null || continuation != null) && page.cataloguePage.isDestination(url)) {
                                delay(1_000)
                                if (browser.loading || browser.web.url != url) continue
                                if (activePlan?.destination != null && !destinationOpened) {
                                    destinationOpened = true
                                    browser.load(activePlan.destination)
                                    continue
                                }
                                session.flushCookies()
                                val verified = SessionProbe.verifyPortal(session)
                                if (verified.accountScope != scope) break
                                if (browser.loading || browser.web.url != url) continue
                                val mailboxLogin = page == OfficialPage.STUDENT_MAIL && browser.hasMailboxLoginForm()
                                if (browser.loading || browser.web.url != url) continue
                                browser.web.clearHistory()
                                retained = true
                                return@withTimeoutOrNull OfficialPageResult.Open(PreparedOfficialPage(browser, mailboxLogin))
                            }
                            when (val state = browser.inspect(submitted = submitted)) {
                                "form", "challenge" -> {
                                    val credentials = session.readCredentials(scope)
                                    if (credentials == null && state == "form") return@withTimeoutOrNull OfficialPageResult.Login(LoginResult.NeedCredentials())
                                    session.pauseAutomaticLogin(scope)
                                    val filled = browser.inspect(credentials, submit = true, submitted = submitted)
                                    if (filled == "submitted" || filled == "loading") {
                                        submitted = true
                                        browser.automaticSubmitted = true
                                        submissionAt = SystemClock.elapsedRealtime()
                                    } else if (filled == "rejected") {
                                        return@withTimeoutOrNull OfficialPageResult.Login(LoginResult.NeedCredentials(rejected = true))
                                    } else if (filled in setOf("challenge", "unsupported", "null")) {
                                        retained = true
                                        return@withTimeoutOrNull OfficialPageResult.Login(LoginResult.ContinueOnWeb(PendingLogin.put(browser)))
                                    }
                                }
                                "rejected" -> {
                                    session.pauseAutomaticLogin(scope)
                                    return@withTimeoutOrNull OfficialPageResult.Login(LoginResult.NeedCredentials(rejected = true))
                                }
                                "waiting" -> if (submitted && SystemClock.elapsedRealtime() - submissionAt > 4_000) {
                                    retained = true
                                    return@withTimeoutOrNull OfficialPageResult.Login(LoginResult.ContinueOnWeb(PendingLogin.put(browser)))
                                }
                                "unsupported", "null" -> {
                                    session.pauseAutomaticLogin(scope)
                                    retained = true
                                    return@withTimeoutOrNull OfficialPageResult.Login(LoginResult.ContinueOnWeb(PendingLogin.put(browser)))
                                }
                            }
                            delay(300)
                        }
                        OfficialPageResult.Failed("账号状态已变化，请重新打开学校网页。")
                    }
                    result ?: OfficialPageResult.Failed(LoginFailure.TIMEOUT.message)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { OfficialPageResult.Failed(LoginFailure.NETWORK.message) }
                finally { watcher.cancel(); if (!retained) browser.destroy() }
            }
        }
    }
}

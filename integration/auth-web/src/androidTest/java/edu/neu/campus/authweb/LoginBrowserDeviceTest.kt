package edu.neu.campus.authweb

import android.widget.FrameLayout
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.neu.campus.session.SchoolCredentials
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.coroutines.resume

/** Synthetic same-origin HTML, no external scripts or requests and no real school account. */
@RunWith(AndroidJUnit4::class)
class LoginBrowserDeviceTest {
    private val html = """
        <html><body><form id="loginForm" method="post" action="/tpass/login">
        <input id="un"><input id="pd" type="password">
        <input type="button" id="index_login_btn" onclick="login()">
        </form><script>window.clickCount=0;window.login=function(){window.clickCount++;};</script></body></html>
    """.trimIndent()

    private fun scenario() = ActivityScenario.launch<OfficialLoginActivity>(OfficialLogin.intent(
        InstrumentationRegistry.getInstrumentation().targetContext))

    private suspend fun load(activity: OfficialLoginActivity, page: String = html,
        base: String = "https://pass.neu.edu.cn/tpass/login"): LoginBrowser {
        val loaded = CompletableDeferred<Unit>()
        val browser = withContext(Dispatchers.Main) {
            LoginBrowser(activity, "synthetic-scope").also {
                it.attachTo(activity, visible = false)
                activity.findViewById<FrameLayout>(android.R.id.content).addView(it.web, FrameLayout.LayoutParams(1, 1))
                it.onLoaded = { loaded.complete(Unit) }
                it.onChange = { if (it.failure != null) loaded.completeExceptionally(AssertionError("Synthetic page failed: ${it.failure}")) }
                it.web.loadDataWithBaseURL(base, page, "text/html", "utf-8", base)
            }
        }
        try { withTimeout(10_000) { loaded.await() } }
        catch (error: Exception) {
            val state = withContext(Dispatchers.Main) { "loading=${browser.loading}, url=${browser.web.url}" }
            withContext(Dispatchers.Main) { browser.destroy() }
            throw AssertionError("Synthetic page did not finish: $state", error)
        }
        return browser
    }

    private suspend fun evaluate(browser: LoginBrowser, script: String): String = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            browser.web.evaluateJavascript(script) { if (continuation.isActive) continuation.resume(it) }
        }
    }

    @Test fun invisibleWebViewSubmitsExactlyOnceUsingOfficialButton() = runBlocking {
        scenario().use { scenario ->
            lateinit var activity: OfficialLoginActivity
            scenario.onActivity { activity = it }
            val browser = load(activity)
            try {
                assertEquals("form", withContext(Dispatchers.Main) { browser.inspect() })
                assertEquals("submitted", withContext(Dispatchers.Main) { browser.inspect(SchoolCredentials("synthetic", "test-secret"), submit = true) })
                assertEquals("waiting", withContext(Dispatchers.Main) { browser.inspect(SchoolCredentials("synthetic", "test-secret"), submit = true) })
                assertEquals("1", evaluate(browser, "window.clickCount"))
            } finally { withContext(Dispatchers.Main) { browser.destroy() } }
        }
    }

    @Test fun captchaAndFilledFieldsSurviveHiddenToVisibleHandoff() = runBlocking {
        scenario().use { scenario ->
            lateinit var activity: OfficialLoginActivity
            scenario.onActivity { activity = it }
            val browser = load(activity, html.replace("</form>", "<input name='captcha'></form>"))
            try {
                assertEquals("challenge", withContext(Dispatchers.Main) { browser.inspect(SchoolCredentials("synthetic", "test-secret"), submit = true) })
                val original = browser.web
                withContext(Dispatchers.Main) {
                    val token = PendingLogin.put(browser)
                    assertSame(browser, PendingLogin.take(token))
                    browser.attachTo(activity, visible = true)
                    activity.findViewById<FrameLayout>(android.R.id.content).addView(browser.web)
                }
                assertSame(original, browser.web)
                assertEquals("true", evaluate(browser, "document.getElementById('pd').value === 'test-secret'"))
                assertEquals("0", evaluate(browser, "window.clickCount"))
            } finally { withContext(Dispatchers.Main) { browser.destroy() } }
        }
    }

    @Test fun untrustedPageDoesNotReceiveSavedCredentials() = runBlocking {
        scenario().use { scenario ->
            lateinit var activity: OfficialLoginActivity
            scenario.onActivity { activity = it }
            val browser = load(activity, base = "https://evil.example/tpass/login")
            try {
                assertEquals("unsupported", withContext(Dispatchers.Main) { browser.inspect(SchoolCredentials("synthetic", "test-secret"), submit = true) })
                assertEquals("true", evaluate(browser, "document.getElementById('pd').value === ''"))
                assertEquals("0", evaluate(browser, "window.clickCount"))
            } finally { withContext(Dispatchers.Main) { browser.destroy() } }
        }
    }

    @Test fun mailboxFormNeverReceivesSchoolCredentialsAndServiceIdentitySurvivesHandoff() = runBlocking {
        scenario().use { scenario ->
            lateinit var activity: OfficialLoginActivity
            scenario.onActivity { activity = it }
            val browser = load(activity, html.replace("</form>", "<input id='uid'><input id='fakePassword' type='password'></form>"),
                base = "https://mails.neu.edu.cn/coremail/index.jsp")
            try {
                withContext(Dispatchers.Main) { browser.pageTarget = OfficialPage.STUDENT_MAIL }
                assertTrue(withContext(Dispatchers.Main) { browser.hasMailboxLoginForm() })
                assertEquals("unsupported", withContext(Dispatchers.Main) {
                    browser.inspect(SchoolCredentials("synthetic-school-account", "test-secret"), submit = true)
                })
                assertEquals("true", evaluate(browser, "document.getElementById('pd').value === '' && document.getElementById('un').value === ''"))
                withContext(Dispatchers.Main) {
                    val prepared = PreparedOfficialPage(browser)
                    val token = PendingLogin.put(browser)
                    val resumed = PendingLogin.take(token)!!
                    assertSame(prepared.web, resumed.web)
                    assertEquals(OfficialPage.STUDENT_MAIL, resumed.pageTarget)
                    assertFalse(resumed.web.isSaveEnabled)
                }
            } finally { withContext(Dispatchers.Main) { browser.destroy() } }
        }
    }

    @Test fun serviceRedirectHandoffKeepsBrowserAndSubmissionBudgetAfterComposeRelease() = runBlocking {
        scenario().use { scenario ->
            lateinit var activity: OfficialLoginActivity
            scenario.onActivity { activity = it }
            val browser = load(activity)
            try {
                val token = CompletableDeferred<String>()
                withContext(Dispatchers.Main) {
                    browser.pageTarget = OfficialPage.CARD_RECHARGE
                    browser.automaticSubmitted = true
                    val prepared = PreparedOfficialPage(browser)
                    prepared.attach(activity, {}, {}, {}, {}, { token.complete((it as LoginResult.ContinueOnWeb).token) })
                    activity.findViewById<FrameLayout>(android.R.id.content).addView(prepared.web)
                    browser.onStarted()
                    prepared.close() // AndroidView release must not destroy a transferred school challenge.
                }
                withContext(Dispatchers.Main) {
                    val resumed = PendingLogin.take(token.await())!!
                    assertSame(browser.web, resumed.web)
                    assertFalse(resumed.destroyed)
                    assertTrue(resumed.automaticSubmitted)
                    assertEquals(OfficialPage.CARD_RECHARGE, resumed.pageTarget)
                    assertFalse(resumed.serviceVisible)
                }
            } finally { withContext(Dispatchers.Main) { browser.destroy() } }
        }
    }

    @Test fun continuationOpensNativeEntryAndSurvivesRotationWithoutChangingScope() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val session = withContext(Dispatchers.Main) { LocalSession.get(context) }
        val scope = session.beginLogin(clearCookies = true)
        try {
            scenario().use { source ->
                lateinit var activity: OfficialLoginActivity
                source.onActivity { activity = it }
                val browser = load(activity, html.replace("</form>", "<input name='captcha'></form>"))
                val token = withContext(Dispatchers.Main) {
                    browser.scope = scope
                    PendingLogin.put(browser)
                }
                ActivityScenario.launch<OfficialLoginActivity>(OfficialLogin.intent(context, continuation = token)).use { destination ->
                    destination.onActivity {
                        val model = ViewModelProvider(it)[LoginScreenModel::class.java]
                        assertNull(model.browser)
                        assertSame(browser, model.pendingBrowser)
                        assertNull(browser.web.parent)
                    }
                    destination.recreate()
                    destination.onActivity {
                        val model = ViewModelProvider(it)[LoginScreenModel::class.java]
                        assertNull(model.browser)
                        assertSame(browser, model.pendingBrowser)
                        assertFalse(browser.destroyed)
                    }
                    assertEquals(scope, session.state.value.accountScope)
                }
                assertTrue(browser.destroyed)
            }
        } finally { session.signOut() }
    }
}

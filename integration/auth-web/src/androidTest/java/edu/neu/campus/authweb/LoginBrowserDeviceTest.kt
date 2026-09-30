package edu.neu.campus.authweb

import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.neu.campus.session.SchoolCredentials
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
}

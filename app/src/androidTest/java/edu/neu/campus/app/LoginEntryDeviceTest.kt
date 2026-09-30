package edu.neu.campus.app

import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.runtime.MutableState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.authweb.OfficialLoginActivity
import edu.neu.campus.authweb.OfficialLogin
import edu.neu.campus.authweb.LoginResult
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.session.LocalSession
import edu.neu.campus.session.SchoolCredentials
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Dedicated emulator only. Synthetic paused credentials are never submitted to the school. */
@RunWith(AndroidJUnit4::class)
class LoginEntryDeviceTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var session: LocalSession

    @Before fun prepare() = runBlocking {
        instrumentation.runOnMainSync { session = LocalSession.get(instrumentation.targetContext) }
        session.signOut()
        instrumentation.runOnMainSync { AppNavigator.navigateToTab(MainTab.SETTINGS) }
    }

    @After fun cleanUp() = runBlocking { session.signOut() }

    @Test fun freshLoginOpensNativeEntry() {
        assertNativeEntry { }
    }

    @Test fun expiredLegacySessionOpensNativeEntryWithoutRotatingScope() = runBlocking {
        val scope = session.beginLogin()
        session.mark(Domain.PORTAL, DomainStatus.UNREACHABLE)
        session.mark(Domain.ACADEMIC, DomainStatus.UNREACHABLE)
        assertNativeEntry {
            it.recoverSchool = { LoginResult.NeedCredentials() }
            session.mark(Domain.PORTAL, DomainStatus.EXPIRED)
            session.mark(Domain.ACADEMIC, DomainStatus.EXPIRED)
        }
        assertEquals(scope, session.state.value.accountScope)
    }

    @Test fun pausedSavedLoginOpensNativeEntry() = runBlocking {
        val scope = session.beginLogin()
        session.saveCredentials(scope, SchoolCredentials("synthetic-account", "test-secret"))
        session.pauseAutomaticLogin(scope)
        session.mark(Domain.PORTAL, DomainStatus.UNREACHABLE)
        session.mark(Domain.ACADEMIC, DomainStatus.UNREACHABLE)
        assertNativeEntry()
        assertEquals(scope, session.state.value.accountScope)
    }

    @Test fun manualLoginReusesPendingRecoveryWithoutCancellingOrOpeningEntry(): Unit = runBlocking {
        val scope = session.beginLogin()
        session.mark(Domain.PORTAL, DomainStatus.UNREACHABLE)
        session.mark(Domain.ACADEMIC, DomainStatus.UNREACHABLE)
        val pending = Job()
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val monitor = instrumentation.addMonitor(OfficialLoginActivity::class.java.name, null, false)
                scenario.onActivity { activity ->
                // Keep a recovery pending without loading a school page or submitting a password.
                MainActivity::class.java.getDeclaredField("recoveryJob").apply { isAccessible = true }.set(activity, pending)
                val field = MainActivity::class.java.getDeclaredField("connectingSchool\$delegate").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                (field.get(activity) as MutableState<Boolean>).value = true
                }
                try {
                    compose.onNodeWithText("登录学校账号").assertIsEnabled().performClick()
                    compose.waitForIdle()
                    assertFalse(pending.isCancelled)
                    assertEquals(0, monitor.hits)
                } finally { instrumentation.removeMonitor(monitor) }
            }
            assertEquals(scope, session.state.value.accountScope)
        } finally { pending.cancel() }
    }

    @Test fun successfulSilentLoginKeepsCurrentPageWithoutOpeningEntry(): Unit = runBlocking {
        val scope = session.beginLogin()
        session.mark(Domain.PORTAL, DomainStatus.UNREACHABLE)
        session.mark(Domain.ACADEMIC, DomainStatus.UNREACHABLE)
        val monitor = instrumentation.addMonitor(OfficialLoginActivity::class.java.name, null, false)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                var attempts = 0
                scenario.onActivity { activity ->
                    stopAutomaticRecovery(activity)
                    activity.recoverSchool = { attempts++; LoginResult.Connected }
                }
                compose.onNodeWithText("登录学校账号").performClick()
                compose.waitUntil(timeoutMillis = 15_000) { attempts > 0 }
                assertEquals(1, attempts)
                assertEquals(0, monitor.hits)
                assertEquals(scope, session.state.value.accountScope)
            }
        } finally { instrumentation.removeMonitor(monitor) }
    }

    @Test fun silentFailureDoesNotAutomaticallyRetrySavedPasswordInFallback() = runBlocking {
        val scope = session.beginLogin()
        session.saveCredentials(scope, SchoolCredentials("synthetic-account", "test-secret"))
        session.pauseAutomaticLogin(scope)
        session.mark(Domain.PORTAL, DomainStatus.UNREACHABLE)
        session.mark(Domain.ACADEMIC, DomainStatus.UNREACHABLE)
        assertNativeEntry { activity ->
            activity.recoverSchool = {
                // Enable only inside the synthetic connector; never block the UI on the session lock.
                session.confirmSavedAccount(scope, "synthetic-account")
                LoginResult.Failed("合成网络失败")
            }
        }
        assertEquals(scope, session.state.value.accountScope)
    }

    @Test fun pausedCredentialsAreFilledAutomaticallyWithoutSubmittingAndSurviveRotation() = runBlocking {
        val scope = session.beginLogin()
        session.saveCredentials(scope, SchoolCredentials("synthetic-account", "test-secret"))
        session.pauseAutomaticLogin(scope)
        ActivityScenario.launch<OfficialLoginActivity>(OfficialLogin.intent(instrumentation.targetContext)).use { scenario ->
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText("synthetic-account").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("synthetic-account").assertExists()
            scenario.onActivity { assertSavedPassword(it) }
            scenario.recreate()
            compose.onNodeWithText("synthetic-account").assertExists()
            compose.onNodeWithText("填入已保存账号密码").assertDoesNotExist()
            scenario.onActivity { assertSavedPassword(it) }
            scenario.onActivity { assertFalse(hasWebView(it.window.decorView)) }
            assertEquals(scope, session.state.value.accountScope)
            assertEquals(edu.neu.campus.session.SavedLoginStatus.PAUSED, session.savedLoginStatus.value)
        }
    }

    @Test fun enabledSavedCredentialsConnectOnEntryWithoutClicking(): Unit = runBlocking {
        val scope = session.beginLogin()
        session.saveCredentials(scope, SchoolCredentials("synthetic-account", "test-secret"))
        // An already verified session exercises the automatic entry without any school requests.
        session.mark(Domain.PORTAL, DomainStatus.READY)
        session.mark(Domain.ACADEMIC, DomainStatus.READY)
        ActivityScenario.launchActivityForResult<OfficialLoginActivity>(OfficialLogin.intent(instrumentation.targetContext)).use {
            assertEquals(android.app.Activity.RESULT_OK, it.result.resultCode)
            assertEquals(scope, session.state.value.accountScope)
        }
    }

    private fun assertSavedPassword(activity: OfficialLoginActivity) {
        val model = OfficialLoginActivity::class.java.getDeclaredField("model").apply { isAccessible = true }.get(activity)
        assertEquals("test-secret", model.javaClass.getMethod("getPassword").invoke(model))
    }

    @Test fun savedStatusRefreshDoesNotOverwriteEditedAccount(): Unit = runBlocking {
        val scope = session.beginLogin()
        session.saveCredentials(scope, SchoolCredentials("synthetic-account", "test-secret"))
        session.pauseAutomaticLogin(scope)
        ActivityScenario.launch<OfficialLoginActivity>(OfficialLogin.intent(instrumentation.targetContext)).use { scenario ->
            compose.waitUntil(timeoutMillis = 15_000) {
                compose.onAllNodesWithText("synthetic-account").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("synthetic-account").performTextReplacement("edited-account")
            session.confirmSavedAccount(scope, "synthetic-account")
            compose.onNodeWithText("使用已保存账号继续").performScrollTo().assertExists()
            compose.onNodeWithText("edited-account").assertExists()
            scenario.recreate()
            compose.onNodeWithText("edited-account").assertExists()
            scenario.onActivity { assertFalse(hasWebView(it.window.decorView)) }
        }
    }

    private fun assertNativeEntry(beforeClick: (MainActivity) -> Unit = {
        it.recoverSchool = { LoginResult.NeedCredentials() }
    }) {
        val monitor = instrumentation.addMonitor(OfficialLoginActivity::class.java.name, null, false)
        var login: OfficialLoginActivity? = null
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { stopAutomaticRecovery(it); beforeClick(it) }
                compose.onNodeWithText("登录学校账号").assertIsEnabled().performClick()
                login = instrumentation.waitForMonitorWithTimeout(monitor, 5_000) as? OfficialLoginActivity
                assertNotNull("A failed silent attempt must open native credential entry", login)
                compose.onNodeWithText("保存并登录").assertIsDisplayed()
                compose.onNodeWithText("使用学校网页登录").assertIsDisplayed()
                instrumentation.runOnMainSync {
                    assertNull(login!!.intent.getStringExtra("continuation"))
                    assertTrue(login!!.intent.getBooleanExtra("silent_attempted", false))
                    assertFalse(hasWebView(login!!.window.decorView))
                    assertTrue(login!!.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                }
            }
        } finally {
            instrumentation.runOnMainSync { login?.finish() }
            instrumentation.waitForIdleSync()
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun stopAutomaticRecovery(activity: MainActivity) {
        val field = MainActivity::class.java.getDeclaredField("recoveryJob").apply { isAccessible = true }
        (field.get(activity) as? Job)?.cancel()
        field.set(activity, null)
        session.state.value.accountScope?.let { scope ->
            val gate = MainActivity::class.java.getDeclaredField("automaticLoginGate").apply { isAccessible = true }
                .get(activity) as AutomaticLoginGate
            gate.begin(scope, Domain.PORTAL, 0)
            gate.pause(scope, Domain.PORTAL)
            gate.pause(scope, Domain.ACADEMIC)
        }
    }

    private fun hasWebView(view: View): Boolean = view is WebView ||
        view is ViewGroup && (0 until view.childCount).any { hasWebView(view.getChildAt(it)) }
}

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
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.authweb.OfficialLoginActivity
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
        assertNativeEntry()
    }

    @Test fun expiredLegacySessionOpensNativeEntryWithoutRotatingScope() = runBlocking {
        val scope = session.beginLogin()
        session.mark(Domain.PORTAL, DomainStatus.EXPIRED)
        session.mark(Domain.ACADEMIC, DomainStatus.EXPIRED)
        assertNativeEntry()
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

    @Test fun manualLoginRemainsAvailableAndCancelsPendingRecovery() = runBlocking {
        val scope = session.beginLogin()
        session.mark(Domain.PORTAL, DomainStatus.UNREACHABLE)
        session.mark(Domain.ACADEMIC, DomainStatus.UNREACHABLE)
        val pending = Job()
        try {
            assertNativeEntry { activity ->
                // Keep a recovery pending without loading a school page or submitting a password.
                MainActivity::class.java.getDeclaredField("recoveryJob").apply { isAccessible = true }.set(activity, pending)
                val field = MainActivity::class.java.getDeclaredField("connectingSchool\$delegate").apply { isAccessible = true }
                @Suppress("UNCHECKED_CAST")
                (field.get(activity) as MutableState<Boolean>).value = true
            }
            assertTrue(pending.isCancelled)
            assertEquals(scope, session.state.value.accountScope)
        } finally { pending.cancel() }
    }

    private fun assertNativeEntry(beforeClick: (MainActivity) -> Unit = {}) {
        val monitor = instrumentation.addMonitor(OfficialLoginActivity::class.java.name, null, false)
        var login: OfficialLoginActivity? = null
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity(beforeClick)
                compose.onNodeWithText("登录学校账号").assertIsEnabled().performClick()
                login = instrumentation.waitForMonitorWithTimeout(monitor, 5_000) as? OfficialLoginActivity
                assertNotNull("Manual login must immediately open native credential entry", login)
                compose.onNodeWithText("保存并登录").assertIsDisplayed()
                compose.onNodeWithText("使用学校网页登录").assertIsDisplayed()
                instrumentation.runOnMainSync {
                    assertNull(login!!.intent.getStringExtra("continuation"))
                    assertFalse(hasWebView(login!!.window.decorView))
                    assertTrue(login!!.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
                }
            }
        } finally {
            instrumentation.runOnMainSync { login?.finish() }
            instrumentation.removeMonitor(monitor)
        }
    }

    private fun hasWebView(view: View): Boolean = view is WebView ||
        view is ViewGroup && (0 until view.childCount).any { hasWebView(view.getChildAt(it)) }
}

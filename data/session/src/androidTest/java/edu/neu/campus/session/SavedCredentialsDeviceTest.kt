package edu.neu.campus.session

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.neu.campus.contract.DomainStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/** Runs in this library's isolated test APK. No networking or real school credentials. */
@RunWith(AndroidJUnit4::class)
class SavedCredentialsDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var vault: SavedSchoolCredentials

    @Before fun resetVault() {
        vault = SavedSchoolCredentials(context)
        vault.clear()
    }

    @Test fun keystoreRoundTripSurvivesRecreationAndStoresOnlyCiphertext() {
        vault.save("scope", SchoolCredentials("synthetic-account", "synthetic-secret"))
        val preferences = context.getSharedPreferences("school_credentials_v1", Context.MODE_PRIVATE)
        assertFalse(preferences.all.toString().contains("synthetic-account"))
        assertFalse(preferences.all.toString().contains("synthetic-secret"))
        assertFalse(preferences.all.toString().contains("scope"))
        val restored = SavedSchoolCredentials(context).read("scope")!!
        assertEquals("synthetic-account", restored.account)
        assertEquals("synthetic-secret", restored.password)
        assertNull(vault.read("other-scope"))
    }

    @Test fun pauseRequiresExplicitConfirmationAndCorruptDataCannotBeRead() {
        vault.save("scope", SchoolCredentials("synthetic-account", "synthetic-secret"))
        vault.pause()
        assertEquals(SavedLoginStatus.PAUSED, vault.status.value)
        assertNull(SavedSchoolCredentials(context).read("scope"))
        assertNotNull(vault.read("scope", includePaused = true))
        vault.enable()
        assertNotNull(vault.read("scope"))
        context.getSharedPreferences("school_credentials_v1", Context.MODE_PRIVATE).edit()
            .putString("ciphertext", "invalid ciphertext").commit()
        assertNull(vault.read("scope"))
        assertEquals(SavedLoginStatus.NONE, vault.status.value)
    }

    @Test fun clearRemovesCiphertextAndKeystoreKey() {
        vault.save("scope", SchoolCredentials("synthetic-account", "synthetic-secret"))
        vault.clear()
        assertNull(SavedSchoolCredentials(context).read("scope", includePaused = true))
        assertEquals(SavedLoginStatus.NONE, vault.status.value)
        assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias("neo_neu_school_credentials_v1"))
    }

    @Test fun signOutAndScopeChangeRejectOldCredentialWrites() = runBlocking {
        val session = withContext(Dispatchers.Main) { LocalSession(context) }
        val scope = session.beginLogin(clearCookies = true)
        assertTrue(session.saveCredentials(scope, SchoolCredentials("synthetic-account", "synthetic-secret")))
        val newScope = session.beginLogin(keepCredentials = true)
        assertNull(session.readCredentials(scope, includePaused = true))
        assertFalse(session.saveCredentials(scope, SchoolCredentials("old-account", "old-secret")))
        assertEquals(SavedLoginStatus.PAUSED, session.savedLoginStatus.value)
        session.confirmSavedAccount(newScope, "synthetic-account")
        assertNotNull(session.readCredentials(newScope))
        session.signOut()
        assertNull(session.state.value.accountScope)
        assertEquals(DomainStatus.SIGNED_OUT, session.state.value.portal)
        assertFalse(session.saveCredentials(newScope, SchoolCredentials("old-account", "old-secret")))
        assertNull(session.readCredentials(newScope, includePaused = true))
        assertEquals(SavedLoginStatus.NONE, session.savedLoginStatus.value)
    }

    @Test fun missingAccountPausesAndChangedAccountDeletesSavedPassword() = runBlocking {
        val session = withContext(Dispatchers.Main) { LocalSession(context) }
        val scope = session.beginLogin(clearCookies = true)
        session.saveCredentials(scope, SchoolCredentials("synthetic-account", "synthetic-secret"))
        session.confirmSavedAccount(scope, null)
        assertEquals(SavedLoginStatus.PAUSED, session.savedLoginStatus.value)
        session.confirmSavedAccount(scope, "different-account")
        assertEquals(SavedLoginStatus.NONE, session.savedLoginStatus.value)
        assertNull(session.readCredentials(scope, includePaused = true))
        session.signOut()
    }
}

package edu.neu.campus.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

enum class SavedLoginStatus { NONE, ENABLED, PAUSED }

/** Never put credentials in data-class toString(), intents, saved state or diagnostics. */
class SchoolCredentials(val account: String, val password: String) {
    override fun toString() = "SchoolCredentials(redacted)"
}

internal object CredentialEnvelope {
    private val aad = "NEO NEU school credentials v1".toByteArray(Charsets.UTF_8)

    fun seal(key: SecretKey, plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad)
        require(cipher.iv.size == 12)
        return byteArrayOf(1) + cipher.iv + cipher.doFinal(plain)
    }

    fun open(key: SecretKey, payload: ByteArray): ByteArray {
        require(payload.size >= 29 && payload[0] == 1.toByte())
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, payload.copyOfRange(1, 13)))
        cipher.updateAAD(aad)
        return cipher.doFinal(payload, 13, payload.size - 13)
    }
}

/** Call on Dispatchers.IO. Only ciphertext and non-sensitive state are persisted. */
internal class SavedSchoolCredentials(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("school_credentials_v1", Context.MODE_PRIVATE)
    private val alias = "neo_neu_school_credentials_v1"
    private val mutableStatus = MutableStateFlow(currentStatus())
    val status: StateFlow<SavedLoginStatus> = mutableStatus

    private fun currentStatus() = when {
        !preferences.contains("ciphertext") -> SavedLoginStatus.NONE
        preferences.getBoolean("paused", false) -> SavedLoginStatus.PAUSED
        else -> SavedLoginStatus.ENABLED
    }

    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
        }.generateKey()
    }

    @Synchronized fun save(scope: String, credentials: SchoolCredentials, paused: Boolean = false) {
        require(credentials.account.isNotBlank() && credentials.account.length <= 256)
        require(credentials.password.isNotEmpty() && credentials.password.length <= 4096)
        val plain = ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use {
                it.writeUTF(scope); it.writeUTF(credentials.account); it.writeUTF(credentials.password)
            }
            bytes.toByteArray()
        }
        val payload = try { CredentialEnvelope.seal(key(create = true), plain) } finally { plain.fill(0) }
        check(preferences.edit().putString("ciphertext", Base64.encodeToString(payload, Base64.NO_WRAP))
            .putBoolean("paused", paused).commit())
        mutableStatus.value = currentStatus()
    }

    @Synchronized fun read(scope: String, includePaused: Boolean = false): SchoolCredentials? {
        if (!includePaused && mutableStatus.value != SavedLoginStatus.ENABLED) return null
        val encoded = preferences.getString("ciphertext", null) ?: return null
        return try {
            val plain = CredentialEnvelope.open(key(create = false), Base64.decode(encoded, Base64.NO_WRAP))
            try {
                DataInputStream(ByteArrayInputStream(plain)).use {
                    val storedScope = it.readUTF()
                    val account = it.readUTF()
                    val password = it.readUTF()
                    check(it.available() == 0)
                    if (storedScope == scope) SchoolCredentials(account, password) else null
                }
            } finally { plain.fill(0) }
        } catch (_: Exception) {
            // A missing/invalidated key or corrupt record never falls back to plaintext storage.
            clear()
            null
        }
    }

    @Synchronized fun pause() {
        if (!preferences.contains("ciphertext")) return
        check(preferences.edit().putBoolean("paused", true).commit())
        mutableStatus.value = SavedLoginStatus.PAUSED
    }

    @Synchronized fun enable() {
        if (!preferences.contains("ciphertext")) return
        check(preferences.edit().putBoolean("paused", false).commit())
        mutableStatus.value = SavedLoginStatus.ENABLED
    }

    @Synchronized fun clear() {
        val removed = preferences.edit().clear().commit()
        mutableStatus.value = SavedLoginStatus.NONE
        val keyRemoved = runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(alias) }.isSuccess
        // Destroy the key even if a disk write fails, so retained ciphertext cannot be reused.
        check(removed && keyRemoved)
    }
}

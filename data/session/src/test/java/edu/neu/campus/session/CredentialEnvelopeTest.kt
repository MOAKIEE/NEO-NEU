package edu.neu.campus.session

import org.junit.Assert.*
import org.junit.Test
import javax.crypto.KeyGenerator

class CredentialEnvelopeTest {
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test fun ciphertextRoundTripsWithFreshIv() {
        val key = key()
        val plain = "synthetic account\u0000test password".toByteArray()
        val first = CredentialEnvelope.seal(key, plain)
        val second = CredentialEnvelope.seal(key, plain)
        assertArrayEquals(plain, CredentialEnvelope.open(key, first))
        assertFalse(first.contentEquals(second))
        assertFalse(first.toString(Charsets.UTF_8).contains("test password"))
    }

    @Test fun tamperingAndWrongKeysAreRejected() {
        val key = key()
        val payload = CredentialEnvelope.seal(key, "synthetic".toByteArray())
        assertTrue(runCatching { CredentialEnvelope.open(key(), payload) }.isFailure)
        payload[payload.lastIndex] = (payload.last().toInt() xor 1).toByte()
        assertTrue(runCatching { CredentialEnvelope.open(key, payload) }.isFailure)
    }

    @Test fun truncatedAndUnknownVersionsAreRejected() {
        val key = key()
        assertTrue(runCatching { CredentialEnvelope.open(key, ByteArray(12)) }.isFailure)
        val payload = CredentialEnvelope.seal(key, byteArrayOf(1))
        payload[0] = 2
        assertTrue(runCatching { CredentialEnvelope.open(key, payload) }.isFailure)
    }

    @Test fun credentialsDoNotAppearInDiagnostics() {
        assertEquals("SchoolCredentials(redacted)", SchoolCredentials("synthetic", "secret").toString())
    }
}

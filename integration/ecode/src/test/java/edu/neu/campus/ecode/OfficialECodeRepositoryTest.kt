package edu.neu.campus.ecode

import edu.neu.campus.contract.ECodeResult
import org.junit.Assert.*
import org.junit.Test

class OfficialECodeRepositoryTest {
    @Test fun parsesVerifiedSiblingFieldsAndLifetime() {
        val result = parseECode("""{"data":[{"attributes":{"qrCode":"school-code","createTime":"100000","qrInvalidTime":"111000"}}]}""", 100000)
        assertTrue(result is ECodeResult.Ready)
        assertEquals("school-code", (result as ECodeResult.Ready).token.payload)
        assertEquals(9000, result.token.remainingMillis)
    }

    @Test fun rejectsMissingAndExpiredCodes() {
        assertEquals(ECodeResult.InvalidResponse, parseECode("""{"data":[{"attributes":{"qrCode":"x"}}]}""", 100000))
        assertEquals(ECodeResult.InvalidResponse, parseECode("""{"data":[{"attributes":{"qrCode":"x","createTime":"100000","qrInvalidTime":"111000"}}]}""", 110000))
    }

    @Test fun slowDownloadConsumesValidityBeforeTheCodeIsDisplayed() {
        val body = """{"data":[{"attributes":{"qrCode":"synthetic-code","createTime":"100000","qrInvalidTime":"111000"}}]}"""
        val result = parseECode(body, 100000, requestDurationMillis = 8000, receivedAtElapsedMillis = 50000)
        assertTrue(result is ECodeResult.Ready)
        val token = (result as ECodeResult.Ready).token
        assertEquals(1000L, token.remainingMillisAt(50000))
        assertEquals(250L, token.remainingMillisAt(50750))
        assertEquals(0L, token.remainingMillisAt(52000))
        assertEquals(ECodeResult.InvalidResponse, parseECode(body, 100000, requestDurationMillis = 9000))
        assertEquals(ECodeResult.InvalidResponse, parseECode(body, 100000, requestDurationMillis = 12000))
    }

    @Test fun invalidMonotonicDurationIsNeverTreatedAsExtraValidity() {
        val body = """{"data":[{"attributes":{"qrCode":"synthetic-code","createTime":"100000","qrInvalidTime":"111000"}}]}"""
        assertEquals(ECodeResult.InvalidResponse, parseECode(body, 100000, requestDurationMillis = -1))
        assertEquals(ECodeResult.InvalidResponse, parseECode(body, 100000, requestDurationMillis = Long.MAX_VALUE))
    }
}

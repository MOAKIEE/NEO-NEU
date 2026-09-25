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
}

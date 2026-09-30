package edu.neu.campus.authweb

import org.junit.Assert.assertEquals
import org.junit.Test

class LoginScriptTest {
    @Test fun credentialsAreEscapedAsOneJavascriptString() {
        assertEquals("\"a\\\"\\\\\\n\\r\\t\\u0000\\u2028\\u2029\"", jsString("a\"\\\n\r\t\u0000\u2028\u2029"))
        assertEquals("null", jsString(null))
        assertEquals("\"'; login(); //\"", jsString("'; login(); //"))
    }
}

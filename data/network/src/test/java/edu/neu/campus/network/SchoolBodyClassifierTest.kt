package edu.neu.campus.network

import edu.neu.campus.contract.QueryErrorKind
import org.junit.Assert.*
import org.junit.Test

class SchoolBodyClassifierTest {
    @Test fun htmlWithSuccessHttpStatusStillNeedsAuthentication() {
        val error = assertThrows(SchoolHttpException::class.java) {
            SchoolBodyClassifier.validate("text/html", "<html><form><input type='password'></form></html>")
        }
        assertEquals(QueryErrorKind.AUTH_REQUIRED, error.safeError.kind)
    }

    @Test fun unexpectedPlainTextIsSchemaFailure() {
        val error = assertThrows(SchoolHttpException::class.java) {
            SchoolBodyClassifier.validate("text/plain", "temporary message")
        }
        assertEquals(QueryErrorKind.SCHEMA_CHANGED, error.safeError.kind)
    }

    @Test fun ordinaryRedirectAnd304AreNotAuthenticationFailures() {
        assertEquals(QueryErrorKind.REDIRECT, SchoolResponseClassifier.status(302, "/elsewhere", null)?.safeError?.kind)
        assertEquals(QueryErrorKind.NOT_MODIFIED, SchoolResponseClassifier.status(304, null, null)?.safeError?.kind)
        assertEquals(QueryErrorKind.AUTH_REQUIRED, SchoolResponseClassifier.status(302, "https://pass.neu.edu.cn/login", null)?.safeError?.kind)
        assertEquals(QueryErrorKind.FORBIDDEN, SchoolResponseClassifier.status(403, null, null)?.safeError?.kind)
    }

    @Test fun maintenancePageAndRateLimitAreDistinct() {
        assertTrue(SchoolBodyClassifier.isMaintenance("<html>系统维护</html>"))
        val page = assertThrows(SchoolHttpException::class.java) {
            SchoolBodyClassifier.validate("text/html", "<html>系统维护</html>")
        }
        assertEquals(QueryErrorKind.MAINTENANCE, page.safeError.kind)
        val limited = SchoolResponseClassifier.status(429, null, "15")!!
        assertEquals(QueryErrorKind.RATE_LIMITED, limited.safeError.kind)
        assertEquals(15_000L, limited.retryAfterMillis)
    }
}

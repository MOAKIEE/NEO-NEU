package edu.neu.campus.network

import edu.neu.campus.contract.QueryErrorKind
import org.junit.Assert.*
import org.junit.Test

class SchoolBodyClassifierTest {
    @Test fun htmlWithSuccessHttpStatusStillNeedsAuthentication() {
        val error = assertThrows(SchoolHttpException::class.java) {
            SchoolBodyClassifier.validate("text/html", "<html><form></form></html>")
        }
        assertEquals(QueryErrorKind.AUTH_REQUIRED, error.safeError.kind)
    }

    @Test fun unexpectedPlainTextIsSchemaFailure() {
        val error = assertThrows(SchoolHttpException::class.java) {
            SchoolBodyClassifier.validate("text/plain", "temporary message")
        }
        assertEquals(QueryErrorKind.SCHEMA_CHANGED, error.safeError.kind)
    }
}

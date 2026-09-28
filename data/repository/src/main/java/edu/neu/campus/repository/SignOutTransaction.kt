package edu.neu.campus.repository

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

/** Scope invalidation disposes the calling screen, but must not interrupt local cleanup. */
internal suspend fun completeSignOut(
    clearSession: suspend () -> Unit,
    clearCache: suspend () -> Unit
) = withContext(NonCancellable) {
    try {
        clearSession()
    } finally {
        clearCache()
    }
}

package edu.neu.campus.contract

/** A one-use school code. Keep [payload] in memory only and never put it in saved state or logs. */
class ECodeToken(val payload: String, val remainingMillis: Long, val receivedAtElapsedMillis: Long = 0) {
    /** All callers use the same Android elapsed-realtime clock, which includes device sleep. */
    fun remainingMillisAt(nowElapsedMillis: Long): Long =
        (remainingMillis - (nowElapsedMillis - receivedAtElapsedMillis).coerceAtLeast(0)).coerceAtLeast(0)
}

sealed interface ECodeResult {
    data class Ready(val token: ECodeToken) : ECodeResult
    data object LoginRequired : ECodeResult
    data object Unavailable : ECodeResult
    data object InvalidResponse : ECodeResult
}

interface ECodeRepository {
    suspend fun fetch(): ECodeResult
}

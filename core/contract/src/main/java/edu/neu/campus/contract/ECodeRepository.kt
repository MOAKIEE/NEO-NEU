package edu.neu.campus.contract

/** A one-use school code. Keep [payload] in memory only and never put it in saved state or logs. */
class ECodeToken(val payload: String, val remainingMillis: Long)

sealed interface ECodeResult {
    data class Ready(val token: ECodeToken) : ECodeResult
    data object LoginRequired : ECodeResult
    data object Unavailable : ECodeResult
    data object InvalidResponse : ECodeResult
}

interface ECodeRepository {
    suspend fun fetch(): ECodeResult
}

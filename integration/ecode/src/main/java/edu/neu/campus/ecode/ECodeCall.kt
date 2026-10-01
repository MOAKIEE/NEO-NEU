package edu.neu.campus.ecode

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resumeWithException

/** Cancellation must release a hidden SSO browser promptly before a new account can log in. */
internal suspend fun Call.awaitECodeResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }
    })
}

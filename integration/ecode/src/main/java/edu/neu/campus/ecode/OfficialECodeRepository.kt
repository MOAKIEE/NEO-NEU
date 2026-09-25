package edu.neu.campus.ecode

import android.content.Context
import edu.neu.campus.contract.ECodeRepository
import edu.neu.campus.contract.ECodeResult
import edu.neu.campus.contract.ECodeToken
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/** Only the verified read endpoint is used. Dynamic codes are never cached or logged. */
class OfficialECodeRepository(context: Context) : ECodeRepository {
    private val session = LocalSession.get(context.applicationContext)
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    override suspend fun fetch(): ECodeResult = withContext(Dispatchers.IO) {
        val scope = session.state.value.accountScope
        val cookie = session.cookieHeader(URL) ?: return@withContext ECodeResult.LoginRequired
        val request = Request.Builder().url(URL).header("Cookie", cookie)
            .header("Accept", "application/json").get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (scope != session.state.value.accountScope) throw CancellationException("Account changed")
                if (scope != null) session.acceptSetCookies(scope, URL, response.headers("Set-Cookie"))
                when (response.code) {
                    401, 403 -> return@withContext ECodeResult.LoginRequired
                    in 300..399 -> return@withContext ECodeResult.LoginRequired
                }
                if (!response.isSuccessful) return@withContext ECodeResult.Unavailable
                val body = response.body?.string().orEmpty()
                if (!response.header("Content-Type").orEmpty().contains("json", ignoreCase = true)) {
                    return@withContext ECodeResult.InvalidResponse
                }
                val serverNow = response.header("Date")?.let(::parseHttpDate) ?: System.currentTimeMillis()
                coroutineContext.ensureActive()
                if (scope != session.state.value.accountScope) throw CancellationException("Account changed")
                parseECode(body, serverNow)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            ECodeResult.Unavailable
        } catch (_: Exception) {
            ECodeResult.InvalidResponse
        }
    }

    /** Checks only authenticated status. The response may contain identity data and is not read. */
    suspend fun hasSession(): Boolean = withContext(Dispatchers.IO) {
        val scope = session.state.value.accountScope
        val cookie = session.cookieHeader(USER_INFO_URL) ?: return@withContext false
        val request = Request.Builder().url(USER_INFO_URL).header("Cookie", cookie)
            .header("Accept", "application/json").get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (scope != null) session.acceptSetCookies(scope, USER_INFO_URL, response.headers("Set-Cookie"))
                scope == session.state.value.accountScope && response.code == 200 &&
                    response.header("Content-Type").orEmpty().contains("json", ignoreCase = true)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            false
        }
    }

    private fun parseHttpDate(value: String): Long? = runCatching {
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("GMT")
        }.parse(value)?.time
    }.getOrNull()

    companion object {
        private const val URL = "https://ecode.neu.edu.cn/ecode/api/qr-code"
        private const val USER_INFO_URL = "https://ecode.neu.edu.cn/ecode/api/user-info"
    }
}

/** Pure parser for the authenticated response shape verified in docs/校园服务接口验证.md. */
fun parseECode(body: String, serverNowMillis: Long): ECodeResult = try {
    val attributes = JSONObject(body).getJSONArray("data").getJSONObject(0).getJSONObject("attributes")
    val payload = attributes.opt("qrCode") as? String ?: return ECodeResult.InvalidResponse
    val created = (attributes.opt("createTime") as? String)?.toLongOrNull() ?: return ECodeResult.InvalidResponse
    val expires = (attributes.opt("qrInvalidTime") as? String)?.toLongOrNull() ?: return ECodeResult.InvalidResponse
    // HTTP Date has second precision; a two-second margin avoids displaying an expired code.
    val remaining = expires - serverNowMillis - 2_000L
    if (payload.isBlank() || created <= 0 || expires <= created ||
        expires - created > 120_000 || remaining <= 0 || remaining > 120_000
    ) ECodeResult.InvalidResponse else ECodeResult.Ready(ECodeToken(payload, remaining))
} catch (_: Exception) {
    ECodeResult.InvalidResponse
}

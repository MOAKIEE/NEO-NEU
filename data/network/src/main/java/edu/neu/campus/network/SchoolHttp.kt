package edu.neu.campus.network

import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.QueryError
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.coroutineContext

/** Every entry is a read operation, even where the school's front end uses POST. */
enum class SchoolCall(val domain: Domain, val method: String, val path: String, val keys: Set<String>) {
    CURRENT_TERM(Domain.ACADEMIC, "POST", "/sys/jwpubapp/modules/gg/cxmrxnxq.do", setOf("CSDM", "ZCSDM", "SFSY")),
    TERMS(Domain.ACADEMIC, "POST", "/sys/jwpubapp/modules/zdgl/xnxqcx.do", setOf("*order")),
    WEEKDAYS(Domain.ACADEMIC, "POST", "/sys/kbbpapp/api/kbbp/cxxqzd.do", emptySet()),
    WEEKS(Domain.ACADEMIC, "POST", "/sys/kbbpapp/api/schoolCalendar/getTermWeeks.do", setOf("XNXQDM")),
    CAMPUSES(Domain.ACADEMIC, "POST", "/sys/kbapp/api/wdkbcx/getMyScheduledCampus.do", setOf("XNXQDM")),
    SECTIONS(Domain.ACADEMIC, "POST", "/sys/kbapp/api/wdkbcx/getMySectionList.do", setOf("XNXQDM", "XQDM", "ZC")),
    TIMETABLE(Domain.ACADEMIC, "POST", "/sys/kbapp/api/wdkbcx/getMyScheduleDetail.do", setOf("XNXQDM", "XQDM", "ZC")),
    GRADE_TERMS(Domain.ACADEMIC, "POST", "/sys/cjzhcxapp/modules/wdcj/cxwdcjxnxq.do", emptySet()),
    GRADES(Domain.ACADEMIC, "POST", "/sys/cjzhcxapp/modules/wdcj/cxwdcj.do", setOf("querySetting")),
    GRADE_DETAIL(Domain.ACADEMIC, "POST", "/sys/cjzhcxapp/api/wdcj/details.do", setOf("WID")),
    GRADE_SUMMARY(Domain.ACADEMIC, "POST", "/sys/cjzhcxapp/api/wdcj/queryPjxfjd.do", emptySet()),
    EXAMS(Domain.ACADEMIC, "POST", "/sys/wdkwapp/api/wdks/queryMyExamArrangeMent.do", setOf("XNXQDM")),
    PORTAL_INFO(Domain.PORTAL, "GET", "/personal/frontend/data/info", emptySet()),
    BALANCE_ITEMS(Domain.PORTAL, "GET", "/personal/frontend/data/items", setOf("type")),
    BALANCE_DETAIL(Domain.PORTAL, "GET", "/personal/frontend/data/detail", setOf("id")),
    MESSAGES(Domain.PORTAL, "GET", "/ucs/frontend/msg/index", setOf("keyword", "ucs_type", "source", "starttime", "page", "pagesize", "status")),
    TASKS(Domain.PORTAL, "GET", "/personal/frontend/task", setOf("type", "page", "pageSize")),
    APPLICATIONS(Domain.PORTAL, "GET", "/personal/frontend/task/apply", setOf("type", "page", "pageSize"));

    val baseUrl: String get() = when (domain) {
        Domain.ACADEMIC -> "https://jwxt.neu.edu.cn/jwapp"
        Domain.PORTAL -> "https://personal.neu.edu.cn/portal"
    }
}

class SchoolHttpException(val safeError: QueryError, val retryAfterMillis: Long? = null) : Exception(safeError.message)

class SchoolHttp(private val session: LocalSession, client: OkHttpClient? = null) {
    private val client = client ?: OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()
    private val gate = RequestGate()

    init { session.addScopeListener { gate.cancelAll() } }

    fun allowImmediateRetry() = gate.allowImmediateRetry()

    suspend fun execute(call: SchoolCall, parameters: Map<String, String> = emptyMap(), includeCookies: Boolean = true): String {
        require(parameters.keys.all { it in call.keys }) { "Unsupported query parameter" }
        val scope = session.state.value.accountScope
        val key = buildString {
            append(scope).append('|').append(call.name).append('|').append(includeCookies)
            parameters.toSortedMap().forEach { (k, v) -> append('|').append(k.length).append(':').append(k).append(v.length).append(':').append(v) }
        }
        return gate.run(key, call.domain) { executeOnce(call, parameters, includeCookies, scope) }
    }

    private suspend fun executeOnce(call: SchoolCall, parameters: Map<String, String>, includeCookies: Boolean, scope: String?): String = withContext(Dispatchers.IO) {
        if (scope != session.state.value.accountScope) throw CancellationException("Account scope changed")
        val base = (call.baseUrl + call.path).toHttpUrl()
        val url = if (call.method == "GET") base.newBuilder().apply {
            parameters.toSortedMap().forEach { (k, v) -> addQueryParameter(k, v) }
        }.build() else base
        val builder = Request.Builder().url(url).header("Accept", "application/json, text/javascript, */*; q=0.01")
        if (includeCookies) session.cookieHeader(url.toString())?.let { builder.header("Cookie", it) }
        val request = if (call.method == "POST") {
            val body = FormBody.Builder().apply { parameters.toSortedMap().forEach { (k, v) -> add(k, v) } }.build()
            builder.post(body).build()
        } else builder.get().build()
        try {
            client.newCall(request).awaitResponse().use { response ->
                if (includeCookies && scope != null) session.acceptSetCookies(scope, url.toString(), response.headers("Set-Cookie"))
                if (scope != session.state.value.accountScope) throw CancellationException("Account scope changed")
                if (response.code in 500..599 && "html" in response.header("Content-Type").orEmpty().lowercase()) {
                    val page = response.body?.string().orEmpty()
                    if (SchoolBodyClassifier.isMaintenance(page)) {
                        throw SchoolHttpException(QueryError(QueryErrorKind.MAINTENANCE, "学校页面正在维护", true))
                    }
                }
                SchoolResponseClassifier.status(response.code, response.header("Location"), response.header("Retry-After"))?.let { throw it }
                val body = response.body?.string()?.trim().orEmpty()
                SchoolBodyClassifier.validate(response.header("Content-Type").orEmpty(), body)
                coroutineContext.ensureActive()
                if (scope != session.state.value.accountScope) throw CancellationException("Account scope changed")
                body
            }
        } catch (error: IOException) {
            coroutineContext.ensureActive()
            throw SchoolHttpException(QueryError(QueryErrorKind.NETWORK, "网络连接失败，请稍后重试", true))
        }
    }
}

private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) else response.close()
        }
    })
}

object SchoolResponseClassifier {
    fun status(code: Int, location: String?, retryAfter: String?): SchoolHttpException? {
        val error = when {
            code == 401 -> QueryError(QueryErrorKind.AUTH_REQUIRED, "学校登录状态需要恢复", true)
            code == 304 -> QueryError(QueryErrorKind.NOT_MODIFIED, "学校返回 304，当前查询没有可复用的条件响应", true)
            code in 300..399 && isAuthLocation(location) -> QueryError(QueryErrorKind.AUTH_REQUIRED, "学校跳转到认证入口", true)
            code in 300..399 -> QueryError(QueryErrorKind.REDIRECT, "学校查询发生了未确认的跳转", false)
            code == 403 -> QueryError(QueryErrorKind.FORBIDDEN, "学校账户暂无此查询权限", false)
            code == 429 -> QueryError(QueryErrorKind.RATE_LIMITED, "学校请求过于频繁，请稍后重试", true)
            code !in 200..299 -> QueryError(QueryErrorKind.SERVER, "学校服务暂时不可用", true)
            else -> null
        } ?: return null
        val delay = if (code == 429) retryAfterDelay(retryAfter) else null
        return SchoolHttpException(error, delay)
    }

    private fun retryAfterDelay(value: String?): Long? {
        val raw = value?.trim() ?: return null
        raw.toLongOrNull()?.let { return it.coerceIn(0, 300) * 1000 }
        val date = runCatching {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US).parse(raw)?.time
        }.getOrNull() ?: return null
        return (date - System.currentTimeMillis()).coerceIn(0, 300_000)
    }

    private fun isAuthLocation(location: String?): Boolean {
        val value = location?.lowercase() ?: return false
        return value.contains("pass.neu.edu.cn") || value.contains("/login") || value.contains("/cas/") || value.contains("webvpn")
    }
}

object SchoolBodyClassifier {
    fun isMaintenance(body: String): Boolean =
        listOf("maintenance", "维护", "系统升级", "temporarily unavailable").any { it in body.lowercase() }

    fun validate(contentType: String, body: String) {
        if (body.startsWith("<") || "text/html" in contentType.lowercase()) {
            val lower = body.lowercase()
            val error = when {
                isMaintenance(body) ->
                    QueryError(QueryErrorKind.MAINTENANCE, "学校页面正在维护", true)
                listOf("password", "login", "cas", "统一身份认证", "登录", "认证").any { it in lower } ->
                    QueryError(QueryErrorKind.AUTH_REQUIRED, "学校返回了登录页面，需要重新认证", true)
                else -> QueryError(QueryErrorKind.SCHEMA_CHANGED, "学校返回了非业务网页", false)
            }
            throw SchoolHttpException(error)
        }
        if (!body.startsWith("{") && !body.startsWith("[")) {
            throw SchoolHttpException(QueryError(QueryErrorKind.SCHEMA_CHANGED, "学校响应格式已变化", false))
        }
    }
}

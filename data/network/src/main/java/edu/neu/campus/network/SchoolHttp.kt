package edu.neu.campus.network

import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.QueryError
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

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

class SchoolHttpException(val safeError: QueryError) : Exception(safeError.message)

class SchoolHttp(private val session: LocalSession, client: OkHttpClient? = null) {
    private val client = (client ?: OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build())

    suspend fun execute(call: SchoolCall, parameters: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        require(parameters.keys.all { it in call.keys }) { "Unsupported query parameter" }
        val base = (call.baseUrl + call.path).toHttpUrl()
        val url = if (call.method == "GET") {
            base.newBuilder().apply { parameters.forEach { (k, v) -> addQueryParameter(k, v) } }.build()
        } else base
        val builder = Request.Builder().url(url).header("Accept", "application/json, text/javascript, */*; q=0.01")
        session.cookieHeader(url.toString())?.let { builder.header("Cookie", it) }
        val request = if (call.method == "POST") {
            val body = FormBody.Builder().apply { parameters.forEach { (k, v) -> add(k, v) } }.build()
            builder.post(body).build()
        } else builder.get().build()
        try {
            client.newCall(request).execute().use { response ->
                response.headers("Set-Cookie").forEach { session.acceptSetCookie(url.toString(), it) }
                val code = response.code
                if (code == 401 || code in 300..399) throw SchoolHttpException(QueryError(QueryErrorKind.AUTH_REQUIRED, "学校登录状态需要恢复", true))
                if (code == 403) throw SchoolHttpException(QueryError(QueryErrorKind.FORBIDDEN, "学校账户暂无此查询权限", false))
                if (code !in 200..299) throw SchoolHttpException(QueryError(QueryErrorKind.SERVER, "学校服务暂时不可用", true))
                val contentType = response.header("Content-Type").orEmpty().lowercase()
                val body = response.body?.string()?.trim().orEmpty()
                if (body.startsWith("<") || "text/html" in contentType) {
                    throw SchoolHttpException(QueryError(QueryErrorKind.AUTH_REQUIRED, "学校返回了登录页面，需要重新认证", true))
                }
                if (!body.startsWith("{") && !body.startsWith("[")) {
                    throw SchoolHttpException(QueryError(QueryErrorKind.SCHEMA_CHANGED, "学校响应格式已变化", false))
                }
                body
            }
        } catch (e: IOException) {
            throw SchoolHttpException(QueryError(QueryErrorKind.NETWORK, "网络连接失败，请稍后重试", true))
        }
    }
}

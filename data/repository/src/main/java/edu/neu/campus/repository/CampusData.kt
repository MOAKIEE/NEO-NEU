package edu.neu.campus.repository

import android.content.Context
import edu.neu.campus.academic.AcademicApi
import edu.neu.campus.contract.*
import edu.neu.campus.database.QueryCache
import edu.neu.campus.network.SchoolCall
import edu.neu.campus.network.SchoolHttp
import edu.neu.campus.network.SchoolHttpException
import edu.neu.campus.portal.PortalApi
import edu.neu.campus.portal.PortalAuthException
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

/** Application-scoped entry point. Keep one instance for the process. */
class CampusData private constructor(context: Context) {
    val localSession = LocalSession.get(context)
    private val cache = QueryCache(context)
    private val http = SchoolHttp(localSession)
    private val academicApi = AcademicApi(http)
    private val portalApi = PortalApi(http)
    private val slots = ConcurrentHashMap<String, Slot<*>>()
    private val mutableShapes = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Debug host may inspect field names and types only; response values never leave the adapter. */
    val responseShapes: StateFlow<Map<String, String>> = mutableShapes
    val session: SessionRepository = object : SessionRepository {
        override val state: StateFlow<SessionState> = localSession.state
        override suspend fun signOut() {
            val oldScope = state.value.accountScope
            localSession.signOut()
            if (oldScope != null) cache.clearScope(oldScope)
        }
    }
    val academic: AcademicRepository = AcademicImpl()
    val portal: PortalRepository = PortalImpl()

    init { localSession.addScopeListener {
        slots.values.forEach { it.invalidate() }; slots.clear(); mutableShapes.value = emptyMap()
    } }

    companion object {
        @Volatile private var instance: CampusData? = null
        fun get(context: Context): CampusData = instance ?: synchronized(this) {
            instance ?: CampusData(context.applicationContext).also { instance = it }
        }
    }

    private class Slot<T>(initial: QuerySnapshot<T>) {
        val state = MutableStateFlow(initial)
        val lock = Mutex()
        fun invalidate() { state.value = QuerySnapshot() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> slot(key: String, parse: (String) -> T): Slot<T> {
        val scope = localSession.state.value.accountScope
        val fullKey = "$scope|$key"
        return slots.getOrPut(fullKey) {
            val cached = if (scope == null) null else cache.read(scope, key)
            val value = cached?.let { runCatching { parse(it.payload) }.getOrNull() }
            Slot(QuerySnapshot(value, if (value == null) QueryPhase.IDLE else QueryPhase.READY,
                if (value == null) null else cached?.savedAtEpochMillis, value != null))
        } as Slot<T>
    }

    private suspend fun <T> refresh(key: String, domain: Domain, parse: (String) -> T, fetch: suspend () -> String) {
        val scope = localSession.state.value.accountScope
        val holder = slot(key, parse)
        holder.lock.withLock {
            val before = holder.state.value
            if (scope == null) {
                holder.state.value = before.copy(phase = QueryPhase.FAILED, isStale = before.data != null,
                    error = QueryError(QueryErrorKind.AUTH_REQUIRED, "请先登录学校账号", true))
                return
            }
            holder.state.value = before.copy(phase = QueryPhase.LOADING, isStale = before.data != null, error = null)
            try {
                val raw = fetch()
                mutableShapes.value = mutableShapes.value + (key to ResponseShape.describe(raw))
                val parsed = parse(raw)
                if (localSession.state.value.accountScope != scope) return
                val now = System.currentTimeMillis()
                cache.save(scope, key, raw, now)
                holder.state.value = QuerySnapshot(parsed, QueryPhase.READY, now, false, null)
                localSession.mark(domain, DomainStatus.READY)
            } catch (error: Exception) {
                if (localSession.state.value.accountScope != scope) return
                val safe = when (error) {
                    is SchoolHttpException -> error.safeError
                    is PortalAuthException -> QueryError(QueryErrorKind.AUTH_REQUIRED, "门户登录状态需要恢复", true)
                    is JSONException, is edu.neu.campus.academic.SchemaException,
                    is edu.neu.campus.portal.PortalSchemaException -> QueryError(QueryErrorKind.SCHEMA_CHANGED, "学校返回字段尚未核验或发生变化", false)
                    else -> QueryError(QueryErrorKind.UNKNOWN, "查询失败，请稍后重试", true)
                }
                if (safe.kind == QueryErrorKind.AUTH_REQUIRED) localSession.mark(domain, DomainStatus.EXPIRED)
                holder.state.value = before.copy(phase = QueryPhase.FAILED, isStale = before.data != null, error = safe)
            }
        }
    }

    private inner class AcademicImpl : AcademicRepository {
        private fun termParams(term: String) = mapOf("XNXQDM" to term)
        private fun parseTerms(raw: String): List<Term> {
            val root = JSONObject(raw)
            return academicApi.terms(root.getString("list"), root.getString("current"))
        }
        override fun terms() = slot("terms", ::parseTerms).state
        override suspend fun refreshTerms() = refresh("terms", Domain.ACADEMIC, ::parseTerms) {
            JSONObject().put("list", academicApi.raw(SchoolCall.TERMS, mapOf("*order" to "+DM")))
                .put("current", academicApi.raw(SchoolCall.CURRENT_TERM,
                    mapOf("CSDM" to "SYS", "ZCSDM" to "DQXNXQDM", "SFSY" to "1"))).toString()
        }
        override fun weeks(termId: String) = slot("weeks:$termId", academicApi::weeks).state
        override suspend fun refreshWeeks(termId: String) = refresh("weeks:$termId", Domain.ACADEMIC, academicApi::weeks) {
            academicApi.raw(SchoolCall.WEEKS, termParams(termId))
        }
        override fun campuses(termId: String) = slot("campuses:$termId", academicApi::campuses).state
        override suspend fun refreshCampuses(termId: String) = refresh("campuses:$termId", Domain.ACADEMIC, academicApi::campuses) {
            academicApi.raw(SchoolCall.CAMPUSES, termParams(termId))
        }
        private fun parseTimetable(term: String, week: Int?, raw: String): Timetable {
            val parts = JSONObject(raw).getJSONArray("campuses")
            val campuses = mutableListOf<Campus>()
            val sectionMap = linkedMapOf<String, List<Section>>()
            val arranged = mutableListOf<CourseOccurrence>()
            val unscheduled = mutableListOf<UnscheduledCourse>()
            val practice = mutableListOf<UnscheduledCourse>()
            for (i in 0 until parts.length()) {
                val part = parts.getJSONObject(i)
                val campus = Campus(part.getString("id"), part.optString("name").takeIf { it.isNotBlank() })
                val sections = academicApi.sections(part.getString("sections"))
                val table = academicApi.timetable(part.getString("schedule"), term, week, campus, sections)
                campuses += campus; sectionMap[campus.id] = sections
                arranged += table.arranged; unscheduled += table.unscheduled; practice += table.practice
            }
            return Timetable(term, week, campuses, sectionMap, arranged, unscheduled, practice)
        }
        override fun timetable(termId: String, week: Int?) = slot("table:$termId:$week", { parseTimetable(termId, week, it) }).state
        override suspend fun refreshTimetable(termId: String, week: Int?) = refresh("table:$termId:$week", Domain.ACADEMIC, { parseTimetable(termId, week, it) }) {
            val campusBody = academicApi.raw(SchoolCall.CAMPUSES, termParams(termId))
            val campuses = academicApi.campuses(campusBody)
            val parts = JSONArray()
            for (campus in campuses) {
                val params = mutableMapOf("XNXQDM" to termId, "XQDM" to campus.id)
                if (week != null) params["ZC"] = week.toString()
                parts.put(JSONObject().put("id", campus.id).put("name", campus.name)
                    .put("sections", academicApi.raw(SchoolCall.SECTIONS, params))
                    .put("schedule", academicApi.raw(SchoolCall.TIMETABLE, params)))
            }
            JSONObject().put("campuses", parts).toString()
        }
        override fun grades(termId: String) = slot("grades:$termId", academicApi::grades).state
        override suspend fun refreshGrades(termId: String) = refresh("grades:$termId", Domain.ACADEMIC, academicApi::grades) {
            val query = JSONArray().put(JSONObject().put("name", "XNXQDM").put("value", termId).put("builder", "m_value_equal").put("linkOpt", "AND"))
            academicApi.raw(SchoolCall.GRADES, mapOf("querySetting" to query.toString()))
        }
        override fun gradeSummary() = slot("grade-summary", academicApi::gradeSummary).state
        override suspend fun refreshGradeSummary() = refresh("grade-summary", Domain.ACADEMIC, academicApi::gradeSummary) {
            academicApi.raw(SchoolCall.GRADE_SUMMARY)
        }
        override fun exams(termId: String) = slot("exams:$termId", academicApi::exams).state
        override suspend fun refreshExams(termId: String) = refresh("exams:$termId", Domain.ACADEMIC, academicApi::exams) {
            academicApi.raw(SchoolCall.EXAMS, termParams(termId))
        }
    }

    private inner class PortalImpl : PortalRepository {
        private fun parseBalances(raw: String): List<Balance> {
            val root = JSONObject(raw)
            val detailArray = root.getJSONArray("details")
            return portalApi.balances(root.getString("items"), (0 until detailArray.length()).map { detailArray.getString(it) })
        }
        override fun balances() = slot("balances", ::parseBalances).state
        override suspend fun refreshBalances() = refresh("balances", Domain.PORTAL, ::parseBalances) {
            val items = portalApi.raw(SchoolCall.BALANCE_ITEMS, mapOf("type" to "personal_data"))
            val details = JSONArray()
            for (id in portalApi.balanceItemIds(items)) details.put(portalApi.raw(SchoolCall.BALANCE_DETAIL, mapOf("id" to id)))
            JSONObject().put("items", items).put("details", details).toString()
        }
        private fun messageKey(page: Int, size: Int, status: Int) = "messages:$page:$size:$status"
        override fun messages(page: Int, pageSize: Int, status: Int) = slot(messageKey(page, pageSize, status), portalApi::messages).state
        override suspend fun refreshMessages(page: Int, pageSize: Int, status: Int) = refresh(messageKey(page, pageSize, status), Domain.PORTAL, portalApi::messages) {
            require(page > 0 && pageSize in 1..100 && status in 0..2)
            portalApi.raw(SchoolCall.MESSAGES, mapOf("keyword" to "", "ucs_type" to "", "source" to "", "starttime" to "",
                "page" to page.toString(), "pagesize" to pageSize.toString(), "status" to status.toString()))
        }
        private fun taskKey(kind: TaskKind, page: Int, size: Int) = "tasks:$kind:$page:$size"
        override fun tasks(kind: TaskKind, page: Int, pageSize: Int) = slot(taskKey(kind, page, pageSize), { portalApi.tasks(it, kind) }).state
        override suspend fun refreshTasks(kind: TaskKind, page: Int, pageSize: Int) = refresh(taskKey(kind, page, pageSize), Domain.PORTAL, { portalApi.tasks(it, kind) }) {
            require(page > 0 && pageSize in 1..100)
            val call = if (kind == TaskKind.APPLICATION) SchoolCall.APPLICATIONS else SchoolCall.TASKS
            val type = when (kind) { TaskKind.TODO -> "todo"; TaskKind.DONE -> "done"; TaskKind.APPLICATION -> "all" }
            portalApi.raw(call, mapOf("type" to type, "page" to page.toString(), "pageSize" to pageSize.toString()))
        }
    }
}

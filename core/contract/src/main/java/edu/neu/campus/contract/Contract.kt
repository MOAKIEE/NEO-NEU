package edu.neu.campus.contract

import kotlinx.coroutines.flow.StateFlow

/** Frozen data-layer contract for the first native frontend integration. */
object ContractVersion { const val V1 = 1 }

enum class QueryPhase { IDLE, LOADING, READY, FAILED }
enum class QueryErrorKind { AUTH_REQUIRED, FORBIDDEN, NETWORK, SERVER, SCHEMA_CHANGED, UNSUPPORTED, UNKNOWN }
data class QueryError(val kind: QueryErrorKind, val message: String, val retryable: Boolean)
data class QuerySnapshot<T>(
    val data: T? = null,
    val phase: QueryPhase = QueryPhase.IDLE,
    val lastSuccessEpochMillis: Long? = null,
    val isStale: Boolean = false,
    val error: QueryError? = null
)

object SnapshotTransitions {
    fun <T> loading(previous: QuerySnapshot<T>): QuerySnapshot<T> = previous.copy(
        phase = QueryPhase.LOADING, isStale = previous.data != null, error = null
    )
    fun <T> succeeded(value: T, time: Long): QuerySnapshot<T> = QuerySnapshot(value, QueryPhase.READY, time, false, null)
    fun <T> failed(previous: QuerySnapshot<T>, error: QueryError): QuerySnapshot<T> = previous.copy(
        phase = QueryPhase.FAILED, isStale = previous.data != null, error = error
    )
}

enum class Domain { PORTAL, ACADEMIC }
enum class DomainStatus { SIGNED_OUT, AUTHENTICATING, UNVERIFIED, READY, EXPIRED, UNREACHABLE }
data class SessionState(val accountScope: String?, val portal: DomainStatus, val academic: DomainStatus)

data class Term(val id: String, val name: String, val isCurrent: Boolean = false)
data class TeachingWeek(val number: Int, val startDate: String?, val endDate: String?, val isCurrent: Boolean)
data class Campus(val id: String, val name: String?)
data class Section(val code: String, val name: String?, val sourceId: String?, val startTime: String?, val endTime: String?)
data class CourseOccurrence(
    val sourceId: String?, val campusId: String, val title: String,
    val dayOfWeek: Int, val beginSection: Int, val endSection: Int,
    val beginTime: String?, val endTime: String?, val teacher: String?, val place: String?,
    val scheduleDescription: String?
)
data class UnscheduledCourse(val sourceId: String?, val campusId: String, val title: String, val reason: String?)
data class Timetable(
    val termId: String, val week: Int?, val campuses: List<Campus>,
    val sectionsByCampus: Map<String, List<Section>>,
    val arranged: List<CourseOccurrence>,
    val unscheduled: List<UnscheduledCourse>,
    val practice: List<UnscheduledCourse>
)
data class Grade(
    val sourceId: String, val termId: String, val courseCode: String,
    val courseName: String, val rawScore: String?, val credit: String?,
    val officialGradePoint: String?, val passDescription: String?, val retakeDescription: String?
)
data class GradeComponent(val code: String, val name: String, val rawValue: String?, val passed: Boolean?, val highestInProportion: Boolean?)
data class GradeDetail(val sourceId: String, val rawScore: String?, val officialGradePoint: String?, val passed: Boolean?, val components: List<GradeComponent>)
data class GradeSummary(val officialGpa: String?, val scope: String)
data class Exam(val courseName: String, val timeDescription: String?, val place: String?, val seat: String?, val status: String?, val arranged: Boolean)
enum class BalanceKind { CAMPUS_CARD, NETWORK }
data class Balance(val kind: BalanceKind, val rawValue: String?, val unit: String?, val isMasked: Boolean, val sourceUpdatedAt: String?)
data class CampusMessage(val id: String, val title: String, val contentLines: List<String>, val time: String?, val serverRead: Boolean?, val source: String?)
enum class TaskKind { TODO, DONE, APPLICATION }
data class CampusTask(val id: String, val title: String, val time: String?, val kind: TaskKind)
data class Page<T>(val items: List<T>, val total: Int?, val unreadCount: Int? = null)

interface SessionRepository {
    val state: StateFlow<SessionState>
    suspend fun verify()
    suspend fun signOut()
}

interface AcademicRepository {
    fun terms(): StateFlow<QuerySnapshot<List<Term>>>
    suspend fun refreshTerms()
    fun weeks(termId: String): StateFlow<QuerySnapshot<List<TeachingWeek>>>
    suspend fun refreshWeeks(termId: String)
    fun campuses(termId: String): StateFlow<QuerySnapshot<List<Campus>>>
    suspend fun refreshCampuses(termId: String)
    fun timetable(termId: String, week: Int?): StateFlow<QuerySnapshot<Timetable>>
    suspend fun refreshTimetable(termId: String, week: Int?)
    fun gradeTermIds(): StateFlow<QuerySnapshot<List<String>>>
    suspend fun refreshGradeTermIds()
    fun grades(termId: String): StateFlow<QuerySnapshot<List<Grade>>>
    suspend fun refreshGrades(termId: String)
    fun gradeDetail(termId: String, sourceId: String): StateFlow<QuerySnapshot<GradeDetail>>
    suspend fun refreshGradeDetail(termId: String, sourceId: String)
    fun gradeSummary(): StateFlow<QuerySnapshot<GradeSummary>>
    suspend fun refreshGradeSummary()
    fun exams(termId: String): StateFlow<QuerySnapshot<List<Exam>>>
    suspend fun refreshExams(termId: String)
}

interface PortalRepository {
    fun balance(kind: BalanceKind): StateFlow<QuerySnapshot<Balance>>
    suspend fun refreshBalance(kind: BalanceKind)
    fun messages(page: Int, pageSize: Int, status: Int = 0): StateFlow<QuerySnapshot<Page<CampusMessage>>>
    suspend fun refreshMessages(page: Int, pageSize: Int, status: Int = 0)
    fun tasks(kind: TaskKind, page: Int, pageSize: Int): StateFlow<QuerySnapshot<Page<CampusTask>>>
    suspend fun refreshTasks(kind: TaskKind, page: Int, pageSize: Int)
}

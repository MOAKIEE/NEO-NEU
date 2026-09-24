package edu.neu.campus.app

import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.contract.BalanceKind
import edu.neu.campus.contract.TaskKind
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

enum class SyncReason { COLD_START, PAGE_ENTER, FOREGROUND, MANUAL }

/** One entry point for visible business data. Navigation and resume in the same round are merged. */
class SyncCoordinator {
    private val eventGate = VisibleEventGate()
    private val visibleOverrides = mutableMapOf<String, suspend () -> Unit>()

    fun registerVisible(tab: MainTab, destination: AppDestination, refresh: suspend () -> Unit): () -> Unit {
        val key = "$tab|$destination"
        synchronized(visibleOverrides) { visibleOverrides[key] = refresh }
        return { synchronized(visibleOverrides) { if (visibleOverrides[key] === refresh) visibleOverrides.remove(key) }; Unit }
    }

    suspend fun requestVisible(tab: MainTab, destination: AppDestination, reason: SyncReason) {
        val scope = CampusDataProvider.session.state.value.accountScope ?: return
        val event = "$scope|$tab|$destination"
        val now = android.os.SystemClock.elapsedRealtime()
        if (!eventGate.shouldRun(event, reason, now)) return
        // A genuine entry, foreground return or explicit refresh can retry after an earlier failure.
        CampusDataProvider.allowImmediateRetry()
        val overrideKey = "$tab|$destination"
        var override = synchronized(visibleOverrides) { visibleOverrides[overrideKey] }
        if (override == null && reason != SyncReason.FOREGROUND) {
            // Compose installs the entering page's DisposableEffect after the route changes.
            delay(50)
            override = synchronized(visibleOverrides) { visibleOverrides[overrideKey] }
        }
        if (override != null) {
            override()
            return
        }
        val academic = CampusDataProvider.academic
        val portal = CampusDataProvider.portal
        suspend fun refreshCurrentTerm(withWeeks: Boolean, withExams: Boolean, withTable: Boolean) {
            academic.refreshTerms()
            val term = academic.terms().value.data?.firstOrNull { it.isCurrent } ?: return
            coroutineScope {
                if (withExams) launch { academic.refreshExams(term.id) }
                if (withWeeks) launch { academic.refreshWeeks(term.id) }
                if (withTable && !withWeeks) launch { academic.refreshTimetable(term.id, null) }
            }
            if (withTable && withWeeks) {
                val week = academic.weeks(term.id).value.data?.firstOrNull { it.isCurrent }
                if (week != null) academic.refreshTimetable(term.id, week.number)
            }
        }
        when (destination) {
            AppDestination.Main -> when (tab) {
                MainTab.TODAY -> coroutineScope {
                    launch { refreshCurrentTerm(withWeeks = true, withExams = true, withTable = true) }
                    launch { portal.refreshBalance(BalanceKind.CAMPUS_CARD) }
                    launch { portal.refreshBalance(BalanceKind.NETWORK) }
                    launch { portal.refreshMessages(1, 5) }
                    launch { portal.refreshTasks(TaskKind.TODO, 1, 5) }
                }
                MainTab.TIMETABLE -> {
                    refreshCurrentTerm(withWeeks = true, withExams = false, withTable = true)
                    academic.terms().value.data?.firstOrNull { it.isCurrent }?.let { academic.refreshCampuses(it.id) }
                }
                MainTab.QUERY, MainTab.SETTINGS -> Unit
            }
            AppDestination.Grades -> {
                coroutineScope {
                    launch { academic.refreshTerms() }
                    launch { academic.refreshGradeTermIds() }
                    launch { academic.refreshGradeSummary() }
                }
                academic.gradeTermIds().value.data?.firstOrNull()?.let { academic.refreshGrades(it) }
            }
            is AppDestination.GradeDetail -> {
                academic.refreshGrades(destination.termId)
                academic.refreshGradeDetail(destination.termId, destination.sourceId)
            }
            AppDestination.Exams -> refreshCurrentTerm(false, true, false)
            is AppDestination.ExamDetail -> academic.refreshExams(destination.termId)
            is AppDestination.BalanceDetail -> portal.refreshBalance(destination.kind)
            AppDestination.Messages -> portal.refreshMessages(1, 30)
            is AppDestination.MessageDetail -> portal.refreshMessages(destination.page, 30, destination.status)
            AppDestination.Tasks, is AppDestination.TaskDetail -> portal.refreshTasks(TaskKind.TODO, 1, 20)
            AppDestination.Schedule, AppDestination.BellSchedule -> {
                academic.refreshTerms()
                academic.terms().value.data?.firstOrNull { it.isCurrent }?.let {
                    coroutineScope {
                        launch { academic.refreshWeeks(it.id) }
                        launch { academic.refreshTimetable(it.id, null) }
                    }
                }
            }
            else -> Unit
        }
    }
}

/** Uses navigation identity, not Compose invocation count, to merge one UI round. */
class VisibleEventGate {
    private var lastEvent: String? = null
    private var lastEventAt = 0L

    @Synchronized fun shouldRun(event: String, reason: SyncReason, now: Long): Boolean {
        if (reason != SyncReason.MANUAL && lastEvent == event && now - lastEventAt < 900) return false
        lastEvent = event
        lastEventAt = now
        return true
    }
}

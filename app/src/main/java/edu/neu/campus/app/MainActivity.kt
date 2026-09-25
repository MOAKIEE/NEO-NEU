package edu.neu.campus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import edu.neu.campus.app.config.HomeLayoutConfigManager
import edu.neu.campus.app.feature.exams.ExamDetailScreen
import edu.neu.campus.app.feature.exams.ExamsScreen
import edu.neu.campus.app.feature.grades.GradeDetailScreen
import edu.neu.campus.app.feature.grades.GradesScreen
import edu.neu.campus.app.feature.timetable.TimetableScreen
import edu.neu.campus.app.feature.today.TodayScreen
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.authweb.OfficialLogin
import edu.neu.campus.authweb.AcademicSsoConnector
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text

class MainActivity : ComponentActivity() {
    private val authCoordinator = AuthCoordinator()
    private var academicReconnectJob: Job? = null
    private var openLoginAfterReconnect = false
    private var lastAutomaticReconnectScope: String? = null
    private var lastAutomaticReconnectAt = 0L
    private var connectingAcademic by mutableStateOf(false)

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (authCoordinator.complete(result.resultCode == RESULT_OK)) {
            lifecycleScope.launch {
                try {
                    CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, SyncReason.MANUAL)
                } finally {
                    authCoordinator.replayFinished()
                }
            }
        } else {
            lifecycleScope.launch { CampusDataProvider.session.verify() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        authCoordinator.restorePending(savedInstanceState?.getString("recovery_domain"))
        CampusDataProvider.init(this)
        ThemeManager.init(this)
        HomeLayoutConfigManager.init(this)
        edu.neu.campus.app.feature.balance.BalancePrivacyManager.init(this)
        edu.neu.campus.app.feature.messages.MessagesManager.init(this)
        if (!authCoordinator.suppressResume() && CampusDataProvider.session.state.value.accountScope != null) {
            lifecycleScope.launch { CampusDataProvider.session.verify() }
        }

        setContent {
            CampusTheme {
                val session by CampusDataProvider.session.state.collectAsState()
                val tab = AppNavigator.currentTab
                val destination = AppNavigator.currentDestination
                LaunchedEffect(session.accountScope, session.portal, session.academic) {
                    maybeReconnectAcademic()
                }
                LaunchedEffect(session.accountScope, tab, destination) {
                    CampusDataProvider.sync.requestVisible(tab, destination, SyncReason.PAGE_ENTER)
                }
                key(session.accountScope) {
                MainScreen(
                    todayScreen = {
                        TodayScreen(onLoginClick = { launchLogin() })
                    },
                    timetableScreen = {
                        TimetableScreen(onLoginClick = { launchLogin() })
                    },
                    queryScreen = {
                        edu.neu.campus.app.feature.query.QueryScreen()
                    },
                    settingsScreen = {
                        edu.neu.campus.app.feature.settings.SettingsScreen(
                            onLoginClick = { launchLogin() },
                            connectingAcademic = connectingAcademic
                        )
                    },
                    subScreen = { dest ->
                        when (dest) {
                            is AppDestination.Grades -> {
                                GradesScreen(
                                    onBack = { AppNavigator.popBack() },
                                    onLoginClick = { launchLogin() }
                                )
                            }
                            is AppDestination.GradeDetail -> {
                                if (dest.sourceId.isBlank()) {
                                    GradesScreen(
                                        onBack = { AppNavigator.popBack() },
                                        onLoginClick = { launchLogin() }
                                    )
                                } else {
                                    GradeDetailScreen(
                                        termId = dest.termId,
                                        sourceId = dest.sourceId,
                                        onBack = { AppNavigator.popBack() }
                                    )
                                }
                            }
                            is AppDestination.Exams -> {
                                ExamsScreen(
                                    onBack = { AppNavigator.popBack() },
                                    onLoginClick = { launchLogin() }
                                )
                            }
                            is AppDestination.ExamDetail -> {
                                if (dest.termId.isBlank()) {
                                    ExamsScreen(
                                        onBack = { AppNavigator.popBack() },
                                        onLoginClick = { launchLogin() }
                                    )
                                } else {
                                    ExamDetailScreen(
                                        termId = dest.termId,
                                        selectedExam = dest.exam,
                                        onBack = { AppNavigator.popBack() }
                                    )
                                }
                            }
                            is AppDestination.BalanceDetail -> {
                                edu.neu.campus.app.feature.balance.BalanceDetailScreen(
                                    kind = dest.kind,
                                    onBack = { AppNavigator.popBack() }
                                )
                            }
                            is AppDestination.Messages -> {
                                edu.neu.campus.app.feature.messages.MessagesScreen(
                                    onBack = { AppNavigator.popBack() },
                                    onLoginClick = { launchLogin() }
                                )
                            }
                            is AppDestination.MessageDetail -> {
                                if (dest.messageId.isBlank()) {
                                    edu.neu.campus.app.feature.messages.MessagesScreen(
                                        onBack = { AppNavigator.popBack() },
                                        onLoginClick = { launchLogin() }
                                    )
                                } else {
                                    edu.neu.campus.app.feature.messages.MessageDetailScreen(
                                        messageId = dest.messageId,
                                        page = dest.page,
                                        status = dest.status,
                                        onBack = { AppNavigator.popBack() }
                                    )
                                }
                            }
                            is AppDestination.Tasks, is AppDestination.TaskDetail -> {
                                edu.neu.campus.app.feature.tasks.TasksScreen(
                                    onBack = { AppNavigator.popBack() },
                                    onLoginClick = { launchLogin() }
                                )
                            }
                            is AppDestination.Schedule, is AppDestination.BellSchedule -> {
                                edu.neu.campus.app.feature.schedule.ScheduleScreen(
                                    onBack = { AppNavigator.popBack() },
                                    onLoginClick = { launchLogin() }
                                )
                            }
                            is AppDestination.ServicesCatalog -> {
                                edu.neu.campus.app.feature.services.ServicesCatalogScreen(
                                    onBack = { AppNavigator.popBack() }
                                )
                            }
                            is AppDestination.HomeSettings -> {
                                edu.neu.campus.app.feature.settings.HomeConfigScreen(
                                    onBack = { AppNavigator.popBack() }
                                )
                            }
                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    edu.neu.campus.ui.components.CampusTopBar(
                                        title = "详情",
                                        onBack = { AppNavigator.popBack() }
                                    )
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("此页面暂时无法打开", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                )
                }
            }
        }
    }

    private fun launchLogin() {
        val destination = AppNavigator.currentDestination
        val state = CampusDataProvider.session.state.value
        val academicPage = AppNavigator.currentTab == edu.neu.campus.app.navigation.MainTab.TIMETABLE ||
            destination is AppDestination.Grades || destination is AppDestination.GradeDetail ||
            destination is AppDestination.Exams || destination is AppDestination.ExamDetail ||
            destination is AppDestination.Schedule || destination is AppDestination.BellSchedule
        val domain = preferredLoginDomain(state, academicPage)
        if (domain == Domain.ACADEMIC && state.portal == DomainStatus.READY &&
            state.academic != DomainStatus.READY) {
            reconnectAcademic(openLoginOnFailure = true)
            return
        }
        openVisibleLogin(domain)
    }

    private fun maybeReconnectAcademic() {
        val state = CampusDataProvider.session.state.value
        if (authCoordinator.suppressResume() || state.accountScope == null ||
            state.portal != DomainStatus.READY ||
            state.academic !in setOf(DomainStatus.EXPIRED, DomainStatus.UNREACHABLE)) return
        val now = android.os.SystemClock.elapsedRealtime()
        if (lastAutomaticReconnectScope == state.accountScope && now - lastAutomaticReconnectAt < 60_000) return
        lastAutomaticReconnectScope = state.accountScope
        lastAutomaticReconnectAt = now
        reconnectAcademic(openLoginOnFailure = false)
    }

    private fun reconnectAcademic(openLoginOnFailure: Boolean) {
        if (academicReconnectJob?.isActive == true) {
            openLoginAfterReconnect = openLoginAfterReconnect || openLoginOnFailure
            return
        }
        val scope = CampusDataProvider.session.state.value.accountScope ?: return
        openLoginAfterReconnect = openLoginOnFailure
        academicReconnectJob = lifecycleScope.launch {
            connectingAcademic = true
            try {
                val connected = try {
                    AcademicSsoConnector.connect(this@MainActivity)
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Exception) {
                    false
                }
                if (CampusDataProvider.session.state.value.accountScope != scope) return@launch
                if (connected) {
                    CampusDataProvider.allowImmediateRetry()
                    lifecycleScope.launch {
                        CampusDataProvider.sync.requestVisible(
                            AppNavigator.currentTab, AppNavigator.currentDestination, SyncReason.MANUAL
                        )
                    }
                } else if (openLoginAfterReconnect) {
                    val fallback = if (CampusDataProvider.session.state.value.portal == DomainStatus.READY) {
                        Domain.ACADEMIC
                    } else Domain.PORTAL
                    openVisibleLogin(fallback)
                }
            } finally {
                connectingAcademic = false
                openLoginAfterReconnect = false
                academicReconnectJob = null
            }
        }
    }

    private fun openVisibleLogin(domain: Domain) {
        if (!authCoordinator.begin(domain)) return
        loginLauncher.launch(OfficialLogin.intent(this, domain))
    }

    override fun onResume() {
        super.onResume()
        // First resume is coalesced with the initial route event by SyncCoordinator.
        if (!authCoordinator.suppressResume()) lifecycleScope.launch {
            CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, SyncReason.FOREGROUND)
            maybeReconnectAcademic()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("recovery_domain", authCoordinator.pendingDomainName())
        super.onSaveInstanceState(outState)
    }
}

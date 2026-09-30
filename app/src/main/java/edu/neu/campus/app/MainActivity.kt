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
import androidx.lifecycle.Lifecycle
import edu.neu.campus.app.config.HomeLayoutConfigManager
import edu.neu.campus.app.feature.ecode.ECodePreferences
import edu.neu.campus.app.feature.ecode.ECodeScreen
import edu.neu.campus.app.feature.services.OfficialWebScreen
import edu.neu.campus.app.feature.exams.ExamDetailScreen
import edu.neu.campus.app.feature.exams.ExamsScreen
import edu.neu.campus.app.feature.grades.GradeDetailScreen
import edu.neu.campus.app.feature.grades.GradesScreen
import edu.neu.campus.app.feature.timetable.TimetableScreen
import edu.neu.campus.app.feature.today.TodayScreen
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.authweb.OfficialLogin
import edu.neu.campus.authweb.LoginResult
import edu.neu.campus.ecode.ECodeSsoConnector
import edu.neu.campus.ecode.OfficialECodeRepository
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.job
import kotlinx.coroutines.CoroutineStart
import top.yukonga.miuix.kmp.basic.Text

class MainActivity : ComponentActivity() {
    private val authCoordinator = AuthCoordinator()
    private var recoveryJob: Job? = null
    private var recoveryRequested = false
    internal var recoverSchool: suspend (Domain) -> LoginResult = { OfficialLogin.recover(this, it) }
    private var pendingRecovery: Pair<String, String>? = null // account scope and in-memory browser token
    private val automaticLoginGate = AutomaticLoginGate()
    private var lastECodeWarmScope: String? = null
    private var eCodeWarmJob: Job? = null
    private var connectingSchool by mutableStateOf(false)
    private var loginNotice by mutableStateOf<String?>(null)
    private var lastVerificationScope: String? = null
    private var lastVerificationAt = 0L
    private var officialPageRevision by mutableIntStateOf(0)
    private var officialPageLoginCancelled by mutableStateOf(false)

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) automaticLoginGate.loginSucceeded()
        if (AppNavigator.currentDestination is AppDestination.OfficialWeb) {
            officialPageLoginCancelled = result.resultCode != RESULT_OK
            officialPageRevision++
        }
        lastVerificationScope = CampusDataProvider.session.state.value.accountScope
        lastVerificationAt = android.os.SystemClock.elapsedRealtime()
        if (authCoordinator.complete(result.resultCode == RESULT_OK)) {
            lifecycleScope.launch {
                try {
                    CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, SyncReason.MANUAL)
                } finally {
                    authCoordinator.replayFinished()
                    maybeRecoverSession()
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
        ECodePreferences.init(this)
        edu.neu.campus.app.feature.balance.BalancePrivacyManager.init(this)
        edu.neu.campus.app.feature.messages.MessagesManager.init(this)
        setContent {
            CampusTheme {
                val session by CampusDataProvider.session.state.collectAsState()
                val tab = AppNavigator.currentTab
                val destination = AppNavigator.currentDestination
                LaunchedEffect(destination) { officialPageLoginCancelled = false }
                LaunchedEffect(session.accountScope, session.portal, session.academic) {
                    maybeRecoverSession()
                    maybeWarmECode()
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
                            connectingSchool = connectingSchool,
                            loginNotice = loginNotice
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
                                        onBack = { AppNavigator.popBack() },
                                        onLoginClick = { launchLogin() }
                                    )
                                }
                            }
                            is AppDestination.BalanceDetail -> {
                                edu.neu.campus.app.feature.balance.BalanceDetailScreen(
                                    kind = dest.kind,
                                    onBack = { AppNavigator.popBack() },
                                    onLoginClick = { launchLogin() }
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
                            is AppDestination.ECode -> {
                                ECodeScreen(onBack = { AppNavigator.popBack() })
                            }
                            is AppDestination.OfficialWeb -> {
                                OfficialWebScreen(service = dest.service, onBack = { AppNavigator.popBack() },
                                    loginRevision = officialPageRevision, loginCancelled = officialPageLoginCancelled,
                                    onLogin = { result ->
                                        when (result) {
                                            is LoginResult.ContinueOnWeb -> openVisibleLogin(Domain.PORTAL, result.token)
                                            is LoginResult.NeedCredentials -> openVisibleLogin(Domain.PORTAL, credentialRejected = result.rejected)
                                            else -> openVisibleLogin(Domain.PORTAL)
                                        }
                                    })
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
        if (authCoordinator.suppressResume()) return
        eCodeWarmJob?.cancel()
        loginNotice = null
        val pending = pendingRecovery
        pendingRecovery = null
        if (pending != null && pending.first != state.accountScope) OfficialLogin.discardContinuation(pending.second)
        val continuation = pending?.takeIf { it.first == state.accountScope }?.second
        if (continuation != null) {
            // This attempt already reached an interactive challenge; keep its browser and budget.
            openVisibleLogin(domain, continuation)
        } else recoverSession(domain, requested = true)
    }

    private fun maybeRecoverSession() {
        pendingRecovery?.let { pending ->
            if (pending.first != CampusDataProvider.session.state.value.accountScope) {
                OfficialLogin.discardContinuation(pending.second)
                pendingRecovery = null
                loginNotice = null
            }
        }
        // Official pages own their portal-first recovery and the pending school callback.
        if (AppNavigator.currentDestination is AppDestination.OfficialWeb) return
        val state = CampusDataProvider.session.state.value
        val scope = state.accountScope ?: return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) || authCoordinator.suppressResume() ||
            recoveryJob?.isActive == true) return
        val domain = when {
            state.portal == DomainStatus.EXPIRED -> Domain.PORTAL
            state.portal == DomainStatus.READY && state.academic == DomainStatus.EXPIRED -> Domain.ACADEMIC
            else -> return
        }
        val now = android.os.SystemClock.elapsedRealtime()
        if (!automaticLoginGate.begin(scope, domain, now)) return
        recoverSession(domain)
    }

    private fun maybeWarmECode() {
        if (authCoordinator.suppressResume() || eCodeWarmJob?.isActive == true) return
        if (AppNavigator.currentDestination is AppDestination.OfficialWeb) return
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        val state = CampusDataProvider.session.state.value
        val scope = state.accountScope ?: return
        if (state.portal != DomainStatus.READY || state.academic != DomainStatus.READY ||
            lastECodeWarmScope == scope) return
        lastECodeWarmScope = scope
        eCodeWarmJob = lifecycleScope.launch {
            try {
                ECodeSsoConnector.warm(this@MainActivity, OfficialECodeRepository(this@MainActivity))
            } catch (cancelled: CancellationException) {
                lastECodeWarmScope = null
                throw cancelled
            } catch (_: Exception) {
                // The e-code page keeps its own visible authentication fallback.
            }
        }
    }

    private fun recoverSession(domain: Domain, requested: Boolean = false) {
        if (authCoordinator.suppressResume()) return
        if (requested) recoveryRequested = true
        if (recoveryJob?.isActive == true) return
        val scope = CampusDataProvider.session.state.value.accountScope
        recoveryJob = lifecycleScope.launch(start = CoroutineStart.LAZY) {
            connectingSchool = true
            loginNotice = null
            try {
                val result = recoverSchool(domain)
                if (CampusDataProvider.session.state.value.accountScope != scope) {
                    if (result is LoginResult.ContinueOnWeb) OfficialLogin.discardContinuation(result.token)
                    return@launch
                }
                when (result) {
                    LoginResult.Connected -> {
                        if (recoveryRequested) automaticLoginGate.loginSucceeded()
                        CampusDataProvider.allowImmediateRetry()
                        CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, SyncReason.MANUAL)
                    }
                    is LoginResult.ContinueOnWeb -> {
                        scope?.let { automaticLoginGate.pause(it, domain) }
                        if (recoveryRequested && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            openVisibleLogin(domain, result.token)
                        } else if (scope != null) {
                            pendingRecovery?.let { OfficialLogin.discardContinuation(it.second) }
                            pendingRecovery = scope to result.token
                            loginNotice = "学校要求继续验证，请点击登录学校账号。"
                        } else OfficialLogin.discardContinuation(result.token)
                    }
                    is LoginResult.NeedCredentials -> {
                        scope?.let { automaticLoginGate.pause(it, domain) }
                        loginNotice = if (result.rejected) "账号密码未通过学校认证，自动登录已暂停。" else "请填写账号密码以启用自动登录。"
                        if (recoveryRequested) openVisibleLogin(domain, credentialRejected = result.rejected)
                    }
                    is LoginResult.Failed -> {
                        loginNotice = result.message
                        if (recoveryRequested) openVisibleLogin(domain)
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                loginNotice = "学校连接暂时无法恢复，请稍后重试。"
                if (recoveryRequested && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) openVisibleLogin(domain)
            } finally {
                if (recoveryJob === coroutineContext.job) {
                    connectingSchool = false
                    recoveryJob = null
                    recoveryRequested = false
                }
            }
        }
        recoveryJob?.start()
    }

    private fun openVisibleLogin(domain: Domain, continuation: String? = null, credentialRejected: Boolean = false) {
        if (!authCoordinator.begin(domain)) {
            continuation?.let { OfficialLogin.discardContinuation(it) }
            return
        }
        loginLauncher.launch(OfficialLogin.intent(this, domain, continuation, credentialRejected, silentAttempted = true))
    }

    override fun onResume() {
        super.onResume()
        maybeWarmECode()
        // First resume is coalesced with the initial route event by SyncCoordinator.
        if (!authCoordinator.suppressResume()) lifecycleScope.launch {
            val state = CampusDataProvider.session.state.value
            val now = android.os.SystemClock.elapsedRealtime()
            if (state.accountScope != null && (state.accountScope != lastVerificationScope ||
                now - lastVerificationAt >= 60_000 || state.portal == DomainStatus.UNVERIFIED || state.academic == DomainStatus.UNVERIFIED)) {
                lastVerificationScope = state.accountScope
                lastVerificationAt = now
                CampusDataProvider.session.verify()
            }
            CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, SyncReason.FOREGROUND)
            maybeRecoverSession()
        }
    }

    override fun onStop() {
        recoveryRequested = false
        recoveryJob?.cancel()
        eCodeWarmJob?.cancel()
        super.onStop()
    }

    override fun onDestroy() {
        pendingRecovery?.let { OfficialLogin.discardContinuation(it.second) }
        pendingRecovery = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("recovery_domain", authCoordinator.pendingDomainName())
        super.onSaveInstanceState(outState)
    }
}

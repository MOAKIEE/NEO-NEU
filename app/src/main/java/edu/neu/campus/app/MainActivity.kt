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
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text

class MainActivity : ComponentActivity() {

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val state = CampusDataProvider.session.state.value
        if (result.resultCode != RESULT_OK ||
            state.portal != edu.neu.campus.contract.DomainStatus.READY ||
            state.academic != edu.neu.campus.contract.DomainStatus.READY
        ) {
            lifecycleScope.launch { CampusDataProvider.session.verify() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CampusDataProvider.init(this)
        ThemeManager.init(this)
        HomeLayoutConfigManager.init(this)
        edu.neu.campus.app.feature.balance.BalancePrivacyManager.init(this)
        edu.neu.campus.app.feature.messages.MessagesManager.init(this)

        // Only restore a session that existed at launch. A new login may start
        // before this coroutine is scheduled, and must not be probed prematurely.
        if (CampusDataProvider.session.state.value.accountScope != null) {
            lifecycleScope.launch { CampusDataProvider.session.verify() }
        }

        setContent {
            CampusTheme {
                val session by CampusDataProvider.session.state.collectAsState()
                LaunchedEffect(session.accountScope, session.academic == edu.neu.campus.contract.DomainStatus.READY) {
                    if (session.academic == edu.neu.campus.contract.DomainStatus.READY) {
                        val repo = CampusDataProvider.academic
                        repo.refreshTerms()
                        repo.terms().value.data?.firstOrNull { it.isCurrent }?.let { term ->
                            repo.refreshWeeks(term.id)
                            repo.weeks(term.id).value.data?.firstOrNull { it.isCurrent }?.let { week ->
                                repo.refreshTimetable(term.id, week.number)
                            }
                        }
                    }
                }
                LaunchedEffect(session.accountScope, session.portal == edu.neu.campus.contract.DomainStatus.READY) {
                    if (session.portal == edu.neu.campus.contract.DomainStatus.READY) {
                        CampusDataProvider.portal.refreshBalance(edu.neu.campus.contract.BalanceKind.CAMPUS_CARD)
                        CampusDataProvider.portal.refreshBalance(edu.neu.campus.contract.BalanceKind.NETWORK)
                    }
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
                            onLoginClick = { launchLogin() }
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
        loginLauncher.launch(OfficialLogin.intent(this))
    }
}

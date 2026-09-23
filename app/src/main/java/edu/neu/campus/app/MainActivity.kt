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

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        lifecycleScope.launch {
            CampusDataProvider.session.verify()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CampusDataProvider.init(this)
        ThemeManager.init(this)
        HomeLayoutConfigManager.init(this)
        edu.neu.campus.app.feature.balance.BalancePrivacyManager.init(this)
        edu.neu.campus.app.feature.messages.MessagesManager.init(this)

        // 启动时复验会话
        lifecycleScope.launch {
            CampusDataProvider.session.verify()
        }

        setContent {
            CampusTheme {
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
                                if (dest.courseName.isBlank()) {
                                    ExamsScreen(
                                        onBack = { AppNavigator.popBack() },
                                        onLoginClick = { launchLogin() }
                                    )
                                } else {
                                    ExamDetailScreen(
                                        courseName = dest.courseName,
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
                                    onBack = { AppNavigator.popBack() }
                                )
                            }
                            is AppDestination.MessageDetail -> {
                                if (dest.messageId.isBlank()) {
                                    edu.neu.campus.app.feature.messages.MessagesScreen(
                                        onBack = { AppNavigator.popBack() }
                                    )
                                } else {
                                    edu.neu.campus.app.feature.messages.MessageDetailScreen(
                                        messageId = dest.messageId,
                                        onBack = { AppNavigator.popBack() }
                                    )
                                }
                            }
                            is AppDestination.Tasks, is AppDestination.TaskDetail -> {
                                edu.neu.campus.app.feature.tasks.TasksScreen(
                                    onBack = { AppNavigator.popBack() }
                                )
                            }
                            is AppDestination.Schedule, is AppDestination.BellSchedule -> {
                                edu.neu.campus.app.feature.schedule.ScheduleScreen(
                                    onBack = { AppNavigator.popBack() }
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

    private fun launchLogin() {
        loginLauncher.launch(OfficialLogin.intent(this))
    }
}

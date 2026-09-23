package edu.neu.campus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

class MainActivity : ComponentActivity() {

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        lifecycleScope.launch {
            CampusDataProvider.session.verify()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CampusDataProvider.init(this)
        HomeLayoutConfigManager.init(this)

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
                    queryScreen = { PlaceholderPage("查询") },
                    settingsScreen = { PlaceholderPage("我的") },
                    subScreen = { dest ->
                        when (dest) {
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
                            else -> {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    TopAppBar(
                                        title = "详情",
                                        navigationIcon = {
                                            Button(onClick = { AppNavigator.popBack() }) {
                                                Text("‹ 返回")
                                            }
                                        }
                                    )
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Text("页面：$dest", fontSize = 16.sp)
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

@Composable
private fun PlaceholderPage(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, fontSize = 20.sp)
    }
}

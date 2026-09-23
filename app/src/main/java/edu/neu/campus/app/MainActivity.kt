package edu.neu.campus.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.config.HomeLayoutConfigManager
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CampusDataProvider.init(this)
        HomeLayoutConfigManager.init(this)

        setContent {
            CampusTheme {
                MainScreen(
                    todayScreen = { PlaceholderPage("今日") },
                    timetableScreen = { PlaceholderPage("课表") },
                    queryScreen = { PlaceholderPage("查询") },
                    settingsScreen = { PlaceholderPage("我的") },
                    subScreen = { dest ->
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
                )
            }
        }
    }
}

@Composable
private fun PlaceholderPage(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, fontSize = 20.sp)
    }
}

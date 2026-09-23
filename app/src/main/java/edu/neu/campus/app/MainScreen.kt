package edu.neu.campus.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import edu.neu.campus.app.demo.DemoModeBanner
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun MainScreen(
    todayScreen: @Composable () -> Unit,
    timetableScreen: @Composable () -> Unit,
    queryScreen: @Composable () -> Unit,
    settingsScreen: @Composable () -> Unit,
    subScreen: @Composable (AppDestination) -> Unit
) {
    val currentTab = AppNavigator.currentTab
    val destination = AppNavigator.currentDestination

    BackHandler(enabled = destination != AppDestination.Main) {
        AppNavigator.popBack()
    }

    if (destination != AppDestination.Main) {
        Column(modifier = Modifier.fillMaxSize()) {
            DemoModeBanner()
            subScreen(destination)
        }
    } else {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentTab == MainTab.TODAY,
                        onClick = { AppNavigator.navigateToTab(MainTab.TODAY) },
                        icon = Icons.Default.Home,
                        label = MainTab.TODAY.title
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.TIMETABLE,
                        onClick = { AppNavigator.navigateToTab(MainTab.TIMETABLE) },
                        icon = Icons.Default.DateRange,
                        label = MainTab.TIMETABLE.title
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.QUERY,
                        onClick = { AppNavigator.navigateToTab(MainTab.QUERY) },
                        icon = Icons.Default.Search,
                        label = MainTab.QUERY.title
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { AppNavigator.navigateToTab(MainTab.SETTINGS) },
                        icon = Icons.Default.Person,
                        label = MainTab.SETTINGS.title
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                DemoModeBanner()
                Box(modifier = Modifier.weight(1f)) {
                    // 保留四个 Tab 的状态与滚动位置
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentTab == MainTab.TODAY
                    ) {
                        todayScreen()
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentTab == MainTab.TIMETABLE
                    ) {
                        timetableScreen()
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentTab == MainTab.QUERY
                    ) {
                        queryScreen()
                    }
                    androidx.compose.animation.AnimatedVisibility(
                        visible = currentTab == MainTab.SETTINGS
                    ) {
                        settingsScreen()
                    }
                }
            }
        }
    }
}

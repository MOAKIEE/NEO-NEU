package edu.neu.campus.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold

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
    val colors = CampusTheme.colors

    BackHandler(enabled = destination != AppDestination.Main) {
        AppNavigator.popBack()
    }

    if (destination != AppDestination.Main) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding()
        ) {
            subScreen(destination)
        }
    } else {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .statusBarsPadding(),
            bottomBar = {
                val navItemColors = NavigationBarDefaults.navigationBarItemColors(
                    selectedContentColor = colors.brand,
                    unselectedContentColor = colors.textSecondary
                )
                NavigationBar(
                    color = colors.surface,
                    showDivider = true,
                    defaultWindowInsetsPadding = true
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.TODAY,
                        onClick = { AppNavigator.navigateToTab(MainTab.TODAY) },
                        icon = Icons.Default.Home,
                        label = MainTab.TODAY.title,
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.TIMETABLE,
                        onClick = { AppNavigator.navigateToTab(MainTab.TIMETABLE) },
                        icon = Icons.Default.DateRange,
                        label = MainTab.TIMETABLE.title,
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.QUERY,
                        onClick = { AppNavigator.navigateToTab(MainTab.QUERY) },
                        icon = Icons.Default.Search,
                        label = MainTab.QUERY.title,
                        colors = navItemColors
                    )
                    NavigationBarItem(
                        selected = currentTab == MainTab.SETTINGS,
                        onClick = { AppNavigator.navigateToTab(MainTab.SETTINGS) },
                        icon = Icons.Default.Person,
                        label = MainTab.SETTINGS.title,
                        colors = navItemColors
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
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

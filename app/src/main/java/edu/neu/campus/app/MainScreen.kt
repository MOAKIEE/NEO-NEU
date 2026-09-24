package edu.neu.campus.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarDefaults
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Weeks

private val MainTab.icon: androidx.compose.ui.graphics.vector.ImageVector
    get() = when (this) {
        MainTab.TODAY -> MiuixIcons.Regular.Home
        MainTab.TIMETABLE -> MiuixIcons.Regular.Weeks
        MainTab.QUERY -> MiuixIcons.Regular.Search
        MainTab.SETTINGS -> MiuixIcons.Regular.Contacts
    }

/**
 * 应用主框架。
 *
 * 动效约定：
 * - 主标签之间：按左右方向轻微横移 + 淡入淡出，避免横向大幅滑动带来的眩晕
 * - 进入二级页面：自右侧滑入；返回主框架：淡出并轻微右移
 * - 每个标签与详情路由使用独立的 [rememberSaveableStateHolder] 状态槽，切换不丢失滚动位置
 */
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

    val stateHolder = rememberSaveableStateHolder()

    Scaffold(
        // 系统栏内边距由 Miuix Scaffold 的 contentWindowInsets 统一计入 paddingValues；
        // 这里不要再叠加 statusBarsPadding 之类的修饰符，否则会造成顶部重复留白。
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        containerColor = colors.background,
        bottomBar = {
            if (destination == AppDestination.Main) {
                val navItemColors = NavigationBarDefaults.navigationBarItemColors(
                    selectedContentColor = colors.brand,
                    unselectedContentColor = colors.textTertiary
                )
                NavigationBar(
                    color = colors.surface,
                    showDivider = true,
                    defaultWindowInsetsPadding = true
                ) {
                    MainTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { AppNavigator.navigateToTab(tab) },
                            icon = tab.icon,
                            label = tab.title,
                            colors = navItemColors
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // 必须同时标记为已消费：Miuix TopAppBar 无论 defaultWindowInsetsPadding 取值
                // 都会自行叠加 systemBars 顶部边距，不消费会让所有标题栏上方多出一整条状态栏高度。
                .consumeWindowInsets(paddingValues)
        ) {
            // 一级路由转场：主框架 <-> 二级详情
            AnimatedContent(
                targetState = destination,
                transitionSpec = {
                    val enteringFromDetail = initialState != AppDestination.Main
                    val leavingToDetail = targetState != AppDestination.Main
                    when {
                        // 进入二级页面：从右侧滑入
                        leavingToDetail -> {
                            (slideInHorizontally(
                                animationSpec = tween(
                                    CampusMotion.Duration.long,
                                    easing = CampusMotion.Easing.emphasizedDecelerate
                                )
                            ) { full -> full / 4 } + fadeIn(tween(CampusMotion.Duration.medium)))
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(
                                            CampusMotion.Duration.medium,
                                            easing = CampusMotion.Easing.emphasizedAccelerate
                                        )
                                    ) { full -> -full / 8 } + fadeOut(tween(CampusMotion.Duration.short))
                                )
                        }
                        // 返回主框架：轻微右移淡出
                        enteringFromDetail -> {
                            (fadeIn(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)) +
                                slideInHorizontally(
                                    animationSpec = tween(
                                        CampusMotion.Duration.long,
                                        easing = CampusMotion.Easing.emphasizedDecelerate
                                    )
                                ) { full -> -full / 8 })
                                .togetherWith(
                                    slideOutHorizontally(
                                        animationSpec = tween(
                                            CampusMotion.Duration.medium,
                                            easing = CampusMotion.Easing.emphasizedAccelerate
                                        )
                                    ) { full -> full / 4 } + fadeOut(tween(CampusMotion.Duration.short))
                                )
                        }
                        else -> fadeIn(tween(CampusMotion.Duration.medium))
                            .togetherWith(fadeOut(tween(CampusMotion.Duration.short)))
                    }
                        .using(SizeTransform(clip = false))
                },
                label = "appRoute"
            ) { dest ->
                val stateKey = if (dest == AppDestination.Main) "shell" else "page:$dest"
                stateHolder.SaveableStateProvider(stateKey) {
                    if (dest != AppDestination.Main) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                // 底部安全区已由 Miuix Scaffold 的 contentWindowInsets 计入
                                // paddingValues，这里只补键盘避让，避免重复留白。
                                .imePadding()
                        ) {
                            subScreen(dest)
                        }
                    } else {
                        // 二级标签转场：按位于左右方向轻微横移
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                val forward = targetState.ordinal > initialState.ordinal
                                val direction = if (forward) 1 else -1
                                (fadeIn(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)) +
                                    slideInHorizontally(
                                        animationSpec = tween(
                                            CampusMotion.Duration.medium,
                                            easing = CampusMotion.Easing.emphasizedDecelerate
                                        )
                                    ) { full -> direction * full / 16 })
                                    .togetherWith(
                                        fadeOut(tween(CampusMotion.Duration.short)) +
                                            slideOutHorizontally(
                                                animationSpec = tween(
                                                    CampusMotion.Duration.short,
                                                    easing = CampusMotion.Easing.emphasizedAccelerate
                                                )
                                            ) { full -> -direction * full / 16 }
                                    )
                                    .using(SizeTransform(clip = false))
                            },
                            label = "mainTab"
                        ) { tab ->
                            stateHolder.SaveableStateProvider("tab:$tab") {
                                when (tab) {
                                    MainTab.TODAY -> todayScreen()
                                    MainTab.TIMETABLE -> timetableScreen()
                                    MainTab.QUERY -> queryScreen()
                                    MainTab.SETTINGS -> settingsScreen()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

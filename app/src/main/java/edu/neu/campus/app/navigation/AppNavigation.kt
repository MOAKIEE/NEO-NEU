package edu.neu.campus.app.navigation

import androidx.compose.runtime.*
import edu.neu.campus.contract.BalanceKind

enum class MainTab(val title: String) {
    TODAY("今日"),
    TIMETABLE("课表"),
    QUERY("查询"),
    SETTINGS("我的")
}

sealed interface AppDestination {
    data object Main : AppDestination
    data object Grades : AppDestination
    data class GradeDetail(val termId: String, val sourceId: String) : AppDestination
    data object Exams : AppDestination
    data class ExamDetail(val courseName: String) : AppDestination
    data class BalanceDetail(val kind: BalanceKind) : AppDestination
    data object Messages : AppDestination
    data class MessageDetail(val messageId: String) : AppDestination
    data object Tasks : AppDestination
    data class TaskDetail(val taskId: String) : AppDestination
    data object Schedule : AppDestination
    data object BellSchedule : AppDestination
    data object ServicesCatalog : AppDestination
    data object HomeSettings : AppDestination
}

object AppNavigator {
    var currentTab by mutableStateOf(MainTab.TODAY)
    var currentDestination by mutableStateOf<AppDestination>(AppDestination.Main)
    private val backStack = mutableStateListOf<AppDestination>()

    fun navigateToTab(tab: MainTab) {
        currentTab = tab
        currentDestination = AppDestination.Main
        backStack.clear()
    }

    fun navigateTo(dest: AppDestination) {
        backStack.add(currentDestination)
        currentDestination = dest
    }

    fun popBack(): Boolean {
        if (backStack.isNotEmpty()) {
            currentDestination = backStack.removeAt(backStack.size - 1)
            return true
        }
        if (currentDestination != AppDestination.Main) {
            currentDestination = AppDestination.Main
            return true
        }
        return false
    }
}

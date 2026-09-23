package edu.neu.campus.app.registry

import androidx.compose.ui.graphics.vector.ImageVector
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*

enum class FeatureCategory(val displayName: String) {
    STUDY("学习"),
    CAMPUS_LIFE("校园生活"),
    MESSAGES_AFFAIRS("消息与事务"),
    SERVICES("学校服务")
}

enum class FeatureStatus(val label: String) {
    READY("可用"),
    AUTH_REQUIRED("需要登录"),
    NOT_CONNECTED("暂未接入")
}

data class FeatureItem(
    val id: String,
    val title: String,
    val description: String,
    val category: FeatureCategory,
    val searchAliases: List<String>,
    val isNative: Boolean = true,
    val status: FeatureStatus = FeatureStatus.READY,
    val route: String
)

object FeatureRegistry {
    const val ID_TIMETABLE = "timetable"
    const val ID_GRADES = "grades"
    const val ID_EXAMS = "exams"
    const val ID_CAMPUS_CARD = "campus_card"
    const val ID_NETWORK = "network_balance"
    const val ID_MESSAGES = "messages"
    const val ID_TASKS = "tasks"
    const val ID_BELL_SCHEDULE = "bell_schedule"
    const val ID_SCHEDULE_CALENDAR = "schedule_calendar"
    const val ID_SERVICES_CATALOG = "services_catalog"

    val allFeatures: List<FeatureItem> = listOf(
        FeatureItem(
            id = ID_GRADES,
            title = "成绩查询",
            description = "成绩与官方绩点",
            category = FeatureCategory.STUDY,
            searchAliases = listOf("GPA", "绩点", "成绩", "分数", "期末成绩", "查分", "chengji", "cj", "jidian"),
            route = "grades"
        ),
        FeatureItem(
            id = ID_EXAMS,
            title = "考试安排",
            description = "时间与考场",
            category = FeatureCategory.STUDY,
            searchAliases = listOf("考场", "考试", "座位号", "期末考试", "kaoshi", "ks", "kaochang"),
            route = "exams"
        ),
        FeatureItem(
            id = ID_TIMETABLE,
            title = "课表与校历",
            description = "周课表与学期校历",
            category = FeatureCategory.STUDY,
            searchAliases = listOf("课表", "课程表", "上课", "周课表", "kebiao", "kb", "kcb"),
            route = "timetable"
        ),
        FeatureItem(
            id = ID_BELL_SCHEDULE,
            title = "作息时间",
            description = "校区上课作息时刻表",
            category = FeatureCategory.STUDY,
            searchAliases = listOf("作息", "作息表", "几点上课", "节次时间", "下课时间", "zuoxi", "zx", "shijian"),
            route = "bell_schedule"
        ),
        FeatureItem(
            id = ID_CAMPUS_CARD,
            title = "校园卡余额",
            description = "一卡通卡片余额",
            category = FeatureCategory.CAMPUS_LIFE,
            searchAliases = listOf("一卡通", "饭卡", "卡余额", "校园卡", "刷卡", "xiaoyuanka", "xyk", "ykt", "fanka"),
            route = "campus_card"
        ),
        FeatureItem(
            id = ID_NETWORK,
            title = "网费余额",
            description = "校园网账户余额",
            category = FeatureCategory.CAMPUS_LIFE,
            searchAliases = listOf("校园网", "宽带", "网费", "上网", "wangfei", "wf", "xyw"),
            route = "network"
        ),
        FeatureItem(
            id = ID_MESSAGES,
            title = "消息中心",
            description = "门户与教务系统通知",
            category = FeatureCategory.MESSAGES_AFFAIRS,
            searchAliases = listOf("通知", "消息", "公告", "统一消息", "xiaoxi", "xx", "tongzhi", "tz"),
            route = "messages"
        ),
        FeatureItem(
            id = ID_TASKS,
            title = "待办与申请",
            description = "待办事项与流程审批状态",
            category = FeatureCategory.MESSAGES_AFFAIRS,
            searchAliases = listOf("待办", "已办", "我的申请", "审批", "事务", "daiban", "db", "shenpi"),
            route = "tasks"
        ),
        FeatureItem(
            id = ID_SERVICES_CATALOG,
            title = "学校服务目录",
            description = "官方网页办事指南及跳转",
            category = FeatureCategory.SERVICES,
            searchAliases = listOf("服务", "校务", "官方系统", "办事大厅", "fuwu", "fw", "banshi", "mulu"),
            isNative = false,
            route = "services_catalog"
        )
    )

    fun findById(id: String): FeatureItem? = allFeatures.firstOrNull { it.id == id }

    fun search(query: String): List<FeatureItem> {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return allFeatures
        val lower = trimmed.lowercase()

        val exactTitle = mutableListOf<FeatureItem>()
        val titleMatch = mutableListOf<FeatureItem>()
        val aliasMatch = mutableListOf<FeatureItem>()
        val descMatch = mutableListOf<FeatureItem>()

        for (item in allFeatures) {
            when {
                item.title.equals(trimmed, ignoreCase = true) -> exactTitle.add(item)
                item.title.lowercase().contains(lower) -> titleMatch.add(item)
                item.searchAliases.any { it.lowercase().contains(lower) } -> aliasMatch.add(item)
                item.description.lowercase().contains(lower) -> descMatch.add(item)
            }
        }

        return (exactTitle + titleMatch + aliasMatch + descMatch).distinctBy { it.id }
    }
}

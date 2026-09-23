package edu.neu.campus.app.config

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.*
import org.json.JSONArray
import org.json.JSONObject

data class SummaryModuleConfig(
    val id: String,
    val title: String,
    val enabled: Boolean
)

object HomeLayoutConfigManager {
    const val MODULE_TODAY_COURSES = "today_courses"
    const val MODULE_CAMPUS_LIFE = "campus_life"
    const val MODULE_RECENT_EXAMS = "recent_exams"
    const val MODULE_RECENT_TASKS = "recent_tasks"

    private const val PREFS_NAME = "neo_neu_home_config"
    private const val KEY_QUICK_ACTIONS = "quick_actions"
    private const val KEY_MODULES = "modules"
    private const val KEY_HIDE_BALANCE = "hide_balance"

    private val defaultQuickActions = listOf("grades", "exams", "tasks")

    private val defaultModules = listOf(
        SummaryModuleConfig(MODULE_TODAY_COURSES, "今天的课程", true),
        SummaryModuleConfig(MODULE_CAMPUS_LIFE, "校园生活 (余额)", true),
        SummaryModuleConfig(MODULE_RECENT_EXAMS, "最近考试", true),
        SummaryModuleConfig(MODULE_RECENT_TASKS, "待办事项", true)
    )

    private var prefs: SharedPreferences? = null

    var quickActionIds by mutableStateOf(defaultQuickActions)
        private set

    var modules by mutableStateOf(defaultModules)
        private set

    var hideBalanceByDefault by mutableStateOf(true)
        private set

    fun init(context: Context) {
        if (prefs != null) return
        val p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = p

        hideBalanceByDefault = p.getBoolean(KEY_HIDE_BALANCE, true)

        val quickJson = p.getString(KEY_QUICK_ACTIONS, null)
        if (quickJson != null) {
            runCatching {
                val array = JSONArray(quickJson)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                if (list.isNotEmpty()) quickActionIds = list
            }
        }

        val modulesJson = p.getString(KEY_MODULES, null)
        if (modulesJson != null) {
            runCatching {
                val array = JSONArray(modulesJson)
                val list = mutableListOf<SummaryModuleConfig>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SummaryModuleConfig(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            enabled = obj.getBoolean("enabled")
                        )
                    )
                }
                // 确保默认已知模块存在（如果升级加入了新模块，新模块默认关闭）
                val existingIds = list.map { it.id }.toSet()
                val completeList = list.toMutableList()
                defaultModules.forEach { def ->
                    if (def.id !in existingIds) {
                        completeList.add(def.copy(enabled = false)) // 新增摘要默认关闭
                    }
                }
                modules = completeList
            }
        }
    }

    fun setHideBalance(hide: Boolean) {
        hideBalanceByDefault = hide
        prefs?.edit()?.putBoolean(KEY_HIDE_BALANCE, hide)?.apply()
    }

    fun updateQuickActions(newActions: List<String>) {
        quickActionIds = newActions
        val array = JSONArray()
        newActions.forEach { array.put(it) }
        prefs?.edit()?.putString(KEY_QUICK_ACTIONS, array.toString())?.apply()
    }

    fun toggleModule(id: String, enabled: Boolean) {
        modules = modules.map { if (it.id == id) it.copy(enabled = enabled) else it }
        saveModules()
    }

    fun moveModuleUp(index: Int) {
        if (index <= 0 || index >= modules.size) return
        val list = modules.toMutableList()
        val item = list.removeAt(index)
        list.add(index - 1, item)
        modules = list
        saveModules()
    }

    fun moveModuleDown(index: Int) {
        if (index < 0 || index >= modules.size - 1) return
        val list = modules.toMutableList()
        val item = list.removeAt(index)
        list.add(index + 1, item)
        modules = list
        saveModules()
    }

    private fun saveModules() {
        val array = JSONArray()
        modules.forEach {
            val obj = JSONObject()
            obj.put("id", it.id)
            obj.put("title", it.title)
            obj.put("enabled", it.enabled)
            array.put(obj)
        }
        prefs?.edit()?.putString(KEY_MODULES, array.toString())?.apply()
    }

    fun restoreDefaults() {
        quickActionIds = defaultQuickActions
        modules = defaultModules
        hideBalanceByDefault = true
        prefs?.edit()?.clear()?.apply()
    }
}

package edu.neu.campus.verification

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import edu.neu.campus.authweb.OfficialLogin
import edu.neu.campus.contract.QuerySnapshot
import edu.neu.campus.contract.TaskKind
import edu.neu.campus.repository.CampusData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Minimal status-only host. It never displays school response values or credentials. */
class VerificationActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var data: CampusData
    private lateinit var output: TextView
    private var termId: String? = null
    private val lines = linkedMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = CampusData.get(this)
        output = TextView(this).apply { textSize = 15f; setPadding(24, 24, 24, 24) }
        val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; isFocusableInTouchMode = true }
        fun button(label: String, action: suspend () -> Unit) {
            actions.addView(Button(this).apply { text = label; setOnClickListener { scope.launch { action() } } })
        }
        button("在学校官方页面登录") { startActivity(OfficialLogin.intent(this@VerificationActivity)) }
        button("1. 学期 → 周次 → 校区 → 个人课表") {
            data.academic.refreshTerms()
            val terms = data.academic.terms().value.data
            termId = terms?.firstOrNull { it.isCurrent }?.id ?: terms?.firstOrNull()?.id
            val term = termId ?: return@button log("课表链路", "学期查询无可用结果")
            data.academic.refreshWeeks(term)
            data.academic.refreshCampuses(term)
            data.academic.refreshTimetable(term, null)
            log("学期", describe(data.academic.terms().value, terms?.size))
            log("周次", describe(data.academic.weeks(term).value, data.academic.weeks(term).value.data?.size))
            log("校区", describe(data.academic.campuses(term).value, data.academic.campuses(term).value.data?.size))
            log("课表", describe(data.academic.timetable(term, null).value, data.academic.timetable(term, null).value.data?.arranged?.size))
        }
        button("2. 成绩、考试") {
            val term = termId ?: return@button log("成绩考试", "请先查询学期")
            data.academic.refreshGrades(term); data.academic.refreshGradeSummary(); data.academic.refreshExams(term)
            log("成绩", describe(data.academic.grades(term).value, data.academic.grades(term).value.data?.size))
            log("官方统计", describe(data.academic.gradeSummary().value, null))
            log("考试", describe(data.academic.exams(term).value, data.academic.exams(term).value.data?.size))
        }
        button("3. 余额、消息、待办") {
            data.portal.refreshBalances(); data.portal.refreshMessages(1, 20); data.portal.refreshTasks(TaskKind.TODO, 1, 20)
            log("余额", describe(data.portal.balances().value, data.portal.balances().value.data?.size))
            log("消息", describe(data.portal.messages(1, 20).value, data.portal.messages(1, 20).value.data?.items?.size))
            log("待办", describe(data.portal.tasks(TaskKind.TODO, 1, 20).value, data.portal.tasks(TaskKind.TODO, 1, 20).value.data?.items?.size))
        }
        button("查看本地缓存状态") {
            val term = termId
            if (term != null) log("课表缓存", describe(data.academic.timetable(term, null).value, data.academic.timetable(term, null).value.data?.arranged?.size))
            log("余额缓存", describe(data.portal.balances().value, data.portal.balances().value.data?.size))
        }
        button("查看响应字段结构（不含值）") {
            data.responseShapes.value.forEach { (key, shape) -> log("结构 $key", shape) }
        }
        button("退出并清除当前账号") { data.session.signOut(); termId = null; log("会话", "已退出") }
        actions.addView(output)
        val scroll = ScrollView(this).apply { addView(actions, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
        setContentView(scroll)
        actions.requestFocus()
        scroll.post { scroll.scrollTo(0, 0) }
        scope.launch { data.session.state.collect { log("会话", "门户=${it.portal} 教务=${it.academic} 范围=${if (it.accountScope == null) "无" else "本机隔离"}") } }
    }

    private fun describe(snapshot: QuerySnapshot<*>, count: Int?): String =
        "${snapshot.phase} 条目=${count ?: "未取得"} 旧缓存=${snapshot.isStale} 错误=${snapshot.error?.kind ?: "无"}"

    private fun log(key: String, value: String) { lines[key] = value; output.text = lines.entries.joinToString("\n") { "${it.key}: ${it.value}" } }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

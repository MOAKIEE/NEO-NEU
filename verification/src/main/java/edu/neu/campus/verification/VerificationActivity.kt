package edu.neu.campus.verification

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.view.WindowInsets
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import edu.neu.campus.authweb.OfficialLogin
import edu.neu.campus.contract.QuerySnapshot
import edu.neu.campus.contract.BalanceKind
import edu.neu.campus.contract.TaskKind
import edu.neu.campus.repository.CampusData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Minimal status-only host. It never displays school response values or credentials. */
class VerificationActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var data: CampusData
    private lateinit var output: TextView
    private var termId: String? = null
    private var gradeTermId: String? = null
    private val lines = linkedMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = CampusData.get(this)
        actionBar?.hide()
        output = TextView(this).apply { textSize = 15f; setPadding(24, 24, 24, 24) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun button(label: String, parent: LinearLayout = actions, action: suspend () -> Unit) {
            parent.addView(Button(this).apply { text = label; setOnClickListener { scope.launch { action() } } })
        }
        button("在学校官方页面登录", header) { startActivity(OfficialLogin.intent(this@VerificationActivity)) }
        button("1. 学期 → 周次 → 校区 → 个人课表", header) {
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
        button("1b. 当前周课表") {
            val term = termId ?: return@button log("周课表", "请先查询学期")
            val week = data.academic.weeks(term).value.data?.firstOrNull { it.isCurrent }?.number
                ?: return@button log("周课表", "未取得当前教学周")
            data.academic.refreshTimetable(term, week)
            val snapshot = data.academic.timetable(term, week).value
            log("周课表", describe(snapshot, snapshot.data?.arranged?.size))
        }
        button("刷新已缓存的学期课表") {
            val term = termId ?: data.academic.terms().value.data?.firstOrNull { it.isCurrent }?.id
                ?: return@button log("课表重试", "请先查询学期")
            data.academic.refreshTimetable(term, null)
            val snapshot = data.academic.timetable(term, null).value
            log("课表重试", describe(snapshot, snapshot.data?.arranged?.size))
        }
        button("验证认证失效与恢复（仅调试）") {
            val term = termId ?: data.academic.terms().value.data?.firstOrNull { it.isCurrent }?.id
                ?: return@button log("认证失效", "请先查询学期")
            data.verificationOnlyAuthFailureProbe(term)
            val failed = data.academic.timetable(term, null).value
            log("认证失效", describe(failed, failed.data?.arranged?.size))
            data.academic.refreshTimetable(term, null)
            val recovered = data.academic.timetable(term, null).value
            log("认证恢复", describe(recovered, recovered.data?.arranged?.size))
        }
        button("验证已保存的会话") { data.session.verify() }
        button("2. 成绩、考试") {
            val term = termId ?: return@button log("成绩考试", "请先查询学期")
            data.academic.refreshGrades(term); data.academic.refreshGradeSummary(); data.academic.refreshExams(term)
            log("成绩", describe(data.academic.grades(term).value, data.academic.grades(term).value.data?.size))
            log("官方统计", describe(data.academic.gradeSummary().value, null))
            log("考试", describe(data.academic.exams(term).value, data.academic.exams(term).value.data?.size))
        }
        button("2b. 查找有成绩的历史学期") {
            data.academic.refreshGradeTermIds()
            val ids = data.academic.gradeTermIds().value.data?.toSet().orEmpty()
            log("成绩学期", describe(data.academic.gradeTermIds().value, ids.size))
            val current = termId ?: data.academic.terms().value.data?.firstOrNull { it.isCurrent }?.id
            val candidates = data.academic.terms().value.data.orEmpty().asReversed()
                .filter { it.id in ids && it.id != current }.take(5)
            for (term in candidates) {
                data.academic.refreshGrades(term.id)
                val result = data.academic.grades(term.id).value
                log("历史成绩", describe(result, result.data?.size))
                if (!result.data.isNullOrEmpty()) { gradeTermId = term.id; break }
            }
        }
        button("2c. 核对一门成绩详情字段") {
            val term = gradeTermId ?: return@button log("成绩详情", "请先查询有成绩的历史学期")
            val id = data.academic.grades(term).value.data?.firstOrNull()?.sourceId
                ?: return@button log("成绩详情", "历史成绩列表为空")
            data.academic.refreshGradeDetail(term, id)
            val result = data.academic.gradeDetail(term, id).value
            log("成绩详情", describe(result, result.data?.components?.size))
        }
        button("3. 余额、消息、待办") {
            data.portal.refreshBalance(BalanceKind.CAMPUS_CARD); data.portal.refreshBalance(BalanceKind.NETWORK)
            data.portal.refreshMessages(1, 20); data.portal.refreshTasks(TaskKind.TODO, 1, 20)
            log("校园卡", describe(data.portal.balance(BalanceKind.CAMPUS_CARD).value, null) + " 遮罩=${data.portal.balance(BalanceKind.CAMPUS_CARD).value.data?.isMasked}")
            log("网费", describe(data.portal.balance(BalanceKind.NETWORK).value, null) + " 遮罩=${data.portal.balance(BalanceKind.NETWORK).value.data?.isMasked}")
            log("消息", describe(data.portal.messages(1, 20).value, data.portal.messages(1, 20).value.data?.items?.size))
            log("待办", describe(data.portal.tasks(TaskKind.TODO, 1, 20).value, data.portal.tasks(TaskKind.TODO, 1, 20).value.data?.items?.size))
        }
        button("3b. 已办与我的申请") {
            for (kind in listOf(TaskKind.DONE, TaskKind.APPLICATION)) {
                data.portal.refreshTasks(kind, 1, 20)
                val result = data.portal.tasks(kind, 1, 20).value
                log(kind.name, describe(result, result.data?.items?.size))
            }
        }
        button("查看本地缓存状态") {
            val terms = data.academic.terms()
            delay(500)
            val term = termId ?: terms.value.data?.firstOrNull { it.isCurrent }?.id
            val table = term?.let { data.academic.timetable(it, null) }
            val card = data.portal.balance(BalanceKind.CAMPUS_CARD)
            val network = data.portal.balance(BalanceKind.NETWORK)
            delay(500)
            if (table != null) log("课表缓存", describe(table.value, table.value.data?.arranged?.size))
            log("校园卡缓存", describe(card.value, null))
            log("网费缓存", describe(network.value, null))
        }
        button("查看响应字段结构（不含值）") {
            data.responseShapes.value.forEach { (key, shape) -> log("结构 $key", shape) }
        }
        button("退出并清除当前账号") {
            data.session.signOut(); termId = null; gradeTermId = null
            lines.clear(); log("会话", "已退出")
        }
        actions.addView(output)
        val scroll = ScrollView(this).apply { addView(actions, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(header)
            addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        }
        root.setOnApplyWindowInsetsListener { view, insets ->
            val top = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.statusBars()).top else insets.systemWindowInsetTop
            val bottom = if (Build.VERSION.SDK_INT >= 30) insets.getInsets(WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            view.setPadding(0, top, 0, bottom)
            insets
        }
        setContentView(root)
        scope.launch { data.session.state.collect { log("会话", "门户=${it.portal} 教务=${it.academic} 范围=${if (it.accountScope == null) "无" else "本机隔离"}") } }
    }

    private fun describe(snapshot: QuerySnapshot<*>, count: Int?): String =
        "${snapshot.phase} 条目=${count ?: "未取得"} 旧缓存=${snapshot.isStale} 错误=${snapshot.error?.kind ?: "无"}"

    private fun log(key: String, value: String) { lines[key] = value; output.text = lines.entries.joinToString("\n") { "${it.key}: ${it.value}" } }
    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}

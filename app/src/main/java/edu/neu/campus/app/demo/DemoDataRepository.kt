package edu.neu.campus.app.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import top.yukonga.miuix.kmp.basic.Text

object DemoModeManager {
    var isDemoMode by mutableStateOf(false)

    fun toggleDemoMode(enabled: Boolean) {
        isDemoMode = enabled
    }
}

@Composable
fun DemoModeBanner(modifier: Modifier = Modifier) {
    if (DemoModeManager.isDemoMode) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(Color(0xFFFFF3CD))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚠️ 当前处于演示模式 (DEMO DATA)，数据仅供界面预览与测试",
                color = Color(0xFF856404),
                fontSize = 12.sp
            )
        }
    }
}

class DemoDataRepository : AcademicRepository, PortalRepository, SessionRepository {
    private val currentTime = System.currentTimeMillis()

    override val state: StateFlow<SessionState> = MutableStateFlow(
        SessionState(
            accountScope = "demo-account-2026",
            portal = DomainStatus.READY,
            academic = DomainStatus.READY
        )
    )

    override suspend fun verify() {}
    override suspend fun signOut() {
        DemoModeManager.isDemoMode = false
    }

    private val demoTerms = listOf(
        Term("2026-2027-1", "2026—2027 秋季学期", isCurrent = true),
        Term("2025-2026-2", "2025—2026 春季学期", isCurrent = false),
        Term("2025-2026-1", "2025—2026 秋季学期", isCurrent = false)
    )

    private val demoWeeks = (1..20).map { w ->
        TeachingWeek(
            number = w,
            startDate = "2026-09-${(w * 7 - 6).coerceIn(1, 30)}",
            endDate = "2026-09-${(w * 7).coerceIn(1, 30)}",
            isCurrent = w == 3
        )
    }

    private val demoCampuses = listOf(
        Campus("1", "南湖校区"),
        Campus("2", "浑南校区")
    )

    private val demoArrangedCourses = listOf(
        CourseOccurrence(
            sourceId = "c1", campusId = "1", title = "高等数学 (上)",
            dayOfWeek = 1, beginSection = 1, endSection = 2,
            beginTime = "08:00", endTime = "09:40", teacher = "张教授", place = "基础楼 301",
            scheduleDescription = "1-16周 每周"
        ),
        CourseOccurrence(
            sourceId = "c2", campusId = "1", title = "大学物理实验",
            dayOfWeek = 1, beginSection = 3, endSection = 4,
            beginTime = "10:00", endTime = "11:40", teacher = "李老师", place = "实验中心 A204",
            scheduleDescription = "实验课程 · 1-8周"
        ),
        CourseOccurrence(
            sourceId = "c3", campusId = "2", title = "计算机系统结构",
            dayOfWeek = 3, beginSection = 3, endSection = 4,
            beginTime = "10:00", endTime = "11:40", teacher = "王教授", place = "浑南信息楼 B102",
            scheduleDescription = "浑南校区 · 3-18周"
        ),
        CourseOccurrence(
            sourceId = "c4", campusId = "1", title = "软件工程导论",
            dayOfWeek = 3, beginSection = 3, endSection = 4,
            beginTime = "10:00", endTime = "11:40", teacher = "赵老师", place = "机房 202",
            scheduleDescription = "排课冲突演示课程"
        ),
        CourseOccurrence(
            sourceId = "c5", campusId = "1", title = "数据结构与算法",
            dayOfWeek = 4, beginSection = 5, endSection = 6,
            beginTime = "14:00", endTime = "15:40", teacher = "孙老师", place = "逸夫楼 105",
            scheduleDescription = "1-16周 每周"
        )
    )

    private val demoUnscheduled = listOf(
        UnscheduledCourse(
            sourceId = "u1", campusId = "1", title = "专业综合实践实习",
            reason = "集中实践环节，时间地点由指导教师另行通知"
        )
    )

    override fun terms() = MutableStateFlow(QuerySnapshot(demoTerms, QueryPhase.READY, currentTime))
    override suspend fun refreshTerms() {}
    override fun weeks(termId: String) = MutableStateFlow(QuerySnapshot(demoWeeks, QueryPhase.READY, currentTime))
    override suspend fun refreshWeeks(termId: String) {}
    override fun campuses(termId: String) = MutableStateFlow(QuerySnapshot(demoCampuses, QueryPhase.READY, currentTime))
    override suspend fun refreshCampuses(termId: String) {}

    override fun timetable(termId: String, week: Int?) = MutableStateFlow(
        QuerySnapshot(
            Timetable(
                termId = termId, week = week ?: 3, campuses = demoCampuses,
                sectionsByCampus = mapOf(
                    "1" to listOf(
                        Section("1", "第 1 节", "s1", "08:00", "08:45"),
                        Section("2", "第 2 节", "s2", "08:55", "09:40"),
                        Section("3", "第 3 节", "s3", "10:00", "10:45"),
                        Section("4", "第 4 节", "s4", "10:55", "11:40"),
                        Section("5", "第 5 节", "s5", "14:00", "14:45"),
                        Section("6", "第 6 节", "s6", "14:55", "15:40")
                    )
                ),
                arranged = demoArrangedCourses,
                unscheduled = demoUnscheduled,
                practice = emptyList()
            ),
            QueryPhase.READY,
            currentTime
        )
    )
    override suspend fun refreshTimetable(termId: String, week: Int?) {}

    private val demoGrades = listOf(
        Grade("g1", "2026-2027-1", "MATH101", "高等数学 (上)", "92", "5.0", "4.2", "正常考核", "初修"),
        Grade("g2", "2026-2027-1", "CS102", "程序设计基础", "88", "4.0", "3.8", "正常考核", "初修"),
        Grade("g3", "2026-2027-1", "ENG101", "大学英语", "优秀", "2.0", "4.5", "等级制", "初修"),
        Grade("g4", "2025-2026-2", "PHYS201", "大学物理", "76", "4.0", "2.6", "正常考核", "初修")
    )

    override fun gradeTermIds() = MutableStateFlow(QuerySnapshot(listOf("2026-2027-1", "2025-2026-2"), QueryPhase.READY, currentTime))
    override suspend fun refreshGradeTermIds() {}
    override fun grades(termId: String) = MutableStateFlow(QuerySnapshot(demoGrades, QueryPhase.READY, currentTime))
    override suspend fun refreshGrades(termId: String) {}

    override fun gradeDetail(termId: String, sourceId: String) = MutableStateFlow(
        QuerySnapshot(
            GradeDetail(
                sourceId = sourceId,
                rawScore = "92",
                officialGradePoint = "4.2",
                passed = true,
                components = listOf(
                    GradeComponent("COMP1", "平时成绩 (出勤与作业)", "95", passed = true, highestInProportion = false),
                    GradeComponent("COMP2", "期中测验", "88", passed = true, highestInProportion = false),
                    GradeComponent("COMP3", "期末考试", "93", passed = true, highestInProportion = true)
                )
            ),
            QueryPhase.READY,
            currentTime
        )
    )
    override suspend fun refreshGradeDetail(termId: String, sourceId: String) {}

    override fun gradeSummary() = MutableStateFlow(
        QuerySnapshot(
            GradeSummary(officialGpa = "3.78", scope = "学校返回全学程统计值"),
            QueryPhase.READY,
            currentTime
        )
    )
    override suspend fun refreshGradeSummary() {}

    private val demoExams = listOf(
        Exam(
            courseName = "高等数学 (上)",
            timeDescription = "2026-10-15 09:00—11:00",
            place = "基础楼 301",
            seat = "024 号",
            status = "已排考",
            arranged = true
        ),
        Exam(
            courseName = "大学物理实验",
            timeDescription = null,
            place = null,
            seat = null,
            status = "随堂考查",
            arranged = false
        )
    )

    override fun exams(termId: String) = MutableStateFlow(QuerySnapshot(demoExams, QueryPhase.READY, currentTime))
    override suspend fun refreshExams(termId: String) {}

    override fun balance(kind: BalanceKind) = MutableStateFlow(
        QuerySnapshot(
            when (kind) {
                BalanceKind.CAMPUS_CARD -> Balance(BalanceKind.CAMPUS_CARD, "128.50", "元", false, "2026-09-23 12:30")
                BalanceKind.NETWORK -> Balance(BalanceKind.NETWORK, "30.00", "元", false, "2026-09-23 10:00")
            },
            QueryPhase.READY,
            currentTime
        )
    )
    override suspend fun refreshBalance(kind: BalanceKind) {}

    private val demoMessages = listOf(
        CampusMessage(
            id = "m1",
            title = "关于 2026 年秋季学期选课与课表核对的通知",
            contentLines = listOf(
                "各位同学：",
                "请登录教务系统核对自己学期的排课情况，若有实验或实习课请与学院教务老师联系。",
                "教务处"
            ),
            time = "2026-09-20 15:30",
            serverRead = false,
            source = "教务处"
        ),
        CampusMessage(
            id = "m2",
            title = "校园网认证网关升级维护公告",
            contentLines = listOf("为提供更优质的校园网服务，网络中心将于今晚 23:00 进行例行维护。"),
            time = "2026-09-19 09:00",
            serverRead = true,
            source = "网络中心"
        )
    )

    override fun messages(page: Int, pageSize: Int, status: Int) = MutableStateFlow(
        QuerySnapshot(Page(demoMessages, total = 2, unreadCount = 1), QueryPhase.READY, currentTime)
    )
    override suspend fun refreshMessages(page: Int, pageSize: Int, status: Int) {}

    private val demoTasks = listOf(
        CampusTask("t1", "2026 学年家庭经济状况调查表填报", "2026-09-25", TaskKind.TODO),
        CampusTask("t2", "体质健康测试预约", "2026-09-18", TaskKind.DONE)
    )

    override fun tasks(kind: TaskKind, page: Int, pageSize: Int) = MutableStateFlow(
        QuerySnapshot(Page(demoTasks.filter { it.kind == kind }, total = 1), QueryPhase.READY, currentTime)
    )
    override suspend fun refreshTasks(kind: TaskKind, page: Int, pageSize: Int) {}
}

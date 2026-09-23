package edu.neu.campus.app.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.QuerySnapshot
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.LocalCampusColors
import kotlinx.coroutines.flow.MutableStateFlow
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BellSection(
    val sectionNumber: Int,
    val name: String,
    val startTime: String,
    val endTime: String
)

enum class ScheduleTab(val label: String) {
    BELL_SCHEDULE("作息时间表"),
    CALENDAR("学期校历")
}

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val campusColors = LocalCampusColors.current
    var activeTab by remember { mutableStateOf(ScheduleTab.BELL_SCHEDULE) }
    var selectedCampus by remember { mutableStateOf("浑南校区") }

    val termsSnapshot by CampusDataProvider.academic.terms().collectAsState()
    val currentTerm = termsSnapshot.data?.firstOrNull { it.isCurrent }
    val termId = currentTerm?.id ?: ""

    val weeksSnapshot by remember(termId) {
        if (termId.isNotBlank()) {
            CampusDataProvider.academic.weeks(termId)
        } else {
            MutableStateFlow(QuerySnapshot())
        }
    }.collectAsState()
    val currentWeek = weeksSnapshot.data?.firstOrNull { it.isCurrent }

    val nowTimeStr = remember {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(campusColors.background)
    ) {
        CampusTopBar(
            title = "作息与校历",
            onBack = onBack
        )

        // 分段切换胶囊：作息时间表 / 学期校历
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(campusColors.surfaceMuted)
                .padding(4.dp)
        ) {
            ScheduleTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) campusColors.surface else Color.Transparent)
                        .clickable { activeTab = tab }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) campusColors.brand else campusColors.textSecondary
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (activeTab) {
                ScheduleTab.BELL_SCHEDULE -> {
                    // 校区选择药丸
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("浑南校区", "南湖校区").forEach { campus ->
                            val isSel = selectedCampus == campus
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSel) campusColors.brandContainer else campusColors.surfaceMuted)
                                    .clickable { selectedCampus = campus }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = campus,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) campusColors.brand else campusColors.textSecondary
                                )
                            }
                        }
                    }

                    // 上午课程
                    SectionGroupSection(
                        title = "上午作息 (08:00 - 11:30)",
                        sections = listOf(
                            BellSection(1, "第 1 节", "08:00", "08:45"),
                            BellSection(2, "第 2 节", "08:50", "09:35"),
                            BellSection(3, "第 3 节", "09:55", "10:40"),
                            BellSection(4, "第 4 节", "10:45", "11:30")
                        ),
                        currentTime = nowTimeStr
                    )

                    // 下午课程
                    SectionGroupSection(
                        title = "下午作息 (13:30 - 17:00)",
                        sections = listOf(
                            BellSection(5, "第 5 节", "13:30", "14:15"),
                            BellSection(6, "第 6 节", "14:20", "15:05"),
                            BellSection(7, "第 7 节", "15:25", "16:10"),
                            BellSection(8, "第 8 节", "16:15", "17:00")
                        ),
                        currentTime = nowTimeStr
                    )

                    // 晚间课程
                    SectionGroupSection(
                        title = "晚间作息 (18:00 - 20:25)",
                        sections = listOf(
                            BellSection(9, "第 9 节", "18:00", "18:45"),
                            BellSection(10, "第 10 节", "18:50", "19:35"),
                            BellSection(11, "第 11 节", "19:40", "20:25"),
                            BellSection(12, "第 12 节", "20:30", "21:15")
                        ),
                        currentTime = nowTimeStr
                    )

                    SafeDataTag(text = "东大两校区教学节次作息标准一致")
                }

                ScheduleTab.CALENDAR -> {
                    // 当前学期与周次主卡
                    val termName = currentTerm?.name ?: "当前学期校历"
                    val weekNum = currentWeek?.number
                    val weekStart = currentWeek?.startDate
                    val weekEnd = currentWeek?.endDate

                    Card(
                        insideMargin = PaddingValues(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = termName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = campusColors.textPrimary
                            )

                            val weekText = if (weekNum != null) {
                                "当前进度：第 $weekNum 教学周"
                            } else {
                                "教学周次同步中..."
                            }

                            Text(
                                text = weekText,
                                fontSize = 20.sp,
                                color = campusColors.brand,
                                fontWeight = FontWeight.Bold
                            )

                            if (weekStart != null && weekEnd != null) {
                                Text(
                                    text = "本周日期范围：$weekStart ~ $weekEnd",
                                    fontSize = 13.sp,
                                    color = campusColors.textSecondary
                                )
                            }
                        }
                    }

                    // 教学周阶段规划卡片
                    CampusSection(title = "关键教学周节点") {
                        CampusGroup {
                            CalendarMilestone(week = "第 1 周", title = "学期开学", desc = "学生返校报到、注册，正式课程开始")
                            CampusGroupDivider()
                            CalendarMilestone(week = "第 2-3 周", title = "选退课窗口", desc = "通识选修与个性化培养方案退补选截止")
                            CampusGroupDivider()
                            CalendarMilestone(week = "第 9-10 周", title = "期中教学检查", desc = "期中阶段性考核与教学质量反馈")
                            CampusGroupDivider()
                            CalendarMilestone(week = "第 17 周", title = "停课复习", desc = "课堂教学结束，进入期末备考")
                            CampusGroupDivider()
                            CalendarMilestone(week = "第 18-19 周", title = "期末考试周", desc = "全校期末集中统一考试")
                            CampusGroupDivider()
                            CalendarMilestone(week = "第 20 周", title = "假期开始", desc = "寒假/暑假开始，成绩陆续评定归档")
                        }
                    }

                    SafeDataTag(text = "校历安排以学校教务处及学院最新下发文件为准")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionGroupSection(
    title: String,
    sections: List<BellSection>,
    currentTime: String,
    modifier: Modifier = Modifier
) {
    val campusColors = LocalCampusColors.current
    CampusSection(title = title, modifier = modifier) {
        CampusGroup {
            sections.forEachIndexed { index, s ->
                if (index > 0) CampusGroupDivider()
                val isCurrent = currentTime in s.startTime..s.endTime
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.name,
                            fontSize = 14.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) campusColors.brand else campusColors.textPrimary
                        )
                        if (isCurrent) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(campusColors.brandContainer)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "进行中",
                                    fontSize = 10.sp,
                                    color = campusColors.brand,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Text(
                        text = "${s.startTime} - ${s.endTime}",
                        fontSize = 14.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) campusColors.brand else campusColors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarMilestone(
    week: String,
    title: String,
    desc: String,
    modifier: Modifier = Modifier
) {
    val campusColors = LocalCampusColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(campusColors.surfaceMuted)
                .padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = week,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = campusColors.brand
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = campusColors.textPrimary
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = campusColors.textSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

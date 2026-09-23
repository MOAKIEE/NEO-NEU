package edu.neu.campus.app.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.QuerySnapshot
import edu.neu.campus.ui.components.SafeDataTag
import kotlinx.coroutines.flow.MutableStateFlow
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
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

/**
 * 校历与作息时间原生查看页面。
 * 遵循 docs/06-UI页面布局设计.md 第 10.2 节要求：
 * - 浑南/南湖校区标准节次作息时间表
 * - 结合本地时间高亮当前节次
 * - 学期校历周次与关键教学周节点指示
 */
@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            .background(MiuixTheme.colorScheme.background)
    ) {
        edu.neu.campus.ui.components.CampusTopBar(
            title = "作息与校历",
            onBack = onBack
        )

        // 分段切换：作息时间表 / 学期校历
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScheduleTab.entries.forEach { tab ->
                val isSelected = activeTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer)
                        .clickable { activeTab = tab }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (activeTab) {
                ScheduleTab.BELL_SCHEDULE -> {
                    // 校区选择
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("浑南校区", "南湖校区").forEach { campus ->
                            val isSel = selectedCampus == campus
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSel) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer)
                                    .clickable { selectedCampus = campus }
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = campus,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // 上午课程
                    SectionGroupCard(
                        title = "上午 (08:00 - 11:30)",
                        sections = listOf(
                            BellSection(1, "第 1 节", "08:00", "08:45"),
                            BellSection(2, "第 2 节", "08:50", "09:35"),
                            BellSection(3, "第 3 节", "09:55", "10:40"),
                            BellSection(4, "第 4 节", "10:45", "11:30")
                        ),
                        currentTime = nowTimeStr
                    )

                    // 下午课程
                    SectionGroupCard(
                        title = "下午 (13:30 - 17:00)",
                        sections = listOf(
                            BellSection(5, "第 5 节", "13:30", "14:15"),
                            BellSection(6, "第 6 节", "14:20", "15:05"),
                            BellSection(7, "第 7 节", "15:25", "16:10"),
                            BellSection(8, "第 8 节", "16:15", "17:00")
                        ),
                        currentTime = nowTimeStr
                    )

                    // 晚间课程
                    SectionGroupCard(
                        title = "晚间 (18:00 - 20:25)",
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
                    // 当前学期信息
                    val termName = currentTerm?.name ?: "当前学期校历"
                    val weekNum = currentWeek?.number
                    val weekStart = currentWeek?.startDate
                    val weekEnd = currentWeek?.endDate

                    Card(
                        colors = CardDefaults.defaultColors(),
                        insideMargin = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = termName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.onSurface
                            )

                            val weekText = if (weekNum != null) {
                                "当前进度：第 $weekNum 教学周"
                            } else {
                                "教学周次同步中..."
                            }

                            Text(
                                text = weekText,
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            if (weekStart != null && weekEnd != null) {
                                Text(
                                    text = "本周日期范围：$weekStart ~ $weekEnd",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }
                    }

                    // 教学周阶段规划卡片
                    Card(
                        colors = CardDefaults.defaultColors(),
                        insideMargin = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "学期关键教学周节点",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )

                            CalendarMilestone(week = "第 1 周", title = "学期开学", desc = "学生返校报到、注册，正式课程开始")
                            CalendarMilestone(week = "第 2-3 周", title = "选退课窗口", desc = "通识选修与个性化培养方案退补选截止")
                            CalendarMilestone(week = "第 9-10 周", title = "期中教学检查", desc = "期中阶段性考核与教学质量反馈")
                            CalendarMilestone(week = "第 17 周", title = "停课复习", desc = "课堂教学结束，进入期末备考")
                            CalendarMilestone(week = "第 18-19 周", title = "期末考试周", desc = "全校期末集中统一考试")
                            CalendarMilestone(week = "第 20 周", title = "假期开始", desc = "寒假/暑假开始，成绩陆续评定归档")
                        }
                    }

                    SafeDataTag(text = "校历安排以学校教务处及学院最新下发文件为准")
                }
            }
        }
    }
}

@Composable
private fun SectionGroupCard(
    title: String,
    sections: List<BellSection>,
    currentTime: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.defaultColors(),
        insideMargin = PaddingValues(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface
            )

            sections.forEach { s ->
                val isCurrent = currentTime in s.startTime..s.endTime
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCurrent) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f) else MiuixTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.name,
                            fontSize = 13.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                        )
                        if (isCurrent) {
                            Text(
                                text = "进行中",
                                fontSize = 10.sp,
                                color = MiuixTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "${s.startTime} - ${s.endTime}",
                        fontSize = 13.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(60.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MiuixTheme.colorScheme.surfaceContainer)
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = week,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.onSurfaceSecondary
            )
        }
    }
}

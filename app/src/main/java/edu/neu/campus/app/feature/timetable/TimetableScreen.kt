package edu.neu.campus.app.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.QueryCard
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.timetable.CourseDetailBottomSheet
import edu.neu.campus.ui.timetable.TimetableGrid
import edu.neu.campus.ui.timetable.dayOfWeekText
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TimetableScreen(
    onLoginClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val academic = CampusDataProvider.academic

    val termsSnapshot by academic.terms().collectAsState()
    val terms = termsSnapshot.data.orEmpty()
    var selectedTerm by remember { mutableStateOf<Term?>(null) }

    LaunchedEffect(terms) {
        if (selectedTerm == null && terms.isNotEmpty()) {
            selectedTerm = terms.firstOrNull { it.isCurrent } ?: terms.first()
        }
    }

    LaunchedEffect(Unit) {
        if (terms.isEmpty()) {
            academic.refreshTerms()
        }
    }

    val currentTerm = selectedTerm
    var weeks by remember { mutableStateOf<List<TeachingWeek>>(emptyList()) }
    var campuses by remember { mutableStateOf<List<Campus>>(emptyList()) }
    var selectedWeekNumber by remember { mutableStateOf<Int?>(null) }
    var selectedCampusId by remember { mutableStateOf<String?>(null) } // null 为全部校区
    var isListView by remember { mutableStateOf(false) }

    LaunchedEffect(currentTerm?.id) {
        val termId = currentTerm?.id ?: return@LaunchedEffect
        academic.refreshWeeks(termId)
        academic.refreshCampuses(termId)
    }

    val weeksSnapshot = currentTerm?.let { academic.weeks(it.id).collectAsState().value }
    LaunchedEffect(weeksSnapshot?.data) {
        val wList = weeksSnapshot?.data.orEmpty()
        weeks = wList
        if (selectedWeekNumber == null && wList.isNotEmpty()) {
            selectedWeekNumber = wList.firstOrNull { it.isCurrent }?.number ?: wList.first().number
        }
    }

    val campusesSnapshot = currentTerm?.let { academic.campuses(it.id).collectAsState().value }
    LaunchedEffect(campusesSnapshot?.data) {
        campuses = campusesSnapshot?.data.orEmpty()
    }

    val timetableSnapshot = if (currentTerm != null) {
        academic.timetable(currentTerm.id, selectedWeekNumber).collectAsState().value
    } else null

    LaunchedEffect(currentTerm?.id, selectedWeekNumber) {
        val termId = currentTerm?.id ?: return@LaunchedEffect
        academic.refreshTimetable(termId, selectedWeekNumber)
    }

    var inspectingCourse by remember { mutableStateOf<CourseOccurrence?>(null) }
    var conflictCourses by remember { mutableStateOf<List<CourseOccurrence>?>(null) }
    var showWeekPicker by remember { mutableStateOf(false) }
    var showTermPicker by remember { mutableStateOf(false) }
    var showUnscheduledSheet by remember { mutableStateOf(false) }

    val rawTable = timetableSnapshot?.data
    val filteredCourses = remember(rawTable?.arranged, selectedCampusId) {
        val list = rawTable?.arranged.orEmpty()
        if (selectedCampusId == null) list else list.filter { it.campusId == selectedCampusId }
    }

    val currentWeekObj = weeks.firstOrNull { it.number == selectedWeekNumber }

    Column(modifier = Modifier.fillMaxSize()) {
        // 第一行：顶部标题与操作栏
        TopAppBar(
            title = "课表",
            actions = {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .clickable { isListView = !isListView }
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isListView) Icons.Default.DateRange else Icons.Default.Menu,
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isListView) "网格" else "列表",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showUnscheduledSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        )

        // 第二行：学期选择与筛选栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .clickable { showTermPicker = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTerm?.name ?: "选择学期",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface
                )
                Text(text = " ▾", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
            }

            // 校区筛选
            if (campuses.size > 1) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .clickable {
                            val allIds = listOf<String?>(null) + campuses.map { it.id }
                            val nextIndex = (allIds.indexOf(selectedCampusId) + 1) % allIds.size
                            selectedCampusId = allIds[nextIndex]
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    val label = if (selectedCampusId == null) "全部校区" else campuses.firstOrNull { it.id == selectedCampusId }?.name ?: "校区"
                    Text(text = "$label ▾", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                }
            }
        }

        // 第三行：周次控制器（‹ 上一周、第 N 周、› 下一周、回本周）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .clickable(enabled = (selectedWeekNumber ?: 1) > 1) {
                            val current = selectedWeekNumber ?: 1
                            if (current > 1) selectedWeekNumber = current - 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "上一周",
                        tint = if ((selectedWeekNumber ?: 1) > 1) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .clickable { showWeekPicker = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sDate = currentWeekObj?.startDate
                    val eDate = currentWeekObj?.endDate
                    val dateRange = if (sDate != null && eDate != null) {
                        " · ${sDate.takeLast(5)}—${eDate.takeLast(5)}"
                    } else ""
                    Text(
                        text = "第 ${selectedWeekNumber ?: 1} 周$dateRange ▾",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.primary
                    )
                }

                val maxWeek = if (weeks.isNotEmpty()) weeks.maxOf { it.number } else 25
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .clickable(enabled = (selectedWeekNumber ?: 1) < maxWeek) {
                            val current = selectedWeekNumber ?: 1
                            if (current < maxWeek) selectedWeekNumber = current + 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "下一周",
                        tint = if ((selectedWeekNumber ?: 1) < maxWeek) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 回本周按钮
            val currentActualWeek = weeks.firstOrNull { it.isCurrent }?.number
            if (currentActualWeek != null && selectedWeekNumber != currentActualWeek) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MiuixTheme.colorScheme.primary)
                        .clickable { selectedWeekNumber = currentActualWeek }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "回本周",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onPrimary
                    )
                }
            }
        }

        // 状态处理：加载中、错误重试、会话过期
        if (timetableSnapshot != null && (timetableSnapshot.phase == QueryPhase.LOADING || timetableSnapshot.phase == QueryPhase.FAILED)) {
            LoadStatePanel(
                isLoading = timetableSnapshot.phase == QueryPhase.LOADING && timetableSnapshot.data == null,
                error = timetableSnapshot.error,
                onRetry = {
                    currentTerm?.let {
                        coroutineScope.launch { academic.refreshTimetable(it.id, selectedWeekNumber) }
                    }
                },
                onLogin = onLoginClick
            )
        }

        // 课表内容展示
        Box(modifier = Modifier.weight(1f)) {
            if (!isListView) {
                // 网格视图
                TimetableGrid(
                    courses = filteredCourses,
                    currentWeek = currentWeekObj,
                    onCourseClick = { inspectingCourse = it },
                    onConflictClick = { conflictCourses = it }
                )
            } else {
                // 列表视图（按周一至周日平铺）
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (day in 1..7) {
                        val dayCourses = filteredCourses.filter { it.dayOfWeek == day }
                        if (dayCourses.isNotEmpty()) {
                            item {
                                Text(
                                    text = "星期${dayOfWeekText(day)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                )
                            }
                            items(dayCourses) { c ->
                                QueryCard(
                                    title = c.title,
                                    subtitle = "第 ${c.beginSection}-${c.endSection} 节 (${c.beginTime ?: ""}-${c.endTime ?: ""}) · ${c.place ?: "教室待定"}",
                                    onClick = { inspectingCourse = c }
                                ) {
                                    if (!c.teacher.isNullOrBlank()) {
                                        Text(
                                            text = "教师：${c.teacher}",
                                            fontSize = 13.sp,
                                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (filteredCourses.isEmpty() && timetableSnapshot?.phase == QueryPhase.READY) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "本周暂无已安排排课",
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 底部来源及未排课提示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val unscheduledCount = (rawTable?.unscheduled?.size ?: 0) + (rawTable?.practice?.size ?: 0)
            if (unscheduledCount > 0) {
                Text(
                    text = "未排课或实践课程 $unscheduledCount 项 ›",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showUnscheduledSheet = true }
                )
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            SafeDataTag(
                sourceName = "教务系统",
                lastSuccessEpochMillis = timetableSnapshot?.lastSuccessEpochMillis,
                isStale = timetableSnapshot?.isStale ?: false
            )
        }
    }

    // 抽屉 1：课程详情原生抽屉
    inspectingCourse?.let { c ->
        val others = rawTable?.arranged?.filter { it.title == c.title && it != c }.orEmpty()
        val campusName = campuses.firstOrNull { it.id == c.campusId }?.name
        CourseDetailBottomSheet(
            course = c,
            otherOccurrences = others,
            campusName = campusName,
            lastUpdatedTime = timetableSnapshot?.lastSuccessEpochMillis,
            onDismiss = { inspectingCourse = null }
        )
    }

    // 抽屉 2：冲突课程选择抽屉
    conflictCourses?.let { conflicts ->
        OverlayBottomSheet(
            show = true,
            title = "同时段课程冲突 (${conflicts.size} 项)",
            onDismissRequest = { conflictCourses = null },
            endAction = {
                Button(onClick = { conflictCourses = null }) { Text("关闭") }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "该时间段存在多门排课，请选择查看具体详情：",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
                conflicts.forEach { c ->
                    QueryCard(
                        title = c.title,
                        subtitle = "第 ${c.beginSection}-${c.endSection} 节 · ${c.place ?: "教室待定"}",
                        onClick = {
                            conflictCourses = null
                            inspectingCourse = c
                        }
                    ) {}
                }
            }
        }
    }

    // 抽屉 3：周次选择抽屉
    if (showWeekPicker) {
        OverlayBottomSheet(
            show = true,
            title = "选择教学周",
            onDismissRequest = { showWeekPicker = false },
            endAction = { Button(onClick = { showWeekPicker = false }) { Text("关闭") } }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weeks) { w ->
                    val isSelected = w.number == selectedWeekNumber
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f) else MiuixTheme.colorScheme.surfaceContainer)
                            .clickable {
                                selectedWeekNumber = w.number
                                showWeekPicker = false
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "第 ${w.number} 周" + if (w.isCurrent) " (本周)" else "",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                        )
                        if (w.startDate != null && w.endDate != null) {
                            Text(
                                text = "${w.startDate} ~ ${w.endDate}",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    // 抽屉 4：学期选择抽屉
    if (showTermPicker) {
        OverlayBottomSheet(
            show = true,
            title = "选择学期",
            onDismissRequest = { showTermPicker = false },
            endAction = { Button(onClick = { showTermPicker = false }) { Text("关闭") } }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(terms) { t ->
                    val isSelected = t.id == currentTerm?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f) else MiuixTheme.colorScheme.surfaceContainer)
                            .clickable {
                                selectedTerm = t
                                selectedWeekNumber = null
                                showTermPicker = false
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = t.name + if (t.isCurrent) " (当前)" else "",
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    // 抽屉 5：未排课与更多页面菜单
    if (showUnscheduledSheet) {
        val unscheduledList = (rawTable?.unscheduled.orEmpty() + rawTable?.practice.orEmpty())
        OverlayBottomSheet(
            show = true,
            title = "更多课表选项与未排课",
            onDismissRequest = { showUnscheduledSheet = false },
            endAction = { Button(onClick = { showUnscheduledSheet = false }) { Text("关闭") } }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 快捷跳转作息时间与校历
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            showUnscheduledSheet = false
                            AppNavigator.navigateTo(AppDestination.BellSchedule)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("查看校区作息时间")
                    }
                }

                Text(
                    text = "未安排节次或集中实践课程 (${unscheduledList.size} 门)：",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                if (unscheduledList.isEmpty()) {
                    Text(
                        text = "本学期暂无未安排节次的课程记录。",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                } else {
                    unscheduledList.forEach { u ->
                        QueryCard(
                            title = u.title,
                            subtitle = u.reason ?: "集中实践/未排课，请以学院具体安排为准"
                        ) {}
                    }
                }
            }
        }
    }
}

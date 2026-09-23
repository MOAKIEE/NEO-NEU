package edu.neu.campus.app.feature.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.timetable.CourseDetailBottomSheet
import edu.neu.campus.ui.timetable.TimetableGrid
import edu.neu.campus.ui.timetable.dayOfWeekText
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

@Composable
fun TimetableScreen(
    onLoginClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val academic = CampusDataProvider.academic
    val colors = CampusTheme.colors

    val termsSnapshot by academic.terms().collectAsState()
    val terms = termsSnapshot.data.orEmpty()
    var selectedTermId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTerm = terms.firstOrNull { it.id == selectedTermId }

    LaunchedEffect(terms) {
        if (selectedTerm == null && terms.isNotEmpty()) {
            selectedTermId = (terms.firstOrNull { it.isCurrent } ?: terms.first()).id
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
    var selectedWeekNumber by rememberSaveable(currentTerm?.id) { mutableStateOf<Int?>(null) }
    var selectedCampusId by rememberSaveable(currentTerm?.id) { mutableStateOf<String?>(null) }
    var isListView by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(currentTerm?.id) {
        val termId = currentTerm?.id ?: return@LaunchedEffect
        academic.refreshWeeks(termId)
        academic.refreshCampuses(termId)
    }

    val weeksSnapshot = currentTerm?.let { academic.weeks(it.id).collectAsState().value }
    LaunchedEffect(weeksSnapshot?.data) {
        val wList = weeksSnapshot?.data.orEmpty()
        weeks = wList
        if (wList.none { it.number == selectedWeekNumber } && wList.isNotEmpty()) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // 1. 顶部标题栏（包含网格/列表切换与更多菜单）
        TopAppBar(
            title = "课表",
            actions = {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.brandContainer)
                        .clickable { isListView = !isListView }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isListView) Icons.Default.DateRange else Icons.Default.Menu,
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isListView) "网格" else "列表",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.brand
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showUnscheduledSheet = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多",
                        tint = colors.textPrimary
                    )
                }
            }
        )

        // 2. 学期选择与校区筛选栏（同一控制行，Section 5）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .clickable { showTermPicker = true }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentTerm?.name ?: "选择学期",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(text = " ▾", fontSize = 11.sp, color = colors.textSecondary)
            }

            // 校区筛选
            if (campuses.size > 1) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .clickable {
                            val allIds = listOf<String?>(null) + campuses.map { it.id }
                            val nextIndex = (allIds.indexOf(selectedCampusId) + 1) % allIds.size
                            selectedCampusId = allIds[nextIndex]
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    val label = if (selectedCampusId == null) "全部校区" else campuses.firstOrNull { it.id == selectedCampusId }?.name ?: "校区"
                    Text(text = "$label ▾", fontSize = 12.sp, color = colors.textSecondary)
                }
            }
        }

        // 3. 周次控制器（独立一行，‹ 上一周、第 N 周、› 下一周、回本周）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 上一周按钮 (touch target >= 44dp)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .clickable(enabled = (selectedWeekNumber ?: 1) > 1) {
                            val current = selectedWeekNumber ?: 1
                            if (current > 1) selectedWeekNumber = current - 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "上一周",
                        tint = if ((selectedWeekNumber ?: 1) > 1) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // 当前周次指示与下拉
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .clickable { showWeekPicker = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                        color = colors.brand
                    )
                }

                // 下一周按钮
                val maxWeek = if (weeks.isNotEmpty()) weeks.maxOf { it.number } else 25
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .clickable(enabled = (selectedWeekNumber ?: 1) < maxWeek) {
                            val current = selectedWeekNumber ?: 1
                            if (current < maxWeek) selectedWeekNumber = current + 1
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "下一周",
                        tint = if ((selectedWeekNumber ?: 1) < maxWeek) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 回本周按钮（离开本周时出现）
            val currentActualWeek = weeks.firstOrNull { it.isCurrent }?.number
            if (currentActualWeek != null && selectedWeekNumber != currentActualWeek) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.brand)
                        .clickable { selectedWeekNumber = currentActualWeek }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "回本周",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }
        }

        if (termsSnapshot.error != null || weeksSnapshot?.error != null) {
            LoadStatePanel(false, error = weeksSnapshot?.error ?: termsSnapshot.error,
                onRetry = { coroutineScope.launch { academic.refreshTerms(); currentTerm?.let { academic.refreshWeeks(it.id) } } },
                onLogin = onLoginClick)
        }
        if (timetableSnapshot?.isStale == true) SafeDataTag(text = "当前显示上次同步课表，可能已变化")
        // 状态处理：加载与异常重试
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
            if (!isListView && LocalDensity.current.fontScale < 1.3f && !(selectedCampusId == null && campuses.size > 1)) {
                // 网格视图
                TimetableGrid(
                    courses = filteredCourses,
                    currentWeek = currentWeekObj,
                    sections = rawTable?.sectionsByCampus?.let { if (selectedCampusId != null) it[selectedCampusId].orEmpty() else it.values.flatten() }.orEmpty(),
                    onCourseClick = { inspectingCourse = it },
                    onConflictClick = { conflictCourses = it }
                )
            } else {
                // 列表视图：按周一至周日以 B 类 Surface 分组排布
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (day in 1..7) {
                        val dayCourses = filteredCourses.filter { it.dayOfWeek == day }.sortedWith(compareBy({ it.campusId }, { it.beginSection }))
                        if (dayCourses.isNotEmpty()) {
                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "星期${dayOfWeekText(day)}${if (selectedCampusId == null && campuses.size > 1) " · 按校区显示" else ""}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                    CampusGroup {
                                        dayCourses.forEachIndexed { idx, c ->
                                            val (accentColor, _) = colors.courseColor(c.title)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { inspectingCourse = c }
                                                    .padding(vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier.weight(1f),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(4.dp)
                                                            .height(36.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(accentColor)
                                                    )
                                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                        Text(
                                                            text = if (campuses.size > 1) "${c.title} · ${campuses.firstOrNull { it.id == c.campusId }?.name ?: c.campusId}" else c.title,
                                                            fontSize = 15.sp,
                                                            fontWeight = FontWeight.Medium,
                                                            color = colors.textPrimary
                                                        )
                                                        val timeStr = if (!c.beginTime.isNullOrBlank()) " (${c.beginTime}-${c.endTime})" else ""
                                                        Text(
                                                            text = "第 ${c.beginSection}-${c.endSection} 节$timeStr · ${c.place ?: "教室待定"}",
                                                            fontSize = 12.sp,
                                                            color = colors.textSecondary
                                                        )
                                                    }
                                                }
                                                val teacherName = c.teacher
                                                if (!teacherName.isNullOrBlank()) {
                                                    Text(
                                                        text = teacherName,
                                                        fontSize = 12.sp,
                                                        color = colors.textSecondary,
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }
                                            }
                                            if (idx < dayCourses.lastIndex) {
                                                CampusGroupDivider(startIndent = 14.dp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (filteredCourses.isEmpty() && timetableSnapshot?.phase == QueryPhase.READY) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "本周暂无已安排课程",
                                    color = colors.textSecondary,
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
                .background(colors.surface)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val unscheduledCount = (rawTable?.unscheduled?.size ?: 0) + (rawTable?.practice?.size ?: 0)
            if (unscheduledCount > 0) {
                Text(
                    text = "未排课或实践课程 $unscheduledCount 项 ›",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.brand,
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "该时间段存在多门排课，请选择具体课程查看详情：",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                CampusGroup {
                    conflicts.forEachIndexed { idx, c ->
                        val (accentColor, _) = colors.courseColor(c.title)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    conflictCourses = null
                                    inspectingCourse = c
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(accentColor)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = if (campuses.size > 1) "${c.title} · ${campuses.firstOrNull { it.id == c.campusId }?.name ?: c.campusId}" else c.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "第 ${c.beginSection}-${c.endSection} 节 · ${c.place ?: "教室待定"}",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            Text(
                                text = "详情 ›",
                                fontSize = 13.sp,
                                color = colors.brand
                            )
                        }
                        if (idx < conflicts.lastIndex) {
                            CampusGroupDivider(startIndent = 14.dp)
                        }
                    }
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
                    .heightIn(max = 400.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(weeks) { w ->
                    val isSelected = w.number == selectedWeekNumber
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.brandContainer else colors.surface)
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
                            color = if (isSelected) colors.brand else colors.textPrimary
                        )
                        if (w.startDate != null && w.endDate != null) {
                            Text(
                                text = "${w.startDate} ~ ${w.endDate}",
                                fontSize = 12.sp,
                                color = if (isSelected) colors.brand else colors.textSecondary
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
                    .heightIn(max = 400.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(terms) { t ->
                    val isSelected = t.id == currentTerm?.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.brandContainer else colors.surface)
                            .clickable {
                                selectedTermId = t.id
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
                            color = if (isSelected) colors.brand else colors.textPrimary
                        )
                    }
                }
            }
        }
    }

    // 抽屉 5：未排课与作息快捷选项
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 快捷跳转作息时间与校历
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.brandContainer)
                            .clickable {
                                showUnscheduledSheet = false
                                AppNavigator.navigateTo(AppDestination.BellSchedule)
                            }
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "查看校区作息时间",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.brand
                        )
                    }
                }

                Text(
                    text = "未安排节次或集中实践课程 (${unscheduledList.size} 门)：",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                if (unscheduledList.isEmpty()) {
                    Text(
                        text = "本学期暂无未安排节次的排课记录。",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                } else {
                    CampusGroup {
                        unscheduledList.forEachIndexed { idx, u ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = u.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = u.reason ?: "集中实践/未排课，请以学院具体安排为准",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                            if (idx < unscheduledList.lastIndex) {
                                CampusGroupDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

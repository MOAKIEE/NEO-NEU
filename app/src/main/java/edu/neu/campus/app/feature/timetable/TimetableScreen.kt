package edu.neu.campus.app.feature.timetable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusFilterChip
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusSelectionRow
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.timetable.CourseDetailBottomSheet
import edu.neu.campus.ui.timetable.TimetableGrid
import edu.neu.campus.ui.timetable.dayOfWeekText
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.ExpandMore
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Ok
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
    val showGrid = !isListView && LocalDensity.current.fontScale < 1.3f &&
        !(selectedCampusId == null && campuses.size > 1)

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        // 1. 顶部标题栏
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "课表",
            subtitle = currentTerm?.name.orEmpty(),
            actions = {
                ViewModeToggle(isListView = isListView, onToggle = { isListView = !isListView })
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = { showUnscheduledSheet = true }) {
                    Icon(
                        imageVector = MiuixIcons.Regular.More,
                        contentDescription = "更多",
                        tint = colors.textPrimary
                    )
                }
            }
        )

        // 2. 学期与校区筛选
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CampusFilterChip(
                text = currentTerm?.name ?: "选择学期",
                onClick = { showTermPicker = true }
            )
            if (campuses.size > 1) {
                val campusLabel = if (selectedCampusId == null) "全部校区"
                else campuses.firstOrNull { it.id == selectedCampusId }?.name ?: "校区"
                CampusFilterChip(
                    text = campusLabel,
                    active = selectedCampusId != null,
                    onClick = {
                        val allIds = listOf<String?>(null) + campuses.map { it.id }
                        val nextIndex = (allIds.indexOf(selectedCampusId) + 1) % allIds.size
                        selectedCampusId = allIds[nextIndex]
                    }
                )
            }
        }

        // 3. 周次控制器
        WeekController(
            weekNumber = selectedWeekNumber ?: 1,
            week = currentWeekObj,
            maxWeek = if (weeks.isNotEmpty()) weeks.maxOf { it.number } else 25,
            currentActualWeek = weeks.firstOrNull { it.isCurrent }?.number,
            onPrevious = {
                val current = selectedWeekNumber ?: 1
                if (current > 1) selectedWeekNumber = current - 1
            },
            onNext = {
                val current = selectedWeekNumber ?: 1
                val maxWeek = if (weeks.isNotEmpty()) weeks.maxOf { it.number } else 25
                if (current < maxWeek) selectedWeekNumber = current + 1
            },
            onPickWeek = { showWeekPicker = true },
            onBackToCurrent = { weeks.firstOrNull { it.isCurrent }?.let { selectedWeekNumber = it.number } }
        )

        if (termsSnapshot.error != null || weeksSnapshot?.error != null) {
            LoadStatePanel(
                false,
                error = weeksSnapshot?.error ?: termsSnapshot.error,
                onRetry = {
                    coroutineScope.launch {
                        academic.refreshTerms()
                        currentTerm?.let { academic.refreshWeeks(it.id) }
                    }
                },
                onLogin = onLoginClick
            )
        }
        if (timetableSnapshot?.isStale == true) {
            Box(modifier = Modifier.padding(horizontal = CampusSpacing.screenHorizontal)) {
                SafeDataTag(text = "当前显示上次同步课表，可能已变化")
            }
        }
        if (timetableSnapshot != null &&
            (timetableSnapshot.phase == QueryPhase.LOADING || timetableSnapshot.phase == QueryPhase.FAILED)
        ) {
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

        // 4. 课表内容：网格 <-> 列表 转场
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = showGrid,
                transitionSpec = {
                    (fadeIn(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)) +
                        androidx.compose.animation.scaleIn(
                            initialScale = 0.98f,
                            animationSpec = tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)
                        ))
                        .togetherWith(fadeOut(tween(CampusMotion.Duration.instant)))
                        .using(SizeTransform(clip = false))
                },
                label = "timetableMode"
            ) { gridMode ->
                if (gridMode) {
                    // 网格本身是白色表体，收进圆角卡片里，避免与页面底色形成生硬色带。
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                start = CampusSpacing.screenHorizontal,
                                end = CampusSpacing.screenHorizontal,
                                bottom = CampusSpacing.xs
                            )
                            .clip(RoundedCornerShape(CampusShapes.extraLarge))
                            .background(colors.surface)
                            .border(
                                width = 1.dp,
                                color = colors.outlineVariant,
                                shape = RoundedCornerShape(CampusShapes.extraLarge)
                            )
                    ) {
                        TimetableGrid(
                            courses = filteredCourses,
                            currentWeek = currentWeekObj,
                            sections = rawTable?.sectionsByCampus?.let {
                                if (selectedCampusId != null) it[selectedCampusId].orEmpty() else it.values.flatten()
                            }.orEmpty(),
                            onCourseClick = { inspectingCourse = it },
                            onConflictClick = { conflictCourses = it }
                        )
                    }
                } else {
                    TimetableDayList(
                        courses = filteredCourses,
                        showCampusName = campuses.size > 1,
                        campusNameOf = { id -> campuses.firstOrNull { it.id == id }?.name ?: id },
                        isReady = timetableSnapshot?.phase == QueryPhase.READY,
                        onCourseClick = { inspectingCourse = it }
                    )
                }
            }
        }

        // 5. 底部来源与未排课提示（与页面同底色，仅用分隔线区隔，避免多出一条白色色带）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colors.divider)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val unscheduledCount = (rawTable?.unscheduled?.size ?: 0) + (rawTable?.practice?.size ?: 0)
            if (unscheduledCount > 0) {
                Row(
                    modifier = Modifier
                        .tapScale(onClick = { showUnscheduledSheet = true }, pressedScale = 0.95f, clipShape = RoundedCornerShape(CampusShapes.pill))
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "未排课或实践课程 $unscheduledCount 项",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.brand
                    )
                    Icon(
                        imageVector = MiuixIcons.Basic.ArrowRight,
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(13.dp)
                    )
                }
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

    // 抽屉 1：课程详情
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

    // 抽屉 2：冲突课程选择
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
                    .padding(CampusSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
            ) {
                Text(
                    text = "该时间段存在多门排课，请选择具体课程查看详情：",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                CampusGroup {
                    conflicts.forEachIndexed { idx, c ->
                        val accent = colors.courseAccent(c.title)
                        val (_, container) = colors.courseColor(c.title)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .tapScale(
                                    onClick = {
                                        conflictCourses = null
                                        inspectingCourse = c
                                    },
                                    pressedScale = 0.985f
                                )
                                .padding(vertical = CampusSpacing.xxs + 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                        ) {
                            CampusIconBadge(
                                icon = MiuixIcons.Regular.Notes,
                                tint = accent,
                                container = container
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = if (campuses.size > 1) {
                                        "${c.title} · ${campuses.firstOrNull { it.id == c.campusId }?.name ?: c.campusId}"
                                    } else c.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "第 ${c.beginSection}-${c.endSection} 节 · ${c.place ?: "教室待定"}",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Icon(
                                imageVector = MiuixIcons.Basic.ArrowRight,
                                contentDescription = null,
                                tint = colors.textTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        if (idx < conflicts.lastIndex) {
                            CampusGroupDivider(startIndent = 56.dp)
                        }
                    }
                }
            }
        }
    }

    // 抽屉 3：周次选择
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
                    .heightIn(max = 420.dp)
                    .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
            ) {
                itemsIndexed(weeks) { index, w ->
                    val isSelected = w.number == selectedWeekNumber
                    StaggeredAppear(index = index, key = weeks.size) {
                        CampusSelectionRow(
                            title = "第 ${w.number} 周" + if (w.isCurrent) "（本周）" else "",
                            trailing = if (w.startDate != null && w.endDate != null) "${w.startDate} ~ ${w.endDate}" else null,
                            selected = isSelected,
                            onClick = {
                                selectedWeekNumber = w.number
                                showWeekPicker = false
                            }
                        )
                    }
                }
            }
        }
    }

    // 抽屉 4：学期选择
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
                    .heightIn(max = 420.dp)
                    .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
            ) {
                itemsIndexed(terms) { index, t ->
                    val isSelected = t.id == currentTerm?.id
                    StaggeredAppear(index = index, key = terms.size) {
                        CampusSelectionRow(
                            title = t.name + if (t.isCurrent) "（当前）" else "",
                            trailing = null,
                            selected = isSelected,
                            onClick = {
                                selectedTermId = t.id
                                selectedWeekNumber = null
                                showTermPicker = false
                            }
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
                    .padding(CampusSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
            ) {
                CampusCard(
                    onClick = {
                        showUnscheduledSheet = false
                        AppNavigator.navigateTo(AppDestination.BellSchedule)
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                    ) {
                        CampusIconBadge(
                            icon = MiuixIcons.Regular.GridView,
                            tint = colors.brand,
                            container = colors.brandContainer
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "查看校区作息时间",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "按学校返回的节次时间展示",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                        Icon(
                            imageVector = MiuixIcons.Basic.ArrowRight,
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = "未安排节次或集中实践课程（${unscheduledList.size} 门）",
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
                                    .padding(vertical = CampusSpacing.xs),
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

/** 网格/列表切换：胶囊按钮，图标随状态切换并有位移过渡。 */
@Composable
private fun ViewModeToggle(
    isListView: Boolean,
    onToggle: () -> Unit
) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .tapScale(onClick = onToggle, pressedScale = 0.94f, clipShape = RoundedCornerShape(CampusShapes.pill))
            .background(colors.brandContainer)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AnimatedContent(
            targetState = isListView,
            transitionSpec = {
                (fadeIn(tween(CampusMotion.Duration.short)) +
                    slideInHorizontally(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)) { it / 2 })
                    .togetherWith(
                        fadeOut(tween(CampusMotion.Duration.instant)) +
                            slideOutHorizontally(tween(CampusMotion.Duration.short, easing = CampusMotion.Easing.emphasizedAccelerate)) { -it / 2 }
                    )
            },
            label = "viewModeIcon"
        ) { list ->
            Icon(
                imageVector = if (list) MiuixIcons.Regular.GridView else MiuixIcons.Regular.ListView,
                contentDescription = null,
                tint = colors.brand,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = if (isListView) "网格" else "列表",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = colors.brand
        )
    }
}

/** 周次控制条。 */
@Composable
private fun WeekController(
    weekNumber: Int,
    week: TeachingWeek?,
    maxWeek: Int,
    currentActualWeek: Int?,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickWeek: () -> Unit,
    onBackToCurrent: () -> Unit
) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
        ) {
            StepperButton(
                enabled = weekNumber > 1,
                onClick = onPrevious
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.ChevronBackward,
                    contentDescription = "上一周",
                    tint = if (weekNumber > 1) colors.textPrimary else colors.textDisabled,
                    modifier = Modifier.size(17.dp)
                )
            }

            Row(
                modifier = Modifier
                    .tapScale(onClick = onPickWeek, pressedScale = 0.95f, clipShape = RoundedCornerShape(CampusShapes.pill))
                    .background(colors.brandContainer)
                    .padding(horizontal = CampusSpacing.md, vertical = CampusSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                val sDate = week?.startDate
                val eDate = week?.endDate
                val dateRange = if (sDate != null && eDate != null) " · ${sDate.takeLast(5)}—${eDate.takeLast(5)}" else ""
                Text(
                    text = "第 $weekNumber 周$dateRange",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.brand
                )
                Icon(
                    imageVector = MiuixIcons.Regular.ExpandMore,
                    contentDescription = null,
                    tint = colors.brand,
                    modifier = Modifier.size(15.dp)
                )
            }

            StepperButton(
                enabled = weekNumber < maxWeek,
                onClick = onNext
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.ChevronForward,
                    contentDescription = "下一周",
                    tint = if (weekNumber < maxWeek) colors.textPrimary else colors.textDisabled,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = currentActualWeek != null && weekNumber != currentActualWeek,
            enter = fadeIn(tween(CampusMotion.Duration.medium)) +
                expandVertically(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)),
            exit = fadeOut(tween(CampusMotion.Duration.instant)) + shrinkVertically(tween(CampusMotion.Duration.short))
        ) {
            Row(
                modifier = Modifier
                    .tapScale(onClick = onBackToCurrent, pressedScale = 0.94f, clipShape = RoundedCornerShape(CampusShapes.pill))
                    .background(colors.brand)
                    .padding(horizontal = CampusSpacing.sm + 2.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = MiuixIcons.Regular.Ok,
                    contentDescription = null,
                    tint = colors.onBrand,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "回本周",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onBrand
                )
            }
        }
    }
}

@Composable
private fun StepperButton(
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(CampusShapes.extraSmall))
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(CampusShapes.extraSmall))
            .tapScale(onClick = onClick, enabled = enabled, pressedScale = 0.9f),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** 每日列表视图：按星期分组，逐组交错入场。 */
@Composable
private fun TimetableDayList(
    courses: List<CourseOccurrence>,
    showCampusName: Boolean,
    campusNameOf: (String) -> String,
    isReady: Boolean,
    onCourseClick: (CourseOccurrence) -> Unit
) {
    val colors = CampusTheme.colors
    val days = remember(courses) {
        (1..7).map { day ->
            day to courses.filter { it.dayOfWeek == day }
                .sortedWith(compareBy({ it.campusId }, { it.beginSection }))
        }.filter { it.second.isNotEmpty() }
    }

    if (days.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(CampusSpacing.xxl),
            contentAlignment = Alignment.Center
        ) {
            if (isReady) {
                Text(
                    text = "本周暂无已安排课程",
                    color = colors.textSecondary,
                    fontSize = 15.sp
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
    ) {
        itemsIndexed(days, key = { _, item -> item.first }) { dayIndex, (day, dayCourses) ->
            StaggeredAppear(
                index = dayIndex,
                key = days.size,
                modifier = Modifier.animateItem()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)) {
                    Text(
                        text = "星期${dayOfWeekText(day)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    CampusGroup {
                        dayCourses.forEachIndexed { idx, c ->
                            val accent = colors.courseAccent(c.sourceId ?: "${c.campusId}:${c.title}")
                            val (_, container) = colors.courseColor(c.sourceId ?: "${c.campusId}:${c.title}")
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tapScale(onClick = { onCourseClick(c) }, pressedScale = 0.985f)
                                    .padding(vertical = CampusSpacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                ) {
                                    CampusIconBadge(
                                        icon = MiuixIcons.Regular.Notes,
                                        tint = accent,
                                        container = container,
                                        size = 38.dp,
                                        iconSize = 20.dp
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = if (showCampusName) "${c.title} · ${campusNameOf(c.campusId)}" else c.title,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val timeStr = if (!c.beginTime.isNullOrBlank()) "（${c.beginTime}-${c.endTime}）" else ""
                                        Text(
                                            text = "第 ${c.beginSection}-${c.endSection} 节$timeStr · ${c.place ?: "教室待定"}",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (!c.teacher.isNullOrBlank()) {
                                    Text(
                                        text = c.teacher!!,
                                        fontSize = 12.sp,
                                        color = colors.textTertiary,
                                        modifier = Modifier.padding(start = CampusSpacing.xs)
                                    )
                                }
                            }
                            if (idx < dayCourses.lastIndex) {
                                CampusGroupDivider(startIndent = 50.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

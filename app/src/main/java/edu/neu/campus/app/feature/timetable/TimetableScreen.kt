package edu.neu.campus.app.feature.timetable

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.SyncReason
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusDropdownArrow
import edu.neu.campus.ui.components.CampusEmptyHint
import edu.neu.campus.ui.components.CampusFilterChip
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusSelectionRow
import edu.neu.campus.ui.components.CampusSheetCloseAction
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.TimeFormatter
import edu.neu.campus.ui.components.rememberSchoolToday
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.timetable.CourseDetailBottomSheet
import edu.neu.campus.ui.timetable.TimetableDayList
import edu.neu.campus.ui.timetable.TimetableGrid
import edu.neu.campus.ui.timetable.TimetableLayout
import edu.neu.campus.ui.timetable.buildTimetableLayout
import edu.neu.campus.ui.timetable.colorKey
import edu.neu.campus.ui.timetable.dayOfWeekText
import edu.neu.campus.ui.timetable.inferFirstDayOfWeek
import edu.neu.campus.ui.timetable.isoDayOfWeek
import edu.neu.campus.ui.timetable.shortDate
import edu.neu.campus.ui.timetable.weekDaysOf
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.ListView
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import java.util.Calendar
import java.util.TimeZone

/** 切周后同一周的课表在该时间内视为新鲜，来回滑动不重复请求学校接口。 */
private const val WeekRefreshFreshMillis = 2 * 60_000L

/** 网格一次切周动画所需的全部数据，旧周退场时仍绘制旧数据。 */
private data class GridFrame(val week: Int?, val layout: TimetableLayout, val overlay: String?)

@Composable
fun TimetableScreen(
    onLoginClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val academic = CampusDataProvider.academic
    val colors = CampusTheme.colors

    // ---- 数据：全部直接从快照派生，不经 LaunchedEffect 转存，避免进入页面首帧是空列表 ----
    val termsSnapshot by academic.terms().collectAsState()
    val terms = termsSnapshot.data.orEmpty()
    var pickedTermId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentTerm = terms.firstOrNull { it.id == pickedTermId }
        ?: terms.firstOrNull { it.isCurrent } ?: terms.firstOrNull()
    val termId = currentTerm?.id

    val weeksSnapshot = termId?.let { academic.weeks(it).collectAsState().value }
    val weeks = weeksSnapshot?.data.orEmpty()
    val campuses = termId?.let { academic.campuses(it).collectAsState().value }?.data.orEmpty()
    val firstDayOfWeek = remember(weeks) { inferFirstDayOfWeek(weeks) }
    val currentActualWeek = weeks.firstOrNull { it.isCurrent }?.number
    val minWeek = weeks.minOfOrNull { it.number }
    val maxWeek = weeks.maxOfOrNull { it.number }

    var pickedWeek by rememberSaveable(termId) { mutableStateOf<Int?>(null) }
    val weekNumber = pickedWeek?.takeIf { n -> weeks.isEmpty() || weeks.any { it.number == n } }
        ?: currentActualWeek ?: weeks.firstOrNull()?.number
    val week = weeks.firstOrNull { it.number == weekNumber }
    var selectedCampusId by rememberSaveable(termId) { mutableStateOf<String?>(null) }
    var isListView by rememberSaveable { mutableStateOf(false) }

    val timetableSnapshot = termId?.let { academic.timetable(it, weekNumber).collectAsState().value }
    val table = timetableSnapshot?.data

    // ---- 同步：进入页面由 SyncCoordinator 统一刷新；这里只补用户主动切换学期／周次后的请求 ----
    DisposableEffect(termId, weekNumber) {
        val unregister = CampusDataProvider.sync.registerVisible(MainTab.TIMETABLE, AppDestination.Main) {
            academic.refreshTerms()
            val id = termId ?: academic.terms().value.data?.firstOrNull { it.isCurrent }?.id
            if (id != null) {
                academic.refreshWeeks(id)
                academic.refreshCampuses(id)
                val targetWeek = weekNumber ?: academic.weeks(id).value.data?.firstOrNull { it.isCurrent }?.number
                academic.refreshTimetable(id, targetWeek)
            }
        }
        onDispose { unregister() }
    }
    var termSeen by remember { mutableStateOf(false) }
    LaunchedEffect(termId) {
        val id = termId ?: return@LaunchedEffect
        if (!termSeen) termSeen = true
        else {
            academic.refreshWeeks(id)
            academic.refreshCampuses(id)
        }
    }
    var weekSeen by remember { mutableStateOf(false) }
    LaunchedEffect(termId, weekNumber) {
        val id = termId ?: return@LaunchedEffect
        val targetWeek = weekNumber ?: return@LaunchedEffect
        if (!weekSeen) {
            weekSeen = true
            return@LaunchedEffect
        }
        val snapshot = academic.timetable(id, targetWeek).value
        val fresh = snapshot.phase == QueryPhase.READY && !snapshot.isStale &&
            System.currentTimeMillis() - (snapshot.lastSuccessEpochMillis ?: 0L) < WeekRefreshFreshMillis
        if (snapshot.phase != QueryPhase.LOADING && !fresh) academic.refreshTimetable(id, targetWeek)
    }
    val retry: () -> Unit = {
        coroutineScope.launch {
            CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, SyncReason.MANUAL)
        }
    }

    // ---- 派生展示模型 ----
    // 切周时新周数据尚未就绪，沿用已知节次画出空网格骨架，不闪成整页加载态。
    var knownSections by remember { mutableStateOf<Map<String, List<Section>>>(emptyMap()) }
    val tableSections = table?.sectionsByCampus
    if (tableSections != null && tableSections.isNotEmpty() && tableSections != knownSections) {
        SideEffect { knownSections = tableSections }
    }
    val sectionsByCampus = tableSections?.takeIf { it.isNotEmpty() } ?: knownSections
    val courses = remember(table, selectedCampusId) {
        val list = table?.arranged.orEmpty()
        if (selectedCampusId == null) list else list.filter { it.campusId == selectedCampusId }
    }
    val sections = remember(sectionsByCampus, selectedCampusId) {
        if (selectedCampusId != null) sectionsByCampus[selectedCampusId].orEmpty() else sectionsByCampus.values.flatten()
    }
    val days = remember(week, firstDayOfWeek) { weekDaysOf(week, firstDayOfWeek) }
    val layout = remember(courses, sections, days) { buildTimetableLayout(courses, sections, days) }
    val today = rememberSchoolToday()
    val gridScroll = rememberScrollState()

    val showGrid = !isListView && LocalDensity.current.fontScale < 1.3f &&
        !(selectedCampusId == null && campuses.size > 1)
    val syncError = timetableSnapshot?.error ?: weeksSnapshot?.error ?: termsSnapshot.error
    val isSyncing = termsSnapshot.phase == QueryPhase.LOADING || weeksSnapshot?.phase == QueryPhase.LOADING ||
        timetableSnapshot?.phase == QueryPhase.LOADING
    val hasStructure = table != null || sectionsByCampus.isNotEmpty()
    val contentMessage = when {
        table == null && syncError != null -> "本周课表同步失败"
        table == null -> "正在加载${weekNumber?.let { "第 $it 周" } ?: ""}课表…"
        courses.isEmpty() && timetableSnapshot?.phase != QueryPhase.LOADING -> "本周没有已安排的课程"
        else -> null
    }

    var inspectingCourse by remember { mutableStateOf<CourseOccurrence?>(null) }
    var conflictCourses by remember { mutableStateOf<List<CourseOccurrence>?>(null) }
    var showWeekPicker by remember { mutableStateOf(false) }
    var showTermPicker by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    fun stepWeek(delta: Int) {
        val current = weekNumber ?: return
        val target = current + delta
        if ((minWeek == null || target >= minWeek) && (maxWeek == null || target <= maxWeek) && target >= 1) {
            pickedWeek = target
        }
    }

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "课表",
            subtitle = todaySubtitle(today, currentActualWeek),
            actions = {
                ViewModeToggle(isListView = isListView, onToggle = { isListView = !isListView })
                IconButton(onClick = { showMoreSheet = true }) {
                    Icon(
                        imageVector = MiuixIcons.Regular.More,
                        contentDescription = "更多",
                        tint = colors.textPrimary
                    )
                }
            }
        )

        // 学期、校区筛选与「回本周」
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CampusFilterChip(
                    text = currentTerm?.name ?: "选择学期",
                    onClick = { showTermPicker = true },
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (campuses.size > 1) {
                    val campusLabel = if (selectedCampusId == null) "全部校区"
                    else campuses.firstOrNull { it.id == selectedCampusId }?.name ?: "校区"
                    CampusFilterChip(
                        text = campusLabel,
                        active = selectedCampusId != null,
                        onClick = {
                            val allIds = listOf<String?>(null) + campuses.map { it.id }
                            selectedCampusId = allIds[(allIds.indexOf(selectedCampusId) + 1) % allIds.size]
                        }
                    )
                }
            }
            AnimatedVisibility(
                visible = currentActualWeek != null && weekNumber != currentActualWeek,
                enter = fadeIn(tween(CampusMotion.Duration.short)),
                exit = fadeOut(tween(CampusMotion.Duration.instant))
            ) {
                Text(
                    text = "回本周",
                    modifier = Modifier
                        .tapScale(
                            onClick = { pickedWeek = currentActualWeek },
                            pressedScale = 0.94f,
                            clipShape = RoundedCornerShape(CampusShapes.pill)
                        )
                        .background(colors.brand)
                        .padding(horizontal = CampusSpacing.sm, vertical = 7.dp),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onBrand
                )
            }
        }

        if (termId != null) WeekBar(
            weekNumber = weekNumber,
            week = week,
            isCurrent = weekNumber != null && weekNumber == currentActualWeek,
            canPrevious = weekNumber != null && weekNumber > (minWeek ?: 1),
            canNext = weekNumber != null && maxWeek != null && weekNumber < maxWeek,
            onPrevious = { stepWeek(-1) },
            onNext = { stepWeek(1) },
            onPickWeek = { showWeekPicker = true }
        )

        // 已有课表时，同步失败只以一行提示呈现，不替换正文，也不在后台刷新时反复出现／消失。
        AnimatedVisibility(
            visible = hasStructure && syncError != null,
            enter = fadeIn(tween(CampusMotion.Duration.short)) + expandVertically(tween(CampusMotion.Duration.short)),
            exit = fadeOut(tween(CampusMotion.Duration.instant)) + shrinkVertically(tween(CampusMotion.Duration.short))
        ) {
            SyncIssueBar(
                error = syncError,
                showingCache = table != null,
                onRetry = retry,
                onLogin = onLoginClick
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (!hasStructure) {
                if (syncError != null) {
                    LoadStatePanel(isLoading = false, error = syncError, onRetry = retry, onLogin = onLoginClick)
                } else {
                    LoadStatePanel(
                        isLoading = isSyncing,
                        emptyMessage = if (isSyncing) null else "暂无课表数据"
                    )
                }
            } else if (showGrid) {
                // 网格收进圆角卡片，避免白色表体与页面底色形成生硬直边。
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = CampusSpacing.sm, end = CampusSpacing.sm, bottom = CampusSpacing.xs)
                        .clip(RoundedCornerShape(CampusShapes.large))
                        .background(colors.surface)
                        .border(1.dp, colors.outlineVariant, RoundedCornerShape(CampusShapes.large))
                ) {
                    AnimatedContent(
                        targetState = GridFrame(weekNumber, layout, contentMessage),
                        contentKey = { it.week },
                        transitionSpec = {
                            val direction = if ((targetState.week ?: 0) >= (initialState.week ?: 0)) 1 else -1
                            (fadeIn(tween(CampusMotion.Duration.medium)) +
                                slideInHorizontally(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)) { direction * it / 8 })
                                .togetherWith(
                                    fadeOut(tween(CampusMotion.Duration.instant)) +
                                        slideOutHorizontally(tween(CampusMotion.Duration.short, easing = CampusMotion.Easing.emphasizedAccelerate)) { -direction * it / 8 }
                                )
                        },
                        label = "timetableWeek"
                    ) { frame ->
                        TimetableGrid(
                            layout = frame.layout,
                            today = today,
                            scrollState = gridScroll,
                            overlayMessage = frame.overlay,
                            onCourseClick = { inspectingCourse = it },
                            onConflictClick = { conflictCourses = it },
                            onSwipeWeek = { stepWeek(it) }
                        )
                    }
                }
            } else {
                TimetableDayList(
                    days = days,
                    courses = courses,
                    today = today,
                    campusNameOf = if (campuses.size > 1) { id -> campuses.firstOrNull { it.id == id }?.name ?: id } else null,
                    emptyMessage = contentMessage,
                    onCourseClick = { inspectingCourse = it }
                )
            }
        }

        val unscheduledCount = (table?.unscheduled?.size ?: 0) + (table?.practice?.size ?: 0)
        if (unscheduledCount > 0 || isSyncing || timetableSnapshot?.isStale == true) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.divider)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (unscheduledCount > 0) {
                    Row(
                        modifier = Modifier
                            .tapScale(onClick = { showMoreSheet = true }, pressedScale = 0.95f, clipShape = RoundedCornerShape(CampusShapes.pill))
                            .padding(vertical = 4.dp, horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "未排课 $unscheduledCount 项",
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
                }
                if (isSyncing) SafeDataTag(text = "正在同步…")
                else if (timetableSnapshot?.isStale == true) {
                    SafeDataTag(text = "课表旧缓存 · 最近同步 ${TimeFormatter.formatDateTime(timetableSnapshot?.lastSuccessEpochMillis)}")
                }
            }
        }
    }

    // 抽屉 1：课程详情
    inspectingCourse?.let { c ->
        val others = table?.arranged?.filter { it.title == c.title && it != c }.orEmpty()
        CourseDetailBottomSheet(
            course = c,
            otherOccurrences = others,
            campusName = campuses.firstOrNull { it.id == c.campusId }?.name,
            onDismiss = { inspectingCourse = null }
        )
    }

    // 抽屉 2：冲突课程选择
    conflictCourses?.let { conflicts ->
        OverlayBottomSheet(
            show = true,
            title = "同时段课程冲突 (${conflicts.size} 项)",
            onDismissRequest = { conflictCourses = null },
            startAction = { CampusSheetCloseAction(onClick = { conflictCourses = null }) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CampusSpacing.sheetHorizontal, vertical = CampusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
            ) {
                Text(
                    text = "该时间段存在多门排课，请选择具体课程查看详情：",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                CampusGroup {
                    conflicts.forEachIndexed { idx, c ->
                        val (accent, container) = colors.courseColor(c.colorKey())
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
            startAction = { CampusSheetCloseAction(onClick = { showWeekPicker = false }) }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(horizontal = CampusSpacing.sheetHorizontal, vertical = CampusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
            ) {
                if (weeks.isEmpty()) item { CampusEmptyHint(text = "暂无教学周信息") }
                itemsIndexed(weeks, key = { _, w -> w.number }) { _, w ->
                    CampusSelectionRow(
                        title = "第 ${w.number} 周" + if (w.isCurrent) "（本周）" else "",
                        trailing = weekRangeText(w),
                        selected = w.number == weekNumber,
                        onClick = {
                            pickedWeek = w.number
                            showWeekPicker = false
                        }
                    )
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
            startAction = { CampusSheetCloseAction(onClick = { showTermPicker = false }) }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .padding(horizontal = CampusSpacing.sheetHorizontal, vertical = CampusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
            ) {
                if (terms.isEmpty()) item { CampusEmptyHint(text = "暂无可选学期") }
                itemsIndexed(terms, key = { _, t -> t.id }) { _, t ->
                    CampusSelectionRow(
                        title = t.name + if (t.isCurrent) "（当前）" else "",
                        trailing = null,
                        selected = t.id == termId,
                        onClick = {
                            pickedTermId = t.id
                            showTermPicker = false
                        }
                    )
                }
            }
        }
    }

    // 抽屉 5：作息入口与未排课
    if (showMoreSheet) {
        val unscheduledList = table?.unscheduled.orEmpty() + table?.practice.orEmpty()
        OverlayBottomSheet(
            show = true,
            title = "更多课表选项与未排课",
            onDismissRequest = { showMoreSheet = false },
            startAction = { CampusSheetCloseAction(onClick = { showMoreSheet = false }) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CampusSpacing.sheetHorizontal, vertical = CampusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
            ) {
                CampusCard(
                    onClick = {
                        showMoreSheet = false
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
                        Text(
                            text = "查看校区作息时间",
                            modifier = Modifier.weight(1f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
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

/** 「9月25日 星期五 · 教学第 5 周」。 */
private fun todaySubtitle(today: String, currentWeek: Int?): String {
    val parts = today.split("-")
    val dateText = if (parts.size == 3) "${parts[1].toInt()}月${parts[2].toInt()}日" else today
    val weekday = runCatching {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"))
        calendar.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        " 星期${dayOfWeekText(isoDayOfWeek(calendar))}"
    }.getOrDefault("")
    return dateText + weekday + (currentWeek?.let { " · 教学第 $it 周" } ?: "")
}

private fun weekRangeText(week: TeachingWeek?): String? {
    val start = shortDate(week?.startDate) ?: return null
    val end = shortDate(week?.endDate) ?: return null
    return "$start – $end"
}

/** 网格/列表切换：胶囊按钮，显示切换后的目标视图。 */
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
        Icon(
            imageVector = if (isListView) MiuixIcons.Regular.GridView else MiuixIcons.Regular.ListView,
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
}

private val WeekBarHeight = 52.dp
private val WeekStepSize = 36.dp

/** 周次条：左右步进，中间显示周次与日期范围，点按打开周次选择。 */
@Composable
private fun WeekBar(
    weekNumber: Int?,
    week: TeachingWeek?,
    isCurrent: Boolean,
    canPrevious: Boolean,
    canNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPickWeek: () -> Unit
) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CampusSpacing.sm, vertical = CampusSpacing.xxs)
            .height(WeekBarHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WeekStepButton(enabled = canPrevious, onClick = onPrevious, forward = false)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .tapScale(onClick = onPickWeek, pressedScale = 0.97f, clipShape = RoundedCornerShape(CampusShapes.small)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = weekNumber?.let { "第 $it 周" } ?: "整学期",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                if (isCurrent) {
                    Text(
                        text = "本周",
                        modifier = Modifier
                            .clip(RoundedCornerShape(CampusShapes.pill))
                            .background(colors.brandContainer)
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.brand
                    )
                }
                CampusDropdownArrow(tint = colors.textTertiary)
            }
            weekRangeText(week)?.let {
                Text(text = it, fontSize = 12.sp, color = colors.textTertiary)
            }
        }
        WeekStepButton(enabled = canNext, onClick = onNext, forward = true)
    }
}

@Composable
private fun WeekStepButton(enabled: Boolean, onClick: () -> Unit, forward: Boolean) {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .padding(horizontal = CampusSpacing.xs)
            .size(WeekStepSize)
            .tapScale(onClick = onClick, enabled = enabled, pressedScale = 0.9f, clipShape = CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.outlineVariant, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (forward) MiuixIcons.Regular.ChevronForward else MiuixIcons.Regular.ChevronBackward,
            contentDescription = if (forward) "下一周" else "上一周",
            tint = if (enabled) colors.textPrimary else colors.textDisabled,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** 已有课表时的同步失败提示：一行文字 + 操作，不替换正文。 */
@Composable
private fun SyncIssueBar(
    error: QueryError?,
    showingCache: Boolean,
    onRetry: () -> Unit,
    onLogin: () -> Unit
) {
    val colors = CampusTheme.colors
    val needsLogin = error?.kind == QueryErrorKind.AUTH_REQUIRED
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CampusSpacing.sm, vertical = CampusSpacing.xxs)
            .clip(RoundedCornerShape(CampusShapes.small))
            .background(colors.warningContainer)
            .padding(start = CampusSpacing.sm, end = CampusSpacing.xxs, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = (error?.message ?: "同步失败") + if (showingCache) "，当前显示已缓存的课表" else "",
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = colors.onWarningContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (needsLogin || error?.retryable != false) {
            Text(
                text = if (needsLogin) "去登录" else "重试",
                modifier = Modifier
                    .tapScale(
                        onClick = if (needsLogin) onLogin else onRetry,
                        pressedScale = 0.94f,
                        clipShape = RoundedCornerShape(CampusShapes.pill)
                    )
                    .padding(horizontal = CampusSpacing.sm, vertical = CampusSpacing.xs),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onWarningContainer
            )
        }
    }
}

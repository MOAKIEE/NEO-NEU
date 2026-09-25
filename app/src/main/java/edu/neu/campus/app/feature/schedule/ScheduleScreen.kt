package edu.neu.campus.app.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Weeks

/** 列表行图标尺寸；分隔线据此缩进到文字起点。 */
private val WeekIconSize = 38.dp
private val SectionIconSize = 34.dp

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onLoginClick: (() -> Unit)? = null
) {
    val repo = CampusDataProvider.academic
    val scope = rememberCoroutineScope()
    val colors = CampusTheme.colors
    val terms by repo.terms().collectAsState()
    var selectedTermId by rememberSaveable { mutableStateOf<String?>(null) }
    var calendar by rememberSaveable { mutableStateOf(false) }
    val term = terms.data?.firstOrNull { it.id == selectedTermId }
        ?: terms.data?.firstOrNull { it.isCurrent }
    val weeks = term?.let { repo.weeks(it.id).collectAsState().value }
    val table = term?.let { repo.timetable(it.id, null).collectAsState().value }
    DisposableEffect(term?.id, calendar) {
        val unregister = CampusDataProvider.sync.registerVisible(AppNavigator.currentTab, AppNavigator.currentDestination) {
            repo.refreshTerms()
            val termId = term?.id ?: repo.terms().value.data?.firstOrNull { it.isCurrent }?.id
            if (termId != null) {
                if (calendar) repo.refreshWeeks(termId) else repo.refreshTimetable(termId, null)
            }
        }
        onDispose { unregister() }
    }
    var initialScheduleTermSeen by remember { mutableStateOf(false) }
    LaunchedEffect(term?.id) {
        val active = term ?: return@LaunchedEffect
        if (!initialScheduleTermSeen) initialScheduleTermSeen = true
        else { repo.refreshWeeks(active.id); repo.refreshTimetable(active.id, null) }
    }
    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "作息与校历",
            onBack = onBack
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CampusSpacing.screenHorizontal)
                .padding(top = CampusSpacing.xs, bottom = CampusSpacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
        ) {
            StaggeredAppear(index = 0) {
                // 与课表、考试页一致的学期胶囊；点按依次切换学校返回的学期。
                CampusFilterChip(
                    text = term?.name ?: "学期待确认",
                    onClick = {
                        val list = terms.data.orEmpty()
                        if (list.isNotEmpty()) selectedTermId = list[(list.indexOf(term) + 1) % list.size].id
                    }
                )
            }

            StaggeredAppear(index = 1) {
                CampusSegmentedControl(
                    options = listOf("作息", "校历"),
                    selectedIndex = if (calendar) 1 else 0,
                    onSelect = { index -> calendar = index == 1 }
                )
            }

            val error = if (calendar) weeks?.error ?: terms.error else table?.error ?: terms.error
            if (error != null) LoadStatePanel(
                false,
                error = error,
                onRetry = {
                    scope.launch {
                        CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination,
                            edu.neu.campus.app.SyncReason.MANUAL)
                    }
                },
                onLogin = onLoginClick
            )
            if (calendar) {
                val weekList = weeks?.data
                when {
                    // 已有错误面板时不再叠一张「尚未获取」的空状态。
                    weekList == null -> if (error == null) {
                        LoadStatePanel(
                            weeks?.phase == QueryPhase.LOADING || terms.phase == QueryPhase.LOADING,
                            emptyMessage = "尚未获取该学期校历"
                        )
                    }
                    weekList.isEmpty() -> Text("学校未返回该学期教学周", fontSize = 14.sp, color = colors.textSecondary)
                    else -> StaggeredAppear(index = 2) {
                        // 教学周放进同一个分组，行间用缩进分隔线，避免每周一张小卡。
                        CampusGroup {
                            weekList.forEachIndexed { index, week ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = CampusSpacing.xs),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                ) {
                                    CampusIconBadge(
                                        icon = MiuixIcons.Regular.Weeks,
                                        tint = colors.brand,
                                        container = colors.brandContainer,
                                        size = WeekIconSize,
                                        iconSize = 20.dp,
                                        cornerRadius = CampusShapes.extraSmall
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            "第 ${week.number} 周${if (week.isCurrent) " · 本周" else ""}",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (week.isCurrent) colors.brand else colors.textPrimary
                                        )
                                        Text(
                                            "${week.startDate ?: "日期未提供"} — ${week.endDate ?: "日期未提供"}",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                    }
                                }
                                if (index < weekList.lastIndex) {
                                    CampusGroupDivider(startIndent = WeekIconSize + CampusSpacing.sm)
                                }
                            }
                        }
                    }
                }
                if (weeks?.isStale == true) {
                    SafeDataTag(text = "校历旧缓存 · 最近同步 ${TimeFormatter.formatDateTime(weeks?.lastSuccessEpochMillis)}")
                }
            } else {
                val data = table?.data
                if (data == null && error == null) {
                    LoadStatePanel(
                        table?.phase == QueryPhase.LOADING || terms.phase == QueryPhase.LOADING,
                        emptyMessage = "尚未获取学校节次信息"
                    )
                }
                data?.campuses?.forEachIndexed { index, campus ->
                    StaggeredAppear(index = index + 2) {
                        CampusSection(title = campus.name ?: "校区 ${campus.id}") {
                            CampusGroup {
                                val sections = data.sectionsByCampus[campus.id].orEmpty()
                                if (sections.isEmpty()) {
                                    Text("学校当前未提供节次信息", fontSize = 14.sp, color = colors.textSecondary)
                                }
                                sections.forEachIndexed { sectionIndex, section ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = CampusSpacing.xs),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                    ) {
                                        CampusIconBadge(
                                            icon = MiuixIcons.Regular.Stopwatch,
                                            tint = colors.timetableForeground,
                                            container = colors.timetableContainer,
                                            size = SectionIconSize,
                                            iconSize = 18.dp,
                                            cornerRadius = CampusShapes.extraSmall
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                section.name ?: "第 ${section.code} 节",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary
                                            )
                                            Text(
                                                if (section.startTime != null && section.endTime != null)
                                                    "${section.startTime}—${section.endTime}" else "学校当前未提供节次时间",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                    if (sectionIndex < sections.lastIndex) {
                                        CampusGroupDivider(startIndent = SectionIconSize + CampusSpacing.sm)
                                    }
                                }
                            }
                        }
                    }
                }
                if (table?.isStale == true) {
                    SafeDataTag(text = "作息时间旧缓存 · 最近同步 ${TimeFormatter.formatDateTime(table?.lastSuccessEpochMillis)}")
                }
            }
        }
    }
}

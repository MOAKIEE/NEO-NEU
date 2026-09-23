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
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Weeks

@Composable
fun ScheduleScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
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
    LaunchedEffect(Unit) { repo.refreshTerms() }
    LaunchedEffect(term?.id) {
        term?.let { repo.refreshWeeks(it.id); repo.refreshTimetable(it.id, null) }
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
            subtitle = "校区节次与校历",
            onBack = onBack
        )
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(CampusSpacing.screenHorizontal),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
            StaggeredAppear(index = 0) {
                Button(onClick = {
                    val list = terms.data.orEmpty()
                    if (list.isNotEmpty()) selectedTermId = list[(list.indexOf(term) + 1) % list.size].id
                }) { Text(term?.name ?: "学期待确认") }
            }

            StaggeredAppear(index = 1) {
                CampusSegmentedControl(
                    options = listOf("作息", "校历"),
                    selectedIndex = if (calendar) 1 else 0,
                    onSelect = { index -> calendar = index == 1 }
                )
            }

            val error = if (calendar) weeks?.error ?: terms.error else table?.error ?: terms.error
            if (error != null) LoadStatePanel(false, error = error, onRetry = {
                scope.launch {
                    repo.refreshTerms()
                    term?.let { if (calendar) repo.refreshWeeks(it.id) else repo.refreshTimetable(it.id, null) }
                }
            })
            if (calendar) {
                if (weeks?.data == null) {
                    LoadStatePanel(weeks?.phase == QueryPhase.LOADING || terms.phase == QueryPhase.LOADING,
                        emptyMessage = "尚未获取该学期校历")
                } else if (weeks.data.orEmpty().isEmpty()) Text("学校未返回该学期教学周")
                else weeks.data.orEmpty().forEachIndexed { index, week ->
                    StaggeredAppear(index = index) {
                        CampusGroup {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                            ) {
                                CampusIconBadge(
                                    icon = MiuixIcons.Regular.Weeks,
                                    tint = colors.brand,
                                    container = colors.brandContainer,
                                    size = 38.dp,
                                    iconSize = 20.dp,
                                    cornerRadius = CampusShapes.extraSmall
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("第 ${week.number} 周${if (week.isCurrent) " · 本周" else ""}",
                                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.brand)
                                    Text("${week.startDate ?: "日期未提供"} — ${week.endDate ?: "日期未提供"}",
                                        fontSize = 12.sp, color = colors.textSecondary)
                                }
                            }
                        }
                    }
                }
                Text("仅显示学校返回的教学周，不推断选退课、考试或放假节点。", fontSize = 12.sp, color = colors.textSecondary)
                SafeDataTag(sourceName = "教务校历", lastSuccessEpochMillis = weeks?.lastSuccessEpochMillis,
                    isStale = weeks?.isStale ?: false)
            } else {
                val data = table?.data
                if (data == null) LoadStatePanel(table?.phase == QueryPhase.LOADING || terms.phase == QueryPhase.LOADING,
                    emptyMessage = "尚未获取学校节次信息")
                data?.campuses?.forEachIndexed { index, campus ->
                    StaggeredAppear(index = index) {
                        CampusSection(title = campus.name ?: "校区 ${campus.id}") {
                            CampusGroup {
                                val sections = data.sectionsByCampus[campus.id].orEmpty()
                                if (sections.isEmpty()) Text("学校当前未提供节次信息")
                                sections.forEach { section ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                    ) {
                                        CampusIconBadge(
                                            icon = MiuixIcons.Regular.Stopwatch,
                                            tint = colors.timetableForeground,
                                            container = colors.timetableContainer,
                                            size = 34.dp,
                                            iconSize = 18.dp,
                                            cornerRadius = CampusShapes.extraSmall
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(section.name ?: "第 ${section.code} 节",
                                                fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                            Text(if (section.startTime != null && section.endTime != null)
                                                "${section.startTime}—${section.endTime}" else "学校当前未提供节次时间",
                                                fontSize = 12.sp, color = colors.textSecondary)
                                        }
                                    }
                                    CampusGroupDivider()
                                }
                            }
                        }
                    }
                }
                SafeDataTag(sourceName = "教务节次", lastSuccessEpochMillis = table?.lastSuccessEpochMillis,
                    isStale = table?.isStale ?: false)
            }
        }
    }
}

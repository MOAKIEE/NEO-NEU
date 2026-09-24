package edu.neu.campus.app.feature.grades

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Favorites
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

enum class GradeSortOrder(val label: String) {
    DEFAULT("默认排序"),
    CREDIT_DESC("学分从高到低"),
    NAME_ASC("课程名称 A-Z")
}

/**
 * 成绩查询页面。
 *
 * 组件约定：
 * - 紧凑标题栏 -> 成绩功能色统计卡 -> 学期选择/排序 -> 本地搜索 -> 成绩列表
 * - 统计卡大数字使用数字滚动动画，并注明官方口径与最近同步时间
 * - 左右结构成绩行：左侧课程名与学分标签，右侧大号成绩与官方绩点
 * - 学期抽屉使用统一的选中行样式并逐项入场
 */
@Composable
fun GradesScreen(
    onBack: () -> Unit,
    onLoginClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val academic = CampusDataProvider.academic
    val colors = CampusTheme.colors

    val termsSnapshot by academic.terms().collectAsState()
    val gradeTermIdsSnapshot by academic.gradeTermIds().collectAsState()
    val gradeSummarySnapshot by academic.gradeSummary().collectAsState()

    val availableTerms = gradeTermIdsSnapshot.data ?: termsSnapshot.data?.map { it.id }.orEmpty()
    var selectedTermId by rememberSaveable { mutableStateOf<String?>(null) }

    val activeTermId = selectedTermId ?: availableTerms.firstOrNull() ?: ""
    val gradesSnapshot = if (activeTermId.isNotBlank()) {
        academic.grades(activeTermId).collectAsState().value
    } else null

    DisposableEffect(selectedTermId, availableTerms) {
        val unregister = CampusDataProvider.sync.registerVisible(AppNavigator.currentTab, AppDestination.Grades) {
            academic.refreshTerms()
            academic.refreshGradeTermIds()
            academic.refreshGradeSummary()
            val termId = selectedTermId ?: academic.gradeTermIds().value.data?.firstOrNull()
            if (!termId.isNullOrBlank()) academic.refreshGrades(termId)
        }
        onDispose { unregister() }
    }

    var initialGradeTermSeen by remember { mutableStateOf(false) }
    LaunchedEffect(activeTermId) {
        if (activeTermId.isNotBlank()) {
            if (!initialGradeTermSeen) initialGradeTermSeen = true
            else academic.refreshGrades(activeTermId)
        }
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortOrder by rememberSaveable { mutableStateOf(GradeSortOrder.DEFAULT) }
    var showTermPicker by remember { mutableStateOf(false) }

    val rawGrades = gradesSnapshot?.data.orEmpty()
    val filteredGrades = remember(rawGrades, searchQuery, sortOrder) {
        var list = if (searchQuery.isBlank()) rawGrades else {
            rawGrades.filter { it.courseName.contains(searchQuery.trim(), ignoreCase = true) }
        }
        list = when (sortOrder) {
            GradeSortOrder.DEFAULT -> list
            GradeSortOrder.CREDIT_DESC -> list.sortedByDescending { it.credit?.toDoubleOrNull() ?: 0.0 }
            GradeSortOrder.NAME_ASC -> list.sortedBy { it.courseName }
        }
        list
    }

    // 学期或绩点失败时成绩列表通常也会失败，此时只在顶部展示一张状态卡。
    val pageError = gradeSummarySnapshot.error
        ?: gradeTermIdsSnapshot.error
        ?: termsSnapshot.error

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "成绩查询",
            subtitle = activeTermId.ifBlank { "官方原始成绩与绩点" },
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md),
            contentPadding = PaddingValues(
                start = CampusSpacing.screenHorizontal,
                end = CampusSpacing.screenHorizontal,
                top = CampusSpacing.xs,
                bottom = CampusSpacing.screenBottom
            )
        ) {
            // 1. 官方总平均学分绩点统计卡
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CampusShapes.extraLarge))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    colors.gradeContainer,
                                    colors.gradeContainer.copy(alpha = if (colors.isDark) 0.55f else 0.7f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = colors.gradeForeground.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(CampusShapes.extraLarge)
                        )
                        .padding(CampusSpacing.lg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "官方总平均学分绩点",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.gradeForeground
                            )
                            val gpaVal = gradeSummarySnapshot.data?.officialGpa
                            val gpaNumber = gpaVal?.toFloatOrNull()
                            if (gpaNumber != null) {
                                AnimatedNumber(
                                    target = gpaNumber,
                                    decimals = 2,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.gradeForeground
                                )
                            } else {
                                // 大号占位符会被误读成图形，这里改用明确文案；
                                // 学校返回非数值绩点时按原样展示。
                                Text(
                                    text = gpaVal ?: "暂无数据",
                                    fontSize = if (gpaVal != null) 24.sp else 17.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.gradeForeground.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            val scopeText = gradeSummarySnapshot.data?.scope ?: "学校返回统计值"
                            Text(
                                text = "$scopeText · 最近同步 ${TimeFormatter.formatTime(gradeSummarySnapshot.lastSuccessEpochMillis)}",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }

                        CampusIconBadge(
                            icon = MiuixIcons.Regular.Favorites,
                            tint = colors.gradeForeground,
                            container = Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.75f),
                            size = 54.dp,
                            iconSize = 28.dp,
                            cornerRadius = CampusShapes.medium
                        )
                    }
                }
            }

            // 绩点、学期与成绩列表常因同一原因（如登录过期）一起失败，只展示一张状态卡，
            // 重试时一并刷新失败的部分，避免页面上叠出多张相同的提示。
            if (pageError != null) item {
                CampusCard {
                    LoadStatePanel(
                        false, error = pageError,
                        onRetry = {
                            coroutineScope.launch {
                                CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination,
                                    edu.neu.campus.app.SyncReason.MANUAL)
                            }
                        },
                        onLogin = onLoginClick
                    )
                }
            }
            if (gradeSummarySnapshot.isStale) item { SafeDataTag(text = "绩点最近同步 ${TimeFormatter.formatDateTime(gradeSummarySnapshot.lastSuccessEpochMillis)} · 显示旧缓存") }

            // 2. 学期选择与排序
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CampusFilterChip(
                        text = if (activeTermId.isBlank()) "暂无可选学期" else activeTermId,
                        onClick = { showTermPicker = true }
                    )
                    CampusFilterChip(
                        text = sortOrder.label,
                        active = true,
                        onClick = {
                            val all = GradeSortOrder.entries
                            val nextIndex = (all.indexOf(sortOrder) + 1) % all.size
                            sortOrder = all[nextIndex]
                        }
                    )
                }
            }

            // 3. 本地搜索输入框
            item {
                CampusSearchField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "搜索课程名称"
                )
            }

            // 4. 加载与异常状态
            if (pageError == null && gradesSnapshot != null &&
                (gradesSnapshot.phase == QueryPhase.LOADING || gradesSnapshot.phase == QueryPhase.FAILED)
            ) {
                item {
                    CampusCard {
                        LoadStatePanel(
                            isLoading = gradesSnapshot.phase == QueryPhase.LOADING && gradesSnapshot.data == null,
                            error = gradesSnapshot.error,
                            onRetry = { coroutineScope.launch { CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, edu.neu.campus.app.SyncReason.MANUAL) } },
                            onLogin = onLoginClick
                        )
                    }
                }
            }

            // 5. 成绩列表标题
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "课程成绩 (${filteredGrades.size} 门)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    SafeDataTag(
                        sourceName = "教务系统",
                        lastSuccessEpochMillis = gradesSnapshot?.lastSuccessEpochMillis,
                        isStale = gradesSnapshot?.isStale ?: false
                    )
                }
            }

            if (filteredGrades.isEmpty() && gradesSnapshot?.phase == QueryPhase.READY) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CampusSpacing.xxl),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "未找到匹配的课程成绩" else "该学期暂无成绩公布记录",
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            } else if (filteredGrades.isNotEmpty()) {
                item {
                    CampusGroup {
                        filteredGrades.forEachIndexed { idx, g ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .tapScale(
                                        onClick = {
                                            AppNavigator.navigateTo(
                                                AppDestination.GradeDetail(activeTermId, g.sourceId)
                                            )
                                        },
                                        pressedScale = 0.985f
                                    )
                                    .padding(vertical = CampusSpacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：课程名与学分/性质标签
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = g.courseName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!g.credit.isNullOrBlank()) {
                                            CampusPill(text = "${g.credit} 学分")
                                        }
                                        CampusPill(text = g.retakeDescription ?: "初修")
                                        if (!g.passDescription.isNullOrBlank()) {
                                            CampusPill(text = g.passDescription.orEmpty())
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(CampusSpacing.sm))

                                // 右侧：原始成绩与官方绩点
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = g.rawScore ?: "—",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (g.rawScore != null) colors.gradeForeground else colors.textTertiary
                                    )
                                    Text(
                                        text = if (!g.officialGradePoint.isNullOrBlank()) {
                                            "绩点 ${g.officialGradePoint}"
                                        } else {
                                            "官方绩点待公布"
                                        },
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                            if (idx < filteredGrades.lastIndex) {
                                CampusGroupDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    // 学期选择抽屉
    if (showTermPicker) {
        OverlayBottomSheet(
            show = true,
            title = "选择成绩学期",
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
                if (availableTerms.isEmpty()) item { CampusEmptyHint(text = "暂无可选学期") }
                itemsIndexed(availableTerms) { index, termId ->
                    val isSelected = termId == activeTermId
                    StaggeredAppear(index = index, key = availableTerms.size) {
                        CampusSelectionRow(
                            title = termId,
                            selected = isSelected,
                            onClick = {
                                selectedTermId = termId
                                showTermPicker = false
                            }
                        )
                    }
                }
            }
        }
    }
}

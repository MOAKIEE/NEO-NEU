package edu.neu.campus.app.feature.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.contract.Exam
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.contract.Term
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusFilterChip
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.CampusSegmentedControl
import edu.neu.campus.ui.components.CampusSelectionRow
import edu.neu.campus.ui.components.CampusSheetCloseAction
import edu.neu.campus.ui.components.CampusEmptyHint
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.TimeFormatter
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.Location
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

enum class ExamTab {
    ARRANGED, UNARRANGED
}

@Composable
fun ExamsScreen(
    onBack: () -> Unit,
    onLoginClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = CampusTheme.colors
    val academic = CampusDataProvider.academic

    val termsSnapshot by academic.terms().collectAsState()
    val terms = termsSnapshot.data.orEmpty()
    var selectedTermId by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedTerm = terms.firstOrNull { it.id == selectedTermId }

    LaunchedEffect(terms) {
        if (selectedTerm == null && terms.isNotEmpty()) {
            selectedTermId = (terms.firstOrNull { it.isCurrent } ?: terms.first()).id
        }
    }

    val currentTerm = selectedTerm
    val examsSnapshot = if (currentTerm != null) {
        academic.exams(currentTerm.id).collectAsState().value
    } else null

    DisposableEffect(currentTerm?.id) {
        val unregister = CampusDataProvider.sync.registerVisible(AppNavigator.currentTab, AppDestination.Exams) {
            academic.refreshTerms()
            val termId = currentTerm?.id ?: academic.terms().value.data?.firstOrNull { it.isCurrent }?.id
            if (termId != null) academic.refreshExams(termId)
        }
        onDispose { unregister() }
    }

    var initialExamTermSeen by remember { mutableStateOf(false) }
    LaunchedEffect(currentTerm?.id) {
        val termId = currentTerm?.id ?: return@LaunchedEffect
        if (!initialExamTermSeen) initialExamTermSeen = true
        else academic.refreshExams(termId)
    }

    var selectedTab by rememberSaveable { mutableStateOf(ExamTab.ARRANGED) }
    var showTermPicker by remember { mutableStateOf(false) }

    val rawExams = examsSnapshot?.data.orEmpty()
    val arrangedExams = remember(rawExams) { rawExams.filter { it.arranged } }
    val unarrangedExams = remember(rawExams) { rawExams.filter { !it.arranged } }

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "考试安排",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = CampusSpacing.screenHorizontal,
                end = CampusSpacing.screenHorizontal,
                top = CampusSpacing.xs,
                bottom = CampusSpacing.screenBottom
            ),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
        ) {
            // 1. 学期选择与更新状态
            item {
                StaggeredAppear(index = 0) {
                    CampusFilterChip(
                        text = currentTerm?.name ?: "选择学期",
                        onClick = { showTermPicker = true }
                    )
                }
            }
            if (examsSnapshot?.isStale == true) item {
                SafeDataTag(text = "考试安排旧缓存 · 最近同步 ${TimeFormatter.formatDateTime(examsSnapshot?.lastSuccessEpochMillis)}")
            }

            // 2. 分段切换药丸：【已安排 (N)】 / 【未安排 (N)】
            item {
                val countArranged = if (examsSnapshot?.phase == QueryPhase.READY) " (${arrangedExams.size})" else ""
                val countUnarranged = if (examsSnapshot?.phase == QueryPhase.READY) " (${unarrangedExams.size})" else ""

                StaggeredAppear(index = 1) {
                    CampusSegmentedControl(
                        options = listOf("已安排$countArranged", "未安排$countUnarranged"),
                        selectedIndex = if (selectedTab == ExamTab.ARRANGED) 0 else 1,
                        onSelect = { index ->
                            selectedTab = if (index == 0) ExamTab.ARRANGED else ExamTab.UNARRANGED
                        }
                    )
                }
            }

            if (termsSnapshot.error != null) item {
                LoadStatePanel(false, error = termsSnapshot.error,
                    onRetry = { coroutineScope.launch { CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, edu.neu.campus.app.SyncReason.MANUAL) } }, onLogin = onLoginClick)
            }
            // 3. 错误与加载处理
            // 学期已失败时考试列表必然跟着失败，只保留上面那张状态卡。
            if (termsSnapshot.error == null && examsSnapshot != null &&
                (examsSnapshot.phase == QueryPhase.LOADING || examsSnapshot.phase == QueryPhase.FAILED)
            ) {
                item {
                    LoadStatePanel(
                        isLoading = examsSnapshot.phase == QueryPhase.LOADING && examsSnapshot.data == null,
                        error = examsSnapshot.error,
                        onRetry = {
                            coroutineScope.launch { CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination, edu.neu.campus.app.SyncReason.MANUAL) }
                        },
                        onLogin = onLoginClick
                    )
                }
            }

            // 4. 考试内容列表
            when (selectedTab) {
                ExamTab.ARRANGED -> {
                    if (arrangedExams.isEmpty() && examsSnapshot?.phase == QueryPhase.READY) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = CampusSpacing.xxl),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "该学期暂无已安排考试",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    } else {
                        itemsIndexed(arrangedExams) { index, exam ->
                            StaggeredAppear(
                                index = index + 2,
                                modifier = Modifier.animateItem()
                            ) {
                                CampusCard(
                                    onClick = {
                                        AppNavigator.navigateTo(AppDestination.ExamDetail(currentTerm!!.id, exam))
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = exam.courseName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (!exam.seat.isNullOrBlank()) {
                                            CampusPill(
                                                text = "${exam.seat} 座",
                                                contentColor = colors.examForeground,
                                                containerColor = colors.examContainer,
                                                modifier = Modifier.padding(start = CampusSpacing.xs)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(CampusSpacing.sm))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Regular.Months,
                                            contentDescription = null,
                                            tint = colors.textTertiary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = exam.timeDescription ?: "考试时间待定",
                                            fontSize = 13.sp,
                                            color = colors.textSecondary,
                                            modifier = Modifier.padding(start = CampusSpacing.xs - 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(CampusSpacing.xs - 2.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = MiuixIcons.Regular.Location,
                                            contentDescription = null,
                                            tint = colors.textTertiary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = exam.place ?: "考场地点尚未公布",
                                            fontSize = 13.sp,
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(start = CampusSpacing.xs - 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ExamTab.UNARRANGED -> {
                    if (unarrangedExams.isEmpty() && examsSnapshot?.phase == QueryPhase.READY) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = CampusSpacing.xxl),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无未安排考试记录",
                                    fontSize = 14.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    } else {
                        itemsIndexed(unarrangedExams) { index, exam ->
                            StaggeredAppear(
                                index = index + 2,
                                modifier = Modifier.animateItem()
                            ) {
                                CampusCard(
                                    onClick = {
                                        AppNavigator.navigateTo(AppDestination.ExamDetail(currentTerm!!.id, exam))
                                    },
                                    contentPadding = PaddingValues(CampusSpacing.sm + 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                    ) {
                                        CampusIconBadge(
                                            icon = MiuixIcons.Regular.Alarm,
                                            tint = colors.examForeground,
                                            container = colors.examContainer,
                                            size = 40.dp,
                                            iconSize = 20.dp,
                                            cornerRadius = CampusShapes.extraSmall
                                        )

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(CampusSpacing.xxs)
                                        ) {
                                            Text(
                                                text = exam.courseName,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = exam.status ?: "随堂考查或尚未统一安排考场",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        CampusPill(text = "未排考")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 学期选择弹层
    if (showTermPicker) {
        OverlayBottomSheet(
            show = true,
            title = "选择考试学期",
            onDismissRequest = { showTermPicker = false },
            startAction = { CampusSheetCloseAction(onClick = { showTermPicker = false }) }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                contentPadding = PaddingValues(horizontal = CampusSpacing.sheetHorizontal, vertical = CampusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
            ) {
                if (terms.isEmpty()) item { CampusEmptyHint(text = "暂无可选学期") }
                itemsIndexed(terms) { index, t ->
                    val isSelected = t.id == currentTerm?.id
                    StaggeredAppear(
                        index = index,
                        key = terms.size,
                        modifier = Modifier.animateItem()
                    ) {
                        CampusSelectionRow(
                            title = t.name + if (t.isCurrent) " (当前)" else "",
                            selected = isSelected,
                            onClick = {
                                selectedTermId = t.id
                                showTermPicker = false
                            }
                        )
                    }
                }
            }
        }
    }
}

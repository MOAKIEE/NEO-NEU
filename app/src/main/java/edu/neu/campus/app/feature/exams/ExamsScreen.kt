package edu.neu.campus.app.feature.exams

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
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class ExamTab {
    ARRANGED, UNARRANGED
}

@Composable
fun ExamsScreen(
    onBack: () -> Unit,
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
    val examsSnapshot = if (currentTerm != null) {
        academic.exams(currentTerm.id).collectAsState().value
    } else null

    LaunchedEffect(currentTerm?.id) {
        val termId = currentTerm?.id ?: return@LaunchedEffect
        academic.refreshExams(termId)
    }

    var selectedTab by remember { mutableStateOf(ExamTab.ARRANGED) }
    var showTermPicker by remember { mutableStateOf(false) }

    val rawExams = examsSnapshot?.data.orEmpty()
    val arrangedExams = remember(rawExams) { rawExams.filter { it.arranged } }
    val unarrangedExams = remember(rawExams) { rawExams.filter { !it.arranged } }

    Column(modifier = Modifier.fillMaxSize()) {
        edu.neu.campus.ui.components.CampusTopBar(
            title = "考试安排",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 学期选择栏
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer)
                            .clickable { showTermPicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentTerm?.name ?: "选择学期",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(text = " ▾", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                    }

                    SafeDataTag(
                        sourceName = "教务系统",
                        lastSuccessEpochMillis = examsSnapshot?.lastSuccessEpochMillis,
                        isStale = examsSnapshot?.isStale ?: false
                    )
                }
            }

            // 2. 分段切换：【已安排】 / 【未安排】
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MiuixTheme.colorScheme.surfaceContainer)
                        .padding(4.dp)
                ) {
                    val countArranged = if (examsSnapshot?.phase == QueryPhase.READY) " (${arrangedExams.size})" else ""
                    val countUnarranged = if (examsSnapshot?.phase == QueryPhase.READY) " (${unarrangedExams.size})" else ""

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == ExamTab.ARRANGED) MiuixTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedTab = ExamTab.ARRANGED }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "已安排$countArranged",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == ExamTab.ARRANGED) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == ExamTab.ARRANGED) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == ExamTab.UNARRANGED) MiuixTheme.colorScheme.surface else Color.Transparent)
                            .clickable { selectedTab = ExamTab.UNARRANGED }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未安排$countUnarranged",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == ExamTab.UNARRANGED) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == ExamTab.UNARRANGED) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // 3. 错误与加载处理
            if (examsSnapshot != null && (examsSnapshot.phase == QueryPhase.LOADING || examsSnapshot.phase == QueryPhase.FAILED)) {
                item {
                    LoadStatePanel(
                        isLoading = examsSnapshot.phase == QueryPhase.LOADING && examsSnapshot.data == null,
                        error = examsSnapshot.error,
                        onRetry = {
                            currentTerm?.let {
                                coroutineScope.launch { academic.refreshExams(it.id) }
                            }
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
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "该学期暂无已安排考试",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "学校尚未公布具体排考，请留意教务通知或查看“未安排”选项",
                                        fontSize = 13.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(arrangedExams) { exam ->
                            QueryCard(
                                title = exam.courseName,
                                subtitle = exam.timeDescription ?: "考试时间待定",
                                onClick = {
                                    AppNavigator.navigateTo(AppDestination.ExamDetail(exam.courseName))
                                }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "考场：${exam.place ?: "尚未公布"}",
                                        fontSize = 13.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                    if (!exam.seat.isNullOrBlank()) {
                                        Text(
                                            text = "座位：${exam.seat}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MiuixTheme.colorScheme.primary
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
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "暂无未安排考试记录",
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }
                    } else {
                        items(unarrangedExams) { exam ->
                            QueryCard(
                                title = exam.courseName,
                                subtitle = exam.status ?: "随堂考查或尚未统一安排考场",
                                onClick = {
                                    AppNavigator.navigateTo(AppDestination.ExamDetail(exam.courseName))
                                }
                            ) {}
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // 学期选择弹层
    if (showTermPicker) {
        OverlayBottomSheet(
            show = true,
            title = "选择考试学期",
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
}

package edu.neu.campus.app.feature.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import edu.neu.campus.contract.Exam
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.contract.Term
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.LocalCampusColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
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
    val campusColors = LocalCampusColors.current
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

    var selectedTab by rememberSaveable { mutableStateOf(ExamTab.ARRANGED) }
    var showTermPicker by remember { mutableStateOf(false) }

    val rawExams = examsSnapshot?.data.orEmpty()
    val arrangedExams = remember(rawExams) { rawExams.filter { it.arranged } }
    val unarrangedExams = remember(rawExams) { rawExams.filter { !it.arranged } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(campusColors.background)
    ) {
        CampusTopBar(
            title = "考试安排",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 学期选择与更新状态
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
                            .clip(RoundedCornerShape(20.dp))
                            .background(campusColors.surface)
                            .clickable { showTermPicker = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentTerm?.name ?: "选择学期",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = campusColors.textPrimary
                        )
                        Text(
                            text = " ▾",
                            fontSize = 12.sp,
                            color = campusColors.textSecondary
                        )
                    }

                    SafeDataTag(
                        sourceName = "教务系统",
                        lastSuccessEpochMillis = examsSnapshot?.lastSuccessEpochMillis,
                        isStale = examsSnapshot?.isStale ?: false
                    )
                }
            }

            // 2. 分段切换药丸：【已安排 (N)】 / 【未安排 (N)】
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(campusColors.surfaceMuted)
                        .padding(4.dp)
                ) {
                    val countArranged = if (examsSnapshot?.phase == QueryPhase.READY) " (${arrangedExams.size})" else ""
                    val countUnarranged = if (examsSnapshot?.phase == QueryPhase.READY) " (${unarrangedExams.size})" else ""

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == ExamTab.ARRANGED) campusColors.surface else Color.Transparent)
                            .clickable { selectedTab = ExamTab.ARRANGED }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "已安排$countArranged",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == ExamTab.ARRANGED) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == ExamTab.ARRANGED) campusColors.examText else campusColors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTab == ExamTab.UNARRANGED) campusColors.surface else Color.Transparent)
                            .clickable { selectedTab = ExamTab.UNARRANGED }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "未安排$countUnarranged",
                            fontSize = 14.sp,
                            fontWeight = if (selectedTab == ExamTab.UNARRANGED) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == ExamTab.UNARRANGED) campusColors.brand else campusColors.textSecondary
                        )
                    }
                }
            }

            if (termsSnapshot.error != null) item {
                LoadStatePanel(false, error = termsSnapshot.error,
                    onRetry = { coroutineScope.launch { academic.refreshTerms() } }, onLogin = onLoginClick)
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
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = campusColors.textPrimary
                                    )
                                    Text(
                                        text = "当前查询未返回已安排考试，可查看“未安排”选项",
                                        fontSize = 13.sp,
                                        color = campusColors.textSecondary,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(arrangedExams) { exam ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        AppNavigator.navigateTo(AppDestination.ExamDetail(currentTerm!!.id, exam))
                                    },
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = exam.courseName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = campusColors.textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        if (!exam.seat.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(campusColors.examLight)
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "${exam.seat} 座",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = campusColors.examText
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = campusColors.textSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = exam.timeDescription ?: "考试时间待定",
                                            fontSize = 13.sp,
                                            color = campusColors.textSecondary,
                                            modifier = Modifier.padding(start = 6.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = campusColors.textSecondary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = exam.place ?: "考场地点尚未公布",
                                            fontSize = 13.sp,
                                            color = campusColors.textPrimary,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.padding(start = 6.dp)
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
                                    color = campusColors.textSecondary
                                )
                            }
                        }
                    } else {
                        items(unarrangedExams) { exam ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        AppNavigator.navigateTo(AppDestination.ExamDetail(currentTerm!!.id, exam))
                                    },
                                insideMargin = PaddingValues(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = exam.courseName,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = campusColors.textPrimary
                                        )
                                        Text(
                                            text = exam.status ?: "随堂考查或尚未统一安排考场",
                                            fontSize = 12.sp,
                                            color = campusColors.textSecondary,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(campusColors.surfaceMuted)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "未排考",
                                            fontSize = 11.sp,
                                            color = campusColors.textSecondary
                                        )
                                    }
                                }
                            }
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) campusColors.brandContainer else campusColors.surfaceMuted)
                            .clickable {
                                selectedTermId = t.id
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
                            color = if (isSelected) campusColors.brand else campusColors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

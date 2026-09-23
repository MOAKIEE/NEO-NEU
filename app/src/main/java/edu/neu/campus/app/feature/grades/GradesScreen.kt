package edu.neu.campus.app.feature.grades

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
import edu.neu.campus.ui.components.TimeFormatter
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class GradeSortOrder(val label: String) {
    DEFAULT("默认排序"),
    CREDIT_DESC("学分从高到低"),
    NAME_ASC("课程名称 A-Z")
}

@Composable
fun GradesScreen(
    onBack: () -> Unit,
    onLoginClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val academic = CampusDataProvider.academic

    // 观察学期列表与成绩学期
    val termsSnapshot by academic.terms().collectAsState()
    val gradeTermIdsSnapshot by academic.gradeTermIds().collectAsState()
    val gradeSummarySnapshot by academic.gradeSummary().collectAsState()

    val availableTerms = gradeTermIdsSnapshot.data ?: termsSnapshot.data?.map { it.id }.orEmpty()
    var selectedTermId by remember { mutableStateOf<String?>(null) } // null 为全部学期

    LaunchedEffect(Unit) {
        academic.refreshTerms()
        academic.refreshGradeTermIds()
        academic.refreshGradeSummary()
    }

    // 当前选中学期的成绩快照
    val activeTermId = selectedTermId ?: availableTerms.firstOrNull() ?: ""
    val gradesSnapshot = if (activeTermId.isNotBlank()) {
        academic.grades(activeTermId).collectAsState().value
    } else null

    LaunchedEffect(activeTermId) {
        if (activeTermId.isNotBlank()) {
            academic.refreshGrades(activeTermId)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(GradeSortOrder.DEFAULT) }
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

    Column(modifier = Modifier.fillMaxSize()) {
        edu.neu.campus.ui.components.CampusTopBar(
            title = "成绩查询",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 独立统计卡：“官方总平均学分绩点”
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    insideMargin = PaddingValues(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "官方总平均学分绩点",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                        val gpaVal = gradeSummarySnapshot.data?.officialGpa ?: "—.—"
                        Text(
                            text = gpaVal,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        val scopeText = gradeSummarySnapshot.data?.scope ?: "学校统计口径 · 全学程"
                        Text(
                            text = scopeText,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                        Text(
                            text = "更新于 ${TimeFormatter.formatTime(gradeSummarySnapshot.lastSuccessEpochMillis)}",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // 2. 学期选择与排序
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            text = if (activeTermId.isBlank()) "全部学期" else activeTermId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(text = " ▾", fontSize = 12.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                    }

                    // 排序切换
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer)
                            .clickable {
                                val all = GradeSortOrder.values()
                                val nextIndex = (all.indexOf(sortOrder) + 1) % all.size
                                sortOrder = all[nextIndex]
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${sortOrder.label} ▾",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 3. 课程名搜索框
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "搜索课程名称",
                    useLabelAsPlaceholder = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. 加载与状态提示
            if (gradesSnapshot != null && (gradesSnapshot.phase == QueryPhase.LOADING || gradesSnapshot.phase == QueryPhase.FAILED)) {
                item {
                    LoadStatePanel(
                        isLoading = gradesSnapshot.phase == QueryPhase.LOADING && gradesSnapshot.data == null,
                        error = gradesSnapshot.error,
                        onRetry = {
                            coroutineScope.launch { academic.refreshGrades(activeTermId) }
                        },
                        onLogin = onLoginClick
                    )
                }
            }

            // 5. 成绩列表
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "课程成绩 (${filteredGrades.size} 门)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
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
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "未找到匹配的课程成绩" else "该学期暂无成绩公布记录",
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }
            } else {
                items(filteredGrades) { g ->
                    QueryCard(
                        onClick = {
                            AppNavigator.navigateTo(AppDestination.GradeDetail(activeTermId, g.sourceId))
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = g.courseName,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                val creditStr = if (!g.credit.isNullOrBlank()) "${g.credit} 学分" else ""
                                val gpStr = if (!g.officialGradePoint.isNullOrBlank()) "绩点 ${g.officialGradePoint}" else "绩点以学校公布为准"
                                Text(
                                    text = "$creditStr · $gpStr",
                                    fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Row(
                                    modifier = Modifier.padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val retake = g.retakeDescription ?: "初修"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = retake, fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                                    }
                                    if (!g.passDescription.isNullOrBlank()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MiuixTheme.colorScheme.surfaceContainerHigh)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = g.passDescription.orEmpty(), fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                                        }
                                    }
                                }
                            }

                            // 原始成绩大字右对齐
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = g.rawScore ?: "—",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.primary
                                )
                                Text(text = "查看详情 ›", fontSize = 11.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    // 学期选择弹层
    if (showTermPicker) {
        OverlayBottomSheet(
            show = true,
            title = "选择成绩学期",
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
                items(availableTerms) { tId ->
                    val isSelected = tId == activeTermId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MiuixTheme.colorScheme.primary.copy(alpha = 0.12f) else MiuixTheme.colorScheme.surfaceContainer)
                            .clickable {
                                selectedTermId = tId
                                showTermPicker = false
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tId,
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

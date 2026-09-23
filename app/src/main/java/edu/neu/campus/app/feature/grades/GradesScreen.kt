package edu.neu.campus.app.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

enum class GradeSortOrder(val label: String) {
    DEFAULT("默认排序"),
    CREDIT_DESC("学分从高到低"),
    NAME_ASC("课程名称 A-Z")
}

/**
 * 成绩查询页面。
 * 组件约定：
 * - 紧凑标题栏 -> 浅紫统计卡 -> 学期选择/排序 -> 本地搜索 -> 成绩列表
 * - 统计卡大数字 32-38sp，官方口径说明与最近同步时间
 * - 左右结构成绩行：左侧课程名与学分，右侧 24sp 成绩与官方绩点
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

    LaunchedEffect(Unit) {
        academic.refreshTerms()
        academic.refreshGradeTermIds()
        academic.refreshGradeSummary()
    }

    val activeTermId = selectedTermId ?: availableTerms.firstOrNull() ?: ""
    val gradesSnapshot = if (activeTermId.isNotBlank()) {
        academic.grades(activeTermId).collectAsState().value
    } else null

    LaunchedEffect(activeTermId) {
        if (activeTermId.isNotBlank()) {
            academic.refreshGrades(activeTermId)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        CampusTopBar(
            title = "成绩查询",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 独立统计卡：浅紫背景，官方总平均学分绩点
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.gradeContainer)
                        .padding(20.dp)
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
                            val gpaVal = gradeSummarySnapshot.data?.officialGpa ?: "—.—"
                            Text(
                                text = gpaVal,
                                fontSize = 36.sp,
                                lineHeight = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.gradeForeground
                            )
                            val scopeText = gradeSummarySnapshot.data?.scope ?: "学校返回统计值"
                            Text(
                                text = "$scopeText · 最近同步 ${TimeFormatter.formatTime(gradeSummarySnapshot.lastSuccessEpochMillis)}",
                                fontSize = 11.sp,
                                color = colors.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.7f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = colors.gradeForeground,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            if (gradeSummarySnapshot.error != null) item {
                LoadStatePanel(false, error = gradeSummarySnapshot.error,
                    onRetry = { coroutineScope.launch { academic.refreshGradeSummary() } }, onLogin = onLoginClick)
            }
            if (gradeSummarySnapshot.isStale) item { SafeDataTag(text = "绩点为上次同步数据") }
            if (termsSnapshot.error != null || gradeTermIdsSnapshot.error != null) item {
                LoadStatePanel(false, error = gradeTermIdsSnapshot.error ?: termsSnapshot.error,
                    onRetry = { coroutineScope.launch { academic.refreshTerms(); academic.refreshGradeTermIds() } }, onLogin = onLoginClick)
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
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .clickable { showTermPicker = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeTermId.isBlank()) "暂无可选学期" else activeTermId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        Text(text = " ▾", fontSize = 12.sp, color = colors.textSecondary)
                    }

                    // 排序方式
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .clickable {
                                val all = GradeSortOrder.values()
                                val nextIndex = (all.indexOf(sortOrder) + 1) % all.size
                                sortOrder = all[nextIndex]
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${sortOrder.label} ▾",
                            fontSize = 13.sp,
                            color = colors.brand
                        )
                    }
                }
            }

            // 3. 本地搜索输入框
            item {
                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = "搜索课程名称",
                    useLabelAsPlaceholder = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "搜索",
                            tint = colors.textSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "清除",
                                    tint = colors.textSecondary
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 4. 加载与异常状态
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

            // 5. 成绩列表分组
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
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
                            .padding(40.dp),
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
                                    .clickable {
                                        AppNavigator.navigateTo(AppDestination.GradeDetail(activeTermId, g.sourceId))
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 左侧：课程名与学分/性质
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = g.courseName,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (!g.credit.isNullOrBlank()) {
                                            Text(
                                                text = "${g.credit} 学分",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                        val retake = g.retakeDescription ?: "初修"
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colors.surfaceMuted)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = retake,
                                                fontSize = 10.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                        if (!g.passDescription.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(colors.surfaceMuted)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = g.passDescription.orEmpty(),
                                                    fontSize = 10.sp,
                                                    color = colors.textSecondary
                                                )
                                            }
                                        }
                                    }
                                }

                                // 右侧：原始成绩 24sp，官方绩点 13sp
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = g.rawScore ?: "—",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                    val gpText = if (!g.officialGradePoint.isNullOrBlank()) "绩点 ${g.officialGradePoint}" else "官方绩点待公布"
                                    Text(
                                        text = gpText,
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

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // 学期选择抽屉
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
                    .heightIn(max = 400.dp)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(availableTerms) { termId ->
                    val isSelected = termId == activeTermId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) colors.brandContainer else colors.surface)
                            .clickable {
                                selectedTermId = termId
                                showTermPicker = false
                            }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = termId,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.brand else colors.textPrimary
                        )
                    }
                }
            }
        }
    }
}

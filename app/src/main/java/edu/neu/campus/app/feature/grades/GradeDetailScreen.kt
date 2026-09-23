package edu.neu.campus.app.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.Grade
import edu.neu.campus.contract.GradeDetail
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.LocalCampusColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GradeDetailScreen(
    termId: String,
    sourceId: String,
    onBack: () -> Unit
) {
    val campusColors = LocalCampusColors.current
    val academic = CampusDataProvider.academic
    val scope = rememberCoroutineScope()

    // 从已有成绩列表中取得基本信息
    val gradesSnapshot by academic.grades(termId).collectAsState()
    val gradeItem = gradesSnapshot.data?.firstOrNull { it.sourceId == sourceId }

    // 观察成绩组成详情
    val detailSnapshot = if (termId.isNotBlank() && sourceId.isNotBlank()) {
        academic.gradeDetail(termId, sourceId).collectAsState().value
    } else null

    LaunchedEffect(termId, sourceId) {
        if (termId.isNotBlank() && sourceId.isNotBlank()) {
            academic.refreshGradeDetail(termId, sourceId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(campusColors.background)
    ) {
        CampusTopBar(
            title = "成绩详情",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部主卡：课程名、总成绩大字与官方绩点
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = gradeItem?.courseName ?: "课程成绩",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = campusColors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "官方绩点 (GPA)",
                                fontSize = 12.sp,
                                color = campusColors.textSecondary
                            )
                            val gp = gradeItem?.officialGradePoint ?: detailSnapshot?.data?.officialGradePoint ?: "以学校为准"
                            Text(
                                text = gp,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = campusColors.textPrimary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "总成绩",
                                fontSize = 12.sp,
                                color = campusColors.textSecondary
                            )
                            val score = gradeItem?.rawScore ?: detailSnapshot?.data?.rawScore ?: "—"
                            Text(
                                text = score,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = campusColors.gradeText,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // 2. 课程元信息分组
            CampusSection(title = "课程信息") {
                CampusGroup {
                    DetailRow(label = "课程编号", value = gradeItem?.courseCode ?: "未提供")
                    CampusGroupDivider()
                    DetailRow(label = "学分", value = gradeItem?.credit?.let { "$it 学分" } ?: "未提供")
                    CampusGroupDivider()
                    DetailRow(label = "考核学期", value = termId.ifBlank { "未指定" })
                    CampusGroupDivider()
                    DetailRow(label = "考核性质", value = gradeItem?.retakeDescription ?: "未提供")
                    CampusGroupDivider()
                    DetailRow(label = "通过说明", value = gradeItem?.passDescription ?: "未提供")
                }
            }

            // 3. 成绩组成项（仅在接口明确提供时展示，不反推计算）
            val components = detailSnapshot?.data?.components.orEmpty()
            if (components.isNotEmpty()) {
                CampusSection(
                    title = "成绩组成项",
                    subtitle = "学校官方公布分项构成"
                ) {
                    CampusGroup {
                        components.forEachIndexed { index, comp ->
                            if (index > 0) CampusGroupDivider()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comp.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = campusColors.textPrimary
                                    )
                                    if (comp.highestInProportion == true) {
                                        Text(
                                            text = "占比最高分项",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = campusColors.brand,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = comp.rawValue ?: "—",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = campusColors.gradeText
                                )
                            }
                        }
                    }
                }
            } else if (detailSnapshot?.phase == QueryPhase.LOADING) {
                LoadStatePanel(isLoading = true)
            }

            if (detailSnapshot?.error != null) {
                LoadStatePanel(isLoading = false, error = detailSnapshot.error,
                    onRetry = { scope.launch { academic.refreshGradeDetail(termId, sourceId) } })
            }
            // 4. 来源与更新时间
            SafeDataTag(
                sourceName = "教务系统",
                lastSuccessEpochMillis = detailSnapshot?.lastSuccessEpochMillis ?: gradesSnapshot.lastSuccessEpochMillis,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val campusColors = LocalCampusColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = campusColors.textSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = campusColors.textPrimary)
    }
}

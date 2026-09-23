package edu.neu.campus.app.feature.exams

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.Exam
import edu.neu.campus.ui.components.QueryCard
import edu.neu.campus.ui.components.SafeDataTag
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ExamDetailScreen(
    courseName: String,
    onBack: () -> Unit
) {
    val academic = CampusDataProvider.academic

    val termsSnapshot by academic.terms().collectAsState()
    val currentTerm = termsSnapshot.data?.firstOrNull { it.isCurrent } ?: termsSnapshot.data?.firstOrNull()

    val examsSnapshot = if (currentTerm != null) {
        academic.exams(currentTerm.id).collectAsState().value
    } else null

    val exam = examsSnapshot?.data?.firstOrNull { it.courseName == courseName }

    Column(modifier = Modifier.fillMaxSize()) {
        edu.neu.campus.ui.components.CampusTopBar(
            title = "考试详情",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 课程卡片
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = exam?.courseName ?: courseName.ifBlank { "考试详情" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (exam?.arranged == true) "已统一排考" else "尚未排考 / 随堂考查",
                        fontSize = 13.sp,
                        color = if (exam?.arranged == true) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 考试安排详情
            QueryCard(title = "考场与时间") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "考试时间", value = exam?.timeDescription ?: "待学校统一公布")
                    DetailRow(label = "考场地点", value = exam?.place ?: "尚未公布")
                    DetailRow(label = "考场座位", value = exam?.seat ?: "未指定")
                    DetailRow(label = "当前状态", value = exam?.status ?: "正常")
                }
            }

            // 来源说明
            SafeDataTag(
                sourceName = "教务系统",
                lastSuccessEpochMillis = examsSnapshot?.lastSuccessEpochMillis,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MiuixTheme.colorScheme.onSurface)
    }
}

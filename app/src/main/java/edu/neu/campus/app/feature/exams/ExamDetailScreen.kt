package edu.neu.campus.app.feature.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.Exam
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.LocalCampusColors
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun ExamDetailScreen(
    courseName: String,
    onBack: () -> Unit
) {
    val campusColors = LocalCampusColors.current
    val academic = CampusDataProvider.academic

    val termsSnapshot by academic.terms().collectAsState()
    val currentTerm = termsSnapshot.data?.firstOrNull { it.isCurrent } ?: termsSnapshot.data?.firstOrNull()

    val examsSnapshot = if (currentTerm != null) {
        academic.exams(currentTerm.id).collectAsState().value
    } else null

    val exam = examsSnapshot?.data?.firstOrNull { it.courseName == courseName }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(campusColors.background)
    ) {
        CampusTopBar(
            title = "考试详情",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部主卡：课程名与状态
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(20.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = exam?.courseName ?: courseName.ifBlank { "考试详情" },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = campusColors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )

                        val isArranged = exam?.arranged == true
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isArranged) campusColors.examLight else campusColors.surfaceMuted)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (isArranged) "已统考排考" else "尚未排考/随堂",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isArranged) campusColors.examText else campusColors.textSecondary
                            )
                        }
                    }

                    if (exam?.arranged == true) {
                        Spacer(modifier = Modifier.height(18.dp))

                        // 核心信息方块网格
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 考场方块
                            Column(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(campusColors.surfaceMuted)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "考场地点",
                                    fontSize = 12.sp,
                                    color = campusColors.textSecondary
                                )
                                Text(
                                    text = exam.place ?: "尚未公布",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = campusColors.textPrimary,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // 座位号方块
                            Column(
                                modifier = Modifier
                                    .weight(0.8f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(campusColors.examLight)
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "座位号",
                                    fontSize = 12.sp,
                                    color = campusColors.examText.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = exam.seat?.let { "$it 座" } ?: "无",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = campusColors.examText,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 2. 考试安排详情分组
            CampusSection(title = "安排信息") {
                CampusGroup {
                    DetailRow(label = "考试时间", value = exam?.timeDescription ?: "待学校统一公布")
                    CampusGroupDivider()
                    DetailRow(label = "考场地点", value = exam?.place ?: "尚未公布")
                    CampusGroupDivider()
                    DetailRow(label = "考场座位", value = exam?.seat ?: "未指定")
                    CampusGroupDivider()
                    DetailRow(label = "考查状态", value = exam?.status ?: "正常")
                }
            }

            // 3. 来源说明
            SafeDataTag(
                sourceName = "教务系统",
                lastSuccessEpochMillis = examsSnapshot?.lastSuccessEpochMillis,
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

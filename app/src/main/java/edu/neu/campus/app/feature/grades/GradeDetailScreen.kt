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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.Grade
import edu.neu.campus.contract.GradeDetail
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.QueryCard
import edu.neu.campus.ui.components.SafeDataTag
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun GradeDetailScreen(
    termId: String,
    sourceId: String,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val academic = CampusDataProvider.academic

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

    Column(modifier = Modifier.fillMaxSize()) {
        edu.neu.campus.ui.components.CampusTopBar(
            title = "成绩详情",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 上方：课程名、原始总成绩与官方课程绩点
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(18.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = gradeItem?.courseName ?: "课程成绩",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "官方绩点",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                            val gp = gradeItem?.officialGradePoint ?: detailSnapshot?.data?.officialGradePoint ?: "以学校为准"
                            Text(
                                text = gp,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "总成绩",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                            val score = gradeItem?.rawScore ?: detailSnapshot?.data?.rawScore ?: "—"
                            Text(
                                text = score,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = MiuixTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. 中间：分组展示学分、课程号、学期、性质、类别、考核方式
            QueryCard(title = "课程元信息") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailRow(label = "课程编号", value = gradeItem?.courseCode ?: "未提供")
                    DetailRow(label = "学分", value = gradeItem?.credit?.let { "$it 学分" } ?: "未提供")
                    DetailRow(label = "考核学期", value = termId.ifBlank { "未指定" })
                    DetailRow(label = "考核性质", value = gradeItem?.retakeDescription ?: "初修")
                    DetailRow(label = "通过说明", value = gradeItem?.passDescription ?: "正常考核")
                }
            }

            // 3. 下方：成绩组成项（仅在接口明确提供时展示，不反推计算）
            val components = detailSnapshot?.data?.components.orEmpty()
            if (components.isNotEmpty()) {
                QueryCard(title = "成绩组成项") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        components.forEach { comp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainer)
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = comp.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MiuixTheme.colorScheme.onSurface
                                    )
                                    if (comp.highestInProportion == true) {
                                        Text(
                                            text = "占比最高分项",
                                            fontSize = 11.sp,
                                            color = MiuixTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Text(
                                    text = comp.rawValue ?: "—",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MiuixTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            } else if (detailSnapshot?.phase == QueryPhase.LOADING) {
                LoadStatePanel(isLoading = true)
            }

            // 4. 来源与更新时间
            SafeDataTag(
                sourceName = "教务系统",
                lastSuccessEpochMillis = detailSnapshot?.lastSuccessEpochMillis ?: gradesSnapshot.lastSuccessEpochMillis,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
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

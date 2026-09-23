package edu.neu.campus.app.feature.grades

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.AnimatedNumber
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusPageEnter
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text

/**
 * 成绩详情：课程基本信息 + 官方绩点与总成绩 + 成绩组成项。
 *
 * 约定：
 * - 顶部主卡使用成绩功能色柔化渐变，总成绩带数字滚动动画
 * - 所有字段来自学校返回，缺失字段显式标注「未提供」，不做推断
 */
@Composable
fun GradeDetailScreen(
    termId: String,
    sourceId: String,
    onBack: () -> Unit
) {
    val colors = CampusTheme.colors
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
            .background(colors.background)
    ) {
        CampusTopBar(
            title = "成绩详情",
            subtitle = "官方成绩与分项构成",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CampusSpacing.md, vertical = CampusSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
        ) {
            CampusPageEnter {
                Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                    // 1. 顶部主卡
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = gradeItem?.courseName ?: "课程成绩",
                                fontSize = 21.sp,
                                lineHeight = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )

                            if (!gradeItem?.courseCode.isNullOrBlank()) {
                                CampusPill(
                                    text = gradeItem?.courseCode.orEmpty(),
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(CampusSpacing.md))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "官方绩点 (GPA)",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                    val gp = gradeItem?.officialGradePoint
                                        ?: detailSnapshot?.data?.officialGradePoint
                                        ?: "以学校为准"
                                    Text(
                                        text = gp,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.gradeForeground,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "总成绩",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                    val scoreText = gradeItem?.rawScore ?: detailSnapshot?.data?.rawScore
                                    val scoreDecimals = if (scoreText?.contains('.') == true) 1 else 0
                                    AnimatedNumber(
                                        target = scoreText?.toFloatOrNull(),
                                        fallback = scoreText ?: "—",
                                        decimals = scoreDecimals,
                                        fontSize = 36.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.gradeForeground
                                    )
                                }
                            }
                        }
                    }

                    // 2. 课程元信息分组
                    StaggeredAppear(index = 0) {
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
                    }

                    // 3. 成绩组成项（仅在接口明确提供时展示，不反推计算）
                    val components = detailSnapshot?.data?.components.orEmpty()
                    if (components.isNotEmpty()) {
                        StaggeredAppear(index = 1) {
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
                                                .padding(vertical = CampusSpacing.sm),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Text(
                                                        text = comp.name,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = colors.textPrimary
                                                    )
                                                    if (comp.highestInProportion == true) {
                                                        CampusPill(
                                                            text = "占比最高",
                                                            contentColor = colors.gradeForeground,
                                                            containerColor = colors.gradeContainer
                                                        )
                                                    }
                                                }
                                                if (!comp.code.isBlank()) {
                                                    Text(
                                                        text = comp.code,
                                                        fontSize = 12.sp,
                                                        color = colors.textSecondary,
                                                        modifier = Modifier.padding(top = 2.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = comp.rawValue ?: "—",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.gradeForeground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else if (detailSnapshot?.phase == QueryPhase.LOADING) {
                        CampusCard { LoadStatePanel(isLoading = true) }
                    }

                    if (detailSnapshot?.error != null) {
                        CampusCard {
                            LoadStatePanel(
                                isLoading = false,
                                error = detailSnapshot.error,
                                onRetry = { scope.launch { academic.refreshGradeDetail(termId, sourceId) } }
                            )
                        }
                    }

                    // 4. 来源与更新时间
                    SafeDataTag(
                        sourceName = "教务系统",
                        lastSuccessEpochMillis = detailSnapshot?.lastSuccessEpochMillis
                            ?: gradesSnapshot.lastSuccessEpochMillis,
                        modifier = Modifier.padding(top = CampusSpacing.xxs)
                    )

                    Spacer(modifier = Modifier.height(CampusSpacing.md))
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    CampusRow(
        title = label,
        trailingText = value,
        trailingColor = CampusTheme.colors.textPrimary
    )
}

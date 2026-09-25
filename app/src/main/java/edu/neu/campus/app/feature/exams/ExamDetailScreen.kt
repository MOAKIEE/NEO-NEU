package edu.neu.campus.app.feature.exams

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.Exam
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusPageEnter
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text

/**
 * 考试详情：课程、考查状态与考场信息。
 *
 * 约定：
 * - 主卡使用考试功能色柔化渐变，考场与座位使用等宽信息块
 * - 未安排考试不展示考场网格，仅保留状态
 */
@Composable
fun ExamDetailScreen(
    termId: String,
    selectedExam: Exam,
    onBack: () -> Unit
) {
    val colors = CampusTheme.colors
    val academic = CampusDataProvider.academic

    val examsSnapshot by academic.exams(termId).collectAsState()
    val exam = examsSnapshot.data?.firstOrNull { it == selectedExam }
    val courseName = selectedExam.courseName

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "考试详情",
            onBack = onBack
        )

        if (exam == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(CampusSpacing.xl),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "该考试记录已不可用，请返回所选学期重新查询。",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CampusSpacing.screenHorizontal)
                .padding(top = CampusSpacing.xs, bottom = CampusSpacing.screenBottom),
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
                                        colors.examContainer,
                                        colors.examContainer.copy(alpha = if (colors.isDark) 0.55f else 0.72f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = colors.examForeground.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(CampusShapes.extraLarge)
                            )
                            .padding(CampusSpacing.lg)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = exam.courseName.ifBlank { courseName.ifBlank { "考试详情" } },
                                    fontSize = 20.sp,
                                    lineHeight = 27.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(CampusSpacing.xs))

                                CampusPill(
                                    text = if (exam.arranged) "已安排" else "未安排",
                                    contentColor = if (exam.arranged) colors.examForeground else colors.textSecondary,
                                    containerColor = if (exam.arranged) {
                                        colors.surface.copy(alpha = 0.75f)
                                    } else {
                                        colors.surface.copy(alpha = 0.6f)
                                    }
                                )
                            }

                            if (exam.arranged) {
                                Spacer(modifier = Modifier.height(CampusSpacing.md))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                ) {
                                    // 考场方块
                                    Column(
                                        modifier = Modifier
                                            .weight(1.2f)
                                            .clip(RoundedCornerShape(CampusShapes.medium))
                                            .background(colors.surface.copy(alpha = 0.72f))
                                            .padding(CampusSpacing.sm + 2.dp)
                                    ) {
                                        Text(
                                            text = "考场地点",
                                            fontSize = 12.sp,
                                            color = colors.textSecondary
                                        )
                                        Text(
                                            text = exam.place ?: "未提供",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }

                                    // 座位号方块
                                    Column(
                                        modifier = Modifier
                                            .weight(0.8f)
                                            .clip(RoundedCornerShape(CampusShapes.medium))
                                            .background(colors.examForeground.copy(alpha = 0.12f))
                                            .padding(CampusSpacing.sm + 2.dp)
                                    ) {
                                        Text(
                                            text = "座位号",
                                            fontSize = 12.sp,
                                            color = colors.examForeground.copy(alpha = 0.85f)
                                        )
                                        Text(
                                            text = exam.seat?.let { "$it 座" } ?: "未提供",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.examForeground,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. 考试安排详情
                    StaggeredAppear(index = 0) {
                        CampusSection(title = "安排信息") {
                            CampusGroup {
                                DetailRow(label = "考试时间", value = exam.timeDescription ?: "未提供")
                                CampusGroupDivider()
                                DetailRow(label = "考场地点", value = exam.place ?: "未提供")
                                CampusGroupDivider()
                                DetailRow(label = "考场座位", value = exam.seat ?: "未指定")
                                CampusGroupDivider()
                                DetailRow(label = "考查状态", value = exam.status ?: "未提供")
                            }
                        }
                    }

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

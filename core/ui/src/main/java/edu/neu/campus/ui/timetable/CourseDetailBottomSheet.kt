package edu.neu.campus.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.ui.components.CampusSheetCloseAction
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet

/**
 * 课程详情抽屉 (CourseDetailBottomSheet)。
 * 组件约定：
 * - 顶部细色条与课块颜色一致
 * - 依次展示时间节次、上课地点与校区、任课教师、学校排课安排说明及其他时段排课
 * - 来源与同步时间严格遵循只读真实数据口径
 */
@Composable
fun CourseDetailBottomSheet(
    course: CourseOccurrence?,
    otherOccurrences: List<CourseOccurrence> = emptyList(),
    campusName: String? = null,
    lastUpdatedTime: Long? = null,
    onDismiss: () -> Unit
) {
    val colors = CampusTheme.colors

    OverlayBottomSheet(
        show = course != null,
        title = course?.title ?: "课程详情",
        onDismissRequest = onDismiss,
        startAction = { CampusSheetCloseAction(onClick = onDismiss) }
    ) {
        if (course != null) {
            val (courseAccentColor, _) = colors.courseColor(course.title)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CampusSpacing.sheetHorizontal, vertical = CampusSpacing.xs)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 顶部细色条（与课块同色）
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(courseAccentColor)
                )

                // 1. 时间与节次
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceMuted)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "时间安排",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    val timeStr = if (!course.beginTime.isNullOrBlank() && !course.endTime.isNullOrBlank()) {
                        "${course.beginTime} — ${course.endTime}"
                    } else {
                        "起止时间待学校排课公布"
                    }
                    Text(
                        text = "星期${dayOfWeekText(course.dayOfWeek)} · 第 ${course.beginSection}—${course.endSection} 节 ($timeStr)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                    if (!course.scheduleDescription.isNullOrBlank()) {
                        Text(
                            text = "学校安排说明：${course.scheduleDescription}",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = colors.brand
                        )
                    }
                }

                // 2. 校区与地点
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceMuted)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "上课地点",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    val campusStr = campusName ?: "校区 ${course.campusId}"
                    val placeStr = course.place ?: "教室待定"
                    Text(
                        text = "$campusStr · $placeStr",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                }

                // 3. 任课教师与课程记录标识
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surfaceMuted)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "任课教师及信息",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "教师：${course.teacher ?: "暂未列明"}",
                        fontSize = 15.sp,
                        color = colors.textPrimary
                    )
                    if (!course.sourceId.isNullOrBlank()) {
                        Text(
                            text = "课程记录标识：${course.sourceId}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                // 4. 本课程其他安排
                if (otherOccurrences.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.surfaceMuted)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "本课程其他上课安排",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                        otherOccurrences.forEach { other ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "周${dayOfWeekText(other.dayOfWeek)} 第${other.beginSection}-${other.endSection}节 · ${other.place ?: "待定"}",
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = other.beginTime ?: "",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // 5. 来源与同步时间
                SafeDataTag(
                    sourceName = "教务系统",
                    lastSuccessEpochMillis = lastUpdatedTime,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }
        }
    }
}

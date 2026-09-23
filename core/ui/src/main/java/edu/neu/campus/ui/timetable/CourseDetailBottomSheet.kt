package edu.neu.campus.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.ui.components.SafeDataTag
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun CourseDetailBottomSheet(
    course: CourseOccurrence?,
    otherOccurrences: List<CourseOccurrence> = emptyList(),
    campusName: String? = null,
    lastUpdatedTime: Long? = null,
    onDismiss: () -> Unit
) {
    OverlayBottomSheet(
        show = course != null,
        title = course?.title ?: "课程详情",
        onDismissRequest = onDismiss,
        endAction = {
            Button(onClick = onDismiss) {
                Text("关闭")
            }
        }
    ) {
        if (course != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 时间与节次
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "时间安排",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
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
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    if (!course.scheduleDescription.isNullOrBlank()) {
                        Text(
                            text = course.scheduleDescription.orEmpty(),
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }

                // 2. 校区与地点
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "上课地点",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    val campusStr = campusName ?: "校区 ${course.campusId}"
                    val placeStr = course.place ?: "教室待定"
                    Text(
                        text = "$campusStr · $placeStr",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                // 3. 教师与元信息
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "任课教师及信息",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "教师：${course.teacher ?: "暂未列明"}",
                        fontSize = 15.sp,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    if (!course.sourceId.isNullOrBlank()) {
                        Text(
                            text = "课程记录标识：${course.sourceId}",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }

                // 4. 其他上课安排
                if (otherOccurrences.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MiuixTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "本课程其他上课安排",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
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
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = other.beginTime ?: "",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }
                    }
                }

                // 5. 来源与更新时间
                SafeDataTag(
                    sourceName = "教务系统",
                    lastSuccessEpochMillis = lastUpdatedTime,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }
        }
    }
}

fun dayOfWeekText(day: Int): String = when (day) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> day.toString()
}

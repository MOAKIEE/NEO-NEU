package edu.neu.campus.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text

private val TimeColumnWidth = 44.dp
private val AccentBarWidth = 3.dp
private val AccentBarHeight = 38.dp

/**
 * 课表列表视图：按教学周真实顺序逐日分组，日期与网格表头共用 [WeekDay]，今日带标记。
 * 大字号或多校区合并显示时替代网格。
 */
@Composable
fun TimetableDayList(
    days: List<WeekDay>,
    courses: List<CourseOccurrence>,
    today: String?,
    onCourseClick: (CourseOccurrence) -> Unit,
    modifier: Modifier = Modifier,
    campusNameOf: ((String) -> String)? = null,
    emptyMessage: String? = null
) {
    val colors = CampusTheme.colors
    val groups = remember(days, courses) {
        days.mapNotNull { day ->
            courses.filter { it.dayOfWeek == day.dayOfWeek }
                .sortedWith(compareBy({ it.beginSection }, { it.campusId }))
                .takeIf { it.isNotEmpty() }
                ?.let { day to it }
        }
    }

    if (groups.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(CampusSpacing.xxl),
            contentAlignment = Alignment.Center
        ) {
            if (emptyMessage != null) {
                Text(
                    text = emptyMessage,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = CampusSpacing.screenHorizontal,
            end = CampusSpacing.screenHorizontal,
            top = CampusSpacing.xs,
            bottom = CampusSpacing.screenBottom
        ),
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
    ) {
        items(groups, key = { it.first.dayOfWeek }) { (day, dayCourses) ->
            val isToday = day.date != null && day.date == today
            Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
                ) {
                    Text(
                        text = "星期${dayOfWeekText(day.dayOfWeek)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) colors.brand else colors.textPrimary
                    )
                    shortDate(day.date)?.let {
                        Text(text = it, fontSize = 13.sp, color = colors.textTertiary)
                    }
                    if (isToday) {
                        Text(
                            text = "今天",
                            modifier = Modifier
                                .clip(RoundedCornerShape(CampusShapes.pill))
                                .background(colors.brandContainer)
                                .padding(horizontal = CampusSpacing.xs, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.brand
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${dayCourses.size} 节安排",
                        fontSize = 12.sp,
                        color = colors.textTertiary
                    )
                }
                CampusGroup(contentPadding = CampusSpacing.sm) {
                    dayCourses.forEachIndexed { index, course ->
                        CourseListRow(
                            course = course,
                            campusName = campusNameOf?.invoke(course.campusId),
                            onClick = { onCourseClick(course) }
                        )
                        if (index < dayCourses.lastIndex) {
                            CampusGroupDivider(startIndent = TimeColumnWidth + AccentBarWidth + CampusSpacing.sm * 2)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseListRow(
    course: CourseOccurrence,
    campusName: String?,
    onClick: () -> Unit
) {
    val colors = CampusTheme.colors
    val accent = colors.courseAccent(course.colorKey())
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .tapScale(onClick = onClick, pressedScale = 0.985f, clipShape = RoundedCornerShape(CampusShapes.extraSmall))
            .padding(vertical = CampusSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
    ) {
        Column(
            modifier = Modifier.width(TimeColumnWidth),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = course.beginTime?.takeIf { it.isNotBlank() } ?: "第${course.beginSection}节",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 1
            )
            Text(
                text = course.endTime?.takeIf { it.isNotBlank() } ?: "至${course.endSection}节",
                fontSize = 11.sp,
                color = colors.textTertiary,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(width = AccentBarWidth, height = AccentBarHeight)
                .clip(RoundedCornerShape(CampusShapes.pill))
                .background(accent)
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = course.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val place = listOfNotNull(campusName, course.place ?: "教室待定").joinToString(" · ")
            Text(
                text = "第 ${course.beginSection}-${course.endSection} 节 · $place",
                fontSize = 12.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!course.scheduleDescription.isNullOrBlank()) {
                Text(
                    text = course.scheduleDescription!!,
                    fontSize = 11.sp,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

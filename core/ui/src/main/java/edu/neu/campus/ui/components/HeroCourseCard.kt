package edu.neu.campus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.*
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun HeroCourseCard(
    courses: List<CourseOccurrence>, nowTimeStr: String, isLoading: Boolean,
    error: QueryError?, isStale: Boolean = false, hasConfirmedTerm: Boolean = true,
    hasData: Boolean = false,
    onClickCourse: (CourseOccurrence) -> Unit,
    onConflictClick: (List<CourseOccurrence>) -> Unit,
    onGotoTimetable: () -> Unit, onRetry: () -> Unit, modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val focus = courseFocus(courses, nowTimeStr, !isStale)
    Box(modifier.fillMaxWidth().defaultMinSize(minHeight = 168.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(colors.heroSurface,
            if (colors.isDark) colors.heroSurface else colors.brand)))) {
        Canvas(Modifier.size(120.dp).align(Alignment.TopEnd)) {
            val path = Path().apply {
                moveTo(size.width * 0.4f, size.height * 0.85f)
                lineTo(size.width * 0.4f, size.height * 0.15f)
                lineTo(size.width * 0.85f, size.height * 0.85f)
                lineTo(size.width * 0.85f, size.height * 0.15f)
            }
            drawPath(path, colors.onHero.copy(alpha = 0.08f), style = Stroke(2.dp.toPx()))
        }
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            !hasData && error != null -> {
                Text("课程暂不可用", color = colors.onHero, fontSize = 22.sp)
                Text(error.message, color = colors.onHero)
                Button(onClick = onRetry) { Text("重试") }
            }
            isLoading && !hasData -> Text("正在同步今日课程…", color = colors.onHero)
            !hasConfirmedTerm -> {
                Text("教学周待确认", color = colors.onHero, fontSize = 22.sp)
                Button(onClick = onGotoTimetable) { Text("查看课表") }
            }
            !hasData -> {
                Text("尚未获取今日课程", color = colors.onHero, fontSize = 22.sp)
                Button(onClick = onRetry) { Text("同步课程") }
            }
            else -> {
                Text(if (courses.isEmpty()) "今天没有已安排课程" else focus.label,
                    color = colors.onHero, fontSize = 14.sp)
                if (focus.courses.isNotEmpty()) {
                    val course = focus.courses.first()
                    Text(if (focus.conflict) "同时有 ${focus.courses.size} 项安排" else course.title,
                        fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, color = colors.onHero)
                    Text(if (hasCourseTime(course)) "${course.beginTime}—${course.endTime} · 第 ${course.beginSection}—${course.endSection} 节"
                        else "第 ${course.beginSection}—${course.endSection} 节 · 时间未提供", color = colors.onHero)
                    Text(course.place ?: "地点未提供", color = colors.onHero)
                    Button(onClick = {
                        if (focus.courses.size > 1) onConflictClick(focus.courses) else onClickCourse(course)
                    }) { Text(if (focus.courses.size > 1) "查看课程列表" else "查看课程详情") }
                } else {
                    Button(onClick = onGotoTimetable) { Text("查看本周课表") }
                }
                if (isStale) Text("上次同步课程，可能已变化", color = colors.onHero)
                if (error != null) {
                    Text(error.message, color = colors.onHero)
                    Button(onClick = onRetry) { Text("重试") }
                }
            }
        }
    }
}
}

package edu.neu.campus.ui.timetable

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.CourseOccurrence
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text
import kotlin.math.abs

private val AxisWidth = 34.dp
private val HeaderHeight = 54.dp
private val SectionHeight = 62.dp
private val BlockGap = 2.dp
private val BlockPaddingH = 4.dp
private val BlockPaddingV = 5.dp
private val SwipeThreshold = 72.dp

/**
 * 课表网格 (TimetableGrid)。
 *
 * 布局约定：
 * - 星期列按可用宽度均分，表头与表体使用同一列宽，保证星期、日期与课块逐列对齐
 * - 列顺序与日期取自 [TimetableLayout.days]，由教学周真实日期推算，兼容以周日为一周起点的学校配置
 * - 网格线与今日列底色在一次 `drawBehind` 中绘制；课块只做定位，不做逐块入场动画，切换页面不再掉帧
 * - 左右滑动表体切换教学周（[onSwipeWeek] 传入 -1 / +1），纵向滚动与标题栏折叠互不干扰
 */
@Composable
fun TimetableGrid(
    layout: TimetableLayout,
    today: String?,
    onCourseClick: (CourseOccurrence) -> Unit,
    onConflictClick: (List<CourseOccurrence>) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    overlayMessage: String? = null,
    onSwipeWeek: ((Int) -> Unit)? = null
) {
    val colors = CampusTheme.colors

    if (layout.sections.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = overlayMessage ?: "暂无可显示的节次信息",
                modifier = Modifier.padding(CampusSpacing.screenHorizontal),
                color = colors.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val todayColumn = layout.days.indexOfFirst { it.date != null && it.date == today }
    // 回调随重组更新，但手势检测器不能因此重启，否则滑动到一半会被打断。
    val latestOnSwipe by rememberUpdatedState(onSwipeWeek)
    val swipeModifier = if (onSwipeWeek != null) {
        Modifier.pointerInput(Unit) {
            var total = 0f
            val threshold = SwipeThreshold.toPx()
            detectHorizontalDragGestures(
                onDragStart = { total = 0f },
                onDragEnd = { if (abs(total) > threshold) latestOnSwipe?.invoke(if (total < 0) 1 else -1) },
                onHorizontalDrag = { change, amount ->
                    total += amount
                    change.consume()
                }
            )
        }
    } else Modifier

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val dayWidth = (maxWidth - AxisWidth) / layout.days.size

        Column(modifier = Modifier.fillMaxSize()) {
            GridHeader(layout = layout, todayColumn = todayColumn, dayWidth = dayWidth)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .then(swipeModifier)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    val lineColor = colors.divider
                    val todayTint = colors.brandContainer.copy(alpha = if (colors.isDark) 0.35f else 0.45f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(SectionHeight * layout.sections.size)
                            .drawBehind {
                                val axis = AxisWidth.toPx()
                                val column = dayWidth.toPx()
                                val row = SectionHeight.toPx()
                                if (todayColumn >= 0) {
                                    drawRect(
                                        color = todayTint,
                                        topLeft = Offset(axis + column * todayColumn, 0f),
                                        size = Size(column, size.height)
                                    )
                                }
                                val stroke = 0.5.dp.toPx()
                                for (i in 1 until layout.sections.size) {
                                    drawLine(lineColor, Offset(axis, row * i), Offset(size.width, row * i), stroke)
                                }
                            }
                    ) {
                        SectionAxis(layout.sections)
                        layout.blocks.forEach { block ->
                            key(block.column, block.startRow) {
                                CourseBlockCell(
                                    block = block,
                                    onClick = {
                                        if (block.isConflict) onConflictClick(block.courses)
                                        else onCourseClick(block.courses.first())
                                    },
                                    modifier = Modifier
                                        .offset(x = AxisWidth + dayWidth * block.column, y = SectionHeight * block.startRow)
                                        .size(width = dayWidth, height = SectionHeight * block.rowSpan)
                                        .padding(BlockGap)
                                )
                            }
                        }
                    }
                }

                if (overlayMessage != null) {
                    Text(
                        text = overlayMessage,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(start = AxisWidth)
                            .clip(RoundedCornerShape(CampusShapes.pill))
                            .background(colors.surfaceMuted)
                            .padding(horizontal = CampusSpacing.md, vertical = CampusSpacing.xs),
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GridHeader(layout: TimetableLayout, todayColumn: Int, dayWidth: Dp) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
            .drawBehind {
                drawLine(colors.divider, Offset(0f, size.height), Offset(size.width, size.height), 1f)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(AxisWidth), contentAlignment = Alignment.Center) {
            val month = layout.days.firstOrNull { it.month != null }?.month
            Text(
                text = if (month != null) "${month}月" else "节",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textTertiary
            )
        }
        layout.days.forEachIndexed { index, day ->
            val isToday = index == todayColumn
            Column(
                modifier = Modifier.width(dayWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "周${dayOfWeekText(day.dayOfWeek)}",
                    fontSize = 12.sp,
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isToday) colors.brand else colors.textSecondary,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isToday) colors.brand else colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = day.dayOfMonth?.toString() ?: "–",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isToday) colors.onBrand else colors.textPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionAxis(sections: List<SectionSlot>) {
    val colors = CampusTheme.colors
    Column(modifier = Modifier.width(AxisWidth)) {
        sections.forEach { slot ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(SectionHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = slot.number.toString(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
                slot.startTime?.let {
                    Text(text = it, fontSize = 9.sp, lineHeight = 11.sp, color = colors.textTertiary, maxLines = 1)
                }
                slot.endTime?.let {
                    Text(text = it, fontSize = 9.sp, lineHeight = 11.sp, color = colors.textTertiary, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun CourseBlockCell(
    block: TimetableBlock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val course = block.courses.first()
    val (foreground, container) = colors.courseColor(course.colorKey())
    val fontScale = LocalDensity.current.fontScale

    // 按课块可用高度计算行数，避免文字被硬裁一半。
    val innerHeight = SectionHeight.value * block.rowSpan - (BlockGap.value + BlockPaddingV.value) * 2
    val placeLines = when {
        course.place.isNullOrBlank() || block.isConflict -> 0
        block.rowSpan >= 2 -> 2
        else -> 1
    }
    val titleLines = ((innerHeight - placeLines * 13f * fontScale - 2f) / (14f * fontScale)).toInt().coerceAtLeast(1)

    Box(
        modifier = modifier
            .tapScale(onClick = onClick, pressedScale = 0.95f, clipShape = RoundedCornerShape(CampusShapes.extraSmall))
            .background(container)
            .padding(horizontal = BlockPaddingH, vertical = BlockPaddingV)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = if (block.isConflict) block.courses.joinToString(" / ") { it.title } else course.title,
                color = foreground,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = titleLines,
                overflow = TextOverflow.Ellipsis
            )
            if (placeLines > 0) {
                Text(
                    text = "@${course.place}",
                    color = foreground.copy(alpha = 0.78f),
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    maxLines = placeLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (block.isConflict) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(foreground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = block.courses.size.toString(),
                    color = container,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

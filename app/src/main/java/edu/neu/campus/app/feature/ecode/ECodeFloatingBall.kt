package edu.neu.campus.app.feature.ecode

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import edu.neu.campus.ui.theme.CampusShapes
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import top.yukonga.miuix.kmp.basic.FloatingActionButton
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Scan

/** Position is a fraction of the safe content area, so resizing never strands the ball. */
@Composable
internal fun ECodeFloatingBall(onClick: () -> Unit, expanded: Boolean) {
    val colors = CampusTheme.colors
    var left by remember { mutableStateOf(ECodePreferences.dockLeft) }
    var fraction by remember { mutableFloatStateOf(ECodePreferences.heightFraction) }
    var dragging by remember { mutableStateOf(false) }
    var tucked by remember { mutableStateOf(false) }
    var activity by remember { mutableIntStateOf(0) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragY by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(dragging, expanded, activity) {
        tucked = false
        if (!dragging && !expanded) {
            delay(3000)
            tucked = true
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val diameter = 72.dp
        val height = 56.dp
        val density = LocalDensity.current
        val travelX = with(density) { (maxWidth - diameter).toPx().coerceAtLeast(0f) }
        val margin = with(density) { CampusSpacing.xs.toPx() }
        val travelY = with(density) { (maxHeight - height).toPx().coerceAtLeast(0f) }
        val minY = margin.coerceAtMost(travelY / 2)
        val maxY = (travelY - margin).coerceAtLeast(minY)
        val dockX by animateFloatAsState(
            if (dragging) dragX else if (left) 0f else travelX,
            if (dragging) snap() else CampusMotion.springSmooth(), label = "dock"
        )
        val inset by animateFloatAsState(
            if (tucked) with(density) { diameter.toPx() / 2 } * (if (left) -1 else 1) else 0f,
            CampusMotion.springSmooth(), label = "tuck"
        )
        val y = minY + fraction * (maxY - minY)
        fun settle() {
            left = dragX + with(density) { diameter.toPx() / 2 } < with(density) { maxWidth.toPx() / 2 }
            fraction = if (maxY > minY) ((dragY - minY) / (maxY - minY)).coerceIn(0f, 1f) else 0f
            ECodePreferences.savePosition(left, fraction)
            dragging = false
            activity++
        }
        // Keep the full touch target inside the viewport even while the visual is half hidden.
        Box(
            Modifier.absoluteOffset { IntOffset((if (dragging) dragX else dockX).roundToInt(), (if (dragging) dragY else y).roundToInt()) }
                .size(diameter, height)
                .pointerInput(travelX, travelY, left, fraction) {
                    detectDragGestures(
                        onDragStart = { dragX = dockX; dragY = y; dragging = true },
                        onDragEnd = { settle() },
                        onDragCancel = { settle() },
                        onDrag = { change, delta ->
                            change.consume()
                            dragX = (dragX + delta.x).coerceIn(0f, travelX)
                            dragY = (dragY + delta.y).coerceIn(minY, maxY)
                        }
                    )
                }
                .clickable(onClickLabel = "打开 e 码通") { activity++; onClick() },
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = { activity++; onClick() },
                modifier = Modifier.absoluteOffset { IntOffset(if (dragging) 0 else inset.roundToInt(), 0) }
                    .border(1.dp, colors.brandBorder, CampusShapes.largeShape),
                shape = CampusShapes.largeShape,
                containerColor = colors.surfaceElevated,
                shadowElevation = CampusSpacing.xs,
                minWidth = diameter,
                minHeight = height
            ) {
                Row(
                    Modifier.padding(horizontal = CampusSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xxs)
                ) {
                    if (tucked && left) Spacer(Modifier.width(CampusSpacing.xl))
                    Icon(MiuixIcons.Regular.Scan, contentDescription = "e 码通", tint = colors.brand, modifier = Modifier.size(24.dp))
                    if (!tucked) Text("e", color = colors.brand, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    if (tucked && !left) Spacer(Modifier.width(CampusSpacing.xl))
                }
            }
        }
    }
}

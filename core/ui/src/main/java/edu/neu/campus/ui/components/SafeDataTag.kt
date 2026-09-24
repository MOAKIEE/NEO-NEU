package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object TimeFormatter {
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.SIMPLIFIED_CHINESE).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }
    private val dateTimeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.SIMPLIFIED_CHINESE).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }

    fun formatTime(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "未同步"
        return timeFormat.format(Date(epochMillis))
    }

    fun formatDateTime(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0) return "未同步"
        return dateTimeFormat.format(Date(epochMillis))
    }
}

/**
 * 严格遵循真实数据边界的来源与客户端同步时间标示。
 * 时间语义： "数据时间分别显示'最近同步'，只有真实提供学校更新时间才写'学校更新于'，客户端时间不冒充学校更新时间。"
 */
@Composable
fun SafeDataTag(
    sourceName: String,
    lastSuccessEpochMillis: Long?,
    isStale: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val timeStr = TimeFormatter.formatDateTime(lastSuccessEpochMillis)
        val text = if (isStale) {
            "来源：$sourceName · 最近同步 $timeStr (旧缓存)"
        } else if (lastSuccessEpochMillis != null) {
            "来源：$sourceName · 最近同步 $timeStr"
        } else {
            "来源：$sourceName"
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}

@Composable
fun SafeDataTag(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}

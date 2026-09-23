package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
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

@Composable
fun SafeDataTag(
    sourceName: String,
    lastSuccessEpochMillis: Long?,
    isStale: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val timeStr = TimeFormatter.formatTime(lastSuccessEpochMillis)
        val text = if (isStale) {
            "来源：$sourceName · 上次更新于 $timeStr (旧缓存)"
        } else if (lastSuccessEpochMillis != null) {
            "来源：$sourceName · 更新于 $timeStr"
        } else {
            "来源：$sourceName"
        }
        Text(
            text = text,
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
    }
}

@Composable
fun SafeDataTag(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary
        )
    }
}


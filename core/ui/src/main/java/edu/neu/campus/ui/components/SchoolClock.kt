package edu.neu.campus.ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun rememberSchoolClock(): Calendar {
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            value = System.currentTimeMillis()
            delay(60_000 - (value % 60_000))
        }
    }
    return Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai")).apply { timeInMillis = now }
}

/**
 * 学校时区的当天日期（yyyy-MM-dd）。只在日期变化时触发重组，
 * 适合课表这类只关心「今天是哪天」的页面，避免每分钟重组整页。
 */
@Composable
fun rememberSchoolToday(): String {
    val today by produceState(schoolToday()) {
        while (true) {
            delay(60_000 - (System.currentTimeMillis() % 60_000))
            value = schoolToday()
        }
    }
    return today
}

private fun schoolToday(): String = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    .apply { timeZone = TimeZone.getTimeZone("Asia/Shanghai") }
    .format(System.currentTimeMillis())

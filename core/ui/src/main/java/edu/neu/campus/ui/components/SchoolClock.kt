package edu.neu.campus.ui.components

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.util.Calendar
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

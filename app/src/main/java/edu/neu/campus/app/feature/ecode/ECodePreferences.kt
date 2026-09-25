package edu.neu.campus.app.feature.ecode

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ECodePreferences {
    private var preferences: android.content.SharedPreferences? = null
    var showFloatingBall by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences("ecode_ui_v1", Context.MODE_PRIVATE)
        showFloatingBall = preferences?.getBoolean("floating_ball", false) ?: false
    }

    fun setFloatingBall(enabled: Boolean) {
        showFloatingBall = enabled
        preferences?.edit()?.putBoolean("floating_ball", enabled)?.apply()
    }
}

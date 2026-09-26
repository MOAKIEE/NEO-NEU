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
        dockLeft = preferences?.getBoolean("dock_left", false) ?: false
        heightFraction = (preferences?.getFloat("height_fraction", 0.8f) ?: 0.8f).coerceIn(0f, 1f)
        showFloatingBall = preferences?.getBoolean("floating_ball", false) ?: false
    }

    var dockLeft = false
        private set
    var heightFraction = 0.8f
        private set

    fun savePosition(left: Boolean, fraction: Float) {
        dockLeft = left
        heightFraction = fraction.coerceIn(0f, 1f)
        preferences?.edit()?.putBoolean("dock_left", left)
            ?.putFloat("height_fraction", heightFraction)?.apply()
    }

    fun setFloatingBall(enabled: Boolean) {
        showFloatingBall = enabled
        preferences?.edit()?.putBoolean("floating_ball", enabled)?.apply()
    }
}

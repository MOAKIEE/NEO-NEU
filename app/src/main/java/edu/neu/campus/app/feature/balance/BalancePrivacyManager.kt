package edu.neu.campus.app.feature.balance

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 校园生活资产余额隐私管理（遮罩偏好），跨页面联动与持久化。
 */
object BalancePrivacyManager {
    private const val PREFS_NAME = "neo_neu_balance_privacy"
    private const val KEY_MASKED = "key_balance_masked"

    var isBalanceMasked by mutableStateOf(false)
        private set

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        isBalanceMasked = sp.getBoolean(KEY_MASKED, false)
    }

    fun toggleMasked() {
        val newVal = !isBalanceMasked
        isBalanceMasked = newVal
        prefs?.edit()?.putBoolean(KEY_MASKED, newVal)?.apply()
    }

    fun setMasked(masked: Boolean) {
        isBalanceMasked = masked
        prefs?.edit()?.putBoolean(KEY_MASKED, masked)?.apply()
    }
}

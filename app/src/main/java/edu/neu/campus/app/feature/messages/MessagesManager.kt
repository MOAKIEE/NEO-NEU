package edu.neu.campus.app.feature.messages

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf

/**
 * 本机已读消息记录管理器。
 * 按照设计规范，学校服务端不提供标记已读接口，阅读记录保存在本机，不伪造服务端状态。
 */
object MessagesManager {
    private const val PREFS_NAME = "neo_neu_local_read_messages"
    private const val KEY_READ_IDS = "key_read_ids"

    val localReadIds = mutableStateListOf<String>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp
        val set = sp.getStringSet(KEY_READ_IDS, emptySet()) ?: emptySet()
        localReadIds.clear()
        localReadIds.addAll(set)
    }

    fun markAsLocalRead(id: String) {
        if (!localReadIds.contains(id)) {
            localReadIds.add(id)
            prefs?.edit()?.putStringSet(KEY_READ_IDS, localReadIds.toSet())?.apply()
        }
    }

    fun isLocalRead(id: String): Boolean {
        return localReadIds.contains(id)
    }
}

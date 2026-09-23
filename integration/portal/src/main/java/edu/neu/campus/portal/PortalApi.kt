package edu.neu.campus.portal

import edu.neu.campus.contract.*
import edu.neu.campus.network.SchoolCall
import edu.neu.campus.network.SchoolHttp
import org.json.JSONArray
import org.json.JSONObject

class PortalApi(private val http: SchoolHttp) {
    suspend fun raw(call: SchoolCall, params: Map<String, String> = emptyMap()): String = http.execute(call, params)

    fun balanceItemIds(body: String): List<String> {
        val items = payload(body).getJSONArray("data")
        return (0 until items.length()).map { items.getJSONObject(it).getString("id") }
    }

    /** Item-to-balance mapping awaits verified stable keys, names and value types. */
    fun balances(itemsBody: String, detailBodies: List<String>): List<Balance> {
        val ids = balanceItemIds(itemsBody)
        require(ids.size == detailBodies.size)
        if (ids.isNotEmpty()) throw PortalSchemaException("余额项目映射尚无真实响应核验")
        return emptyList()
    }

    fun messages(body: String): Page<CampusMessage> {
        val data = payload(body)
        val list = data.getJSONArray("list")
        val items = (0 until list.length()).map { index ->
            val item = list.getJSONObject(index)
            CampusMessage(item.getString("log_id"), item.getString("title"), item.stringOrNull("content"),
                item.stringOrNull("time"), item.boolOrNull("has_read"), item.stringOrNull("source"))
        }
        return Page(items, data.intOrNull("count"), data.intOrNull("noReadCount"))
    }

    fun tasks(body: String, kind: TaskKind): Page<CampusTask> {
        val data = payload(body)
        // Only task request parameters are documented. Do not invent item fields.
        if (data.length() != 0) throw PortalSchemaException("待办响应结构尚无真实样本核验")
        return Page(emptyList(), null)
    }
}

class PortalSchemaException(message: String) : Exception(message)

fun payload(body: String): JSONObject {
    val root = JSONObject(body)
    when (root.optInt("e", -1)) {
        0 -> Unit
        10013 -> throw PortalAuthException()
        else -> throw PortalSchemaException("门户业务响应未成功")
    }
    return root.optJSONObject("d") ?: throw PortalSchemaException("门户响应缺少 d")
}

class PortalAuthException : Exception("门户登录状态失效")
private fun JSONObject.stringOrNull(key: String): String? = if (!has(key) || isNull(key)) null else get(key).toString()
private fun JSONObject.intOrNull(key: String): Int? = if (!has(key) || isNull(key)) null else getInt(key)
private fun JSONObject.boolOrNull(key: String): Boolean? = if (!has(key) || isNull(key)) null else when (get(key).toString()) { "1", "true" -> true; "0", "false" -> false; else -> null }

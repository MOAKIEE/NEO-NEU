package edu.neu.campus.portal

import edu.neu.campus.contract.*
import edu.neu.campus.network.SchoolCall
import edu.neu.campus.network.SchoolHttp
import org.json.JSONArray
import org.json.JSONObject

class PortalApi(private val http: SchoolHttp? = null) {
    suspend fun raw(call: SchoolCall, params: Map<String, String> = emptyMap()): String = requireNotNull(http).execute(call, params)

    data class BalanceItemRef(val id: String, val key: String, val name: String, val unit: String?)

    fun balanceItem(body: String, kind: BalanceKind): BalanceItemRef {
        val items = payload(body).getJSONArray("data")
        val key = when (kind) { BalanceKind.CAMPUS_CARD -> "card.balance"; BalanceKind.NETWORK -> "net.balance" }
        val matches = (0 until items.length()).map { items.getJSONObject(it) }.filter { it.optString("key") == key }
        if (matches.size != 1) throw PortalSchemaException("余额目录缺少已核验项目")
        val item = matches.single()
        val expectedName = when (kind) { BalanceKind.CAMPUS_CARD -> "校园卡余额"; BalanceKind.NETWORK -> "网费余额" }
        if (item.optString("name") != expectedName) throw PortalSchemaException("余额目录名称已变化")
        return BalanceItemRef(item.getString("id"), key, expectedName, item.stringOrNull("unit"))
    }

    /** Generic catalog labels and keys only; omits account-specific IDs and values. */
    fun balanceCatalog(body: String): List<String> {
        val items = payload(body).getJSONArray("data")
        return (0 until items.length()).map { items.getJSONObject(it) }
            .filter { it.getString("name").let { name -> "卡" in name || "网" in name } }.map {
            "${it.getString("key")}:${it.getString("name")}:${it.optString("unit")}"
        }
    }

    fun balance(detailBody: String, kind: BalanceKind, item: BalanceItemRef): Balance {
        val data = payload(detailBody).getJSONObject("data")
        val value = data.get("value")
        val text = value.toString().trim()
        val numeric = Regex("-?[0-9]+(\\.[0-9]+)?").matches(text)
        val masked = value is String && !numeric && Regex("[＊*•●xX-]{2,}(\\.[＊*•●xX]+)?").matches(text)
        if (!numeric && !masked) throw PortalSchemaException("余额字段不是金额或已知遮罩")
        val unit = data.stringOrNull("unit")?.takeIf { it.isNotBlank() } ?: item.unit?.takeIf { it.isNotBlank() }
        return Balance(kind, if (numeric) text else null, unit, masked, null)
    }

    fun messages(body: String): Page<CampusMessage> {
        val data = payload(body)
        val list = data.getJSONArray("list")
        val items = (0 until list.length()).map { index ->
            val item = list.getJSONObject(index)
            val content = item.opt("content")
            val contentLines = when (content) {
                is JSONArray -> (0 until content.length()).mapNotNull { i ->
                    when (val part = content.opt(i)) {
                        is String -> part
                        JSONObject.NULL, null -> null
                        else -> throw PortalSchemaException("消息内容字段类型变化")
                    }
                }
                is String -> listOf(content)
                JSONObject.NULL, null -> emptyList()
                else -> throw PortalSchemaException("消息内容字段类型变化")
            }
            CampusMessage(item.getString("log_id"), item.getString("title"),
                contentLines,
                item.stringOrNull("time"), item.boolOrNull("has_read"), item.stringOrNull("source"))
        }
        return Page(items, data.intOrNull("count"), data.intOrNull("noReadCount"))
    }

    fun tasks(body: String, kind: TaskKind): Page<CampusTask> {
        val data = payload(body)
        val list = data.getJSONArray("list")
        if (list.length() != 0) throw PortalSchemaException("非空待办记录字段尚无真实样本核验")
        return Page(emptyList(), data.intOrNull("total"))
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

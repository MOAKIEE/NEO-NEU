package edu.neu.campus.repository

import org.json.JSONArray
import org.json.JSONObject

/** Structure-only diagnostic. Never logs values, URLs, headers, cookies, or tokens. */
internal object ResponseShape {
    private val safeKey = Regex("[A-Za-z_][A-Za-z0-9_]{0,39}")
    fun describe(raw: String): String = runCatching { shape(JSONObject(raw), 0) }.getOrElse { "非 JSON 对象" }
    private fun shape(value: Any?, depth: Int): String {
        if (depth >= 5) return "…"
        return when (value) {
            is JSONObject -> value.keys().asSequence().take(20).map { key ->
                val name = if (safeKey.matches(key)) key else "[dynamic]"
                "$name:${shape(value.opt(key), depth + 1)}"
            }.joinToString(prefix = "{", postfix = "}")
            is JSONArray -> if (value.length() == 0) "[]" else "[${shape(value.opt(0), depth + 1)}]"
            is String -> if (value.startsWith("{")) runCatching { shape(JSONObject(value), depth + 1) }.getOrDefault("string")
                else if (value.startsWith("[")) runCatching { shape(JSONArray(value), depth + 1) }.getOrDefault("string") else "string"
            is Number -> "number"
            is Boolean -> "boolean"
            JSONObject.NULL, null -> "null"
            else -> "unknown"
        }
    }
}

package edu.neu.campus.academic

import edu.neu.campus.contract.*
import edu.neu.campus.network.SchoolCall
import edu.neu.campus.network.SchoolHttp
import org.json.JSONArray
import org.json.JSONObject

class AcademicApi(private val http: SchoolHttp) {
    suspend fun raw(call: SchoolCall, params: Map<String, String> = emptyMap()): String = http.execute(call, params)

    fun terms(listBody: String, currentBody: String): List<Term> {
        val current = objectData(currentBody, "cxmrxnxq").getJSONArray("rows").let { if (it.length() == 0) null else it.getJSONObject(0).getString("XNXQDM") }
        return objectData(listBody, "xnxqcx").getJSONArray("rows").objects().map {
            Term(it.getString("DM"), it.getString("MC"), it.getString("DM") == current)
        }
    }

    fun weeks(body: String): List<TeachingWeek> = arrayData(body, "getTermWeeks").objects().map {
        TeachingWeek(it.getInt("serialNumber"), it.optStringOrNull("startDate"), it.optStringOrNull("endDate"), it.optBoolean("curWeek", false))
    }

    fun campuses(body: String): List<Campus> = arrayData(body, "getMyScheduledCampus").objects().map {
        Campus(it.getString("id"), it.optStringOrNull("name"))
    }

    fun sections(body: String): List<Section> = arrayData(body, "getMySectionList").objects().map {
        Section(it.getString("code"), it.optStringOrNull("startTime"), it.optStringOrNull("endTime"))
    }

    /** Course title/location keys require a real response. A nonempty result is never silently dropped. */
    fun timetable(body: String, term: String, week: Int?, campus: Campus, sections: List<Section>): Timetable {
        val data = objectData(body, "getMyScheduleDetail")
        val arranged = data.getJSONArray("arrangedList")
        val unscheduled = data.getJSONArray("notArrangeList")
        val practice = data.getJSONArray("practiceList")
        if (arranged.length() != 0 || unscheduled.length() != 0 || practice.length() != 0) {
            throw SchemaException("课表课程详情字段尚无真实响应核验")
        }
        return Timetable(term, week, listOf(campus), mapOf(campus.id to sections), emptyList(), emptyList(), emptyList())
    }

    fun grades(body: String): List<Grade> = objectData(body, "cxwdcj").getJSONArray("rows").objects().map {
        Grade(it.getString("WID"), null, it.optStringOrNull("XSZCJ"), null,
            when (it.optStringOrNull("SFJG")) { "1" -> true; "0" -> false; else -> null })
    }

    fun gradeSummary(body: String): GradeSummary = GradeSummary(
        objectData(body, "queryPjxfjd").optStringOrNull("ZPJXFJD"), "学校全局统计"
    )

    fun exams(body: String): List<Exam> {
        val data = objectData(body, "queryMyExamArrangeMent")
        val arranged = data.getJSONArray("arranged").objects().map {
            Exam(it.getString("KCM"), it.optStringOrNull("KSSJMS"), it.optStringOrNull("JASMC"), it.optStringOrNull("ZWH"), it.optStringOrNull("KSZT"), true)
        }
        val notArranged = data.getJSONArray("notArranged").objects().map {
            Exam(it.getString("KCM"), it.optStringOrNull("KSSJMS"), it.optStringOrNull("JASMC"), it.optStringOrNull("ZWH"), it.optStringOrNull("KSZT"), false)
        }
        return arranged + notArranged
    }
}

class SchemaException(message: String) : Exception(message)

fun objectData(body: String, action: String): JSONObject {
    val root = JSONObject(body)
    if (root.optString("code") != "0") throw SchemaException("教务业务响应未成功")
    val datas = root.optJSONObject("datas") ?: throw SchemaException("教务响应缺少 datas")
    val value = datas.opt(action) ?: throw SchemaException("教务响应缺少 $action")
    val obj = value as? JSONObject ?: throw SchemaException("教务 $action 结构不是对象")
    val ext = obj.optJSONObject("extParams")
    if (ext != null && ext.optInt("code", 1) != 1) throw SchemaException("教务业务状态异常")
    return obj
}

fun arrayData(body: String, action: String): JSONArray {
    val root = JSONObject(body)
    if (root.optString("code") != "0") throw SchemaException("教务业务响应未成功")
    val datas = root.optJSONObject("datas") ?: throw SchemaException("教务响应缺少 datas")
    return datas.optJSONArray(action) ?: throw SchemaException("教务 $action 结构不是数组")
}

private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key) || !has(key)) null else get(key).toString()

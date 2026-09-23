package edu.neu.campus.academic

import edu.neu.campus.contract.*
import edu.neu.campus.network.SchoolCall
import edu.neu.campus.network.SchoolHttp
import org.json.JSONArray
import org.json.JSONObject

class AcademicApi(private val http: SchoolHttp? = null) {
    suspend fun raw(call: SchoolCall, params: Map<String, String> = emptyMap()): String = requireNotNull(http).execute(call, params)

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
        Section(it.getString("code"), it.optStringOrNull("name"), it.optStringOrNull("id"),
            it.optStringOrNull("startTime"), it.optStringOrNull("endTime"))
    }

    /** Maps only fields observed in the authenticated 2026-09-23 timetable response. */
    fun timetable(body: String, term: String, week: Int?, campus: Campus, sections: List<Section>): Timetable {
        val data = objectData(body, "getMyScheduleDetail")
        val arranged = data.getJSONArray("arrangedList")
        val unscheduled = data.getJSONArray("notArrangeList")
        val practice = data.getJSONArray("practiceList")
        val arrangedCourses = arranged.objects().map { item ->
            CourseOccurrence(item.optStringOrNull("teachClassId"), campus.id, item.getString("courseName"),
                item.getInt("dayOfWeek"), item.getInt("beginSection"), item.getInt("endSection"),
                item.optStringOrNull("beginTime"), item.optStringOrNull("endTime"), null,
                item.optStringOrNull("placeName"), item.optStringOrNull("weeksAndTeachers"))
        }
        fun extra(list: JSONArray) = list.objects().map { item ->
            UnscheduledCourse(item.optStringOrNull("teachClassId"), campus.id, item.getString("courseName"), null)
        }
        return Timetable(term, week, listOf(campus), mapOf(campus.id to sections), arrangedCourses, extra(unscheduled), extra(practice))
    }

    fun grades(body: String): List<Grade> = objectData(body, "cxwdcj").getJSONArray("rows").objects().map {
        Grade(it.getString("WID"), it.getString("XNXQDM"), it.getString("KCH"), it.getString("KCM"),
            it.optStringOrNull("XSZCJ"), it.optStringOrNull("XF"), it.optStringOrNull("JD"),
            it.optStringOrNull("SFJG_DISPLAY"), it.optStringOrNull("CXCKDM_DISPLAY"))
    }

    fun gradeTermIds(body: String): List<String> = objectData(body, "cxwdcjxnxq").getJSONArray("rows").objects().map {
        it.getString("XNXQDM")
    }

    fun gradeDetail(body: String, sourceId: String): GradeDetail {
        val data = objectData(body, "details")
        val components = data.getJSONArray("itemScores").objects().map {
            GradeComponent(it.getString("code"), it.getString("name"), it.optStringOrNull("value"),
                it.optBooleanOrNull("pass"), it.optBooleanOrNull("highestScoreInProportion"))
        }
        return GradeDetail(sourceId, data.optStringOrNull("score"), data.optStringOrNull("gradePoint"),
            data.optBooleanOrNull("pass"), components)
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
private fun JSONObject.optBooleanOrNull(key: String): Boolean? = if (isNull(key) || !has(key)) null else getBoolean(key)

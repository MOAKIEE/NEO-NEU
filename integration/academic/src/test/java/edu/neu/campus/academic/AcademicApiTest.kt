package edu.neu.campus.academic

import edu.neu.campus.contract.Campus
import org.junit.Assert.*
import org.junit.Test

class AcademicApiTest {
    private val api = AcademicApi()

    @Test fun sourceShapedTermAndWeekSamples() {
        val list = """{"code":"0","datas":{"xnxqcx":{"rows":[{"DM":"term-a","MC":"测试学期"}]}}}"""
        val current = """{"code":"0","datas":{"cxmrxnxq":{"rows":[{"XNXQDM":"term-a","XNXQMC":"测试学期"}]}}}"""
        assertEquals(true, api.terms(list, current).single().isCurrent)
        val weeks = """{"code":"0","datas":{"getTermWeeks":[{"serialNumber":1,"startDate":"2026-09-01","endDate":"2026-09-07","curWeek":true}]}}"""
        assertEquals(1, api.weeks(weeks).single().number)
    }

    @Test fun verifiedCourseFieldsMapWithoutInventingTeacherOrWeek() {
        val body = """{"code":"0","datas":{"getMyScheduleDetail":{"arrangedList":[{"teachClassId":"opaque","courseName":"测试课程","dayOfWeek":1,"beginSection":1,"endSection":2,"beginTime":"08:00","endTime":"09:30","placeName":"测试地点","weeksAndTeachers":"原始描述"}],"notArrangeList":[{"courseName":"未排课程"}],"practiceList":[]}}}"""
        val result = api.timetable(body, "term-a", 1, Campus("campus-a", null), emptyList())
        assertEquals("测试课程", result.arranged.single().title)
        assertEquals("测试地点", result.arranged.single().place)
        assertNull(result.arranged.single().teacher)
        assertEquals("未排课程", result.unscheduled.single().title)
    }

    @Test fun missingStandardEnvelopeFails() {
        assertThrows(Exception::class.java) { api.weeks("""{"code":"0","datas":{}}""") }
    }

    @Test fun observedGradeFieldsKeepOfficialText() {
        val body = """{"code":"0","datas":{"cxwdcj":{"rows":[{"WID":"opaque","XNXQDM":"term-a","KCH":"C1","KCM":"测试课程","XSZCJ":"优秀","XF":2.5,"JD":"4.0","SFJG_DISPLAY":"通过","CXCKDM_DISPLAY":"正常"}]}}}"""
        val grade = api.grades(body).single()
        assertEquals("测试课程", grade.courseName)
        assertEquals("优秀", grade.rawScore)
        assertEquals("2.5", grade.credit)
        assertEquals("通过", grade.passDescription)
    }

    @Test fun observedDetailBreakdownMapsWithoutCalculatingNewScore() {
        val body = """{"code":"0","datas":{"details":{"score":"良好","gradePoint":"3.5","pass":true,"itemScores":[{"code":"part","name":"平时","value":"80","pass":true,"highestScoreInProportion":false}]}}}"""
        val detail = api.gradeDetail(body, "opaque")
        assertEquals("良好", detail.rawScore)
        assertEquals("平时", detail.components.single().name)
        assertEquals("80", detail.components.single().rawValue)
    }
}

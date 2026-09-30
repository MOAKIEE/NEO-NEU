package edu.neu.campus.authweb

import org.junit.Assert.*
import org.junit.Test

class PortalPagePlanTest {
    private fun catalogue(vararg items: String) = """{"e":0,"d":{"list":[${items.joinToString(",")}]}}"""

    @Test fun usesPortalMobileRouteAndFallsBackToUrlOnlyWhenMobileIsEmpty() {
        val mobile = catalogue("""{"name":"教务系统（新）","url":"https://jwxt.neu.edu.cn/jwapp/sys/homeapp/index.do","extra_url":{"mobile_url":"https://jwxt.neu.edu.cn"}}""")
        assertEquals("https://jwxt.neu.edu.cn", PortalPagePlan.resolve(OfficialPage.ACADEMIC, mobile).authenticationEntry)
        val desktop = catalogue("""{"name":"教务系统（新）","url":"https://jwxt.neu.edu.cn/","extra_url":{"mobile_url":""}}""")
        assertEquals("https://jwxt.neu.edu.cn/", PortalPagePlan.resolve(OfficialPage.ACADEMIC, desktop).authenticationEntry)
    }

    @Test fun rechargeUsesPortalPaymentAuthenticationBeforeItsSpecificDestination() {
        val body = catalogue("""{"name":"缴费服务大厅","url":"https://pay.neu.edu.cn/drCasLogin"}""")
        for (page in listOf(OfficialPage.CARD_RECHARGE, OfficialPage.NETWORK_RECHARGE)) {
            val plan = PortalPagePlan.resolve(page, body)
            assertEquals(OfficialPage.PAYMENT_HALL.url, plan.authenticationEntry)
            assertEquals(page.url, plan.destination)
        }
        assertNull(PortalPagePlan.resolve(OfficialPage.PAYMENT_HALL, body).destination)
    }

    @Test fun mailboxHomepageIsConvertedToSchoolPublishedCasEntry() {
        val body = catalogue("""{"name":"学生邮箱","url":"https://mails.neu.edu.cn/"}""")
        assertEquals("https://mails.neu.edu.cn/coremail/cmcu_addon/dbdxsso.jsp",
            PortalPagePlan.resolve(OfficialPage.STUDENT_MAIL, body).authenticationEntry)
        assertEquals(PortalPagePlan.resolve(OfficialPage.STUDENT_MAIL, body), PortalPagePlan.resolve(OfficialPage.STUDENT_MAIL, null))
    }

    @Test fun untrustedMobileEntriesNeverReceiveCredentialsOrOverrideFallback() {
        for (url in listOf("https://evil.example/", "http://jwxt.neu.edu.cn/", "https://jwxt.neu.edu.cn:8443/",
            "https://user:secret@jwxt.neu.edu.cn/", "https://jwxt.neu.edu.cn/?ticket=secret",
            "https://jwxt.neu.edu.cn/unknown-sso", "https://jwxt.neu.edu.cn.evil.example/")) {
            val body = catalogue("""{"name":"教务系统（新）","url":"https://jwxt.neu.edu.cn/","extra_url":{"mobile_url":"$url"}}""")
            assertEquals(OfficialPage.ACADEMIC.url, PortalPagePlan.resolve(OfficialPage.ACADEMIC, body).authenticationEntry)
        }
    }

    @Test fun ambiguousMissingAndChangedDirectoriesUseVerifiedFallback() {
        val item = """{"name":"教务系统（新）","url":"https://jwxt.neu.edu.cn/"}"""
        for (body in listOf(catalogue(item, item), catalogue(), "{", """{"e":10013,"d":{}}""",
            catalogue("""{"name":"教务系统（旧）","url":"https://jwxt.neu.edu.cn/"}"""))) {
            assertEquals(OfficialPage.ACADEMIC.url, PortalPagePlan.resolve(OfficialPage.ACADEMIC, body).authenticationEntry)
        }
    }

    @Test fun loginAndAuthenticationCallbacksDoNotCountAsServiceContent() {
        assertFalse(OfficialPage.PAYMENT_HALL.isDestination("https://pay.neu.edu.cn/tologin.html"))
        assertFalse(OfficialPage.PAYMENT_HALL.isDestination(OfficialPage.PAYMENT_HALL.url))
        assertFalse(OfficialPage.STUDENT_MAIL.isDestination(OfficialPage.STUDENT_MAIL.authenticationEntry))
        assertFalse(OfficialPage.ECODE.isDestination("https://pass.neu.edu.cn/tpass/login"))
        assertFalse(OfficialPage.STUDENT_MAIL.isDestination("https://mails.neu.edu.cn.evil.example/coremail/hxphone/"))
        assertTrue(OfficialPage.STUDENT_MAIL.isDestination("https://mails.neu.edu.cn/coremail/hxphone/"))
        assertTrue(OfficialPage.CARD_RECHARGE.isDestination(OfficialPage.CARD_RECHARGE.url))
    }
}

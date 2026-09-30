package edu.neu.campus.authweb

import org.json.JSONObject
import java.net.URI

/** Public entry points only. Account-specific catalogue data and navigation URLs are never stored. */
enum class OfficialPage(val title: String, val url: String) {
    PORTAL("智慧东大门户", PORTAL_ENTRY),
    ACADEMIC("本科教务系统", ACADEMIC_ENTRY),
    ECODE("e 码通认证", "https://ecode.neu.edu.cn/ecode/#/"),
    CARD_RECHARGE("校园卡充值", "https://pay.neu.edu.cn/scardrecharge51264Z006.html"),
    NETWORK_RECHARGE("网费充值", "https://pay.neu.edu.cn/netdetails51247N005.html"),
    STUDENT_MAIL("学生邮箱", "https://mails.neu.edu.cn/"),
    PAYMENT_HALL("缴费服务大厅", "https://pay.neu.edu.cn/drCasLogin");

    internal val cataloguePage: OfficialPage
        get() = when (this) { CARD_RECHARGE, NETWORK_RECHARGE -> PAYMENT_HALL; else -> this }

    internal val catalogueNames: Set<String>
        get() = when (this) {
            ACADEMIC -> setOf("教务系统（新）", "本科教务系统")
            PAYMENT_HALL -> setOf("缴费服务大厅")
            STUDENT_MAIL -> setOf("学生邮箱", "学生邮件系统")
            ECODE -> setOf("e码通", "e 码通")
            else -> emptySet()
        }

    internal val authenticationEntry: String
        get() = when (this) {
            STUDENT_MAIL -> "https://mails.neu.edu.cn/coremail/cmcu_addon/dbdxsso.jsp"
            CARD_RECHARGE, NETWORK_RECHARGE -> PAYMENT_HALL.url
            else -> url
        }

    internal fun isDestination(url: String): Boolean {
        val uri = safeUri(url) ?: return false
        if (uri.host != URI(this.url).host) return false
        return when (this) {
            CARD_RECHARGE, NETWORK_RECHARGE, PAYMENT_HALL -> uri.path !in setOf("/tologin.html", "/drCasLogin")
            STUDENT_MAIL -> uri.path != "/coremail/cmcu_addon/dbdxsso.jsp"
            else -> true
        }
    }
}

internal data class PortalPagePlan(val authenticationEntry: String, val destination: String?) {
    companion object {
        fun resolve(page: OfficialPage, body: String?): PortalPagePlan {
            val cataloguePage = page.cataloguePage
            val entry = body?.let { catalogueEntry(cataloguePage, it) } ?: cataloguePage.authenticationEntry
            return PortalPagePlan(entry, page.url.takeIf { page == OfficialPage.CARD_RECHARGE || page == OfficialPage.NETWORK_RECHARGE })
        }

        /** Mirrors terminalOpen: Android selects extra_url.mobile_url, then url. No visit/collect writes. */
        private fun catalogueEntry(page: OfficialPage, body: String): String? = runCatching {
            val root = JSONObject(body)
            if (root.optInt("e", -1) != 0) return null
            val list = root.optJSONObject("d")?.optJSONArray("list") ?: return null
            val matching = (0 until list.length()).mapNotNull { list.optJSONObject(it) }
                .filter { it.optString("name") in page.catalogueNames }
            if (matching.size != 1) return null
            val item = matching.single()
            val mobile = item.optJSONObject("extra_url")?.optString("mobile_url").orEmpty()
            val selected = mobile.takeIf { it.isNotBlank() } ?: item.optString("url")
            approvedEntry(page, selected)
        }.getOrNull()
    }
}

private fun safeUri(url: String): URI? = runCatching { URI(url) }.getOrNull()?.takeIf {
    it.scheme == "https" && it.port in setOf(-1, 443) && it.rawUserInfo == null && it.host != null
}

private fun approvedEntry(page: OfficialPage, url: String): String? {
    val uri = safeUri(url) ?: return null
    // School-controlled data does not authorize arbitrary origins or ticket-bearing URLs.
    if (uri.rawQuery != null) return null
    val paths = when (page) {
        OfficialPage.ACADEMIC -> setOf("", "/", "/jwapp/sys/homeapp/index.do")
        OfficialPage.PAYMENT_HALL -> setOf("/drCasLogin")
        OfficialPage.STUDENT_MAIL -> setOf("", "/", "/coremail/cmcu_addon/dbdxsso.jsp")
        OfficialPage.ECODE -> setOf("/ecode/", "/ecode")
        else -> emptySet()
    }
    if (uri.host != URI(page.url).host || uri.rawPath !in paths) return null
    return if (page == OfficialPage.STUDENT_MAIL) page.authenticationEntry else url
}

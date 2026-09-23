package edu.neu.campus.authweb

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.network.SchoolCall
import edu.neu.campus.network.SchoolHttp
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONObject

object OfficialLogin {
    fun intent(context: Context): Intent = Intent(context, OfficialLoginActivity::class.java)
}

/** Only school HTTPS origins are navigable; there is no JavaScript bridge or form inspection. */
class OfficialLoginActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var session: LocalSession
    private lateinit var web: WebView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = LocalSession.get(this)
        if (savedInstanceState == null) session.beginLogin()
        status = TextView(this).apply { text = "请在学校官方页面自行登录；完成后点击验证。"; setPadding(20, 20, 20, 20) }
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(false)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url
                    val host = url.host.orEmpty().lowercase()
                    val trusted = host == "neu.edu.cn" || host.endsWith(".neu.edu.cn")
                    if (!trusted) { status.text = "已阻止非学校域名的跳转"; return true }
                    if (url.scheme == "http" && host == "jwxt.neu.edu.cn") {
                        view.loadUrl(url.buildUpon().scheme("https").build().toString()); return true
                    }
                    return url.scheme != "https"
                }
            }
        }
        val controls = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        fun button(label: String, action: () -> Unit) { controls.addView(Button(this).apply { text = label; setOnClickListener { action() } }) }
        button("门户登录") { web.loadUrl("https://personal.neu.edu.cn/portal") }
        button("教务登录") { web.loadUrl("https://jwxt.neu.edu.cn/jwapp/sys/homeapp/index.do") }
        button("验证会话") { verify() }
        button("返回") { finish() }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(status)
            addView(controls)
            addView(web, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        })
        web.loadUrl("https://personal.neu.edu.cn/portal")
    }

    private fun verify() {
        status.text = "正在验证门户和教务查询会话…"
        scope.launch {
            val http = SchoolHttp(session)
            val portal = runCatching {
                val root = JSONObject(http.execute(SchoolCall.PORTAL_INFO))
                root.optInt("e", -1) == 0 && root.optJSONObject("d") != null
            }.getOrDefault(false)
            session.mark(Domain.PORTAL, if (portal) DomainStatus.READY else DomainStatus.EXPIRED)
            val academic = runCatching {
                val root = JSONObject(http.execute(SchoolCall.CURRENT_TERM,
                    mapOf("CSDM" to "SYS", "ZCSDM" to "DQXNXQDM", "SFSY" to "1")))
                root.optString("code") == "0" && root.optJSONObject("datas") != null
            }.getOrDefault(false)
            session.mark(Domain.ACADEMIC, if (academic) DomainStatus.READY else DomainStatus.EXPIRED)
            status.text = "门户：${if (portal) "查询可用" else "未通过"}；教务：${if (academic) "查询可用" else "未通过"}"
            if (portal && academic) setResult(RESULT_OK)
        }
    }

    override fun onDestroy() {
        web.destroy()
        scope.cancel()
        super.onDestroy()
    }
}

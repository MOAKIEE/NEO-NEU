package edu.neu.campus.authweb

import android.app.Activity
import android.content.MutableContextWrapper
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import edu.neu.campus.session.SchoolCredentials
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal const val PORTAL_ENTRY = "https://personal.neu.edu.cn/portal"
internal const val ACADEMIC_ENTRY = "https://jwxt.neu.edu.cn/jwapp/sys/homeapp/index.do"

internal enum class LoginFailure(val message: String) {
    NETWORK("学校网页暂时无法打开，请检查网络后重试。"),
    SECURITY("学校网页的安全连接验证失败，请检查设备时间或网络。"),
    TIMEOUT("登录连接超时，请稍后重试。"),
    STORAGE("无法加密保存账号密码，请重试或使用学校网页登录。")
}

/** One browser is moved from the invisible attempt into the visible challenge without reloading. */
internal class LoginBrowser(activity: Activity, var scope: String) {
    private val context = MutableContextWrapper(activity)
    private val automation = activity.assets.open("school-login.js").bufferedReader().use { it.readText() }
    var loading = false
        private set
    var failure: LoginFailure? = null
        private set
    var onChange: () -> Unit = {}
    var onLoaded: () -> Unit = {}
    var destroyed = false
        private set
    var siteIndex = 0
        private set
    private var revision = 0
    val web = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.setSupportMultipleWindows(false)
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        isSaveEnabled = false
        if (android.os.Build.VERSION.SDK_INT >= 26) importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (!request.isForMainFrame) return false
                val url = request.url
                val host = url.host.orEmpty().lowercase()
                val school = host == "neu.edu.cn" || host.endsWith(".neu.edu.cn")
                if (school && url.scheme == "http" && (url.port == -1 || url.port == 80)) {
                    view.loadUrl(url.buildUpon().scheme("https").encodedAuthority(host).build().toString())
                    return true
                }
                if (school && url.scheme == "https" && (url.port == -1 || url.port == 443) && url.userInfo == null) return false
                failed(LoginFailure.SECURITY)
                return true
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                when (Uri.parse(url.orEmpty()).host) {
                    "jwxt.neu.edu.cn" -> siteIndex = 1
                    "personal.neu.edu.cn" -> siteIndex = 0
                }
                revision++
                loading = true
                failure = null
                onChange()
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (url != view.url) return
                loading = false
                onChange()
                onLoaded()
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) failed(LoginFailure.NETWORK)
            }

            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                if (request.isForMainFrame) failed(LoginFailure.NETWORK)
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                handler.cancel()
                failed(LoginFailure.SECURITY)
            }
        }
    }

    private fun failed(reason: LoginFailure) {
        loading = false
        failure = reason
        onChange()
    }

    fun load(url: String) {
        siteIndex = if (url == ACADEMIC_ENTRY) 1 else 0
        failure = null
        loading = true
        web.loadUrl(url)
        onChange()
    }

    fun detach() { (web.parent as? ViewGroup)?.removeView(web) }

    fun attachTo(activity: Activity, visible: Boolean) {
        detach()
        context.baseContext = activity
        web.alpha = if (visible) 1f else 0f
        web.isFocusable = visible
        web.isFocusableInTouchMode = visible
        if (!visible) web.clearFocus()
        web.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
    }

    fun park() {
        detach()
        context.baseContext = context.applicationContext
        onChange = {}; onLoaded = {}
    }

    suspend fun inspect(credentials: SchoolCredentials? = null, submit: Boolean = false, submitted: Boolean = false): String {
        if (destroyed || loading) return "loading"
        // Check the native URL and recheck origin/action inside the script before writing secrets.
        val uri = Uri.parse(web.url.orEmpty())
        if (uri.scheme != "https" || uri.host != "pass.neu.edu.cn" || uri.encodedPath != "/tpass/login" ||
            uri.userInfo != null || uri.port !in listOf(-1, 443)) return "unsupported"
        val observedRevision = revision
        val script = "$automation(${jsString(credentials?.account)}, ${jsString(credentials?.password)}, $submit, $submitted)"
        return suspendCancellableCoroutine { continuation ->
            web.evaluateJavascript(script) { result ->
                if (continuation.isActive) continuation.resume(
                    if (observedRevision != revision) "loading" else result.orEmpty().trim('"')
                )
            }
        }
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        onChange = {}; onLoaded = {}
        detach()
        web.stopLoading()
        web.destroy()
        context.baseContext = context.applicationContext
    }
}

/** JSON string escaping, including JS line separators; no shell or JavaScript interpolation. */
internal fun jsString(value: String?): String {
    if (value == null) return "null"
    return buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char < ' ' || char == '\u2028' || char == '\u2029') {
                    append("\\u"); append(char.code.toString(16).padStart(4, '0'))
                } else append(char)
            }
        }
        append('"')
    }
}

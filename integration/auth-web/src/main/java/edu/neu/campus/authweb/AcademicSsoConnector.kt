package edu.neu.campus.authweb

import android.app.Activity
import android.net.Uri
import android.net.http.SslError
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.network.SessionProbe
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers

/** Opens the school's academic entry with the existing portal/CAS cookies, without showing a login form. */
object AcademicSsoConnector {
    private const val ACADEMIC_URL = "https://jwxt.neu.edu.cn/jwapp/sys/homeapp/index.do"
    private val lock = Mutex()

    suspend fun connect(activity: Activity, session: LocalSession = LocalSession.get(activity)): Boolean = lock.withLock {
        val state = session.state.value
        val scope = state.accountScope ?: return@withLock false
        if (state.academic == DomainStatus.READY) return@withLock true
        if (state.portal != DomainStatus.READY) return@withLock false

        val reachedAcademic = withContext(Dispatchers.Main.immediate) {
            val root = activity.findViewById<FrameLayout>(android.R.id.content)
            val completed = CompletableDeferred<Boolean>()
            val web = WebView(activity).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                alpha = 0f
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        val url = request.url
                        val host = url.host.orEmpty().lowercase()
                        val schoolHost = host == "neu.edu.cn" || host.endsWith(".neu.edu.cn")
                        if (url.scheme == "http" && schoolHost) {
                            view.loadUrl(url.buildUpon().scheme("https").build().toString())
                            return true
                        }
                        if (url.scheme == "https" && schoolHost) return false
                        completed.complete(false)
                        return true
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        val host = Uri.parse(url.orEmpty()).host.orEmpty().lowercase()
                        if (host == "jwxt.neu.edu.cn") completed.complete(true)
                        if (host.isNotEmpty() && host != "jwxt.neu.edu.cn") {
                            // A CAS page may redirect with an existing ticket. Give it a moment;
                            // a stationary login form needs the visible official login activity.
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                if (!completed.isCompleted && Uri.parse(view.url.orEmpty()).host == host) {
                                    completed.complete(false)
                                }
                            }, 1200)
                        }
                    }

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (request.isForMainFrame) completed.complete(false)
                    }

                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                        if (request.isForMainFrame) completed.complete(false)
                    }

                    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                        handler.cancel()
                        completed.complete(false)
                    }
                }
            }
            root.addView(web, FrameLayout.LayoutParams(1, 1))
            val scopeWatcher = launch {
                session.state.first { it.accountScope != scope }
                completed.complete(false)
            }
            try {
                web.loadUrl(ACADEMIC_URL)
                withTimeoutOrNull(15_000) { completed.await() } == true
            } finally {
                scopeWatcher.cancel()
                completed.complete(false)
                web.stopLoading()
                root.removeView(web)
                web.destroy()
            }
        }
        if (!reachedAcademic || session.state.value.accountScope != scope) return@withLock false
        // CookieManager writes are shared with native requests; let the last page settle first.
        delay(700)
        session.flushCookies()
        val verified = SessionProbe.verify(session)
        verified.accountScope == scope && verified.academic == DomainStatus.READY
    }
}

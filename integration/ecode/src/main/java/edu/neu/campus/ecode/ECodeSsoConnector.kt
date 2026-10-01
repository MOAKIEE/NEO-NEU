package edu.neu.campus.ecode

import android.app.Activity
import android.net.Uri
import android.net.http.SslError
import android.os.SystemClock
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import edu.neu.campus.contract.ECodeResult
import edu.neu.campus.session.LocalSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** Establishes an e-code session through the school's own WebView flow, without reading login fields. */
object ECodeSsoConnector {
    private val bridgeLock = Mutex()

    enum class Connection { READY, LOGIN_REQUIRED, UNAVAILABLE }

    /** Called after a fresh school login, before the CAS session has a chance to expire. */
    suspend fun warm(activity: Activity, repository: OfficialECodeRepository): Connection = bridgeLock.withLock {
        if (repository.hasSession()) Connection.READY else openOfficialPage(activity, repository)
    }

    /** Fetches a fresh code only after the hidden page has been closed. */
    suspend fun connect(activity: Activity, repository: OfficialECodeRepository): ECodeResult =
        when (warm(activity, repository)) {
            Connection.READY -> repository.fetch()
            Connection.LOGIN_REQUIRED -> ECodeResult.LoginRequired
            Connection.UNAVAILABLE -> ECodeResult.Unavailable
        }

    private suspend fun openOfficialPage(activity: Activity, repository: OfficialECodeRepository): Connection =
        withContext(Dispatchers.Main.immediate) {
            coroutineScope {
                val session = LocalSession.get(activity)
                val scope = session.state.value.accountScope
                val root = activity.findViewById<FrameLayout>(android.R.id.content)
                var loginPageSince: Long? = null
                var pageFailed = false
                val web = WebView(activity).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    alpha = 0f
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (!request.isForMainFrame) return false
                            val target = request.url
                            val host = target.host.orEmpty().lowercase()
                            if (target.scheme == "https" && (host == "neu.edu.cn" || host.endsWith(".neu.edu.cn"))) return false
                            pageFailed = true
                            return true
                        }

                        override fun onPageFinished(view: WebView, url: String?) {
                            loginPageSince = if (Uri.parse(url.orEmpty()).host == "pass.neu.edu.cn") {
                                loginPageSince ?: SystemClock.elapsedRealtime()
                            } else null
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) pageFailed = true
                        }

                        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                            handler.cancel()
                            pageFailed = true
                        }
                    }
                }
                root.addView(web, FrameLayout.LayoutParams(1, 1))
                var closed = false
                fun closeBrowser() {
                    if (closed) return
                    closed = true
                    web.stopLoading()
                    root.removeView(web)
                    web.destroy()
                }
                val watcher = launch {
                    session.state.first { it.accountScope != scope }
                    closeBrowser()
                }
                try {
                    // The front end first checks /user-info and sets KC_REDIRECT before calling SSO.
                    web.loadUrl("https://ecode.neu.edu.cn/ecode/#/")
                    withTimeoutOrNull(15_000) {
                        var outcome = Connection.LOGIN_REQUIRED
                        while (true) {
                            delay(1_000)
                            if (scope != session.state.value.accountScope) break
                            if (pageFailed) {
                                outcome = Connection.UNAVAILABLE
                                break
                            }
                            session.flushCookies()
                            if (repository.hasSession()) {
                                outcome = Connection.READY
                                break
                            }
                            if (loginPageSince?.let { SystemClock.elapsedRealtime() - it >= 4_000 } == true) break
                        }
                        outcome
                    } ?: Connection.UNAVAILABLE
                } finally {
                    watcher.cancel()
                    closeBrowser()
                }
            }
        }
}

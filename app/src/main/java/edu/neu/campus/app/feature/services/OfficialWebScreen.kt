package edu.neu.campus.app.feature.services

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import edu.neu.campus.ui.components.CampusButton
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text

/** One in-app WebView profile, shared with the official login flow through Android CookieManager. */
@Composable
fun OfficialWebScreen(service: OfficialService, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = CampusTheme.colors
    var web by remember(service) { mutableStateOf<WebView?>(null) }
    var currentHost by remember(service) { mutableStateOf(Uri.parse(service.url).host.orEmpty()) }
    var error by remember(service) { mutableStateOf<String?>(null) }

    BackHandler {
        if (web?.canGoBack() == true) web?.goBack() else onBack()
    }
    DisposableEffect(service) {
        onDispose {
            web?.stopLoading()
            web?.destroy()
            web = null
        }
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        CampusTopBar(title = service.title, subtitle = currentHost, onBack = onBack)
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { activity ->
                    WebView(activity).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.setSupportMultipleWindows(false)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val target = request.url
                                if (target.scheme == "https") return false
                                val host = target.host.orEmpty().lowercase()
                                if (target.scheme == "http" && (host == "neu.edu.cn" || host.endsWith(".neu.edu.cn"))) {
                                    view.loadUrl(target.buildUpon().scheme("https").build().toString())
                                    return true
                                }
                                if (request.isForMainFrame && target.scheme in setOf("alipays", "weixin")) {
                                    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, target)) }
                                        .onFailure { Toast.makeText(context, "无法打开支付应用", Toast.LENGTH_SHORT).show() }
                                } else if (request.isForMainFrame) {
                                    error = "该跳转无法在应用内安全打开"
                                }
                                return true
                            }

                            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                                error = null
                                currentHost = Uri.parse(url.orEmpty()).host.orEmpty()
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, webError: WebResourceError) {
                                if (request.isForMainFrame) error = "学校网页暂时无法打开，请检查网络后重试"
                            }

                            override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                                if (request.isForMainFrame) error = "网页返回 ${response.statusCode}，请稍后重试"
                            }

                            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, sslError: SslError) {
                                handler.cancel()
                                error = "安全连接验证失败，请检查设备时间或网络"
                            }
                        }
                        web = this
                        loadUrl(service.url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            error?.let { message ->
                Column(
                    Modifier.fillMaxWidth().background(colors.surface).padding(CampusSpacing.md)
                ) {
                    Text(message, color = colors.warning)
                    Spacer(Modifier.height(CampusSpacing.sm))
                    CampusButton(text = "重试", onClick = { web?.reload() })
                }
            }
        }
    }
}

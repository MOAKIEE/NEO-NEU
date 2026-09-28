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
import android.webkit.WebChromeClient
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import edu.neu.campus.ui.components.CampusButton
import edu.neu.campus.ui.components.CampusWebTopBar
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusIconBadge
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Report
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Text
import kotlinx.coroutines.delay

/** One in-app WebView profile, shared with the official login flow through Android CookieManager. */
@Composable
fun OfficialWebScreen(service: OfficialService, onBack: () -> Unit) {
    val context = LocalContext.current
    val colors = CampusTheme.colors
    var web by remember(service) { mutableStateOf<WebView?>(null) }
    var currentHost by remember(service) { mutableStateOf(Uri.parse(service.url).host.orEmpty()) }
    var error by remember(service) { mutableStateOf<String?>(null) }

    var progress by remember(service) { mutableIntStateOf(0) }
    var loading by remember(service) { mutableStateOf(true) }
    var pageVisible by remember(service) { mutableStateOf(false) }
    var navigationId by remember(service) { mutableIntStateOf(0) }
    val navigateBack: () -> Unit = { if (web?.canGoBack() == true) web?.goBack() else onBack() }
    // Restart at the published entry: reload() can otherwise reload an empty/error document
    // or repeat the last login form submission.
    val reload: () -> Unit = {
        web?.stopLoading()
        error = null
        progress = 0
        loading = true
        pageVisible = false
        navigationId++
        web?.loadUrl(service.url)
    }
    BackHandler(onBack = navigateBack)
    LaunchedEffect(service, navigationId, loading, error) {
        if (loading && error == null) {
            delay(30_000)
            if (!pageVisible) {
                error = "网页加载超时，请检查网络后重新加载"
                loading = false
                web?.stopLoading()
            }
        }
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        CampusWebTopBar(
            title = service.title, host = currentHost, onBack = navigateBack,
            onClose = onBack, onRefresh = reload, refreshing = loading && error == null
        )
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outlineVariant)) {
            if (loading && error == null) Box(
                Modifier.fillMaxWidth((progress / 100f).coerceIn(0.04f, 1f)).fillMaxHeight().background(colors.brand)
            )
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            AndroidView(
                factory = { activity ->
                    WebView(activity).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        // School desktop pages need a full viewport rather than a phone-width crop.
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        settings.setSupportZoom(true)
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.setSupportMultipleWindows(false)
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }
                        }
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
                                loading = true
                                pageVisible = false
                                navigationId++
                                progress = 0
                                currentHost = Uri.parse(url.orEmpty()).host.orEmpty()
                            }

                            override fun onPageFinished(view: WebView, url: String?) {
                                loading = false
                            }

                            override fun onPageCommitVisible(view: WebView, url: String?) {
                                pageVisible = true
                            }

                            override fun onReceivedError(view: WebView, request: WebResourceRequest, webError: WebResourceError) {
                                if (request.isForMainFrame) error = "请检查网络连接后重试"
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
                onRelease = { view ->
                    // AndroidView has detached the view before destroying its rendering resources.
                    view.stopLoading()
                    view.webChromeClient = null
                    view.webViewClient = WebViewClient()
                    view.destroy()
                    if (web === view) web = null
                },
                update = { view ->
                    // Do not expose the WebView's built-in error document underneath native recovery UI.
                    view.visibility = if (error == null) View.VISIBLE else View.INVISIBLE
                },
                modifier = Modifier.fillMaxSize()
            )
            if (loading && !pageVisible && error == null) {
                Column(
                    Modifier.fillMaxSize().background(colors.background),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CampusSpacing.md, Alignment.CenterVertically)
                ) {
                    Text("正在加载学校网页…", color = colors.textSecondary)
                    CampusButton(text = "重新加载", onClick = reload)
                }
            }
            error?.let { message ->
                Column(
                    Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState())
                        .padding(CampusSpacing.screenHorizontal),
                    verticalArrangement = Arrangement.Center
                ) {
                    CampusCard {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = CampusSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
                        ) {
                            CampusIconBadge(MiuixIcons.Regular.Report, colors.warning, colors.warningContainer)
                            Text("网页暂时无法打开", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(message, color = colors.textSecondary, textAlign = TextAlign.Center)
                            CampusButton(text = "重新加载", onClick = reload, primary = true)
                        }
                    }
                }
            }
        }
    }
}

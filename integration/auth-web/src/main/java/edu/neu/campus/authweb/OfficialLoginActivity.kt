package edu.neu.campus.authweb

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.net.http.SslError
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebResourceResponse
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.SessionState
import edu.neu.campus.network.SessionProbe
import edu.neu.campus.session.LocalSession
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusSegmentedControl
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

object OfficialLogin {
    fun intent(context: Context, domain: Domain = Domain.PORTAL): Intent =
        Intent(context, OfficialLoginActivity::class.java).putExtra("target_domain", domain.name)
    suspend fun verifyExisting(context: Context) = SessionProbe.verify(LocalSession.get(context))
}

/** The WebView only opens school HTTPS pages and never reads form fields. */
class OfficialLoginActivity : ComponentActivity() {
    private val portalUrl = "https://personal.neu.edu.cn/portal"
    // The root redirects through HTTP on some school responses; use the observed HTTPS entry.
    private val academicUrl = "https://jwxt.neu.edu.cn/jwapp/sys/homeapp/index.do"
    private lateinit var session: LocalSession
    private lateinit var web: WebView
    private var selectedSite by mutableIntStateOf(0)
    private var pageLoading by mutableStateOf(false)
    private var pageError by mutableStateOf<String?>(null)
    private var checking by mutableStateOf(false)
    private var hasChecked by mutableStateOf(false)
    private var resumingSession = false
    private var autoVerifyOnLoad = false
    private var attemptedAcademicHandoff by mutableStateOf(false)
    private var autoCheckJob: Job? = null
    private var targetDomain = Domain.PORTAL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.init(this)
        session = LocalSession.get(this)
        targetDomain = runCatching { Domain.valueOf(intent.getStringExtra("target_domain").orEmpty()) }.getOrDefault(Domain.PORTAL)
        resumingSession = session.state.value.accountScope != null
        autoVerifyOnLoad = when (targetDomain) {
            Domain.PORTAL -> session.state.value.portal != DomainStatus.READY
            Domain.ACADEMIC -> session.state.value.academic != DomainStatus.READY
        }
        // No stable school account identifier is verified across both domains yet.
        // A visible re-authentication may switch accounts, so rotate the local cache scope.
        selectedSite = savedInstanceState?.getInt("selectedSite")
            ?: if (targetDomain == Domain.ACADEMIC) 1 else 0
        attemptedAcademicHandoff = savedInstanceState?.getBoolean("attemptedAcademicHandoff") ?: false

        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(false)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url
                    val host = url.host.orEmpty().lowercase()
                    val schoolHost = host == "neu.edu.cn" || host.endsWith(".neu.edu.cn")
                    if (url.scheme == "http" && schoolHost) {
                        view.loadUrl(url.buildUpon().scheme("https").build().toString())
                        return true
                    }
                    if (url.scheme == "https" && schoolHost) return false
                    pageError = "已阻止非学校页面的跳转"
                    return true
                }

                override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                    autoCheckJob?.cancel()
                    pageLoading = true
                    pageError = null
                }

                override fun onPageFinished(view: WebView, url: String?) {
                    pageLoading = false
                    val host = Uri.parse(url.orEmpty()).host
                    if (autoVerifyOnLoad && pageError == null &&
                        (host == "personal.neu.edu.cn" || host == "jwxt.neu.edu.cn")) {
                        autoCheckJob?.cancel()
                        autoCheckJob = lifecycleScope.launch {
                            delay(700)
                            while (checking) delay(250)
                            val currentHost = Uri.parse(web.url.orEmpty()).host
                            if (!pageLoading &&
                                (currentHost == "personal.neu.edu.cn" || currentHost == "jwxt.neu.edu.cn")) {
                                checkConnection(automatic = true)
                            }
                        }
                    }
                }

                override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                    if (request.isForMainFrame) {
                        pageLoading = false
                        pageError = "学校网页暂时无法打开，请检查网络后重试。"
                    }
                }

                override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
                    if (request.isForMainFrame) {
                        pageLoading = false
                        pageError = "学校网页返回 ${errorResponse.statusCode}，请稍后重试。"
                    }
                }

                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                    handler.cancel()
                    pageLoading = false
                    pageError = "学校网页的安全连接验证失败，请检查设备时间或网络。"
                }
            }
        }
        lifecycleScope.launch {
            if (savedInstanceState == null) session.beginLogin()
            if (savedInstanceState == null || web.restoreState(savedInstanceState) == null) {
                web.loadUrl(if (selectedSite == 0) portalUrl else academicUrl)
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            if (web.canGoBack()) web.goBack() else finish()
        }
        setContent {
            CampusTheme {
                val state by session.state.collectAsState()
                LoginScreen(
                    state = state,
                    selectedSite = selectedSite,
                    pageLoading = pageLoading,
                    pageError = pageError,
                    checking = checking,
                    hasChecked = hasChecked,
                    resumingSession = resumingSession,
                    allowPortalOnly = targetDomain == Domain.PORTAL && attemptedAcademicHandoff &&
                        state.portal == DomainStatus.READY && state.academic != DomainStatus.READY,
                    web = web,
                    onSelectSite = ::openSite,
                    onCheck = { checkConnection() },
                    onRetry = { web.reload() },
                    onClose = ::finish
                )
            }
        }
    }

    private fun openSite(index: Int) {
        if (index !in 0..1) return
        selectedSite = index
        pageError = null
        web.loadUrl(if (index == 0) portalUrl else academicUrl)
    }

    private fun checkConnection(automatic: Boolean = false) {
        if (checking) return
        checking = true
        lifecycleScope.launch {
            try {
                // Commit cookies set by the official page before native HTTP probes run.
                session.flushCookies()
                val result = SessionProbe.verify(session)
                hasChecked = true
                when (nextLoginStep(targetDomain, result, selectedSite, attemptedAcademicHandoff, automatic)) {
                    LoginStep.OPEN_ACADEMIC -> {
                        attemptedAcademicHandoff = true
                        openSite(1)
                    }
                    LoginStep.FINISH -> {
                        autoVerifyOnLoad = false
                        setResult(RESULT_OK)
                        finish()
                    }
                    LoginStep.WAIT -> Unit
                }
            } finally {
                checking = false
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selectedSite", selectedSite)
        outState.putBoolean("attemptedAcademicHandoff", attemptedAcademicHandoff)
        web.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        autoCheckJob?.cancel()
        web.stopLoading()
        web.destroy()
        super.onDestroy()
    }
}

@Composable
private fun LoginScreen(
    state: SessionState,
    selectedSite: Int,
    pageLoading: Boolean,
    pageError: String?,
    checking: Boolean,
    hasChecked: Boolean,
    resumingSession: Boolean,
    allowPortalOnly: Boolean,
    web: WebView,
    onSelectSite: (Int) -> Unit,
    onCheck: () -> Unit,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    val colors = CampusTheme.colors
    Scaffold(
        modifier = Modifier.fillMaxSize().background(colors.background),
        containerColor = colors.background
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
                .imePadding().background(colors.background)
        ) {
            val compact = maxHeight < 600.dp
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                        .padding(horizontal = CampusSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(MiuixIcons.Regular.Back, contentDescription = "返回", tint = colors.textPrimary)
                    }
                    Column(modifier = Modifier.padding(start = CampusSpacing.xs)) {
                        Text("学校登录", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text("账号密码仅在学校官方网页输入", fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = CampusSpacing.screenHorizontal)
                        .padding(bottom = CampusSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                ) {
                    if (!compact) {
                        CampusCard {
                            Row(horizontalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                                ConnectionStatus("统一门户", state.portal, Modifier.weight(1f))
                                ConnectionStatus("教务系统", state.academic, Modifier.weight(1f))
                            }
                            Spacer(Modifier.height(CampusSpacing.xs))
                            Text(connectionHint(state, hasChecked), fontSize = 12.sp, lineHeight = 18.sp,
                                color = colors.textSecondary)
                        }
                    }
                    CampusSegmentedControl(
                        options = listOf("统一门户", "教务系统"),
                        selectedIndex = selectedSite,
                        onSelect = onSelectSite
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                            .clip(RoundedCornerShape(CampusShapes.medium))
                            .background(Color.White)
                            .border(1.dp, colors.outline, RoundedCornerShape(CampusShapes.medium))
                    ) {
                        AndroidView(factory = { web }, modifier = Modifier.fillMaxSize())
                        if (pageError != null) {
                            Column(
                                modifier = Modifier.fillMaxSize().background(Color.White)
                                    .padding(CampusSpacing.md),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(pageError, fontSize = 14.sp, color = colors.warning)
                                Spacer(Modifier.height(CampusSpacing.md))
                                Button(onClick = onRetry) { Text("重试打开网页") }
                            }
                        } else if (pageLoading) {
                            Text(
                                "正在打开学校网页…",
                                modifier = Modifier.align(Alignment.TopCenter)
                                    .clip(RoundedCornerShape(CampusShapes.small))
                                    .background(colors.surface)
                                    .padding(horizontal = CampusSpacing.sm, vertical = CampusSpacing.xs),
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Button(onClick = onCheck, enabled = !checking && !pageLoading, modifier = Modifier.fillMaxWidth()) {
                        Text(when {
                            checking -> "正在检查门户与教务…"
                            state.portal == DomainStatus.READY && state.academic == DomainStatus.READY -> "检查连接并返回应用"
                            allowPortalOnly -> "仅连接门户并返回应用"
                            else -> "完成登录，检查连接"
                        })
                    }
                    if (resumingSession && !compact) {
                        Text(
                            "已保留学校会话；本地数据已重新隔离，换号后不会沿用旧缓存。",
                            fontSize = 11.sp,
                            color = colors.textTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatus(name: String, status: DomainStatus, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val (label, tint) = when (status) {
        DomainStatus.READY -> "已连接" to colors.success
        DomainStatus.SIGNED_OUT -> "未登录" to colors.textSecondary
        DomainStatus.AUTHENTICATING -> "等待登录" to colors.brand
        DomainStatus.UNVERIFIED -> "待检查" to colors.warning
        DomainStatus.EXPIRED -> "需要登录" to colors.warning
        DomainStatus.UNREACHABLE -> "暂不可达" to colors.textSecondary
    }
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, fontSize = 12.sp, color = colors.textSecondary)
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = tint)
    }
}

private fun connectionHint(state: SessionState, hasChecked: Boolean): String {
    if (!hasChecked) return when {
        state.portal == DomainStatus.READY && state.academic == DomainStatus.READY ->
            "当前连接正常；重新认证后点下方按钮确认连接。"
        state.portal == DomainStatus.READY -> "门户已连接；应用会自动尝试打开教务页面。"
        state.academic == DomainStatus.READY -> "教务已连接；请在门户页面完成登录，再检查连接。"
        else -> "先在网页完成学校登录；门户成功后会自动尝试连接教务。"
    }
    return when {
        state.portal == DomainStatus.READY && state.academic == DomainStatus.READY -> "门户与教务均已连接。"
        state.portal == DomainStatus.READY && state.academic == DomainStatus.EXPIRED -> "门户已连接；请在教务页面完成登录，再检查一次。"
        state.academic == DomainStatus.READY && state.portal == DomainStatus.EXPIRED -> "教务已连接；请在门户页面完成登录，再检查一次。"
        state.portal == DomainStatus.UNREACHABLE || state.academic == DomainStatus.UNREACHABLE ->
            "部分连接暂时无法确认，请检查网络或稍后重试。"
        else -> "尚未建立学校连接，请在官方网页完成登录后重试。"
    }
}

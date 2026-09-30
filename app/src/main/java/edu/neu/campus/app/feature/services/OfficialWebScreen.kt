package edu.neu.campus.app.feature.services

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import edu.neu.campus.authweb.LoginResult
import edu.neu.campus.authweb.OfficialPageResult
import edu.neu.campus.authweb.OfficialPages
import edu.neu.campus.authweb.PreparedOfficialPage
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Report

/** Routes and authentication remain in the adapter; Compose only presents the prepared school page. */
@Composable
fun OfficialWebScreen(service: OfficialService, onBack: () -> Unit, onLogin: (LoginResult) -> Unit,
    loginRevision: Int = 0, loginCancelled: Boolean = false) {
    val activity = LocalContext.current.activity()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val colors = CampusTheme.colors
    var page by remember(service, loginRevision) { mutableStateOf<PreparedOfficialPage?>(null) }
    var currentHost by remember(service) { mutableStateOf("") }
    var error by remember(service, loginRevision) { mutableStateOf<String?>(if (loginCancelled) "学校登录尚未完成，请重试。" else null) }
    var progress by remember(service, loginRevision) { mutableIntStateOf(0) }
    var loading by remember(service, loginRevision) { mutableStateOf(!loginCancelled) }
    var pageVisible by remember(service, loginRevision) { mutableStateOf(false) }
    var navigationId by remember(service) { mutableIntStateOf(0) }
    var refreshId by remember(service) { mutableIntStateOf(0) }
    var authRequested by remember(service, loginRevision) { mutableStateOf(false) }
    var viaPortal by remember(service) { mutableStateOf(true) }
    var pendingContinuation by remember(service, loginRevision) { mutableStateOf<String?>(null) }
    val currentOnLogin by rememberUpdatedState(onLogin)
    val requestLogin: (LoginResult) -> Unit = { result ->
        if (!authRequested) {
            authRequested = true
            pageVisible = false
            loading = true
            currentOnLogin(result)
        }
    }
    val navigateBack: () -> Unit = { if (page?.canGoBack() == true) page?.goBack() else onBack() }
    val reload: () -> Unit = {
        page?.close()
        page = null
        error = null
        progress = 0
        loading = true
        pageVisible = false
        authRequested = false
        pendingContinuation?.let { edu.neu.campus.authweb.OfficialLogin.discardContinuation(it) }
        pendingContinuation = null
        refreshId++
    }
    BackHandler(onBack = navigateBack)

    LaunchedEffect(service, refreshId, loginRevision, pendingContinuation) {
        // Cancels hidden attempts in the background, and keeps a visible service page when returning.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            val token = pendingContinuation
            if (page == null && error == null && (!authRequested || token != null)) {
                val result = try { OfficialPages.prepare(activity, service, viaPortal, token) }
                catch (cancelled: CancellationException) {
                    if (token != null) {
                        pendingContinuation = null
                        authRequested = false
                        loading = false
                        error = "学校连接已暂停，请重新连接。"
                    }
                    throw cancelled
                }
                authRequested = false
                when (result) {
                    is OfficialPageResult.Open -> {
                        page = result.page
                        currentHost = result.page.host
                        loading = false
                        pageVisible = true
                        progress = 100
                    }
                    is OfficialPageResult.Login -> requestLogin(result.result)
                    is OfficialPageResult.Failed -> { error = result.message; loading = false }
                }
                pendingContinuation = null
            }
        }
    }
    LaunchedEffect(navigationId, loading, error, page) {
        if (page != null && loading && error == null && !authRequested) {
            delay(30_000)
            if (!pageVisible) {
                error = "网页加载超时，请检查网络后重新加载"
                loading = false
                page?.stop()
            }
        }
    }
    val prepared = page
    DisposableEffect(prepared) { onDispose { prepared?.close() } }
    DisposableEffect(service, loginRevision) {
        onDispose { pendingContinuation?.let { edu.neu.campus.authweb.OfficialLogin.discardContinuation(it) } }
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        CampusWebTopBar(service.title, currentHost, navigateBack, onBack, reload, loading && error == null)
        Box(Modifier.fillMaxWidth().height(2.dp).background(colors.outlineVariant)) {
            if (loading && error == null) Box(
                Modifier.fillMaxWidth((progress / 100f).coerceIn(0.04f, 1f)).fillMaxHeight().background(colors.brand)
            )
        }
        if (prepared?.requiresMailboxLogin == true && !authRequested && error == null) {
            Text("学校仍要求邮箱登录，请使用邮箱账号和邮箱密码。", color = colors.textSecondary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs))
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            if (prepared != null && !authRequested) AndroidView(
                factory = {
                    prepared.attach(activity,
                        onChange = {
                            currentHost = prepared.host
                            loading = prepared.loading
                            prepared.error?.let { error = it }
                        },
                        onStarted = { error = null; pageVisible = false; progress = 0; navigationId++ },
                        onCommit = { pageVisible = true }, onProgress = { progress = it },
                        onLogin = { result ->
                            // A later CAS redirect gets the same single-submit budget and keeps its original callback.
                            if (result is LoginResult.ContinueOnWeb) {
                                authRequested = true
                                loading = true
                                pageVisible = false
                                page = null
                                pendingContinuation = result.token
                            } else requestLogin(result)
                        })
                    prepared.web.apply {
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                },
                onRelease = { prepared.close() },
                update = { it.visibility = if (error == null) View.VISIBLE else View.INVISIBLE },
                modifier = Modifier.fillMaxSize()
            )
            if (loading && !pageVisible && error == null) {
                Column(Modifier.fillMaxSize().background(colors.background),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(CampusSpacing.md, Alignment.CenterVertically)) {
                    Text(if (prepared == null) "正在通过个人门户连接…" else "正在加载学校网页…", color = colors.textSecondary)
                    if (!authRequested) CampusButton("重新连接", onClick = reload)
                }
            }
            error?.let { message ->
                Column(Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState())
                    .padding(CampusSpacing.screenHorizontal), verticalArrangement = Arrangement.Center) {
                    CampusCard {
                        Column(Modifier.fillMaxWidth().padding(vertical = CampusSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                            CampusIconBadge(MiuixIcons.Regular.Report, colors.warning, colors.warningContainer)
                            Text("网页暂时无法打开", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                            Text(message, color = colors.textSecondary, textAlign = TextAlign.Center)
                            CampusButton("重新连接", onClick = reload, primary = true)
                            if (service != OfficialService.PORTAL) CampusButton("直接连接学校服务", onClick = { viaPortal = false; reload() })
                            CampusButton("登录学校账号", onClick = { requestLogin(LoginResult.NeedCredentials()) })
                        }
                    }
                }
            }
        }
    }
}

private tailrec fun Context.activity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> error("An activity is required to open an official school page")
}

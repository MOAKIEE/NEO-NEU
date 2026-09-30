package edu.neu.campus.authweb

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import edu.neu.campus.contract.Domain
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.SessionState
import edu.neu.campus.network.SessionProbe
import edu.neu.campus.session.LocalSession
import edu.neu.campus.session.SavedLoginStatus
import edu.neu.campus.session.SchoolCredentials
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text

/** Passwords live in memory only; a fresh entry reads the encrypted vault, never saved state. */
internal class LoginScreenModel : ViewModel() {
    var account by mutableStateOf("")
    var accountEdited = false
    var passwordEdited = false
    val entryGate = SavedLoginEntryGate()
    var password by mutableStateOf("")
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var selectedSite by mutableIntStateOf(0)
    var attemptedAcademicHandoff = false
    var browser by mutableStateOf<LoginBrowser?>(null)
    var pendingBrowser by mutableStateOf<LoginBrowser?>(null)
    var browserRevision by mutableIntStateOf(0)
    override fun onCleared() { password = ""; browser?.destroy(); pendingBrowser?.destroy() }
}

/** Native credential entry; the official WebView is visible only for interactive authentication. */
class OfficialLoginActivity : ComponentActivity() {
    private lateinit var session: LocalSession
    private lateinit var model: LoginScreenModel
    private var target = Domain.PORTAL
    private var loginJob: Job? = null
    private var checkJob: Job? = null
    private var checkAgain = false
    private var scopeWatcher: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        ThemeManager.init(this)
        session = LocalSession.get(this)
        model = ViewModelProvider(this)[LoginScreenModel::class.java]
        if (savedInstanceState != null || intent.getBooleanExtra("silent_attempted", false)) {
            model.entryGate.claim(SavedLoginStatus.NONE, false, false, false)
        }
        target = runCatching { Domain.valueOf(intent.getStringExtra("target_domain").orEmpty()) }.getOrDefault(Domain.PORTAL)
        onBackPressedDispatcher.addCallback(this) { closeOrReturn() }
        model.browser?.let { attachVisible(it) }
        if (savedInstanceState == null && model.browser == null) {
            val continuation = intent.getStringExtra("continuation")
            if (continuation != null) {
                val browser = PendingLogin.take(continuation)
                if (browser != null && browser.scope == session.state.value.accountScope) {
                    model.pendingBrowser = browser
                }
                else { browser?.destroy(); model.error = "验证页面已失效，请重新登录。" }
            }
            if (intent.getBooleanExtra("credential_rejected", false)) model.error = "学校未接受账号密码，请检查后重新填写。"
        }
        setContent {
            CampusTheme {
                val state by session.state.collectAsState()
                val saved by session.savedLoginStatus.collectAsState()
                LaunchedEffect(state.accountScope, saved) {
                    run {
                        val scope = state.accountScope
                        try {
                            val credentials = scope?.let { session.readCredentials(it, includePaused = true) }
                            if (session.state.value.accountScope != scope) return@LaunchedEffect
                            if (!model.accountEdited) model.account = credentials?.account.orEmpty()
                            if (!model.accountEdited && !model.passwordEdited) model.password = credentials?.password.orEmpty()
                            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.RESUMED) }
                            if (session.state.value.accountScope != scope) return@LaunchedEffect
                            if (model.entryGate.claim(
                                    if (credentials == null) SavedLoginStatus.NONE else session.savedLoginStatus.value,
                                    model.accountEdited || model.passwordEdited,
                                    model.pendingBrowser != null || model.browser != null,
                                    intent.getBooleanExtra("credential_rejected", false)
                                )) resumeSaved()
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { model.error = "无法读取已保存账号，请重试或重新填写。" }
                    }
                }
                Scaffold(containerColor = CampusTheme.colors.background) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).imePadding()) {
                        val browser = model.browser
                        if (browser == null) CredentialScreen(model, saved, onBack = ::finish,
                            onSubmit = ::saveAndLogin, onSaved = ::resumeSaved, onOfficial = ::openOfficial,
                            onContinue = ::continueChallenge)
                        else ChallengeScreen(model, browser, state, target,
                            onBack = ::closeOrReturn, onCheck = { checkConnection() }, onSite = ::openSite,
                            onRefresh = ::refreshChallenge)
                    }
                }
            }
        }
    }

    private fun saveAndLogin() {
        if (model.busy) return
        val account = model.account.trim()
        if (account.isBlank() || model.password.isEmpty()) { model.error = "请填写学号和密码。"; return }
        val credentials = SchoolCredentials(account, model.password)
        model.passwordEdited = true
        model.password = ""
        runLogin { SchoolLogin.saveAndConnect(this, target, credentials) }
    }

    private fun resumeSaved() {
        runLogin { SchoolLogin.recover(this, target) }
    }

    private fun discardPendingChallenge() {
        model.pendingBrowser?.destroy()
        model.pendingBrowser = null
    }

    private fun continueChallenge() {
        if (model.busy) return
        val browser = model.pendingBrowser ?: return
        model.pendingBrowser = null
        model.busy = true
        loginJob = lifecycleScope.launch {
            try {
                if (browser.destroyed || browser.scope != session.state.value.accountScope) {
                    browser.destroy()
                    model.error = "验证页面已失效，请重新登录。"
                } else showChallenge(browser)
            } catch (cancelled: CancellationException) { browser.destroy(); throw cancelled }
            catch (_: Exception) { browser.destroy(); model.error = LoginFailure.STORAGE.message }
            finally { model.busy = false }
        }
    }

    private fun runLogin(block: suspend () -> LoginResult) {
        if (model.busy) return
        discardPendingChallenge()
        model.busy = true
        model.error = null
        model.attemptedAcademicHandoff = false
        loginJob = lifecycleScope.launch {
            try {
                when (val result = block()) {
                    LoginResult.Connected -> finishConnected()
                    is LoginResult.ContinueOnWeb -> {
                        val browser = PendingLogin.take(result.token)
                        if (browser != null && browser.scope == session.state.value.accountScope) showChallenge(browser)
                        else { browser?.destroy(); model.error = "登录状态已变化，请重试。" }
                    }
                    is LoginResult.NeedCredentials -> model.error = if (result.rejected)
                        "学校未接受账号密码，自动登录已暂停，请检查后重新填写。" else "请重新填写账号密码。"
                    is LoginResult.Failed -> model.error = result.message
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { model.error = "登录未完成，请重试。" }
            finally { model.busy = false }
        }
    }

    private fun openOfficial() {
        if (model.busy) return
        discardPendingChallenge()
        model.password = ""
        model.error = null
        model.attemptedAcademicHandoff = false
        model.busy = true
        loginJob = lifecycleScope.launch {
            try {
                val scope = session.beginLogin()
                val browser = LoginBrowser(this@OfficialLoginActivity, scope)
                model.selectedSite = if (target == Domain.ACADEMIC) 1 else 0
                attachVisible(browser)
                browser.load(if (model.selectedSite == 0) PORTAL_ENTRY else ACADEMIC_ENTRY)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { model.error = "学校网页登录暂时无法打开，请重试。" }
            finally { model.busy = false }
        }
    }

    private suspend fun showChallenge(browser: LoginBrowser) {
        // Visible forms can switch accounts. Isolate personal caches before allowing interaction.
        val scope = try { session.beginLogin(keepCredentials = true, expectedScope = browser.scope) }
        catch (error: Exception) { browser.destroy(); throw error }
        browser.scope = scope
        model.selectedSite = browser.siteIndex
        model.error = null
        attachVisible(browser)
    }

    private fun attachVisible(browser: LoginBrowser) {
        browser.attachTo(this, visible = true)
        model.browser = browser
        browser.onChange = { model.selectedSite = browser.siteIndex; model.browserRevision++ }
        browser.onLoaded = {
            val host = android.net.Uri.parse(browser.web.url.orEmpty()).host
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                (host == "personal.neu.edu.cn" || host == "jwxt.neu.edu.cn" ||
                browser.pageTarget?.cataloguePage?.isDestination(browser.web.url.orEmpty()) == true)) checkConnection(automatic = true)
        }
        scopeWatcher?.cancel()
        scopeWatcher = lifecycleScope.launch {
            session.state.first { it.accountScope != browser.scope }
            browser.destroy()
            if (model.browser === browser) model.browser = null
            model.error = "账号状态已变化，请重新登录。"
        }
    }

    private fun openSite(index: Int) {
        if (model.busy || index !in 0..1) return
        model.selectedSite = index
        model.browser?.load(if (index == 0) PORTAL_ENTRY else ACADEMIC_ENTRY)
    }

    private fun checkConnection(automatic: Boolean = false) {
        if (checkJob?.isActive == true) {
            if (automatic) checkAgain = true
            return
        }
        val browser = model.browser ?: return
        val navigation = browser.navigationRevision
        model.busy = true
        checkJob = lifecycleScope.launch {
            try {
                if (automatic) delay(700)
                if (browser.destroyed || model.browser !== browser || session.state.value.accountScope != browser.scope) return@launch
                session.flushCookies()
                val verified = if (browser.pageTarget != null) SessionProbe.verifyPortal(session) else SessionProbe.verify(session)
                if (verified.accountScope != browser.scope || browser.destroyed || model.browser !== browser ||
                    browser.navigationRevision != navigation || browser.loading) return@launch
                if (browser.pageTarget != null) {
                    // Portal/page handoff returns to the original service; it does not detour through academic login.
                    val host = android.net.Uri.parse(browser.web.url.orEmpty()).host
                    val callbackReached = host == "personal.neu.edu.cn" ||
                        browser.pageTarget!!.cataloguePage.isDestination(browser.web.url.orEmpty())
                    if (callbackReached && verified.portal == DomainStatus.READY) {
                        if (verified.academic == DomainStatus.AUTHENTICATING) {
                            session.markIfScope(browser.scope, Domain.ACADEMIC, DomainStatus.UNVERIFIED)
                        }
                        finishConnected()
                    }
                    else model.error = if (!automatic) "请继续完成学校验证。" else null
                    return@launch
                }
                when (nextLoginStep(target, verified, model.selectedSite, model.attemptedAcademicHandoff, automatic)) {
                    LoginStep.OPEN_ACADEMIC -> {
                        model.attemptedAcademicHandoff = true
                        model.selectedSite = 1
                        browser.load(ACADEMIC_ENTRY)
                    }
                    LoginStep.FINISH -> finishConnected()
                    LoginStep.WAIT -> model.error = if (!automatic) "连接尚未完成，请继续学校验证或检查网络。" else null
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { model.error = "连接检查未完成，请重试。" }
            finally {
                model.busy = false
                checkJob = null
                val repeat = checkAgain
                checkAgain = false
                if (repeat && model.browser === browser && !browser.destroyed &&
                    lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) checkConnection(automatic = true)
            }
        }
    }

    private fun refreshChallenge() {
        if (model.busy) return
        model.browser?.let { browser ->
            browser.load(browser.pageTarget?.authenticationEntry ?: if (model.selectedSite == 0) PORTAL_ENTRY else ACADEMIC_ENTRY)
        }
    }

    private fun finishConnected() {
        model.password = ""
        setResult(RESULT_OK)
        finish()
    }

    private fun closeOrReturn() {
        if (model.browser != null) {
            checkAgain = false
            checkJob?.cancel(); scopeWatcher?.cancel()
            model.browser?.destroy(); model.browser = null
            model.busy = false
            model.error = "学校验证尚未完成。"
            lifecycleScope.launch { SessionProbe.verify(session) }
        } else finish()
    }

    override fun onStop() {
        checkAgain = false
        loginJob?.cancel()
        checkJob?.cancel()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        // Loading may have finished while stopped or while the WebView was parked for rotation.
        model.browser?.onLoaded?.invoke()
    }

    override fun onDestroy() {
        scopeWatcher?.cancel(); checkJob?.cancel()
        if (isChangingConfigurations) model.browser?.park()
        else { model.browser?.destroy(); model.browser = null; discardPendingChallenge(); model.password = "" }
        super.onDestroy()
    }
}

@Composable
private fun CredentialScreen(model: LoginScreenModel, saved: SavedLoginStatus, onBack: () -> Unit,
    onSubmit: () -> Unit, onSaved: () -> Unit, onOfficial: () -> Unit, onContinue: () -> Unit) {
    val colors = CampusTheme.colors
    val scroll = MiuixScrollBehavior()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    Column(Modifier.fillMaxSize().background(colors.background).nestedScroll(scroll.nestedScrollConnection)) {
        CampusTopBar("登录学校账号", onBack = onBack, scrollBehavior = scroll, defaultWindowInsetsPadding = false)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = CampusSpacing.screenHorizontal)
            .padding(top = CampusSpacing.xs, bottom = CampusSpacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
            CampusPageEnter {
                CampusCard {
                    Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                        CampusCredentialField(model.account, { if (it.length <= 256) { model.account = it; model.accountEdited = true } }, "学号", enabled = !model.busy)
                        CampusCredentialField(model.password, { if (it.length <= 4096) { model.password = it; model.passwordEdited = true } }, "学校统一认证密码", password = true, enabled = !model.busy)
                        Text("账号密码加密保存在本机，登录时仅发送到学校认证页面。", fontSize = 12.sp, color = colors.textSecondary)
                        CampusButton(if (model.busy) "正在连接学校…" else "保存并登录", { focus.clearFocus(); keyboard?.hide(); onSubmit() },
                            modifier = Modifier.fillMaxWidth(), primary = true, enabled = !model.busy)
                    }
                }
            }
            if (model.busy) Row(horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Text("正在后台登录并检查门户、教务连接…", fontSize = 13.sp, color = colors.textSecondary)
            }
            model.error?.let { Text(it, fontSize = 13.sp, color = colors.error) }
            if (saved == SavedLoginStatus.ENABLED) CampusButton("使用已保存账号继续", onSaved,
                modifier = Modifier.fillMaxWidth(), enabled = !model.busy)
            if (model.pendingBrowser != null) CampusButton("继续学校验证", onContinue,
                modifier = Modifier.fillMaxWidth(), enabled = !model.busy)
            if (saved == SavedLoginStatus.PAUSED && !model.busy && model.error == null) Text("自动登录已暂停，可重试登录或继续学校验证。", fontSize = 13.sp, color = colors.warning)
            CampusButton("使用学校网页登录", onOfficial, modifier = Modifier.fillMaxWidth(), enabled = !model.busy)
        }
    }
}

@Composable
private fun ChallengeScreen(model: LoginScreenModel, browser: LoginBrowser, state: SessionState, target: Domain,
    onBack: () -> Unit, onCheck: () -> Unit, onSite: (Int) -> Unit, onRefresh: () -> Unit) {
    model.browserRevision // Observe browser callbacks without retaining webpage contents in Compose state.
    val colors = CampusTheme.colors
    Column(Modifier.fillMaxSize()) {
        CampusWebTopBar(title = browser.pageTarget?.title ?: "学校验证", host = android.net.Uri.parse(browser.web.url.orEmpty()).host.orEmpty(),
            refreshing = browser.loading, onBack = onBack,
            onRefresh = onRefresh, onClose = onBack)
        BoxWithConstraints(Modifier.weight(1f)) {
            val compact = maxHeight < 400.dp
            val keyboardSpace = maxHeight < 250.dp
            Column(Modifier.fillMaxSize().padding(horizontal = CampusSpacing.screenHorizontal).padding(bottom = CampusSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)) {
                if (!compact) Text("请完成学校页面上的验证码或其他验证。", fontSize = 13.sp, color = colors.textSecondary)
                if (!keyboardSpace && browser.pageTarget == null) CampusSegmentedControl(listOf("统一门户", "教务系统"), model.selectedSite, onSite)
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    AndroidView(factory = { browser.web }, modifier = Modifier.fillMaxSize())
                    browser.failure?.let { failure ->
                        Column(Modifier.fillMaxSize().background(colors.background).verticalScroll(rememberScrollState()).padding(CampusSpacing.md),
                            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(failure.message, color = colors.error)
                            CampusButton("重试打开网页", onRefresh, enabled = !model.busy)
                        }
                    }
                    if (browser.loading) Text("正在打开学校网页…", modifier = Modifier.align(Alignment.TopCenter)
                        .background(colors.surface).padding(CampusSpacing.xs), fontSize = 12.sp, color = colors.textSecondary)
                }
                if (!compact) {
                    model.error?.let { Text(it, fontSize = 13.sp, color = colors.warning) }
                    if (browser.pageTarget == null) Text("门户：${connectionLabel(state.portal)}  ·  教务：${connectionLabel(state.academic)}", fontSize = 12.sp, color = colors.textSecondary)
                }
                if (!keyboardSpace) CampusButton(when {
                    model.busy -> "正在检查连接…"
                    browser.pageTarget != null -> "完成验证，继续打开"
                    target == Domain.PORTAL && state.portal == DomainStatus.READY && state.academic != DomainStatus.READY -> "仅连接门户并返回"
                    else -> "完成验证，检查连接"
                }, onCheck, modifier = Modifier.fillMaxWidth(), primary = true, enabled = !model.busy && !browser.loading)
            }
        }
    }
}

private fun connectionLabel(status: DomainStatus) = when (status) {
    DomainStatus.READY -> "已连接"
    DomainStatus.UNREACHABLE -> "暂不可达"
    DomainStatus.UNVERIFIED -> "待检查"
    DomainStatus.AUTHENTICATING -> "认证中"
    else -> "未连接"
}

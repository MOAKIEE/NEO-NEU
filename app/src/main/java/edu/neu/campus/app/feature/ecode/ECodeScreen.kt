package edu.neu.campus.app.feature.ecode

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.WindowManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Scan
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.components.CampusSheetCloseAction
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.feature.services.OfficialService
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.contract.ECodeResult
import edu.neu.campus.contract.ECodeToken
import edu.neu.campus.ecode.ECodeSsoConnector
import edu.neu.campus.ecode.OfficialECodeRepository
import edu.neu.campus.ui.components.CampusButton
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusSwitch
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun ECodeScreen(onBack: () -> Unit) {
    val colors = CampusTheme.colors
    val scroll = MiuixScrollBehavior()
    Column(Modifier.fillMaxSize().background(colors.background).nestedScroll(scroll.nestedScrollConnection)) {
        CampusTopBar(title = "e 码通", onBack = onBack, scrollBehavior = scroll)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = CampusSpacing.screenHorizontal)
                .padding(top = CampusSpacing.xs, bottom = CampusSpacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
        ) {
            CampusCard(contentPadding = PaddingValues(CampusSpacing.xl)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)) {
                    CampusIconBadge(MiuixIcons.Regular.Scan, colors.cardForeground, colors.cardContainer)
                    Text("校园动态码", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(CampusSpacing.xl))
                ECodeCodePanel(onAuthenticate = {
                    AppNavigator.navigateTo(AppDestination.OfficialWeb(OfficialService.ECODE))
                })
            }
            CampusCard {
                if (LocalDensity.current.fontScale >= 1.5f) {
                    Text("首页悬浮入口", color = colors.textPrimary, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(CampusSpacing.sm))
                    CampusSwitch(checked = ECodePreferences.showFloatingBall, onCheckedChange = ECodePreferences::setFloatingBall)
                } else CampusRow(
                    title = "首页悬浮入口",
                    subtitle = "拖动调整位置，闲置时自动收至侧边",
                    trailingContent = {
                        CampusSwitch(checked = ECodePreferences.showFloatingBall, onCheckedChange = ECodePreferences::setFloatingBall)
                    }
                )
            }
        }
    }
}

/** The ball exists only over the Today tab. Its dialog owns and clears its dynamic code. */
@Composable
fun ECodeFloatingOverlay() {
    val colors = CampusTheme.colors
    var open by remember { mutableStateOf(false) }
    ECodeFloatingBall(onClick = { open = true }, expanded = open)
    if (open) {
        Dialog(onDismissRequest = { open = false }, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)) {
            CampusCard(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("e 码通", modifier = Modifier.weight(1f), color = colors.textPrimary)
                        CampusSheetCloseAction(onClick = { open = false })
                    }
                    ECodeCodePanel(onAuthenticate = {
                        open = false
                        AppNavigator.navigateTo(AppDestination.OfficialWeb(OfficialService.ECODE))
                    })
                }
            }
        }
    }
}

@Composable
private fun ECodeCodePanel(onAuthenticate: () -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val repository = remember(context.applicationContext) { OfficialECodeRepository(context.applicationContext) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = CampusTheme.colors
    var foreground by remember { mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    var generation by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ECodeResult?>(null) }

    DisposableEffect(activity) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                foreground = false
                result = null
            } else if (event == Lifecycle.Event.ON_START) {
                foreground = true
                generation++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            result = null
        }
    }

    LaunchedEffect(foreground, generation) {
        if (!foreground) return@LaunchedEffect
        var attemptedSso = false
        while (true) {
            result = null
            var fetched = repository.fetch()
            if (fetched == ECodeResult.LoginRequired && !attemptedSso && activity != null &&
                CampusDataProvider.session.state.value.portal == DomainStatus.READY
            ) {
                attemptedSso = true
                fetched = ECodeSsoConnector.connect(activity, repository)
            }
            if (fetched is ECodeResult.Ready) {
                val remaining = fetched.token.remainingMillisAt(SystemClock.elapsedRealtime())
                if (remaining <= 750L) {
                    result = ECodeResult.Unavailable
                    break
                }
                result = fetched
                // Remove the code before expiry, then fetch a fresh one. Never show stale codes.
                delay(remaining - 750L)
                result = null
            } else {
                result = fetched
                break
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp).padding(vertical = CampusSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.md, Alignment.CenterVertically)
    ) {
        when (val state = result) {
            is ECodeResult.Ready -> {
                ECodeQr(state.token)
                Text("动态码自动更新", color = colors.textSecondary, fontSize = 12.sp)
            }
            ECodeResult.LoginRequired -> {
                Text("认证后出示校园码", color = colors.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Text("请在学校官方页面完成认证", color = colors.textSecondary, textAlign = TextAlign.Center)
                CampusButton(text = "打开官方认证", onClick = onAuthenticate, primary = true)
            }
            ECodeResult.Unavailable -> {
                Text("暂时无法获取二维码，请检查网络", color = colors.textSecondary, textAlign = TextAlign.Center)
                CampusButton(text = "重试", onClick = { generation++ })
            }
            ECodeResult.InvalidResponse -> {
                Text("学校二维码数据暂时无法识别", color = colors.textSecondary, textAlign = TextAlign.Center)
                CampusButton(text = "重试", onClick = { generation++ })
            }
            null -> {
                CircularProgressIndicator()
                Text("正在获取二维码…", color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ECodeQr(token: ECodeToken) {
    val bitmap = remember(token.payload) { createQrBitmap(token.payload) }
    if (bitmap == null) {
        Text("二维码绘制失败", color = CampusTheme.colors.warning)
    } else {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "e 码通动态二维码",
            modifier = Modifier.widthIn(max = 240.dp).fillMaxWidth().aspectRatio(1f)
        )
    }
}

private fun createQrBitmap(payload: String): Bitmap? = runCatching {
    val matrix = QRCodeWriter().encode(
        payload, BarcodeFormat.QR_CODE, 640, 640,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H, EncodeHintType.MARGIN to 4)
    )
    val pixels = IntArray(matrix.width * matrix.height) { index ->
        if (matrix[index % matrix.width, index / matrix.width]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
        setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
    }
}.getOrNull()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

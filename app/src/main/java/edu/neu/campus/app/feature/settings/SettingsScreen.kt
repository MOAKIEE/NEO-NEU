package edu.neu.campus.app.feature.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.feature.balance.BalancePrivacyManager
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.AppThemeMode
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Theme

/** 我的：账号概览、偏好、隐私。次要说明按需展开，退出操作只保留一个入口。 */
@Composable
fun SettingsScreen(
    onLoginClick: () -> Unit,
    connectingAcademic: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = CampusTheme.colors
    val sessionState by CampusDataProvider.session.state.collectAsState()
    var showSignOutConfirm by rememberSaveable { mutableStateOf(false) }
    var showThemeOptions by rememberSaveable { mutableStateOf(false) }
    var showAbout by rememberSaveable { mutableStateOf(false) }
    var checkingConnection by remember { mutableStateOf(false) }
    var signingOut by remember { mutableStateOf(false) }
    val themeModes = listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT, AppThemeMode.DARK)
    val fullyConnected = sessionState.portal == DomainStatus.READY && sessionState.academic == DomainStatus.READY
    val partlyConnected = sessionState.portal == DomainStatus.READY || sessionState.academic == DomainStatus.READY
    val pageScrollBehavior = MiuixScrollBehavior()

    Column(
        modifier = modifier.fillMaxSize().background(colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            title = "我的",
            scrollBehavior = pageScrollBehavior
        )
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                .padding(horizontal = CampusSpacing.screenHorizontal)
                .padding(top = CampusSpacing.xs, bottom = CampusSpacing.xxl),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.xl)
        ) {
            CampusCard {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.md)
                ) {
                    CampusIconBadge(
                        icon = MiuixIcons.Regular.Contacts,
                        tint = colors.brand,
                        container = colors.brandContainer,
                        size = 56.dp,
                        iconSize = 28.dp,
                        cornerRadius = CampusShapes.medium
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(CampusSpacing.xxs)) {
                        Text("学校账号", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                        Text(
                            text = when {
                                fullyConnected -> "学校连接正常"
                                partlyConnected -> "部分服务已连接"
                                else -> "登录后查看校园信息"
                            },
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                Spacer(Modifier.height(CampusSpacing.md))
                CampusGroupDivider()
                Spacer(Modifier.height(CampusSpacing.xs))
                DomainStatusRow("统一门户", sessionState.portal)
                DomainStatusRow("教务系统", if (connectingAcademic) DomainStatus.AUTHENTICATING else sessionState.academic)
                Spacer(Modifier.height(CampusSpacing.sm))
                CampusButton(
                    text = when {
                        connectingAcademic -> "正在连接教务系统…"
                        fullyConnected -> "重新认证学校账号"
                        sessionState.portal == DomainStatus.READY -> "连接教务系统"
                        else -> "登录学校账号"
                    },
                    onClick = onLoginClick,
                    modifier = Modifier.fillMaxWidth(),
                    primary = !fullyConnected,
                    enabled = !connectingAcademic
                )
                Spacer(Modifier.height(CampusSpacing.xxs))
                CampusRow(
                    title = if (checkingConnection) "正在检查连接…" else "检查连接",
                    enabled = !checkingConnection && !connectingAcademic && sessionState.accountScope != null,
                    showChevron = true,
                    onClick = {
                        if (!checkingConnection) {
                            checkingConnection = true
                            coroutineScope.launch {
                                try {
                                    CampusDataProvider.session.verify()
                                    val current = CampusDataProvider.session.state.value
                                    val message = when {
                                        current.portal == DomainStatus.READY && current.academic == DomainStatus.READY -> "门户与教务均已连接"
                                        current.portal == DomainStatus.READY -> "门户已连接，教务仍需登录或检查网络"
                                        current.academic == DomainStatus.READY -> "教务已连接，门户仍需登录或检查网络"
                                        else -> "连接尚未恢复，请检查网络或重新登录"
                                    }
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                } finally {
                                    checkingConnection = false
                                }
                            }
                        }
                    }
                )
            }

            SettingsSection(title = "偏好设置") {
                CampusCard {
                    CampusRow(
                        title = "外观主题",
                        leading = { SettingsIcon(MiuixIcons.Regular.Theme) },
                        trailingText = ThemeManager.currentMode.displayName,
                        showChevron = true,
                        onClick = { showThemeOptions = !showThemeOptions }
                    )
                    AnimatedVisibility(visible = showThemeOptions) {
                        Column(modifier = Modifier.padding(bottom = CampusSpacing.sm)) {
                            CampusSegmentedControl(
                                options = themeModes.map { it.displayName },
                                selectedIndex = themeModes.indexOf(ThemeManager.currentMode).coerceAtLeast(0),
                                onSelect = { ThemeManager.setMode(themeModes[it]) }
                            )
                        }
                    }
                    CampusGroupDivider(startIndent = SettingsTextIndent)
                    CampusRow(
                        title = "首页布局",
                        leading = { SettingsIcon(MiuixIcons.Regular.GridView) },
                        showChevron = true,
                        onClick = { AppNavigator.navigateTo(AppDestination.HomeSettings) }
                    )
                }
            }

            SettingsSection(title = "数据与隐私") {
                CampusCard {
                    CampusRow(
                        title = "隐藏余额",
                        leading = { SettingsIcon(MiuixIcons.Regular.Hide) },
                        trailingContent = {
                            CampusSwitch(
                                checked = BalancePrivacyManager.isBalanceMasked,
                                onCheckedChange = { BalancePrivacyManager.setMasked(it) }
                            )
                        }
                    )
                    CampusGroupDivider(startIndent = SettingsTextIndent)
                    CampusRow(
                        title = "退出并清除本地数据",
                        leading = { SettingsIcon(MiuixIcons.Regular.Lock) },
                        showChevron = true,
                        onClick = { showSignOutConfirm = true }
                    )
                }
            }

            CampusCard {
                CampusRow(
                    title = "关于 NEO NEU",
                    leading = { SettingsIcon(MiuixIcons.Regular.Info) },
                    showChevron = true,
                    onClick = { showAbout = !showAbout }
                )
                AnimatedVisibility(visible = showAbout) {
                    Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)) {
                        CampusGroupDivider(startIndent = SettingsTextIndent)
                        // 说明文字与上方行标题同一起点，不再缩回到图标下方。
                        Column(
                            modifier = Modifier.padding(start = SettingsTextIndent, bottom = CampusSpacing.xs),
                            verticalArrangement = Arrangement.spacedBy(CampusSpacing.xxs)
                        ) {
                            Text("版本：1.0.0", fontSize = 13.sp, color = colors.textPrimary)
                            Text(
                                "登录通过学校官方页面完成，本应用不保存你的密码。\n仅提供信息查询，不选课、不支付、不提交申请。",
                                fontSize = 12.sp,
                                lineHeight = 20.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    top.yukonga.miuix.kmp.overlay.OverlayDialog(
        show = showSignOutConfirm,
        title = "退出并清除本地数据",
        summary = "将退出学校账号，清除本机学校会话和当前账号查询缓存。不会修改学校数据；再次查询需要重新登录。",
        onDismissRequest = { if (!signingOut) showSignOutConfirm = false }
    ) {
        // 与 Miuix 对话框一致：取消在左、确认在右，两个按钮等宽并排。
        Row(horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)) {
            CampusButton(
                text = "取消",
                onClick = { showSignOutConfirm = false },
                modifier = Modifier.weight(1f),
                enabled = !signingOut
            )
            CampusButton(
                text = if (signingOut) "正在清除…" else "退出并清除",
                onClick = {
                    if (!signingOut) {
                        signingOut = true
                        coroutineScope.launch {
                            try {
                                CampusDataProvider.session.signOut()
                                showSignOutConfirm = false
                                Toast.makeText(context, "已退出并清除本地数据", Toast.LENGTH_SHORT).show()
                            } finally {
                                signingOut = false
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                primary = true,
                enabled = !signingOut
            )
        }
    }
}

/** 设置项图标尺寸。 */
private val SettingsIconSize = 38.dp

/** 设置项标题相对卡片内容的起点：图标宽度加 [CampusRow] 的图文间距，分隔线与说明文字都对齐到这里。 */
private val SettingsTextIndent = SettingsIconSize + CampusSpacing.sm

/** 本页使用中性色图标，只在账号和连接状态上强调颜色。 */
@Composable
private fun SettingsIcon(icon: ImageVector) {
    CampusIconBadge(
        icon = icon,
        tint = CampusTheme.colors.textSecondary,
        container = CampusTheme.colors.surfaceMuted,
        size = SettingsIconSize,
        iconSize = 20.dp,
        cornerRadius = CampusShapes.extraSmall
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)) {
        // 分组标题与页面大标题、卡片左缘同在 screenHorizontal 起点，不再额外内缩。
        Text(
            title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = CampusTheme.colors.textSecondary
        )
        content()
    }
}

@Composable
private fun DomainStatusRow(name: String, status: DomainStatus) {
    val colors = CampusTheme.colors
    val (statusText, statusColor) = when (status) {
        DomainStatus.READY -> "已连接" to colors.success
        DomainStatus.SIGNED_OUT -> "未登录" to colors.textSecondary
        DomainStatus.AUTHENTICATING -> "认证中…" to colors.brand
        DomainStatus.UNVERIFIED -> "待复验" to colors.warning
        DomainStatus.EXPIRED -> "连接已过期" to colors.warning
        DomainStatus.UNREACHABLE -> "暂不可达" to colors.textSecondary
    }
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = CampusSpacing.xxs),
        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.xxs)
    ) {
        Text(name, modifier = Modifier.weight(1f), fontSize = 13.sp, color = colors.textSecondary)
        Text(statusText, fontSize = 13.sp, color = statusColor)
    }
}

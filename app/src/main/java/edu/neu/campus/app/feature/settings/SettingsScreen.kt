package edu.neu.campus.app.feature.settings

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.feature.balance.BalancePrivacyManager
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusSegmentedControl
import edu.neu.campus.ui.components.CampusSwitch
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.AppThemeMode
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Backup
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Theme

/**
 * 我的与设置页。
 *
 * 约定：
 * - 顶部大标题 + 身份卡，身份卡使用品牌柔化渐变表达「账号」层级
 * - 列表项统一使用 [CampusRow]，分区标题使用 [CampusSection]
 * - 外观切换使用分段控件（带弹簧位移），开关使用 Miuix [CampusSwitch]
 * - 危险操作（退出/清数据）单独成组并以错误色强调
 */
@Composable
fun SettingsScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val colors = CampusTheme.colors
    val sessionState by CampusDataProvider.session.state.collectAsState()

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    val isConnected = sessionState.portal == DomainStatus.READY || sessionState.academic == DomainStatus.READY
    val themeModes = listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT, AppThemeMode.DARK)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = CampusSpacing.screenHorizontal,
                    end = CampusSpacing.screenHorizontal,
                    top = CampusSpacing.sm,
                    bottom = CampusSpacing.xs
                )
        ) {
            Text(
                text = "我的",
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Text(
                text = if (isConnected) "学校连接可用" else "未连接学校账号",
                fontSize = 14.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(
                    start = CampusSpacing.screenHorizontal,
                    end = CampusSpacing.screenHorizontal,
                    top = CampusSpacing.xs,
                    bottom = CampusSpacing.xxl
                ),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.lg)
        ) {
            // 1. 身份卡
            StaggeredAppear(index = 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CampusShapes.extraLarge))
                        .background(
                            Brush.linearGradient(
                                listOf(colors.brandContainer, colors.brandContainer.copy(alpha = 0.6f))
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = colors.brandBorder,
                            shape = RoundedCornerShape(CampusShapes.extraLarge)
                        )
                        .padding(CampusSpacing.lg)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.md)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(colors.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Contacts,
                                contentDescription = "用户头像",
                                tint = colors.brand,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "学校账号",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (isConnected) "会话有效，可正常查询" else "请查看下方连接状态",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                            CampusPill(
                                text = "校园信息查询",
                                contentColor = colors.brand,
                                containerColor = colors.surface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // 2. 连接状态
            StaggeredAppear(index = 1) {
                CampusSection(
                    title = "系统连接状态",
                    actionText = "检查连接",
                    onActionClick = {
                        coroutineScope.launch {
                            CampusDataProvider.session.verify()
                            Toast.makeText(context, "正在检查连接...", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    CampusGroup {
                        DomainStatusRow(name = "智慧东大统一门户", status = sessionState.portal)
                        CampusGroupDivider(startIndent = 26.dp)
                        DomainStatusRow(name = "教务综合管理系统", status = sessionState.academic)

                        Spacer(modifier = Modifier.height(CampusSpacing.xs))

                        Button(
                            onClick = onLoginClick,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("登录学校官方账号")
                        }
                    }
                }
            }

            // 3. 界面与显示
            StaggeredAppear(index = 2) {
                CampusSection(title = "界面与显示") {
                    CampusCard {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                        ) {
                            CampusIconBadge(
                                icon = MiuixIcons.Regular.Theme,
                                tint = colors.gradeForeground,
                                container = colors.gradeContainer
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "外观主题",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "跟随系统或固定深浅色",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(CampusSpacing.sm))

                        CampusSegmentedControl(
                            options = themeModes.map { it.displayName },
                            selectedIndex = themeModes.indexOf(ThemeManager.currentMode).coerceAtLeast(0),
                            onSelect = { index -> ThemeManager.setMode(themeModes[index]) }
                        )
                    }

                    Spacer(modifier = Modifier.height(CampusSpacing.sm))

                    CampusGroup {
                        CampusRow(
                            title = "首页布局与模块配置",
                            subtitle = "自定义快捷入口与首页模块展示",
                            leading = {
                                CampusIconBadge(
                                    icon = MiuixIcons.Regular.GridView,
                                    tint = colors.timetableForeground,
                                    container = colors.timetableContainer,
                                    size = 38.dp,
                                    iconSize = 20.dp,
                                    cornerRadius = CampusShapes.extraSmall
                                )
                            },
                            showChevron = true,
                            onClick = { AppNavigator.navigateTo(AppDestination.HomeSettings) }
                        )
                        CampusGroupDivider(startIndent = 50.dp)
                        CampusRow(
                            title = "默认遮罩资产余额",
                            subtitle = "在首页和详情页默认遮罩校园卡与网费金额",
                            leading = {
                                CampusIconBadge(
                                    icon = MiuixIcons.Regular.Hide,
                                    tint = colors.cardForeground,
                                    container = colors.cardContainer,
                                    size = 38.dp,
                                    iconSize = 20.dp,
                                    cornerRadius = CampusShapes.extraSmall
                                )
                            },
                            trailingContent = {
                                CampusSwitch(
                                    checked = BalancePrivacyManager.isBalanceMasked,
                                    onCheckedChange = { BalancePrivacyManager.setMasked(it) }
                                )
                            }
                        )
                    }
                }
            }

            // 4. 数据与隐私
            StaggeredAppear(index = 3) {
                CampusSection(title = "数据与隐私") {
                    CampusGroup {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                        ) {
                            CampusIconBadge(
                                icon = MiuixIcons.Regular.Backup,
                                tint = colors.networkForeground,
                                container = colors.networkContainer,
                                size = 38.dp,
                                iconSize = 20.dp,
                                cornerRadius = CampusShapes.extraSmall
                            )
                            Text(
                                text = "最近查看的课表、成绩和校园生活信息会保存在本机，网络不可用时仍可查看。",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 18.sp
                            )
                        }

                        CampusGroupDivider(startIndent = 50.dp)

                        CampusRow(
                            title = "退出并清除本地数据",
                            subtitle = "会话与当前账号缓存都会清除",
                            leading = {
                                CampusIconBadge(
                                    icon = MiuixIcons.Regular.Lock,
                                    tint = colors.error,
                                    container = colors.errorContainer,
                                    size = 38.dp,
                                    iconSize = 20.dp,
                                    cornerRadius = CampusShapes.extraSmall
                                )
                            },
                            showChevron = true,
                            onClick = { showClearCacheConfirm = true }
                        )
                    }
                }
            }

            // 5. 关于
            StaggeredAppear(index = 4) {
                CampusSection(title = "关于 NEO NEU") {
                    CampusGroup {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                        ) {
                            CampusIconBadge(
                                icon = MiuixIcons.Regular.Info,
                                tint = colors.brand,
                                container = colors.brandContainer,
                                size = 38.dp,
                                iconSize = 20.dp,
                                cornerRadius = CampusShapes.extraSmall
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "版本：1.0.0",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "登录通过学校官方页面完成，本应用不保存你的密码。",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    lineHeight = 18.sp
                                )
                            }
                        }

                        CampusGroupDivider(startIndent = 50.dp)

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                        ) {
                            CampusIconBadge(
                                icon = MiuixIcons.Regular.Link,
                                tint = colors.textSecondary,
                                container = colors.surfaceMuted,
                                size = 38.dp,
                                iconSize = 20.dp,
                                cornerRadius = CampusShapes.extraSmall
                            )
                            Text(
                                text = "只读查询：不选课、不支付、不提交任何申请。",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // 6. 危险操作
            StaggeredAppear(index = 5) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .tapScale(onClick = { showSignOutConfirm = true }, pressedScale = 0.98f, clipShape = RoundedCornerShape(CampusShapes.large))
                        .background(colors.errorContainer)
                        .border(
                            width = 1.dp,
                            color = colors.error.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(CampusShapes.large)
                        )
                        .padding(CampusSpacing.md),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "退出当前学校账号",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.error
                    )
                }
            }
        }
    }

    top.yukonga.miuix.kmp.overlay.OverlayDialog(
        show = showSignOutConfirm || showClearCacheConfirm,
        title = "退出并清除本地数据",
        summary = "将退出学校账号，清除本机学校会话和当前账号查询缓存。不会修改学校数据；再次查询需要重新登录。",
        onDismissRequest = { showSignOutConfirm = false; showClearCacheConfirm = false }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)) {
            Button(
                onClick = {
                    coroutineScope.launch {
                        CampusDataProvider.session.signOut()
                        showSignOutConfirm = false
                        showClearCacheConfirm = false
                        Toast.makeText(context, "已退出并清除本地数据", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("退出并清除") }
            Button(
                onClick = { showSignOutConfirm = false; showClearCacheConfirm = false },
                modifier = Modifier.fillMaxWidth()
            ) { Text("取消") }
        }
    }
}

@Composable
private fun DomainStatusRow(
    name: String,
    status: DomainStatus,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val (statusText, statusColor) = when (status) {
        DomainStatus.READY -> "已连接" to colors.success
        DomainStatus.SIGNED_OUT -> "未登录" to colors.textTertiary
        DomainStatus.AUTHENTICATING -> "认证中…" to colors.brand
        DomainStatus.UNVERIFIED -> "待复验" to colors.warning
        DomainStatus.EXPIRED -> "连接已过期" to colors.warning
        DomainStatus.UNREACHABLE -> "暂不可达" to colors.textTertiary
    }
    val animatedColor by animateColorAsState(
        targetValue = statusColor,
        animationSpec = tween(CampusMotion.Duration.medium),
        label = "statusColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = CampusSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            color = colors.textPrimary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
            )
            Text(
                text = statusText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = animatedColor
            )
        }
    }
}

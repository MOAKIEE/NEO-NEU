package edu.neu.campus.app.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.feature.balance.BalancePrivacyManager
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.contract.DomainStatus
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.AppThemeMode
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import edu.neu.campus.ui.components.CampusSwitch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * “我的”与设置页面。
 * 遵循 docs/06-UI页面布局设计.md 第 11 节要求：
 * - 身份卡（通用头像、脱敏账号、学生身份）
 * - 门户与教务系统两域连接状态，提供重新登录与复验入口
 * - 显示偏好（外观深浅色、首页布局自定义、余额隐私隐藏）
 * - 数据与隐私（本地 Room 缓存概览、演示模式切换、缓存清除）
 * - 关于应用与退出账号二次确认
 */
@Composable
fun SettingsScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionState by CampusDataProvider.session.state.collectAsState()

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = "我的"
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 身份卡
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MiuixTheme.colorScheme.surfaceContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "用户头像",
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "东北大学在校生",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        )

                        val scope = sessionState.accountScope
                        val accountDisplay = if (scope != null && scope.length > 6) {
                            "账号作用域: ${scope.take(4)}****${scope.takeLast(2)}"
                        } else if (scope != null) {
                            "账号作用域: $scope"
                        } else {
                            "未连接学校账号"
                        }

                        Text(
                            text = accountDisplay,
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "全日制本硕博在读",
                                fontSize = 10.sp,
                                color = MiuixTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // 2. 账号与两域连接状态
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "学校系统连接状态",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface
                        )

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    CampusDataProvider.session.verify()
                                    Toast.makeText(context, "正在复验会话...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "复验",
                                tint = MiuixTheme.colorScheme.primary
                            )
                        }
                    }

                    DomainStatusRow(
                        name = "智慧东大统一门户",
                        status = sessionState.portal
                    )

                    DomainStatusRow(
                        name = "教务综合管理系统",
                        status = sessionState.academic
                    )

                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("登录学校官方账号")
                    }
                }
            }

            // 3. 显示偏好分组
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "显示与界面偏好",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    // 外观深浅色模式切换
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "外观主题",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                AppThemeMode.SYSTEM to "跟随系统",
                                AppThemeMode.LIGHT to "浅色模式",
                                AppThemeMode.DARK to "深色模式"
                            ).forEach { (mode, label) ->
                                val isSel = ThemeManager.currentMode == mode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer)
                                        .clickable { ThemeManager.setMode(mode) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // 首页布局配置入口（第 15 节扩展规则）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { AppNavigator.navigateTo(AppDestination.HomeSettings) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "首页布局与模块配置",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "自定义快捷查询按钮与首页摘要卡片排序",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        Text(
                            text = "去配置 ›",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }

                    // 余额默认隐私隐藏开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "默认遮罩资产余额",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "在首页和详情页默认遮罩校园卡与网费金额",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        CampusSwitch(
                            checked = BalancePrivacyManager.isBalanceMasked,
                            onCheckedChange = { BalancePrivacyManager.setMasked(it) }
                        )
                    }
                }
            }

            // 4. 数据与隐私分组
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "数据与本地缓存",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "已连接的学期课表、最近一次查询的成绩及校园生活资产，均采用 Android Room 本地私有数据库缓存，离线时仍可随时查看。",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        lineHeight = 17.sp
                    )

                    // 演示数据模式开关
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "演示数据模式",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "切换至本地隔离的合成测试样本，带显眼警示横幅",
                                fontSize = 11.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        CampusSwitch(
                            checked = CampusDataProvider.isDemoMode,
                            onCheckedChange = { CampusDataProvider.isDemoMode = it }
                        )
                    }

                    // 清除缓存按钮
                    Button(
                        onClick = { showClearCacheConfirm = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("清除本地缓存数据")
                    }
                }
            }

            // 5. 关于与合规
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "关于 NEO NEU",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "版本：1.0.0 · 智慧东大自用查询客户端",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "技术栈：Kotlin + Jetpack Compose + Miuix 0.9.4",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Text(
                        text = "纯原生架构渲染，不做 WebView 套壳；认证完全在学校官方登录页面完成，客户端不保存用户密码。",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

            // 6. 退出账号按钮
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSignOutConfirm = true }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "退出当前学校账号",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    // 退出确认对话框
    if (showSignOutConfirm) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background.copy(alpha = 0.6f))
                .clickable { showSignOutConfirm = false }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "退出登录确认",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "退出登录将清除本机的学校会话状态与本地账号作用域。重新登录前，需要重新通过官方认证页面输入密码。",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        lineHeight = 19.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    CampusDataProvider.session.signOut()
                                    showSignOutConfirm = false
                                    Toast.makeText(context, "已安全退出当前账号", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("确认退出")
                        }
                        Button(
                            onClick = { showSignOutConfirm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                    }
                }
            }
        }
    }

    // 清除本地缓存确认对话框
    if (showClearCacheConfirm) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.background.copy(alpha = 0.6f))
                .clickable { showClearCacheConfirm = false }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "清除缓存确认",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "这将清理本机本地存储的离线课表和历史成绩快照，不会影响学校服务端的任何数据。清理后需重新联网同步。",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        lineHeight = 19.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    CampusDataProvider.session.signOut()
                                    showClearCacheConfirm = false
                                    Toast.makeText(context, "本地缓存已清理", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("确认清除")
                        }
                        Button(
                            onClick = { showClearCacheConfirm = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("取消")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DomainStatusRow(
    name: String,
    status: DomainStatus,
    modifier: Modifier = Modifier
) {
    val (statusText, statusColor) = when (status) {
        DomainStatus.READY -> "已连接" to MiuixTheme.colorScheme.primary
        DomainStatus.SIGNED_OUT -> "未登录" to MiuixTheme.colorScheme.onSurfaceSecondary
        DomainStatus.AUTHENTICATING -> "认证中…" to MiuixTheme.colorScheme.primary
        DomainStatus.UNVERIFIED -> "待复验" to MiuixTheme.colorScheme.onSurfaceSecondary
        DomainStatus.EXPIRED -> "连接已过期" to MiuixTheme.colorScheme.primary
        DomainStatus.UNREACHABLE -> "暂不可达" to MiuixTheme.colorScheme.onSurfaceSecondary
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurface
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(statusColor)
            )

            Text(
                text = statusText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }
}

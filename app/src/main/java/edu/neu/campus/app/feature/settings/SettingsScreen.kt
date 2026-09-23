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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusSwitch
import edu.neu.campus.ui.theme.AppThemeMode
import edu.neu.campus.ui.theme.LocalCampusColors
import edu.neu.campus.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun SettingsScreen(
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val campusColors = LocalCampusColors.current
    val sessionState by CampusDataProvider.session.state.collectAsState()

    var showSignOutConfirm by remember { mutableStateOf(false) }
    var showClearCacheConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(campusColors.background)
    ) {
        // 顶部大标题
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 12.dp)
        ) {
            Text(
                text = "我的与设置",
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = campusColors.textPrimary
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 身份卡
            Card(
                insideMargin = PaddingValues(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(campusColors.brandContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "用户头像",
                            tint = campusColors.brand,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "东北大学在校生",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = campusColors.textPrimary
                        )

                        Text(
                            text = if (sessionState.accountScope == null) "未登录学校账号" else "已登录学校账号",
                            fontSize = 13.sp,
                            color = campusColors.textSecondary
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(campusColors.surfaceMuted)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "全日制本硕博在读",
                                fontSize = 11.sp,
                                color = campusColors.brand,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // 2. 账号与两域连接状态
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
                    DomainStatusRow(
                        name = "智慧东大统一门户",
                        status = sessionState.portal
                    )
                    CampusGroupDivider()
                    DomainStatusRow(
                        name = "教务综合管理系统",
                        status = sessionState.academic
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("登录学校官方账号")
                    }
                }
            }

            // 3. 显示与界面偏好
            CampusSection(title = "界面与显示") {
                CampusGroup {
                    // 外观深浅色模式切换
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "外观主题",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = campusColors.textPrimary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(campusColors.surfaceMuted)
                                .padding(4.dp)
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSel) campusColors.surface else Color.Transparent)
                                        .clickable { ThemeManager.setMode(mode) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) campusColors.brand else campusColors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    CampusGroupDivider()

                    // 首页布局配置入口
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { AppNavigator.navigateTo(AppDestination.HomeSettings) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "首页布局与模块配置",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = campusColors.textPrimary
                            )
                            Text(
                                text = "自定义快捷入口与首页模块展示",
                                fontSize = 12.sp,
                                color = campusColors.textSecondary
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "去配置",
                            tint = campusColors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    CampusGroupDivider()

                    // 余额默认隐私隐藏开关
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "默认遮罩资产余额",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = campusColors.textPrimary
                            )
                            Text(
                                text = "在首页和详情页默认遮罩校园卡与网费金额",
                                fontSize = 12.sp,
                                color = campusColors.textSecondary
                            )
                        }

                        CampusSwitch(
                            checked = BalancePrivacyManager.isBalanceMasked,
                            onCheckedChange = { BalancePrivacyManager.setMasked(it) }
                        )
                    }
                }
            }

            // 4. 数据与本地缓存
            CampusSection(title = "数据与隐私") {
                CampusGroup {
                    Text(
                        text = "最近查看的课表、成绩和校园生活信息会保存在本机，网络不可用时仍可查看。",
                        fontSize = 12.sp,
                        color = campusColors.textSecondary,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    CampusGroupDivider()

                    // 清除缓存按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showClearCacheConfirm = true }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "清除本地缓存数据",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = campusColors.textPrimary
                        )

                        Text(
                            text = "清理 ›",
                            fontSize = 13.sp,
                            color = campusColors.brand
                        )
                    }
                }
            }

            // 5. 关于与合规
            CampusSection(title = "关于 NEO NEU") {
                CampusGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "版本：1.0.0",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = campusColors.textPrimary
                        )
                        Text(
                            text = "登录通过学校官方页面完成，本应用不保存你的密码。",
                            fontSize = 12.sp,
                            color = campusColors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // 6. 退出账号操作
            CampusGroup(
                modifier = Modifier.clickable { showSignOutConfirm = true }
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "退出当前学校账号",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = campusColors.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // 退出确认对话框
    if (showSignOutConfirm) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showSignOutConfirm = false }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                insideMargin = PaddingValues(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "退出登录确认",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = campusColors.textPrimary
                    )
                    Text(
                        text = "退出后需要重新通过学校官方页面登录。",
                        fontSize = 13.sp,
                        color = campusColors.textPrimary,
                        lineHeight = 20.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    CampusDataProvider.session.signOut()
                                    showSignOutConfirm = false
                                    Toast.makeText(context, "已安全退出当前账号", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
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
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showClearCacheConfirm = false }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                insideMargin = PaddingValues(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "清除缓存确认",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = campusColors.textPrimary
                    )
                    Text(
                        text = "这将清除本机保存的离线课表和历史成绩，不会影响学校网站上的数据。清理后需联网重新获取。",
                        fontSize = 13.sp,
                        color = campusColors.textPrimary,
                        lineHeight = 20.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    CampusDataProvider.session.signOut()
                                    showClearCacheConfirm = false
                                    Toast.makeText(context, "本地缓存已清理", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
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
    val campusColors = LocalCampusColors.current
    val (statusText, statusColor) = when (status) {
        DomainStatus.READY -> "已连接" to campusColors.success
        DomainStatus.SIGNED_OUT -> "未登录" to campusColors.textSecondary
        DomainStatus.AUTHENTICATING -> "认证中…" to campusColors.brand
        DomainStatus.UNVERIFIED -> "待复验" to campusColors.warning
        DomainStatus.EXPIRED -> "连接已过期" to campusColors.warning
        DomainStatus.UNREACHABLE -> "暂不可达" to campusColors.textSecondary
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            fontSize = 14.sp,
            color = campusColors.textPrimary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
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

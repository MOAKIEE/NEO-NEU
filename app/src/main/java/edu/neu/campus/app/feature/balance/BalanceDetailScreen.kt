package edu.neu.campus.app.feature.balance

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import edu.neu.campus.ui.components.TimeFormatter
import edu.neu.campus.contract.BalanceKind
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 校园生活资产余额详情页（校园卡 / 校园网）。
 * 遵循 docs/06-UI页面布局设计.md 第 8 节要求：
 * - 顶部主余额大卡，支持眼睛切换显隐（全局偏好联动），注明更新时间与来源
 * - 来源与账户状态说明，仅提供只读安全查询，不包含直接充值扣款按钮
 * - 提供明确的官方圈存与充值途径指南
 */
@Composable
fun BalanceDetailScreen(
    kind: BalanceKind,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val balanceSnapshot by CampusDataProvider.portal.balance(kind).collectAsState()

    val isCard = kind == BalanceKind.CAMPUS_CARD
    val pageTitle = if (isCard) "校园卡" else "网费"
    val accountTitle = if (isCard) "校园卡主账户余额" else "校园网账户余额"
    val sourceName = if (isCard) "东北大学一卡通服务中心" else "东北大学网络综合自服务平台"

    val isRefreshing = balanceSnapshot.phase == QueryPhase.LOADING
    val isMasked = BalancePrivacyManager.isBalanceMasked

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        edu.neu.campus.ui.components.CampusTopBar(
            title = pageTitle,
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            CampusDataProvider.portal.refreshBalance(kind)
                        }
                    },
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "刷新",
                        tint = if (isRefreshing) MiuixTheme.colorScheme.onSurfaceSecondary else MiuixTheme.colorScheme.primary
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 离线/缓存更新提示条
            if (balanceSnapshot.isStale) {
                SafeDataTag(text = "离线，显示上次同步的本地缓存数据")
            }

            // 1. 主余额大卡片
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = accountTitle,
                            fontSize = 14.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )

                        Text(
                            text = if (isMasked) "显示余额" else "隐藏余额",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { BalancePrivacyManager.toggleMasked() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }

                    // 余额大字
                    val rawVal = balanceSnapshot.data?.rawValue
                    val unitVal = balanceSnapshot.data?.unit ?: "元"
                    val displayValue = when {
                        isMasked -> "••••"
                        rawVal != null -> rawVal
                        balanceSnapshot.phase == QueryPhase.LOADING -> "同步中..."
                        else -> "--.--"
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "¥",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = displayValue,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        if (!isMasked && rawVal != null) {
                            Text(
                                text = unitVal,
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // 更新时间与数据源说明
                    val updateTime = balanceSnapshot.lastSuccessEpochMillis?.let {
                        TimeFormatter.formatTime(it)
                    } ?: balanceSnapshot.data?.sourceUpdatedAt ?: "尚未同步"

                    Text(
                        text = "数据更新于 $updateTime · 来源：$sourceName",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }

            // 2. 账户信息卡片
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "账户详情与状态",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    if (isCard) {
                        InfoRow(label = "卡片状态", value = "正常在用")
                        InfoRow(label = "账户类型", value = "学生主账户")
                        InfoRow(label = "主要用途", value = "食堂就餐、超市商铺、校医院、图书馆门禁借阅")
                        InfoRow(
                            label = "流水说明",
                            value = "消费流水明细请前往各食堂一楼自助圈存机或后勤一卡通综合服务大厅查询。"
                        )
                    } else {
                        InfoRow(label = "服务状态", value = "正常在线")
                        InfoRow(label = "接入类型", value = "学生宿舍区万兆/教学区无线认证网络")
                        InfoRow(label = "计费方式", value = "学生包月/包学期优惠标准")
                        InfoRow(
                            label = "安全提示",
                            value = "修改网关连接密码、解绑终端 MAC 地址请访问校园网综合自服务大厅。"
                        )
                    }
                }
            }

            // 3. 充值与服务指引卡片
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "充值途径与安全指引",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "本应用为本地原生只读查询工具，不存储任何支付密码，不执行资金扣划与第三方支付。",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        lineHeight = 17.sp
                    )

                    if (isCard) {
                        Text(
                            text = "官方充值途径：\n1. 微信/支付宝搜索关注“东大一卡通”或“东北大学财务处”公众号进行线上充值。\n2. 各校区食堂入口、学生活动中心及后勤大厅的多功能自助圈存机进行转账圈存。",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    } else {
                        Text(
                            text = "官方网费续费途径：\n1. 通过“东大一卡通”向校园卡充值后，在圈存机转账至网费账户。\n2. 登录东北大学网络自服务系统 (ipgw.neu.edu.cn) 在线划转续费。",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )

                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("URL", "http://ipgw.neu.edu.cn"))
                                Toast.makeText(context, "网址已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("复制校园网自服务网址")
                        }
                    }
                }
            }

            // 加载/异常状态说明
            if (balanceSnapshot.phase == QueryPhase.FAILED) {
                LoadStatePanel(
                    isLoading = false,
                    error = balanceSnapshot.error,
                    onRetry = {
                        coroutineScope.launch {
                            CampusDataProvider.portal.refreshBalance(kind)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurfaceSecondary,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = MiuixTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

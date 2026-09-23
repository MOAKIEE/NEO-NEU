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
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Info
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
import edu.neu.campus.contract.BalanceKind
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.TimeFormatter
import edu.neu.campus.ui.theme.LocalCampusColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun BalanceDetailScreen(
    kind: BalanceKind,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val campusColors = LocalCampusColors.current
    val balanceSnapshot by CampusDataProvider.portal.balance(kind).collectAsState()

    val isCard = kind == BalanceKind.CAMPUS_CARD
    val pageTitle = if (isCard) "校园卡" else "网费"
    val accountTitle = if (isCard) "校园卡主账户余额" else "校园网账户余额"
    val sourceName = if (isCard) "一卡通中心" else "网络中心"

    val isRefreshing = balanceSnapshot.phase == QueryPhase.LOADING
    val isMasked = BalancePrivacyManager.isBalanceMasked

    val tintBg = if (isCard) campusColors.cardLight else campusColors.networkLight
    val tintText = if (isCard) campusColors.cardText else campusColors.networkText
    val tintIcon = if (isCard) Icons.Default.AccountBox else Icons.Default.Info

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(campusColors.background)
    ) {
        CampusTopBar(
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
                        tint = if (isRefreshing) campusColors.textSecondary else campusColors.brand
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 离线/缓存更新提示条
            if (balanceSnapshot.isStale) {
                SafeDataTag(text = "离线，显示上次同步的本地缓存数据")
            }

            // 1. 主余额大卡片（轻色背景 + 资产强调色）
            Card(
                insideMargin = PaddingValues(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(tintBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = tintIcon,
                                    contentDescription = null,
                                    tint = tintText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = accountTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = campusColors.textSecondary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(campusColors.surfaceMuted)
                                .clickable { BalancePrivacyManager.toggleMasked() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isMasked) "显示余额" else "隐藏余额",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = campusColors.brand
                            )
                        }
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
                            color = tintText,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = displayValue,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = tintText
                        )
                        if (!isMasked && rawVal != null) {
                            Text(
                                text = unitVal,
                                fontSize = 14.sp,
                                color = campusColors.textSecondary,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                    }

                    // 最近同步与数据源说明
                    SafeDataTag(
                        sourceName = sourceName,
                        lastSuccessEpochMillis = balanceSnapshot.lastSuccessEpochMillis,
                        isStale = balanceSnapshot.isStale
                    )
                }
            }

            // 2. 账户信息分组
            CampusSection(title = "账户详情") {
                CampusGroup {
                    if (isCard) {
                        InfoRow(label = "卡片状态", value = "正常在用")
                        CampusGroupDivider()
                        InfoRow(label = "账户类型", value = "学生主账户")
                        CampusGroupDivider()
                        InfoRow(label = "主要用途", value = "食堂就餐、超市商铺、校医院、图书馆门禁借阅")
                        CampusGroupDivider()
                        InfoRow(
                            label = "流水说明",
                            value = "消费流水明细请前往各食堂一楼自助圈存机或后勤一卡通综合服务大厅查询。"
                        )
                    } else {
                        InfoRow(label = "服务状态", value = "正常在线")
                        CampusGroupDivider()
                        InfoRow(label = "接入类型", value = "学生宿舍区万兆/教学区无线认证网络")
                        CampusGroupDivider()
                        InfoRow(label = "计费方式", value = "学生包月/包学期优惠标准")
                        CampusGroupDivider()
                        InfoRow(
                            label = "安全提示",
                            value = "修改网关连接密码、解绑终端 MAC 地址请访问校园网综合自服务大厅。"
                        )
                    }
                }
            }

            // 3. 充值与服务指引分组
            CampusSection(title = "充值途径与安全指引") {
                CampusGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "本应用为本地原生只读查询工具，不存储任何支付密码，不执行资金扣划与第三方支付。",
                            fontSize = 12.sp,
                            color = campusColors.textSecondary,
                            lineHeight = 18.sp
                        )

                        if (isCard) {
                            Text(
                                text = "官方充值途径：\n1. 微信/支付宝搜索关注“东大一卡通”或“东北大学财务处”公众号进行线上充值。\n2. 各校区食堂入口、学生活动中心及后勤大厅的多功能自助圈存机进行转账圈存。",
                                fontSize = 13.sp,
                                color = campusColors.textPrimary,
                                lineHeight = 19.sp
                            )
                        } else {
                            Text(
                                text = "官方网费续费途径：\n1. 通过“东大一卡通”向校园卡充值后，在圈存机转账至网费账户。\n2. 登录东北大学网络自服务系统 (ipgw.neu.edu.cn) 在线划转续费。",
                                fontSize = 13.sp,
                                color = campusColors.textPrimary,
                                lineHeight = 19.sp
                            )

                            Button(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    cm.setPrimaryClip(ClipData.newPlainText("URL", "http://ipgw.neu.edu.cn"))
                                    Toast.makeText(context, "网址已复制到剪贴板", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text("复制校园网自服务网址")
                            }
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val campusColors = LocalCampusColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = campusColors.textSecondary,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = campusColors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}

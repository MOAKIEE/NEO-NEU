package edu.neu.campus.app.feature.balance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.BalanceKind
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.AnimatedNumber
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusPageEnter
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Hide
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Show
import top.yukonga.miuix.kmp.icon.extended.WorldClock

/**
 * 校园卡 / 网费余额详情。
 *
 * 约定：
 * - 顶部大余额卡使用资产功能色柔化渐变，数字带滚动动画
 * - 遮罩切换与刷新位于标题栏与卡片内，不隐藏任何来源信息
 * - 数据说明明确只读边界（不代表卡状态、在线状态或套餐）
 */
@Composable
fun BalanceDetailScreen(
    kind: BalanceKind,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = CampusTheme.colors
    val balanceSnapshot by CampusDataProvider.portal.balance(kind).collectAsState()

    val isCard = kind == BalanceKind.CAMPUS_CARD
    val pageTitle = if (isCard) "校园卡" else "网费"
    val accountTitle = if (isCard) "校园卡主账户余额" else "校园网账户余额"
    val sourceName = if (isCard) "一卡通中心" else "网络中心"

    val isRefreshing = balanceSnapshot.phase == QueryPhase.LOADING
    val isMasked = BalancePrivacyManager.isBalanceMasked

    val tintBg = if (isCard) colors.cardContainer else colors.networkContainer
    val tintText = if (isCard) colors.cardForeground else colors.networkForeground
    val tintIcon = if (isCard) MiuixIcons.Regular.BankCards else MiuixIcons.Regular.WorldClock

    val rawVal = balanceSnapshot.data?.rawValue
    val serverMasked = balanceSnapshot.data?.isMasked == true
    val amount = if (isMasked || serverMasked) null else parseBalanceAmount(rawVal)
    val fallbackValue = when {
        isMasked -> "••••"
        serverMasked -> "学校已遮罩"
        rawVal != null -> rawVal
        balanceSnapshot.phase == QueryPhase.LOADING -> "同步中…"
        else -> "--.--"
    }

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = pageTitle,
            subtitle = if (isCard) "校园卡余额与来源" else "校园网余额与来源",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            CampusDataProvider.sync.requestVisible(edu.neu.campus.app.navigation.AppNavigator.currentTab,
                                edu.neu.campus.app.navigation.AppNavigator.currentDestination,
                                edu.neu.campus.app.SyncReason.MANUAL)
                        }
                    },
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Refresh,
                        contentDescription = "刷新",
                        tint = if (isRefreshing) colors.textDisabled else colors.brand
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CampusSpacing.screenHorizontal)
                .padding(top = CampusSpacing.xs, bottom = CampusSpacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
        ) {
            CampusPageEnter {
                Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                    // 离线/缓存提示
                    if (balanceSnapshot.isStale) {
                        SafeDataTag(text = "离线，显示上次获取的余额")
                    }

                    // 1. 主余额卡
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CampusShapes.extraLarge))
                            .background(
                                Brush.linearGradient(
                                    listOf(tintBg, tintBg.copy(alpha = if (colors.isDark) 0.55f else 0.72f))
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = tintText.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(CampusShapes.extraLarge)
                            )
                            .padding(CampusSpacing.lg)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
                                ) {
                                    CampusIconBadge(
                                        icon = tintIcon,
                                        tint = tintText,
                                        container = Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.75f),
                                        size = 34.dp,
                                        iconSize = 19.dp,
                                        cornerRadius = CampusShapes.extraSmall
                                    )
                                    Text(
                                        text = accountTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .tapScale(onClick = { BalancePrivacyManager.toggleMasked() },
                                            pressedScale = 0.94f, clipShape = RoundedCornerShape(CampusShapes.pill))
                                        .background(Color.White.copy(alpha = if (colors.isDark) 0.14f else 0.7f))
                                        .padding(horizontal = CampusSpacing.xs, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isMasked) MiuixIcons.Regular.Show else MiuixIcons.Regular.Hide,
                                        contentDescription = null,
                                        tint = tintText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (isMasked) "显示余额" else "隐藏余额",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = tintText
                                    )
                                }
                            }

                            // 余额大字（带数字滚动动画）
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
                                AnimatedNumber(
                                    target = amount,
                                    fallback = fallbackValue,
                                    decimals = 2,
                                    fontSize = if (amount != null) 38.sp else 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tintText
                                )
                                if (amount != null) {
                                    Text(
                                        text = balanceSnapshot.data?.unit ?: "元",
                                        fontSize = 14.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            SafeDataTag(
                                sourceName = sourceName,
                                lastSuccessEpochMillis = balanceSnapshot.lastSuccessEpochMillis,
                                isStale = balanceSnapshot.isStale
                            )
                        }
                    }

                    // 2. 数据说明
                    CampusSection(title = "数据说明") {
                        CampusGroup {
                            CampusRow(
                                title = "仅提供余额查询",
                                subtitle = "不代表卡片状态、网络在线状态或套餐信息",
                                leading = {
                                    CampusIconBadge(
                                        icon = MiuixIcons.Regular.Info,
                                        tint = colors.brand,
                                        container = colors.brandContainer,
                                        size = 38.dp,
                                        iconSize = 20.dp,
                                        cornerRadius = CampusShapes.extraSmall
                                    )
                                }
                            )
                            if (serverMasked) {
                                CampusRow(
                                    title = "学校未提供明文余额",
                                    leading = {
                                        CampusIconBadge(
                                            icon = MiuixIcons.Regular.Hide,
                                            tint = colors.warning,
                                            container = colors.warningContainer,
                                            size = 38.dp,
                                            iconSize = 20.dp,
                                            cornerRadius = CampusShapes.extraSmall
                                        )
                                    }
                                )
                            }
                            balanceSnapshot.data?.sourceUpdatedAt?.let { updatedAt ->
                                CampusRow(
                                    title = "学校更新于 $updatedAt",
                                    leading = {
                                        CampusIconBadge(
                                            icon = MiuixIcons.Regular.WorldClock,
                                            tint = colors.textSecondary,
                                            container = colors.surfaceMuted,
                                            size = 38.dp,
                                            iconSize = 20.dp,
                                            cornerRadius = CampusShapes.extraSmall
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // 3. 加载/异常
                    if (balanceSnapshot.phase == QueryPhase.FAILED) {
                        CampusCard {
                            LoadStatePanel(
                                isLoading = false,
                                error = balanceSnapshot.error,
                                onRetry = {
                                    coroutineScope.launch {
                                        CampusDataProvider.sync.requestVisible(edu.neu.campus.app.navigation.AppNavigator.currentTab,
                                            edu.neu.campus.app.navigation.AppNavigator.currentDestination,
                                            edu.neu.campus.app.SyncReason.MANUAL)
                                    }
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(CampusSpacing.sm))
                }
            }
        }
    }
}

/** 从学校返回的余额文本中提取可动画数值，失败时返回 null 以回退原文展示。 */
private fun parseBalanceAmount(raw: String?): Float? {
    if (raw.isNullOrBlank()) return null
    val match = Regex("[-+]?\\d+(\\.\\d+)?").find(raw.replace(",", "").trim()) ?: return null
    return match.value.toFloatOrNull()
}

package edu.neu.campus.app.feature.query

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.app.registry.FeatureItem
import edu.neu.campus.app.registry.FeatureRegistry
import edu.neu.campus.contract.BalanceKind
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Months
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Store
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Weeks

/**
 * 查询主页与功能目录。
 *
 * 组件约定：
 * - 顶部统一搜索，支持名称、别名与拼音
 * - 学习查询：成绩与考试两张重点入口卡片，图标置上、功能色柔化渐变
 * - 常用工具：一个分组承载图标面板（图标底 50dp + 13sp 标签）
 * - 学校服务：独立入口卡片，标注「官方网页」，由用户主动前往浏览器
 * - 搜索结果与默认面板之间使用横向淡入切换，避免闪烁
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QueryScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val isSearching = searchQuery.isNotBlank()
    val searchResults = remember(searchQuery) {
        if (isSearching) FeatureRegistry.search(searchQuery) else emptyList()
    }
    val colors = CampusTheme.colors
    val fontScale = LocalDensity.current.fontScale
    val compact = LocalConfiguration.current.screenWidthDp < 360 || fontScale >= 1.3f
    val toolColumns = if (fontScale >= 1.5f) 1 else if (compact) 2 else 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        CampusTopBar(
            title = "查询",
            subtitle = "校园信息与常用入口"
        )

        // 搜索栏
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs)
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "搜索功能，如成绩、考试、课表、网费",
                useLabelAsPlaceholder = true,
                leadingIcon = {
                    Icon(
                        imageVector = MiuixIcons.Regular.Search,
                        contentDescription = "搜索",
                        tint = colors.textTertiary
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = MiuixIcons.Regular.Close,
                                contentDescription = "清除",
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedContent(
            targetState = isSearching,
            transitionSpec = {
                (fadeIn(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)) +
                    slideInHorizontally(
                        animationSpec = tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)
                    ) { full -> if (targetState) full / 10 else -full / 10 })
                    .togetherWith(
                        fadeOut(tween(CampusMotion.Duration.short)) +
                            slideOutHorizontally(
                                animationSpec = tween(CampusMotion.Duration.short, easing = CampusMotion.Easing.emphasizedAccelerate)
                            ) { full -> if (targetState) -full / 10 else full / 10 }
                    )
                    .using(SizeTransform(clip = false))
            },
            label = "queryPanel",
            modifier = Modifier.weight(1f)
        ) { searching ->
            if (searching) {
                if (searchResults.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(CampusSpacing.xxl),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
                        ) {
                            CampusIconBadge(
                                icon = MiuixIcons.Regular.Search,
                                tint = colors.textTertiary,
                                container = colors.surfaceMuted,
                                size = 60.dp,
                                iconSize = 28.dp,
                                cornerRadius = CampusShapes.large
                            )
                            Text(
                                text = "未找到“$searchQuery”相关的查询功能",
                                fontSize = 15.sp,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                            Button(onClick = { searchQuery = "" }) {
                                Text("清除关键词")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = CampusSpacing.screenHorizontal,
                            end = CampusSpacing.screenHorizontal,
                            top = CampusSpacing.xs,
                            bottom = CampusSpacing.xxl
                        ),
                        verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
                    ) {
                        itemsIndexed(searchResults, key = { _, item -> item.id }) { index, item ->
                            StaggeredAppear(
                                index = index,
                                key = searchQuery,
                                modifier = Modifier.animateItem()
                            ) {
                                CampusCard(
                                    onClick = { navigateToFeature(item.id) },
                                    contentPadding = PaddingValues(CampusSpacing.sm + 2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                    ) {
                                        CampusIconBadge(
                                            icon = getFeatureIcon(item.id),
                                            tint = colors.brand,
                                            container = colors.brandContainer,
                                            size = 42.dp,
                                            iconSize = 21.dp
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
                                            ) {
                                                Text(
                                                    text = item.title,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                                CampusPill(text = item.category.displayName)
                                            }
                                            Text(
                                                text = item.description,
                                                fontSize = 12.sp,
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = CampusSpacing.screenHorizontal,
                            end = CampusSpacing.screenHorizontal,
                            top = CampusSpacing.xs,
                            bottom = CampusSpacing.xxl
                        ),
                    verticalArrangement = Arrangement.spacedBy(CampusSpacing.lg)
                ) {
                    StaggeredAppear(index = 0) {
                        CampusSection(title = "学习查询") {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = if (compact) 1 else 2,
                                verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                            ) {
                                HighlightEntryCard(
                                    title = "成绩查询",
                                    description = "课程成绩与官方绩点",
                                    icon = MiuixIcons.Regular.Notes,
                                    accent = colors.gradeForeground,
                                    container = colors.gradeContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { AppNavigator.navigateTo(AppDestination.Grades) }
                                )

                                HighlightEntryCard(
                                    title = "考试安排",
                                    description = "考试时间与考场地点",
                                    icon = MiuixIcons.Regular.Alarm,
                                    accent = colors.examForeground,
                                    container = colors.examContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { AppNavigator.navigateTo(AppDestination.Exams) }
                                )
                            }
                        }
                    }

                    StaggeredAppear(index = 1) {
                        CampusSection(title = "常用工具") {
                            CampusGroup {
                                val tools = listOf(
                                    ToolItem(FeatureRegistry.ID_TIMETABLE, "课表", MiuixIcons.Regular.Weeks, colors.timetableForeground, colors.timetableContainer),
                                    ToolItem(FeatureRegistry.ID_BELL_SCHEDULE, "校历作息", MiuixIcons.Regular.Months, colors.networkForeground, colors.networkContainer),
                                    ToolItem(FeatureRegistry.ID_CAMPUS_CARD, "校园卡", MiuixIcons.Regular.BankCards, colors.cardForeground, colors.cardContainer),
                                    ToolItem(FeatureRegistry.ID_NETWORK, "网费", MiuixIcons.Regular.Share, colors.networkForeground, colors.networkContainer),
                                    ToolItem(FeatureRegistry.ID_MESSAGES, "消息中心", MiuixIcons.Regular.Messages, colors.messageForeground, colors.messageContainer),
                                    ToolItem(FeatureRegistry.ID_TASKS, "待办申请", MiuixIcons.Regular.Tasks, colors.messageForeground, colors.messageContainer)
                                )

                                val chunked = tools.chunked(toolColumns)
                                chunked.forEachIndexed { rowIndex, rowItems ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = CampusSpacing.xs),
                                        horizontalArrangement = Arrangement.SpaceAround
                                    ) {
                                        rowItems.forEach { tool ->
                                            ToolPanelItem(
                                                title = tool.title,
                                                icon = tool.icon,
                                                iconColor = tool.iconColor,
                                                iconBg = tool.iconBg,
                                                onClick = { navigateToFeature(tool.id) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        if (rowItems.size < toolColumns) {
                                            repeat(toolColumns - rowItems.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                    if (rowIndex < chunked.lastIndex) {
                                        CampusGroupDivider(startIndent = 16.dp)
                                    }
                                }
                            }
                        }
                    }

                    StaggeredAppear(index = 2) {
                        CampusSection(title = "学校服务") {
                            CampusCard(
                                onClick = { AppNavigator.navigateTo(AppDestination.ServicesCatalog) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                    ) {
                                        CampusIconBadge(
                                            icon = MiuixIcons.Regular.Store,
                                            tint = colors.brand,
                                            container = colors.brandContainer
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "学校服务目录",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary
                                                )
                                                CampusPill(text = "官方网页")
                                            }
                                            Text(
                                                text = "在浏览器中查看学校官方办事大厅与服务指南",
                                                fontSize = 12.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = MiuixIcons.Basic.ArrowRight,
                                        contentDescription = null,
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class ToolItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val iconColor: Color,
    val iconBg: Color
)

/**
 * 重点查询入口卡片（成绩与考试）：最小高度 128dp，图标置上，柔化渐变底。
 */
@Composable
private fun HighlightEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
    accent: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 128.dp)
            .tapScale(
                onClick = onClick,
                pressedScale = 0.97f,
                clipShape = RoundedCornerShape(CampusShapes.extraLarge)
            )
            .background(
                Brush.linearGradient(
                    listOf(container, container.copy(alpha = if (colors.isDark) 0.6f else 0.72f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.16f), RoundedCornerShape(CampusShapes.extraLarge))
            .padding(CampusSpacing.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CampusIconBadge(
                icon = icon,
                tint = accent,
                container = Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.75f),
                size = 46.dp,
                iconSize = 24.dp,
                cornerRadius = CampusShapes.small
            )

            Spacer(modifier = Modifier.height(CampusSpacing.md))

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * 常用工具面板单项：最小高 88dp，图标底 50dp + 完整标签。
 */
@Composable
private fun ToolPanelItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Column(
        modifier = modifier
            .defaultMinSize(minHeight = 88.dp)
            .tapScale(onClick = onClick, pressedScale = 0.94f, clipShape = RoundedCornerShape(CampusShapes.small))
            .padding(vertical = CampusSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
    ) {
        CampusIconBadge(
            icon = icon,
            tint = iconColor,
            container = iconBg,
            size = 50.dp,
            iconSize = 24.dp,
            cornerRadius = CampusShapes.small + 2.dp
        )
        Text(
            text = title,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}

private fun getFeatureIcon(id: String): ImageVector {
    return when (id) {
        FeatureRegistry.ID_GRADES -> MiuixIcons.Regular.Notes
        FeatureRegistry.ID_EXAMS -> MiuixIcons.Regular.Alarm
        FeatureRegistry.ID_TIMETABLE -> MiuixIcons.Regular.Weeks
        FeatureRegistry.ID_BELL_SCHEDULE -> MiuixIcons.Regular.Months
        FeatureRegistry.ID_CAMPUS_CARD -> MiuixIcons.Regular.BankCards
        FeatureRegistry.ID_NETWORK -> MiuixIcons.Regular.Share
        FeatureRegistry.ID_MESSAGES -> MiuixIcons.Regular.Messages
        FeatureRegistry.ID_TASKS -> MiuixIcons.Regular.Tasks
        FeatureRegistry.ID_SERVICES_CATALOG -> MiuixIcons.Regular.Store
        else -> MiuixIcons.Regular.Search
    }
}

private fun navigateToFeature(featureId: String) {
    when (featureId) {
        FeatureRegistry.ID_GRADES -> AppNavigator.navigateTo(AppDestination.Grades)
        FeatureRegistry.ID_EXAMS -> AppNavigator.navigateTo(AppDestination.Exams)
        FeatureRegistry.ID_TIMETABLE -> AppNavigator.navigateToTab(MainTab.TIMETABLE)
        FeatureRegistry.ID_BELL_SCHEDULE -> AppNavigator.navigateTo(AppDestination.Schedule)
        FeatureRegistry.ID_CAMPUS_CARD -> AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.CAMPUS_CARD))
        FeatureRegistry.ID_NETWORK -> AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.NETWORK))
        FeatureRegistry.ID_MESSAGES -> AppNavigator.navigateTo(AppDestination.Messages)
        FeatureRegistry.ID_TASKS -> AppNavigator.navigateTo(AppDestination.Tasks)
        FeatureRegistry.ID_SERVICES_CATALOG -> AppNavigator.navigateTo(AppDestination.ServicesCatalog)
        else -> AppNavigator.navigateToTab(MainTab.TODAY)
    }
}

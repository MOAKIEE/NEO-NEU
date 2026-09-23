package edu.neu.campus.app.feature.query

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar

/**
 * 查询主页与功能目录。
 * 组件约定：
 * - 顶部统一搜索，支持名称、别名与拼音
 * - 学习查询：成绩与考试两张重点入口卡片（浅紫/浅橙，高度>=124dp，图标置上）
 * - 常用工具：一个 Surface 分组承载三列图标面板（每个图标底 48dp + 14sp 标签）
 * - 学校服务：独立列表卡片，注明“官方网页”，由用户主动前往浏览器
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
        TopAppBar(title = "查询")

        // 统一功能搜索栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "搜索功能，如成绩、考试、课表、网费",
                useLabelAsPlaceholder = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = colors.textSecondary
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除",
                                tint = colors.textSecondary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isSearching) {
            // 搜索结果列表
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceMuted),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
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
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(searchResults, key = { it.id }) { item ->
                        SearchResultItemRow(
                            item = item,
                            onClick = { navigateToFeature(item.id) }
                        )
                    }
                }
            }
        } else {
            // 默认精选面板视图
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 1. 学习查询：两张重点入口卡片 (C 类浅色卡片，浅紫/浅橙)
                CampusSection(title = "学习查询") {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        maxItemsInEachRow = if (compact) 1 else 2,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 成绩查询重点卡 (Light Purple)
                        HighlightEntryCard(
                            title = "成绩查询",
                            description = "课程成绩与官方绩点",
                            icon = Icons.Default.Star,
                            iconColor = colors.gradeForeground,
                            cardBg = colors.gradeContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { AppNavigator.navigateTo(AppDestination.Grades) }
                        )

                        // 考试安排重点卡 (Light Orange)
                        HighlightEntryCard(
                            title = "考试安排",
                            description = "考试时间与考场地点",
                            icon = Icons.Default.DateRange,
                            iconColor = colors.examForeground,
                            cardBg = colors.examContainer,
                            modifier = Modifier.weight(1f),
                            onClick = { AppNavigator.navigateTo(AppDestination.Exams) }
                        )
                    }
                }

                // 2. 常用工具：一个 Surface 分组承载三列图标面板
                CampusSection(title = "常用工具") {
                    CampusGroup {
                        val tools = listOf(
                            ToolItem(FeatureRegistry.ID_TIMETABLE, "课表", Icons.Default.DateRange, colors.timetableForeground, colors.timetableContainer),
                            ToolItem(FeatureRegistry.ID_BELL_SCHEDULE, "校历作息", Icons.Default.Notifications, colors.networkForeground, colors.networkContainer),
                            ToolItem(FeatureRegistry.ID_CAMPUS_CARD, "校园卡", Icons.Default.AccountBox, colors.cardForeground, colors.cardContainer),
                            ToolItem(FeatureRegistry.ID_NETWORK, "网费", Icons.Default.Share, colors.networkForeground, colors.networkContainer),
                            ToolItem(FeatureRegistry.ID_MESSAGES, "消息中心", Icons.Default.Notifications, colors.messageForeground, colors.messageContainer),
                            ToolItem(FeatureRegistry.ID_TASKS, "待办申请", Icons.Default.CheckCircle, colors.messageForeground, colors.messageContainer)
                        )

                        val chunked = tools.chunked(toolColumns)
                        chunked.forEachIndexed { rowIndex, rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
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

                // 3. 学校服务：独立列表卡片，标注“官方网页”
                CampusSection(title = "学校服务") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(colors.surface)
                            .clickable { AppNavigator.navigateTo(AppDestination.ServicesCatalog) }
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colors.brandContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = null,
                                        tint = colors.brand,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

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
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colors.surfaceMuted)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "官方网页",
                                                fontSize = 10.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "在浏览器中查看学校官方办事大厅与服务指南",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Text(
                                text = "查看目录 ›",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.brand,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
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
 * 重点查询入口卡片（成绩与考试）。
 * 遵循 Section 6：最小高度 124dp，图标置上，标题 16sp，说明 13sp。
 */
@Composable
private fun HighlightEntryCard(
    title: String,
    description: String,
    icon: ImageVector,
    iconColor: Color,
    cardBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 124.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = if (colors.isDark) 0.12f else 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
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
 * 常用工具三列图标面板单项。
 * 遵循 Section 6：每项最小高 88dp，48dp 图标底 + 完整标签。
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
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

/**
 * 搜索结果条目行。
 */
@Composable
private fun SearchResultItemRow(
    item: FeatureItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.brandContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getFeatureIcon(item.id),
                        contentDescription = null,
                        tint = colors.brand,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = item.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.surfaceMuted)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.category.displayName,
                                fontSize = 10.sp,
                                color = colors.textSecondary
                            )
                        }
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

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "进入",
                tint = colors.brand,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

private fun getFeatureIcon(id: String): ImageVector {
    return when (id) {
        FeatureRegistry.ID_GRADES -> Icons.Default.Star
        FeatureRegistry.ID_EXAMS -> Icons.Default.DateRange
        FeatureRegistry.ID_TIMETABLE -> Icons.Default.DateRange
        FeatureRegistry.ID_BELL_SCHEDULE -> Icons.Default.Notifications
        FeatureRegistry.ID_CAMPUS_CARD -> Icons.Default.AccountBox
        FeatureRegistry.ID_NETWORK -> Icons.Default.Share
        FeatureRegistry.ID_MESSAGES -> Icons.Default.Notifications
        FeatureRegistry.ID_TASKS -> Icons.Default.CheckCircle
        FeatureRegistry.ID_SERVICES_CATALOG -> Icons.Default.Menu
        else -> Icons.Default.Search
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

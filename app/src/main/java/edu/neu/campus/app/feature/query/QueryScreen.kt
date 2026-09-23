package edu.neu.campus.app.feature.query

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.app.registry.FeatureCategory
import edu.neu.campus.app.registry.FeatureItem
import edu.neu.campus.app.registry.FeatureRegistry
import edu.neu.campus.contract.BalanceKind
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 查询主页与统一搜索。
 * 遵循 docs/06-UI页面布局设计.md 第 5 节与第 15 节扩展规则：
 * - 顶部固定功能搜索入口，仅搜索功能名称与别名，不混入个人私密数据
 * - 默认按两列卡片分组展示（学习、校园生活、消息与事务）
 * - 次级入口“学校服务目录”，展示带“官方网页”标签的外部服务
 */
@Composable
fun QueryScreen(
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val isSearching = searchQuery.isNotBlank()
    val searchResults = remember(searchQuery) {
        if (isSearching) FeatureRegistry.search(searchQuery) else emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = "查询"
        )

        // 搜索框
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "搜索查询功能 (如: 绩点, 考试, 课表, 校园卡)",
                useLabelAsPlaceholder = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "搜索",
                        tint = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                },
                trailingIcon = {
                    if (isSearching) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "清除",
                                tint = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isSearching) {
            // 搜索结果：单列展示
            if (searchResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "未找到相关查询",
                            fontSize = 15.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                        Button(onClick = { searchQuery = "" }) {
                            Text("清除关键词")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(searchResults, key = { it.id }) { item ->
                        SearchResultItemCard(
                            item = item,
                            onClick = { navigateToFeature(item.id) }
                        )
                    }
                }
            }
        } else {
            // 默认分类卡片视图
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 学习分组
                CategorySection(
                    title = "学习",
                    items = listOf(
                        FeatureRegistry.findById(FeatureRegistry.ID_GRADES),
                        FeatureRegistry.findById(FeatureRegistry.ID_EXAMS),
                        FeatureRegistry.findById(FeatureRegistry.ID_TIMETABLE),
                        FeatureRegistry.findById(FeatureRegistry.ID_BELL_SCHEDULE)
                    ).filterNotNull(),
                    onItemClick = { navigateToFeature(it.id) }
                )

                // 2. 校园生活分组
                CategorySection(
                    title = "校园生活",
                    items = listOf(
                        FeatureRegistry.findById(FeatureRegistry.ID_CAMPUS_CARD),
                        FeatureRegistry.findById(FeatureRegistry.ID_NETWORK)
                    ).filterNotNull(),
                    onItemClick = { navigateToFeature(it.id) }
                )

                // 3. 消息与事务分组
                CategorySection(
                    title = "消息与事务",
                    items = listOf(
                        FeatureRegistry.findById(FeatureRegistry.ID_MESSAGES),
                        FeatureRegistry.findById(FeatureRegistry.ID_TASKS)
                    ).filterNotNull(),
                    onItemClick = { navigateToFeature(it.id) }
                )

                // 4. 学校服务目录次级入口
                Card(
                    colors = CardDefaults.defaultColors(),
                    insideMargin = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { AppNavigator.navigateTo(AppDestination.ServicesCatalog) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "学校服务目录",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "办事大厅、研究生管理、图书馆与财务系统入口",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }

                        Text(
                            text = "查看目录 ›",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategorySection(
    title: String,
    items: List<FeatureItem>,
    onItemClick: (FeatureItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface
        )

        // 双列网格卡片
        val chunked = items.chunked(2)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { item ->
                    Card(
                        colors = CardDefaults.defaultColors(),
                        insideMargin = PaddingValues(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onItemClick(item) }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = item.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
                // 单个时补齐留白
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SearchResultItemCard(
    item: FeatureItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.defaultColors(),
        insideMargin = PaddingValues(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MiuixTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.category.displayName,
                            fontSize = 10.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                }

                Text(
                    text = item.description,
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }

            Text(
                text = "进入 ›",
                fontSize = 12.sp,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
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

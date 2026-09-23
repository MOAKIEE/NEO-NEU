package edu.neu.campus.app.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.contract.CampusMessage
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusFilterChip
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.CampusSegmentedControl
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Email
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Refresh

enum class MessageSourceFilter(val label: String) {
    ALL("全部来源"),
    PORTAL("门户系统")
}

enum class MessageStatusFilter(val label: String) {
    ALL("全部状态"),
    UNREAD("学校未读"),
    READ("学校已读")
}

@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val colors = CampusTheme.colors
    var sourceFilter by rememberSaveable { mutableStateOf(MessageSourceFilter.ALL) }
    var statusFilter by rememberSaveable { mutableStateOf(MessageStatusFilter.ALL) }
    var page by rememberSaveable { mutableStateOf(1) }
    val status = when (statusFilter) {
        MessageStatusFilter.ALL -> 0
        MessageStatusFilter.UNREAD -> 2
        MessageStatusFilter.READ -> 1
    }
    val messagesSnapshot by CampusDataProvider.portal.messages(page, 30, status).collectAsState()
    LaunchedEffect(page, status) { CampusDataProvider.portal.refreshMessages(page, 30, status) }

    val isRefreshing = messagesSnapshot.phase == QueryPhase.LOADING
    val allMessages = messagesSnapshot.data?.items ?: emptyList()

    // 筛选消息
    val filteredMessages = remember(allMessages, sourceFilter, statusFilter) {
        allMessages.filter { msg ->
            val matchSource = true // Only the portal source is currently integrated.

            val matchStatus = when (statusFilter) {
                MessageStatusFilter.ALL -> true
                MessageStatusFilter.UNREAD -> msg.serverRead == false
                MessageStatusFilter.READ -> msg.serverRead == true
            }

            matchSource && matchStatus
        }
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
            title = "消息中心",
            onBack = onBack,
            subtitle = "门户消息与本机已读状态",
            actions = {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            CampusDataProvider.portal.refreshMessages(page, 30, status)
                        }
                    },
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Refresh,
                        contentDescription = "刷新",
                        tint = if (isRefreshing) colors.textSecondary else colors.brand
                    )
                }
            }
        )

        // 规范要求的提示说明条
        Row(modifier = Modifier.padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xxs)) {
            SafeDataTag(text = "在此查看消息不会改变学校网站上的已读状态")
        }

        // 筛选标签行
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs - 2.dp),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
        ) {
            // 来源筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
            ) {
                MessageSourceFilter.entries.forEach { f ->
                    CampusFilterChip(
                        text = f.label,
                        active = f == sourceFilter,
                        onClick = { sourceFilter = f }
                    )
                }
            }

            // 状态筛选
            CampusSegmentedControl(
                options = MessageStatusFilter.entries.map { it.label },
                selectedIndex = MessageStatusFilter.entries.indexOf(statusFilter).coerceAtLeast(0),
                onSelect = { index ->
                    statusFilter = MessageStatusFilter.entries[index]
                    page = 1
                }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = CampusSpacing.screenHorizontal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            top.yukonga.miuix.kmp.basic.Button(onClick = { page-- }, enabled = page > 1 && !isRefreshing) { Text("上一页") }
            Text("第 $page 页 · 门户", fontSize = 13.sp, color = colors.textSecondary)
            val total = messagesSnapshot.data?.total
            val hasNext = if (total != null) page * 30 < total else allMessages.size == 30
            top.yukonga.miuix.kmp.basic.Button(onClick = { page++ }, enabled = hasNext && !isRefreshing) { Text("下一页") }
        }
        if (messagesSnapshot.isStale) {
            Row(modifier = Modifier.padding(horizontal = CampusSpacing.screenHorizontal)) {
                SafeDataTag(text = "显示上次同步消息，可能已变化")
            }
        }
        // 消息列表与状态展示
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                messagesSnapshot.phase in listOf(QueryPhase.IDLE, QueryPhase.LOADING) && messagesSnapshot.data == null -> {
                    LoadStatePanel(isLoading = true)
                }
                messagesSnapshot.phase == QueryPhase.FAILED && allMessages.isEmpty() -> {
                    LoadStatePanel(
                        isLoading = false,
                        error = messagesSnapshot.error,
                        onRetry = {
                            coroutineScope.launch {
                                CampusDataProvider.portal.refreshMessages(page, 30, status)
                            }
                        }
                    )
                }
                filteredMessages.isEmpty() -> {
                    LoadStatePanel(
                        isLoading = false,
                        emptyMessage = "暂无符合条件的消息通知"
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(CampusSpacing.md),
                        verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs + 2.dp)
                    ) {
                        itemsIndexed(filteredMessages, key = { _, item -> item.id }) { index, msg ->
                            StaggeredAppear(
                                index = index,
                                key = statusFilter,
                                modifier = Modifier.animateItem()
                            ) {
                                MessageItemCard(
                                    message = msg,
                                    onClick = {
                                        MessagesManager.markAsLocalRead(msg.id)
                                        AppNavigator.navigateTo(AppDestination.MessageDetail(msg.id, page, status))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageItemCard(
    message: CampusMessage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val isLocalRead = MessagesManager.isLocalRead(message.id)
    val isServerUnread = message.serverRead == false
    val summary = message.contentLines.firstOrNull()?.trim() ?: ""

    val isAcademic = message.source?.contains("教务") == true || message.source?.contains("academic") == true

    CampusCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(CampusSpacing.sm + 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧 40dp 来源图标
            CampusIconBadge(
                icon = if (isAcademic) MiuixIcons.Regular.Email else MiuixIcons.Regular.Messages,
                tint = if (isAcademic) colors.brand else colors.messageForeground,
                container = if (isAcademic) colors.brandContainer else colors.messageContainer,
                size = 40.dp,
                iconSize = 20.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs - 2.dp)
            ) {
                // 标题行与时间
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs - 2.dp)
                    ) {
                        if (isServerUnread && !isLocalRead) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(colors.error)
                            )
                        }

                        Text(
                            text = message.title,
                            fontSize = 15.sp,
                            fontWeight = if (isServerUnread && !isLocalRead) FontWeight.Bold else FontWeight.Medium,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val msgTime = message.time
                    if (msgTime != null) {
                        Text(
                            text = msgTime,
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            modifier = Modifier.padding(start = CampusSpacing.xs - 2.dp)
                        )
                    }
                }

                // 摘要
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 来源标签与状态
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val sourceLabel = message.source ?: "智慧东大门户"
                    Text(
                        text = sourceLabel,
                        fontSize = 11.sp,
                        color = colors.textTertiary
                    )

                    if (isServerUnread) {
                        if (isLocalRead) {
                            CampusPill(text = "学校未读 (本机已查)")
                        } else {
                            CampusPill(
                                text = "未读",
                                contentColor = colors.brand,
                                containerColor = colors.brandContainer
                            )
                        }
                    } else {
                        CampusPill(text = "已读")
                    }
                }
            }
        }
    }
}

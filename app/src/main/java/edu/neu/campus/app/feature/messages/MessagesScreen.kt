package edu.neu.campus.app.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

enum class MessageSourceFilter(val label: String) {
    ALL("全部来源"),
    PORTAL("门户系统"),
    ACADEMIC("教务系统")
}

enum class MessageStatusFilter(val label: String) {
    ALL("全部状态"),
    UNREAD("学校未读"),
    READ("学校已读")
}

/**
 * 消息中心列表页。
 * 遵循 docs/06-UI页面布局设计.md 第 9 节要求：
 * - 来源与已读/未读状态筛选
 * - 纯文本消息列表，展示标题（最多 2 行）、首行摘要、来源和时间
 * - 未读采用红点加状态标识，明确提示“阅读仅记录在本机，不改变学校已读状态”
 */
@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val messagesSnapshot by CampusDataProvider.portal.messages(page = 1, pageSize = 30).collectAsState()

    var sourceFilter by remember { mutableStateOf(MessageSourceFilter.ALL) }
    var statusFilter by remember { mutableStateOf(MessageStatusFilter.ALL) }

    val isRefreshing = messagesSnapshot.phase == QueryPhase.LOADING
    val allMessages = messagesSnapshot.data?.items ?: emptyList()

    // 筛选消息
    val filteredMessages = remember(allMessages, sourceFilter, statusFilter) {
        allMessages.filter { msg ->
            val src = msg.source.orEmpty()
            val matchSource = when (sourceFilter) {
                MessageSourceFilter.ALL -> true
                MessageSourceFilter.PORTAL -> src.isBlank() || src.contains("门户") || src.contains("portal")
                MessageSourceFilter.ACADEMIC -> src.contains("教务") || src.contains("academic")
            }

            // 状态筛选
            val matchStatus = when (statusFilter) {
                MessageStatusFilter.ALL -> true
                MessageStatusFilter.UNREAD -> msg.serverRead == false
                MessageStatusFilter.READ -> msg.serverRead == true
            }

            matchSource && matchStatus
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = "消息中心",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            CampusDataProvider.portal.refreshMessages(page = 1, pageSize = 30)
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

        // 规范要求的提示说明条
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SafeDataTag(text = "阅读仅记录在本机，不改变学校服务端状态")
        }

        // 筛选标签行
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 来源筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MessageSourceFilter.entries.forEach { f ->
                    val isSelected = f == sourceFilter
                    FilterChip(
                        label = f.label,
                        isSelected = isSelected,
                        onClick = { sourceFilter = f }
                    )
                }
            }

            // 状态筛选
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MessageStatusFilter.entries.forEach { f ->
                    val isSelected = f == statusFilter
                    FilterChip(
                        label = f.label,
                        isSelected = isSelected,
                        onClick = { statusFilter = f }
                    )
                }
            }
        }

        // 消息列表与状态展示
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                messagesSnapshot.phase == QueryPhase.LOADING && allMessages.isEmpty() -> {
                    LoadStatePanel(isLoading = true)
                }
                messagesSnapshot.phase == QueryPhase.FAILED && allMessages.isEmpty() -> {
                    LoadStatePanel(
                        isLoading = false,
                        error = messagesSnapshot.error,
                        onRetry = {
                            coroutineScope.launch {
                                CampusDataProvider.portal.refreshMessages(page = 1, pageSize = 30)
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
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredMessages, key = { it.id }) { msg ->
                            MessageItemCard(
                                message = msg,
                                onClick = {
                                    MessagesManager.markAsLocalRead(msg.id)
                                    AppNavigator.navigateTo(AppDestination.MessageDetail(msg.id))
                                }
                            )
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
    val isLocalRead = MessagesManager.isLocalRead(message.id)
    val isServerUnread = message.serverRead == false
    val summary = message.contentLines.firstOrNull()?.trim() ?: ""

    Card(
        colors = CardDefaults.defaultColors(),
        insideMargin = PaddingValues(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // 标题行与未读圆点
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 未读红点
                if (isServerUnread && !isLocalRead) {
                    Box(
                        modifier = Modifier
                            .padding(top = 5.dp)
                            .size(8.dp)
                            .background(MiuixTheme.colorScheme.primary, CircleShape)
                    )
                }

                Text(
                    text = message.title,
                    fontSize = 15.sp,
                    fontWeight = if (isServerUnread && !isLocalRead) FontWeight.Bold else FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                val msgTime = message.time
                if (msgTime != null) {
                    Text(
                        text = msgTime,
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }

            // 摘要
            if (summary.isNotBlank()) {
                Text(
                    text = summary,
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
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
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                if (isServerUnread) {
                    Text(
                        text = if (isLocalRead) "学校未读 (本机已查)" else "未读",
                        fontSize = 11.sp,
                        color = if (isLocalRead) MiuixTheme.colorScheme.onSurfaceSecondary else MiuixTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "已读",
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bg = if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer
    val textColor = if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor
        )
    }
}

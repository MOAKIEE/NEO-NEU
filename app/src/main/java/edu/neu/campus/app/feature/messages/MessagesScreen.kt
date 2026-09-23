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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
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
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.LocalCampusColors
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text

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

@Composable
fun MessagesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val campusColors = LocalCampusColors.current
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
            .background(campusColors.background)
    ) {
        CampusTopBar(
            title = "消息中心",
            onBack = onBack,
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
                        tint = if (isRefreshing) campusColors.textSecondary else campusColors.brand
                    )
                }
            }
        )

        // 规范要求的提示说明条
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            SafeDataTag(text = "在此查看消息不会改变学校网站上的已读状态")
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
                    FilterChip(
                        label = f.label,
                        isSelected = f == sourceFilter,
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
                    FilterChip(
                        label = f.label,
                        isSelected = f == statusFilter,
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
    val campusColors = LocalCampusColors.current
    val isLocalRead = MessagesManager.isLocalRead(message.id)
    val isServerUnread = message.serverRead == false
    val summary = message.contentLines.firstOrNull()?.trim() ?: ""

    val isAcademic = message.source?.contains("教务") == true || message.source?.contains("academic") == true

    Card(
        insideMargin = PaddingValues(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 左侧 40dp 来源图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isAcademic) campusColors.brandContainer else campusColors.messageLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAcademic) Icons.Default.Email else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (isAcademic) campusColors.brand else campusColors.messageText,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isServerUnread && !isLocalRead) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(campusColors.error)
                            )
                        }

                        Text(
                            text = message.title,
                            fontSize = 15.sp,
                            fontWeight = if (isServerUnread && !isLocalRead) FontWeight.Bold else FontWeight.Medium,
                            color = campusColors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val msgTime = message.time
                    if (msgTime != null) {
                        Text(
                            text = msgTime,
                            fontSize = 11.sp,
                            color = campusColors.textSecondary,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }

                // 摘要
                if (summary.isNotBlank()) {
                    Text(
                        text = summary,
                        fontSize = 13.sp,
                        color = campusColors.textSecondary,
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
                        color = campusColors.textSecondary
                    )

                    if (isServerUnread) {
                        Text(
                            text = if (isLocalRead) "学校未读 (本机已查)" else "未读",
                            fontSize = 11.sp,
                            color = if (isLocalRead) campusColors.textSecondary else campusColors.brand
                        )
                    } else {
                        Text(
                            text = "已读",
                            fontSize = 11.sp,
                            color = campusColors.textSecondary
                        )
                    }
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
    val campusColors = LocalCampusColors.current
    val bg = if (isSelected) campusColors.brandContainer else campusColors.surfaceMuted
    val textColor = if (isSelected) campusColors.brand else campusColors.textSecondary

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

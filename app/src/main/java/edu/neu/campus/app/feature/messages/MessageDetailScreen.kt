package edu.neu.campus.app.feature.messages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 消息详情独立页。
 * 遵循 docs/06-UI页面布局设计.md 第 9.2 节要求：
 * - 标题、来源与发送时间、原生纯文本正文渲染
 * - 底部展示学校状态与本机状态说明，支持“在浏览器查看官方原文”
 */
@Composable
fun MessageDetailScreen(
    messageId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messagesSnapshot by CampusDataProvider.portal.messages(page = 1, pageSize = 30).collectAsState()

    // 查找目标消息
    val message = messagesSnapshot.data?.items?.firstOrNull { it.id == messageId }

    // 自动标记本机已读
    LaunchedEffect(messageId) {
        if (messageId.isNotBlank()) {
            MessagesManager.markAsLocalRead(messageId)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = "通知详情",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        )

        if (message == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (messageId.isBlank()) "请选择具体消息查看" else "未找到该消息或本地缓存已过期",
                    fontSize = 15.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 标题与元数据卡
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = message.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        lineHeight = 24.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "来源：${message.source ?: "智慧东大门户"}",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )

                        val msgTime = message.time
                        if (msgTime != null) {
                            Text(
                                text = msgTime,
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }

                    // 状态说明
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val serverStateText = if (message.serverRead == true) "学校状态：已读" else "学校状态：未读"
                        Text(
                            text = serverStateText,
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                        Text(
                            text = "本机状态：已查看",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 2. 正文原生纯文本卡片（可选择与长按复制）
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "正文内容",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface
                        )

                        if (message.contentLines.isEmpty()) {
                            Text(
                                text = "（该通知无附加正文内容）",
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        } else {
                            message.contentLines.forEach { line ->
                                Text(
                                    text = line,
                                    fontSize = 14.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. 官方原文查看指引
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "外部链接与官方原文",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "本通知仅在本地安全呈现纯文本内容。如需查看附件、填报表单或在学校系统标记已读，请通过系统浏览器访问智慧东大官方门户。",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://portal.neu.edu.cn"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "无法启动系统浏览器", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("在浏览器查看官方原文")
                        }

                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("URL", "http://portal.neu.edu.cn"))
                                Toast.makeText(context, "门户网址已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("复制门户网址")
                        }
                    }
                }
            }
        }
    }
}

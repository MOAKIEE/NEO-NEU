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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusPageEnter
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun MessageDetailScreen(
    messageId: String,
    page: Int = 1,
    status: Int = 0,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messagesSnapshot by CampusDataProvider.portal.messages(page = page, pageSize = 30, status = status).collectAsState()

    // 查找目标消息
    val message = messagesSnapshot.data?.items?.firstOrNull { it.id == messageId }

    // 自动标记本机已读
    LaunchedEffect(message?.id) {
        if (message != null) {
            MessagesManager.markAsLocalRead(messageId)
        }
    }

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CampusTheme.colors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "通知详情",
            subtitle = "门户来源与本机已读状态",
            onBack = onBack
        )

        if (message == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (messageId.isBlank()) "请选择具体消息查看" else "未找到该消息或本地缓存已过期",
                    fontSize = 15.sp,
                    color = CampusTheme.colors.textSecondary
                )
            }
            return
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CampusSpacing.screenHorizontal)
                .padding(top = CampusSpacing.xs, bottom = CampusSpacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
        ) {
            // 1. 标题与元数据卡
            CampusCard(contentPadding = PaddingValues(CampusSpacing.lg)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = message.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = CampusTheme.colors.textPrimary,
                        lineHeight = 26.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CampusPill(
                            text = "来源：${message.source ?: "智慧东大门户"}",
                            contentColor = CampusTheme.colors.brand,
                            containerColor = CampusTheme.colors.brandContainer
                        )

                        val msgTime = message.time
                        if (msgTime != null) {
                            Text(
                                text = msgTime,
                                fontSize = 12.sp,
                                color = CampusTheme.colors.textSecondary
                            )
                        }
                    }

                    // 状态说明
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val serverStateText = when (message.serverRead) { true -> "学校状态：已读"; false -> "学校状态：未读"; null -> "学校状态：未提供" }
                        CampusPill(text = serverStateText)
                        CampusPill(
                            text = "已在本机查看",
                            contentColor = CampusTheme.colors.success,
                            containerColor = CampusTheme.colors.successContainer
                        )
                    }
                }
            }

            // 2. 正文原生纯文本卡片（可选择与长按复制）
            CampusSection(title = "正文内容") {
                CampusGroup {
                    SelectionContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (message.contentLines.isEmpty()) {
                                Text(
                                    text = "（该通知无附加正文内容）",
                                    fontSize = 14.sp,
                                    color = CampusTheme.colors.textSecondary
                                )
                            } else {
                                message.contentLines.forEach { line ->
                                    Text(
                                        text = line,
                                        fontSize = 15.sp,
                                        color = CampusTheme.colors.textPrimary,
                                        lineHeight = 24.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. 官方原文查看指引
            CampusSection(title = "官方原文与操作") {
                CampusGroup {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "如需查看附件、填写表单或在学校网站标记已读，请前往智慧东大官方门户。",
                            fontSize = 12.sp,
                            color = CampusTheme.colors.textSecondary,
                            lineHeight = 18.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                                colors = ButtonDefaults.buttonColorsPrimary(),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("在浏览器查看")
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

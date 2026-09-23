package edu.neu.campus.app.feature.tasks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.contract.CampusTask
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.contract.TaskKind
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 待办与申请页。
 * 遵循 docs/06-UI页面布局设计.md 第 10.1 节要求与 docs/handoff/data.md 约束：
 * - 待办 / 已办 / 我的申请 分段切换
 * - 如遇 SCHEMA_CHANGED 友好警示并引导至官方门户，绝不把变动误认为空或错误
 * - 任务详情只读展示，不包含虚构的“审批”“办理”“撤销”本地按钮
 */
@Composable
fun TasksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(TaskKind.TODO) }
    var inspectingTask by remember { mutableStateOf<CampusTask?>(null) }

    // 观察当前选中的任务种类数据
    val tasksSnapshot by CampusDataProvider.portal.tasks(kind = activeTab, page = 1, pageSize = 20).collectAsState()
    val isRefreshing = tasksSnapshot.phase == QueryPhase.LOADING

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        edu.neu.campus.ui.components.CampusTopBar(
            title = "待办与申请",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            CampusDataProvider.portal.refreshTasks(kind = activeTab, page = 1, pageSize = 20)
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

        // 分段切换标签：待办 / 已办 / 我的申请
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                TaskKind.TODO to "待办事项",
                TaskKind.DONE to "已办事项",
                TaskKind.APPLICATION to "我的申请"
            ).forEach { (kind, label) ->
                val isSelected = activeTab == kind
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.surfaceContainer)
                        .clickable { activeTab = kind }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) MiuixTheme.colorScheme.onPrimary else MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // 内容展示区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val taskList = tasksSnapshot.data?.items ?: emptyList()
            val error = tasksSnapshot.error

            when {
                // 特殊错误处理：SCHEMA_CHANGED（数据层交接文档明确指出非空记录目前会触发 SCHEMA_CHANGED）
                error?.kind == QueryErrorKind.SCHEMA_CHANGED -> {
                    SchemaChangedCard(context = context)
                }

                tasksSnapshot.phase == QueryPhase.LOADING && taskList.isEmpty() -> {
                    LoadStatePanel(isLoading = true)
                }

                tasksSnapshot.phase == QueryPhase.FAILED && taskList.isEmpty() -> {
                    LoadStatePanel(
                        isLoading = false,
                        error = error,
                        onRetry = {
                            coroutineScope.launch {
                                CampusDataProvider.portal.refreshTasks(kind = activeTab, page = 1, pageSize = 20)
                            }
                        }
                    )
                }

                taskList.isEmpty() -> {
                    val emptyHint = when (activeTab) {
                        TaskKind.TODO -> "当前暂无待办事项，轻松一下吧"
                        TaskKind.DONE -> "暂无已办事项历史记录"
                        TaskKind.APPLICATION -> "暂无我的申请记录"
                    }
                    LoadStatePanel(
                        isLoading = false,
                        emptyMessage = emptyHint
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(taskList, key = { it.id }) { task ->
                            TaskItemCard(
                                task = task,
                                onClick = { inspectingTask = task }
                            )
                        }
                    }
                }
            }
        }
    }

    // 任务详情弹窗卡片
    inspectingTask?.let { task ->
        TaskDetailDialog(
            task = task,
            context = context,
            onDismiss = { inspectingTask = null }
        )
    }
}

@Composable
private fun TaskItemCard(
    task: CampusTask,
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
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "查看 ›",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "来源：智慧东大办公门户",
                    fontSize = 11.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                val tTime = task.time
                if (tTime != null) {
                    Text(
                        text = tTime,
                        fontSize = 11.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun SchemaChangedCard(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.defaultColors(),
            insideMargin = PaddingValues(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "警示",
                        tint = MiuixTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "学校待办中心接口格式变动",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = "学校智慧门户的待办事项数据返回格式近期发生升级变更。为保障学生个人数据解析准确性与账号安全，移动客户端已暂时挂起该接口的本地解析，未作盲目猜测。",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurface,
                    lineHeight = 19.sp
                )

                Text(
                    text = "请通过学校官方门户网站登录待办系统查看或处理审批事务。",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
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
                        Text("在浏览器打开官方门户")
                    }

                    Button(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("URL", "http://portal.neu.edu.cn"))
                            Toast.makeText(context, "网址已复制到剪贴板", Toast.LENGTH_SHORT).show()
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

@Composable
private fun TaskDetailDialog(
    task: CampusTask,
    context: Context,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background.copy(alpha = 0.6f))
            .clickable(onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.defaultColors(),
            insideMargin = PaddingValues(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = false) {}
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = task.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )

                Text(
                    text = "类型：${if (task.kind == TaskKind.TODO) "待办事项" else if (task.kind == TaskKind.DONE) "已办事项" else "我的申请"}",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                val tTime = task.time
                if (tTime != null) {
                    Text(
                        text = "发生时间：$tTime",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }

                Text(
                    text = "来源系统：智慧东大办公门户",
                    fontSize = 13.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )

                SafeDataTag(text = "只读展示 · 请前往官方系统办理")

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
                                Toast.makeText(context, "无法启动浏览器", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("在官方系统查看")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("关闭")
                    }
                }
            }
        }
    }
}

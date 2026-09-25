package edu.neu.campus.app.feature.tasks

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.contract.CampusTask
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.contract.QueryPhase
import edu.neu.campus.contract.TaskKind
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusSegmentedControl
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.LoadStatePanel
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.components.tapScale
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
import top.yukonga.miuix.kmp.icon.extended.Ok
import top.yukonga.miuix.kmp.icon.extended.Refresh
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Send
import top.yukonga.miuix.kmp.icon.extended.Tasks

@Composable
fun TasksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onLoginClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val campusColors = CampusTheme.colors

    var activeTab by rememberSaveable { mutableStateOf(TaskKind.TODO) }
    var inspectingTask by remember { mutableStateOf<CampusTask?>(null) }

    val taskTabs = listOf(
        TaskKind.TODO to "待办事项",
        TaskKind.DONE to "已办事项",
        TaskKind.APPLICATION to "我的申请"
    )

    DisposableEffect(activeTab) {
        val unregister = CampusDataProvider.sync.registerVisible(AppNavigator.currentTab, AppNavigator.currentDestination) {
            CampusDataProvider.portal.refreshTasks(activeTab, 1, 20)
        }
        onDispose { unregister() }
    }
    var initialTaskTabSeen by remember { mutableStateOf(false) }
    LaunchedEffect(activeTab) {
        if (!initialTaskTabSeen) initialTaskTabSeen = true
        else CampusDataProvider.portal.refreshTasks(activeTab, 1, 20)
    }

    // 观察当前选中的任务种类数据
    val tasksSnapshot by CampusDataProvider.portal.tasks(kind = activeTab, page = 1, pageSize = 20).collectAsState()
    val isRefreshing = tasksSnapshot.phase == QueryPhase.LOADING

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(campusColors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "待办与申请",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination,
                                edu.neu.campus.app.SyncReason.MANUAL)
                        }
                    },
                    enabled = !isRefreshing
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Refresh,
                        contentDescription = "刷新",
                        tint = if (isRefreshing) campusColors.textSecondary else campusColors.brand
                    )
                }
            }
        )

        if (tasksSnapshot.isStale) {
            Row(modifier = Modifier.padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xxs)) {
                SafeDataTag(text = "待办最近同步 ${edu.neu.campus.ui.components.TimeFormatter.formatDateTime(tasksSnapshot.lastSuccessEpochMillis)} · 显示旧缓存")
            }
        }

        // 分段切换：待办事项 / 已办事项 / 我的申请
        CampusSegmentedControl(
            options = taskTabs.map { it.second },
            selectedIndex = taskTabs.indexOfFirst { it.first == activeTab }.coerceAtLeast(0),
            onSelect = { index -> activeTab = taskTabs[index].first },
            modifier = Modifier.padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs)
        )

        // 内容展示区
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val taskList = tasksSnapshot.data?.items ?: emptyList()
            val error = tasksSnapshot.error

            when {
                // 特殊错误处理：SCHEMA_CHANGED
                error?.kind == QueryErrorKind.SCHEMA_CHANGED && tasksSnapshot.data == null -> {
                    SchemaChangedCard(context = context)
                }

                tasksSnapshot.phase in listOf(QueryPhase.IDLE, QueryPhase.LOADING) && taskList.isEmpty() -> {
                    LoadStatePanel(isLoading = true)
                }

                tasksSnapshot.phase == QueryPhase.FAILED && taskList.isEmpty() -> {
                    LoadStatePanel(
                        isLoading = false,
                        error = error,
                        onRetry = {
                            coroutineScope.launch {
                                CampusDataProvider.sync.requestVisible(AppNavigator.currentTab, AppNavigator.currentDestination,
                                    edu.neu.campus.app.SyncReason.MANUAL)
                            }
                        },
                        onLogin = onLoginClick
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
                        contentPadding = PaddingValues(
                            start = CampusSpacing.screenHorizontal,
                            end = CampusSpacing.screenHorizontal,
                            top = CampusSpacing.xs,
                            bottom = CampusSpacing.screenBottom
                        ),
                        verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                    ) {
                        itemsIndexed(taskList, key = { _, task -> task.id }) { index, task ->
                            StaggeredAppear(
                                index = index,
                                key = taskList.size,
                                modifier = Modifier.animateItem()
                            ) {
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
    }

    // 任务详情弹窗
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
    val campusColors = CampusTheme.colors
    val badgeIcon = when (task.kind) {
        TaskKind.TODO -> MiuixIcons.Regular.Tasks
        TaskKind.DONE -> MiuixIcons.Regular.Ok
        TaskKind.APPLICATION -> MiuixIcons.Regular.Send
    }
    val badgeTint = when (task.kind) {
        TaskKind.TODO -> campusColors.brand
        TaskKind.DONE -> campusColors.success
        TaskKind.APPLICATION -> campusColors.gradeForeground
    }
    val badgeContainer = when (task.kind) {
        TaskKind.TODO -> campusColors.brandContainer
        TaskKind.DONE -> campusColors.successContainer
        TaskKind.APPLICATION -> campusColors.gradeContainer
    }

    CampusCard(onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
        ) {
            CampusIconBadge(
                icon = badgeIcon,
                tint = badgeTint,
                container = badgeContainer,
                size = 40.dp,
                iconSize = 20.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = campusColors.textPrimary,
                    maxLines = 2
                )

                task.time?.let { tTime ->
                    Text(text = tTime, fontSize = 12.sp, color = campusColors.textTertiary)
                }
            }

            Text(
                text = "查看 ›",
                fontSize = 13.sp,
                color = campusColors.brand,
                modifier = Modifier.padding(start = CampusSpacing.xxs)
            )
        }
    }
}

@Composable
private fun SchemaChangedCard(context: Context) {
    val campusColors = CampusTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
    ) {
        CampusCard(contentPadding = PaddingValues(CampusSpacing.lg)) {
            Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                ) {
                    Icon(
                        imageVector = MiuixIcons.Regular.Report,
                        contentDescription = "警示",
                        tint = campusColors.warning,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "暂时无法查看待办事项",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = campusColors.textPrimary
                    )
                }

                Text(
                    text = "请前往学校官方门户查看和办理。",
                    fontSize = 13.sp,
                    color = campusColors.textPrimary,
                    lineHeight = 20.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
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
                        Text("打开官方门户")
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
    val campusColors = CampusTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .tapScale(onClick = onDismiss)
            .padding(CampusSpacing.xl),
        contentAlignment = Alignment.Center
    ) {
        CampusCard(
            contentPadding = PaddingValues(CampusSpacing.lg)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                Text(
                    text = task.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = campusColors.textPrimary
                )

                CampusGroup {
                    DetailRow(
                        label = "事项类型",
                        value = when (task.kind) {
                            TaskKind.TODO -> "待办事项"
                            TaskKind.DONE -> "已办事项"
                            TaskKind.APPLICATION -> "我的申请"
                        }
                    )
                    CampusGroupDivider()
                    DetailRow(label = "发生时间", value = task.time ?: "未提供具体时间")
                    CampusGroupDivider()
                    DetailRow(label = "来源系统", value = "智慧东大办公门户")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
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
                        colors = ButtonDefaults.buttonColorsPrimary(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("在官方系统办理")
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

@Composable
private fun DetailRow(label: String, value: String) {
    val campusColors = CampusTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = CampusSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = campusColors.textSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = campusColors.textPrimary)
    }
}

package edu.neu.campus.app.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.config.HomeLayoutConfigManager
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.app.registry.FeatureRegistry
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.QueryCard
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.TimeFormatter
import edu.neu.campus.ui.timetable.CourseDetailBottomSheet
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayScreen(
    onLoginClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val academic = CampusDataProvider.academic
    val portal = CampusDataProvider.portal
    val session = CampusDataProvider.session

    val sessionState by session.state.collectAsState()
    val termsSnapshot by academic.terms().collectAsState()
    val currentTerm = termsSnapshot.data?.firstOrNull { it.isCurrent } ?: termsSnapshot.data?.firstOrNull()

    // 教学周
    val weeksSnapshot = currentTerm?.let { academic.weeks(it.id).collectAsState().value }
    val currentWeek = weeksSnapshot?.data?.firstOrNull { it.isCurrent } ?: weeksSnapshot?.data?.firstOrNull()

    // 今日周几 (1..7)
    val todayDayOfWeek = remember {
        val cal = Calendar.getInstance()
        when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    // 格式化今日公历日期
    val todayDateStr = remember {
        val sdf = SimpleDateFormat("M 月 d 日 EEE", Locale.SIMPLIFIED_CHINESE).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }
        sdf.format(Date())
    }

    // 课表快照
    val timetableSnapshot = if (currentTerm != null) {
        academic.timetable(currentTerm.id, currentWeek?.number).collectAsState().value
    } else null

    // 余额快照
    val cardSnapshot by portal.balance(BalanceKind.CAMPUS_CARD).collectAsState()
    val netSnapshot by portal.balance(BalanceKind.NETWORK).collectAsState()

    // 消息未读
    val messagesSnapshot by portal.messages(page = 1, pageSize = 5).collectAsState()
    val hasUnreadMessage = (messagesSnapshot.data?.unreadCount ?: 0) > 0 ||
            messagesSnapshot.data?.items?.any { it.serverRead == false } == true

    // 考试与待办快照
    val examsSnapshot = currentTerm?.let { academic.exams(it.id).collectAsState().value }
    val tasksSnapshot by portal.tasks(TaskKind.TODO, page = 1, pageSize = 5).collectAsState()

    // 首次自动同步
    LaunchedEffect(Unit) {
        academic.refreshTerms()
        portal.refreshBalance(BalanceKind.CAMPUS_CARD)
        portal.refreshBalance(BalanceKind.NETWORK)
        portal.refreshMessages(page = 1, pageSize = 5)
    }

    LaunchedEffect(currentTerm?.id) {
        val termId = currentTerm?.id ?: return@LaunchedEffect
        academic.refreshWeeks(termId)
        academic.refreshCampuses(termId)
        academic.refreshTimetable(termId, currentWeek?.number)
        academic.refreshExams(termId)
        portal.refreshTasks(TaskKind.TODO, page = 1, pageSize = 5)
    }

    // 计算今日排课
    val arrangedCourses = timetableSnapshot?.data?.arranged.orEmpty()
    val todayCourses = remember(arrangedCourses, todayDayOfWeek) {
        arrangedCourses.filter { it.dayOfWeek == todayDayOfWeek }.sortedBy { it.beginSection }
    }

    // 计算当前课程/下一节课
    val nowTimeStr = remember {
        SimpleDateFormat("HH:mm", Locale.SIMPLIFIED_CHINESE).apply {
            timeZone = TimeZone.getTimeZone("Asia/Shanghai")
        }.format(Date())
    }

    var inspectingCourse by remember { mutableStateOf<CourseOccurrence?>(null) }
    val hideBalance = edu.neu.campus.app.feature.balance.BalancePrivacyManager.isBalanceMasked

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 顶部标题区：左侧“今日”，右侧消息入口与红点
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "今日",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MiuixTheme.colorScheme.onSurface
                )
                val weekText = if (currentWeek != null) " · 第 ${currentWeek.number} 教学周" else ""
                Text(
                    text = "$todayDateStr$weekText",
                    fontSize = 14.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { AppNavigator.navigateTo(AppDestination.Messages) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "消息中心",
                    tint = MiuixTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
                if (hasUnreadMessage) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .background(Color(0xFFE53935), CircleShape)
                    )
                }
            }
        }

        // 2. 必要状态条（离线或会话失效时展示）
        if (sessionState.portal == DomainStatus.EXPIRED || sessionState.academic == DomainStatus.EXPIRED ||
            sessionState.portal == DomainStatus.SIGNED_OUT || sessionState.academic == DomainStatus.SIGNED_OUT
        ) {
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE53935).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "学校账号需要认证",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "会话已过期，重新登录以同步课表与数据",
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text("前往登录")
                    }
                }
            }
        } else if (timetableSnapshot?.isStale == true || cardSnapshot.isStale) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MiuixTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "网络连接受阻，当前显示最后成功的本地缓存",
                    fontSize = 12.sp,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                )
            }
        }

        // 3. 下一节课卡片（本页视觉重点）
        NextCourseCard(
            courses = todayCourses,
            nowTimeStr = nowTimeStr,
            isLoading = timetableSnapshot?.phase == QueryPhase.LOADING && timetableSnapshot.data == null,
            error = timetableSnapshot?.error,
            onCourseClick = { inspectingCourse = it },
            onGotoTimetable = { AppNavigator.navigateToTab(MainTab.TIMETABLE) },
            onRetry = {
                currentTerm?.let {
                    coroutineScope.launch { academic.refreshTimetable(it.id, currentWeek?.number) }
                }
            }
        )

        // 4. 快捷查询一行（前 3 项可配置替换，第 4 项固定“全部”）
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val quickIds = HomeLayoutConfigManager.quickActionIds
            quickIds.take(3).forEach { featureId ->
                val feat = FeatureRegistry.findById(featureId)
                if (feat != null) {
                    QuickActionButton(
                        title = feat.title.take(2),
                        featureId = feat.id,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when (feat.id) {
                                FeatureRegistry.ID_GRADES -> AppNavigator.navigateTo(AppDestination.Grades)
                                FeatureRegistry.ID_EXAMS -> AppNavigator.navigateTo(AppDestination.Exams)
                                FeatureRegistry.ID_CAMPUS_CARD -> AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.CAMPUS_CARD))
                                FeatureRegistry.ID_NETWORK -> AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.NETWORK))
                                FeatureRegistry.ID_MESSAGES -> AppNavigator.navigateTo(AppDestination.Messages)
                                FeatureRegistry.ID_TASKS -> AppNavigator.navigateTo(AppDestination.Tasks)
                                else -> AppNavigator.navigateToTab(MainTab.QUERY)
                            }
                        }
                    )
                }
            }
            // 第 4 项固定“全部”
            QuickActionButton(
                title = "全部",
                featureId = "all",
                modifier = Modifier.weight(1f),
                onClick = { AppNavigator.navigateToTab(MainTab.QUERY) }
            )
        }

        // 5. 可配置摘要区（按用户偏好排序与显示）
        HomeLayoutConfigManager.modules.filter { it.enabled }.forEach { module ->
            when (module.id) {
                HomeLayoutConfigManager.MODULE_TODAY_COURSES -> {
                    // 今天的课程
                    QueryCard(
                        title = "今天的课程",
                        actionText = "看课表 ›",
                        onActionClick = { AppNavigator.navigateToTab(MainTab.TIMETABLE) }
                    ) {
                        if (todayCourses.isEmpty()) {
                            Text(
                                text = "今天没有已排课程安排",
                                fontSize = 14.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                todayCourses.take(4).forEach { c ->
                                    val endTimeStr = c.endTime
                                    val isEnded = endTimeStr != null && endTimeStr < nowTimeStr
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { inspectingCourse = c }
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${c.beginTime ?: "第${c.beginSection}节"}  ${c.title}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (isEnded) MiuixTheme.colorScheme.onSurfaceSecondary else MiuixTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = c.place ?: "教室待定",
                                                fontSize = 12.sp,
                                                color = MiuixTheme.colorScheme.onSurfaceSecondary
                                            )
                                        }
                                        Text(
                                            text = if (isEnded) "已结束" else "待上课",
                                            fontSize = 12.sp,
                                            color = if (isEnded) MiuixTheme.colorScheme.onSurfaceSecondary else MiuixTheme.colorScheme.primary
                                        )
                                    }
                                }
                                if (todayCourses.size > 4) {
                                    Text(
                                        text = "查看全部 ${todayCourses.size} 项 ›",
                                        fontSize = 12.sp,
                                        color = MiuixTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { AppNavigator.navigateToTab(MainTab.TIMETABLE) }
                                            .padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                HomeLayoutConfigManager.MODULE_CAMPUS_LIFE -> {
                    // 校园生活余额摘要
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "校园生活",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MiuixTheme.colorScheme.surfaceContainer)
                                    .clickable { edu.neu.campus.app.feature.balance.BalancePrivacyManager.toggleMasked() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (hideBalance) "显示余额" else "隐藏余额",
                                    fontSize = 12.sp,
                                    color = MiuixTheme.colorScheme.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 校园卡余额卡片
                            QueryCard(
                                modifier = Modifier.weight(1f),
                                title = "校园卡",
                                onClick = { AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.CAMPUS_CARD)) }
                            ) {
                                val card = cardSnapshot.data
                                val hasCardVal = card?.rawValue != null
                                val displayVal = if (hideBalance) "¥ ••••" else if (hasCardVal) "¥ ${card.rawValue}" else "未同步"
                                Text(
                                    text = displayVal,
                                    fontSize = if (hasCardVal || hideBalance) 19.sp else 15.sp,
                                    fontWeight = if (hasCardVal || hideBalance) FontWeight.Bold else FontWeight.Medium,
                                    color = if (hasCardVal || hideBalance) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "更新于 ${TimeFormatter.formatTime(cardSnapshot.lastSuccessEpochMillis)}",
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }

                            // 网费余额卡片
                            QueryCard(
                                modifier = Modifier.weight(1f),
                                title = "网费",
                                onClick = { AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.NETWORK)) }
                            ) {
                                val net = netSnapshot.data
                                val hasNetVal = net?.rawValue != null
                                val displayVal = if (hideBalance) "¥ ••••" else if (hasNetVal) "¥ ${net.rawValue}" else "未同步"
                                Text(
                                    text = displayVal,
                                    fontSize = if (hasNetVal || hideBalance) 19.sp else 15.sp,
                                    fontWeight = if (hasNetVal || hideBalance) FontWeight.Bold else FontWeight.Medium,
                                    color = if (hasNetVal || hideBalance) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Text(
                                    text = "更新于 ${TimeFormatter.formatTime(netSnapshot.lastSuccessEpochMillis)}",
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }
                        }
                    }
                }
                HomeLayoutConfigManager.MODULE_RECENT_EXAMS -> {
                    // 最近考试摘要（有考试数据时才显示卡片，避免空卡）
                    val exams = examsSnapshot?.data.orEmpty()
                    if (exams.isNotEmpty()) {
                        val upcoming = exams.firstOrNull { it.arranged } ?: exams.first()
                        QueryCard(
                            title = "最近考试",
                            actionText = "查看全部 ›",
                            onActionClick = { AppNavigator.navigateTo(AppDestination.ExamDetail("")) },
                            onClick = { AppNavigator.navigateTo(AppDestination.ExamDetail(upcoming.courseName)) }
                        ) {
                            Text(
                                text = upcoming.courseName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = MiuixTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${upcoming.timeDescription ?: "时间待定"} · ${upcoming.place ?: "考场待公布"}",
                                fontSize = 13.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
                HomeLayoutConfigManager.MODULE_RECENT_TASKS -> {
                    // 待办事项摘要
                    val tasks = tasksSnapshot.data?.items.orEmpty()
                    if (tasks.isNotEmpty()) {
                        QueryCard(
                            title = "待办事项 (${tasksSnapshot.data?.total ?: tasks.size})",
                            actionText = "查看列表 ›",
                            onActionClick = { AppNavigator.navigateTo(AppDestination.Tasks) }
                        ) {
                            tasks.take(2).forEach { t ->
                                Text(
                                    text = "• ${t.title}",
                                    fontSize = 13.sp,
                                    color = MiuixTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 详情抽屉
    inspectingCourse?.let { c ->
        CourseDetailBottomSheet(
            course = c,
            lastUpdatedTime = timetableSnapshot?.lastSuccessEpochMillis,
            onDismiss = { inspectingCourse = null }
        )
    }
}

@Composable
private fun NextCourseCard(
    courses: List<CourseOccurrence>,
    nowTimeStr: String,
    isLoading: Boolean,
    error: QueryError?,
    onCourseClick: (CourseOccurrence) -> Unit,
    onGotoTimetable: () -> Unit,
    onRetry: () -> Unit
) {
    // 视觉焦点卡片
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(18.dp)
    ) {
        when {
            isLoading -> {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "正在同步今日排课…",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                }
            }
            error != null -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "排课数据暂未更新",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )
                    Text(
                        text = error.message.ifBlank { "查询失败，请点击重试" },
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )
                    Button(onClick = onRetry) { Text("重新加载") }
                }
            }
            courses.isEmpty() -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "今天没有已安排课程",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "好好休息或查看本周后续排课",
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }
                    Button(onClick = onGotoTimetable) {
                        Text("查看课表 ›")
                    }
                }
            }
            else -> {
                // 查找当前进行中或即将开始的课程
                val ongoing = courses.firstOrNull {
                    val b = it.beginTime
                    val e = it.endTime
                    b != null && e != null && nowTimeStr >= b && nowTimeStr <= e
                }
                val upcoming = courses.firstOrNull {
                    val b = it.beginTime
                    b != null && b > nowTimeStr
                } ?: courses.lastOrNull()

                val target = ongoing ?: upcoming
                val isOngoing = ongoing != null

                if (target != null) {
                    val statusTag = if (isOngoing) "正在上课" else "下一节课"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = statusTag,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOngoing) Color(0xFF2E7D32) else MiuixTheme.colorScheme.primary
                        )
                        val b = target.beginTime
                        val e = target.endTime
                        val timeSpan = if (b != null && e != null) {
                            "$b—$e"
                        } else "第 ${target.beginSection}-${target.endSection} 节"
                        Text(
                            text = timeSpan,
                            fontSize = 13.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }

                    Text(
                        text = target.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Text(
                        text = "${target.place ?: "教室待定"} · 第 ${target.beginSection}-${target.endSection} 节",
                        fontSize = 14.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = { onCourseClick(target) }) {
                            Text("查看详情 ›")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    featureId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor) = when (featureId) {
        FeatureRegistry.ID_GRADES -> Pair(Icons.Default.Star, Color(0xFFFF9800))
        FeatureRegistry.ID_EXAMS -> Pair(Icons.Default.DateRange, Color(0xFF7E57C2))
        FeatureRegistry.ID_TASKS -> Pair(Icons.Default.CheckCircle, Color(0xFF26A69A))
        FeatureRegistry.ID_CAMPUS_CARD -> Pair(Icons.Default.AccountBox, Color(0xFF42A5F5))
        FeatureRegistry.ID_NETWORK -> Pair(Icons.Default.Share, Color(0xFF5C6BC0))
        FeatureRegistry.ID_MESSAGES -> Pair(Icons.Default.Notifications, Color(0xFFEF5350))
        "all" -> Pair(Icons.Default.Menu, MiuixTheme.colorScheme.primary)
        else -> Pair(Icons.Default.Search, MiuixTheme.colorScheme.primary)
    }

    Card(
        colors = CardDefaults.defaultColors(),
        insideMargin = PaddingValues(0.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(19.dp)
                )
            }
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MiuixTheme.colorScheme.onSurface
            )
        }
    }
}

package edu.neu.campus.app.feature.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.timetable.CourseDetailBottomSheet
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayBottomSheet
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(
    onLoginClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val academic = CampusDataProvider.academic
    val portal = CampusDataProvider.portal
    val session = CampusDataProvider.session
    val colors = CampusTheme.colors
    val compact = LocalConfiguration.current.screenWidthDp < 360 || LocalDensity.current.fontScale >= 1.3f

    val sessionState by session.state.collectAsState()
    val termsSnapshot by academic.terms().collectAsState()
    val currentTerm = termsSnapshot.data?.firstOrNull { it.isCurrent }

    // 教学周
    val weeksSnapshot = currentTerm?.let { academic.weeks(it.id).collectAsState().value }
    val currentWeek = weeksSnapshot?.data?.firstOrNull { it.isCurrent }

    val schoolClock = rememberSchoolClock()
    val todayDayOfWeek = (schoolClock.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1
    val todayDateStr = SimpleDateFormat("M 月 d 日 EEE", Locale.SIMPLIFIED_CHINESE).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }.format(schoolClock.time)

    // 课表快照
    val timetableSnapshot = if (currentTerm != null && currentWeek != null) {
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

    LaunchedEffect(currentTerm?.id, todayDateStr) {
        val termId = currentTerm?.id ?: return@LaunchedEffect
        academic.refreshWeeks(termId)
        academic.refreshCampuses(termId)
        academic.refreshExams(termId)
        portal.refreshTasks(TaskKind.TODO, page = 1, pageSize = 5)
    }

    // 计算今日排课
    val arrangedCourses = timetableSnapshot?.data?.arranged.orEmpty()
    val todayCourses = remember(arrangedCourses, todayDayOfWeek) {
        arrangedCourses.filter { it.dayOfWeek == todayDayOfWeek }.sortedBy { it.beginSection }
    }

    LaunchedEffect(currentTerm?.id, currentWeek?.number) {
        val termId = currentTerm?.id ?: return@LaunchedEffect
        val week = currentWeek?.number ?: return@LaunchedEffect
        academic.refreshTimetable(termId, week)
    }
    val nowTimeStr = SimpleDateFormat("HH:mm", Locale.CHINA).apply {
        timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    }.format(schoolClock.time)

    var inspectingCourse by remember { mutableStateOf<CourseOccurrence?>(null) }
    var conflictCourses by remember { mutableStateOf<List<CourseOccurrence>?>(null) }
    val hideBalance = edu.neu.campus.app.feature.balance.BalancePrivacyManager.isBalanceMasked

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. 顶部标题区：标题 28sp Bold，日期 TextSecondary，右侧 48dp 消息按钮带圆底
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "今日",
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                val weekText = if (currentWeek != null) " · 第 ${currentWeek.number} 教学周" else ""
                Text(
                    text = "$todayDateStr$weekText",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.surface)
                    .clickable { AppNavigator.navigateTo(AppDestination.Messages) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "消息中心",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(24.dp)
                )
                if (hasUnreadMessage) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-8).dp, y = 8.dp)
                            .clip(CircleShape)
                            .background(colors.error)
                    )
                }
            }
        }

        // 2. 状态提示条（会话失效或离线旧缓存时呈现）
        if (sessionState.portal == DomainStatus.EXPIRED || sessionState.academic == DomainStatus.EXPIRED ||
            sessionState.portal == DomainStatus.SIGNED_OUT || sessionState.academic == DomainStatus.SIGNED_OUT
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .padding(14.dp)
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
                                .background(colors.error.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = colors.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "学校账号需要重新认证",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "登录已过期，请重新登录以获取最新课表",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColorsPrimary()
                    ) {
                        Text("登录")
                    }
                }
            }
        } else if (timetableSnapshot?.isStale == true || cardSnapshot.isStale) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceMuted)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "当前包含上次同步的数据，请留意各项同步时间",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        // 3. A 类品牌主课程卡（NextCourseCard）
        HeroCourseCard(
            courses = todayCourses,
            nowTimeStr = nowTimeStr,
            isLoading = (termsSnapshot.phase == QueryPhase.LOADING || weeksSnapshot?.phase == QueryPhase.LOADING || timetableSnapshot?.phase == QueryPhase.LOADING) && timetableSnapshot?.data == null,
            error = timetableSnapshot?.error ?: weeksSnapshot?.error ?: termsSnapshot.error,
            isStale = timetableSnapshot?.isStale ?: false,
            hasConfirmedTerm = currentTerm != null && currentWeek != null,
            hasData = timetableSnapshot?.data != null,
            onClickCourse = { inspectingCourse = it },
            onConflictClick = { conflictCourses = it },
            onGotoTimetable = { AppNavigator.navigateToTab(MainTab.TIMETABLE) },
            onRetry = {
                coroutineScope.launch {
                    academic.refreshTerms()
                    currentTerm?.let { term ->
                        academic.refreshWeeks(term.id)
                        currentWeek?.let { academic.refreshTimetable(term.id, it.number) }
                    }
                }
            }
        )

        // 4. 快捷入口无外壳四等列（图标上、标签下，采用完整短标签与统一功能色）
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = if (compact) 2 else 4,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val quickIds = HomeLayoutConfigManager.quickActionIds
            val candidateItems = listOf(
                FeatureRegistry.ID_GRADES to ("成绩" to (Icons.Default.Star to (colors.gradeForeground to colors.gradeContainer))),
                FeatureRegistry.ID_EXAMS to ("考试" to (Icons.Default.DateRange to (colors.examForeground to colors.examContainer))),
                FeatureRegistry.ID_TASKS to ("待办" to (Icons.Default.CheckCircle to (colors.messageForeground to colors.messageContainer))),
                FeatureRegistry.ID_CAMPUS_CARD to ("校园卡" to (Icons.Default.AccountBox to (colors.cardForeground to colors.cardContainer))),
                FeatureRegistry.ID_NETWORK to ("网费" to (Icons.Default.Share to (colors.networkForeground to colors.networkContainer))),
                FeatureRegistry.ID_MESSAGES to ("消息" to (Icons.Default.Notifications to (colors.messageForeground to colors.messageContainer)))
            ).toMap()

            quickIds.take(3).forEach { featureId ->
                val feat = FeatureRegistry.findById(featureId)
                val config = candidateItems[featureId]
                val label = config?.first ?: (feat?.title ?: "功能")
                val icon = config?.second?.first ?: Icons.Default.Search
                val (fg, bg) = config?.second?.second ?: (colors.brand to colors.brandContainer)

                QuickActionItem(
                    title = label,
                    icon = icon,
                    iconTint = fg,
                    iconContainer = bg,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        when (featureId) {
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

            // 第 4 项固定“全部”
            QuickActionItem(
                title = "全部",
                icon = Icons.Default.Menu,
                iconTint = colors.brand,
                iconContainer = colors.brandContainer,
                modifier = Modifier.weight(1f),
                onClick = { AppNavigator.navigateToTab(MainTab.QUERY) }
            )
        }

        // 5. 可配置摘要模块区（遵循 Section 4.2）
        HomeLayoutConfigManager.modules.filter { it.enabled }.forEach { module ->
            when (module.id) {
                HomeLayoutConfigManager.MODULE_TODAY_COURSES -> {
                    // 今日课程时间轴
                    CampusSection(
                        title = "今日课程",
                        actionText = "完整课表 ›",
                        onActionClick = { AppNavigator.navigateToTab(MainTab.TIMETABLE) }
                    ) {
                        if (timetableSnapshot?.data == null) {
                            LoadStatePanel(
                                isLoading = timetableSnapshot?.phase == QueryPhase.LOADING,
                                error = timetableSnapshot?.error ?: weeksSnapshot?.error ?: termsSnapshot.error,
                                emptyMessage = "尚未获取已确认教学周的课程"
                            )
                        } else CourseTimeline(
                            courses = todayCourses,
                            trusted = !timetableSnapshot.isStale,
                            nowTimeStr = nowTimeStr,
                            onCourseClick = { inspectingCourse = it },
                            onSeeAllClick = { AppNavigator.navigateToTab(MainTab.TIMETABLE) }
                        )
                    }
                }

                HomeLayoutConfigManager.MODULE_CAMPUS_LIFE -> {
                    // 校园生活：两张轻色余额卡片（校园卡绿、网费青）
                    CampusSection(
                        title = "校园生活",
                        actionText = if (hideBalance) "显示余额" else "隐藏余额",
                        onActionClick = { edu.neu.campus.app.feature.balance.BalancePrivacyManager.toggleMasked() }
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            maxItemsInEachRow = if (compact) 1 else 2,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // 校园卡余额卡片 (Light Green)
                            BalanceSummaryCard(
                                title = "校园卡",
                                balanceValue = cardSnapshot.data?.rawValue,
                                serverMasked = cardSnapshot.data?.isMasked == true,
                                stateError = cardSnapshot.error?.message,
                                stale = cardSnapshot.isStale,
                                lastSuccessEpochMillis = cardSnapshot.lastSuccessEpochMillis,
                                isMasked = hideBalance,
                                cardBg = colors.cardContainer,
                                iconColor = colors.cardForeground,
                                icon = Icons.Default.AccountBox,
                                modifier = Modifier.weight(1f),
                                onClick = { AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.CAMPUS_CARD)) }
                            )

                            // 网费余额卡片 (Light Cyan)
                            BalanceSummaryCard(
                                title = "网费",
                                balanceValue = netSnapshot.data?.rawValue,
                                serverMasked = netSnapshot.data?.isMasked == true,
                                stateError = netSnapshot.error?.message,
                                stale = netSnapshot.isStale,
                                lastSuccessEpochMillis = netSnapshot.lastSuccessEpochMillis,
                                isMasked = hideBalance,
                                cardBg = colors.networkContainer,
                                iconColor = colors.networkForeground,
                                icon = Icons.Default.Share,
                                modifier = Modifier.weight(1f),
                                onClick = { AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.NETWORK)) }
                            )
                        }
                    }
                }

                HomeLayoutConfigManager.MODULE_RECENT_EXAMS -> {
                    // 最近考试摘要
                    val exams = examsSnapshot?.data.orEmpty()
                    if (examsSnapshot?.error != null) SafeDataTag(text = "考试更新失败：${examsSnapshot.error?.message}")
                    else if (examsSnapshot?.isStale == true) SafeDataTag(text = "考试摘要为上次同步数据")
                    if (exams.isNotEmpty()) {
                        val upcoming = exams.firstOrNull { it.arranged } ?: exams.first()
                        CampusSection(
                            title = "考试摘要",
                            actionText = "查看全部 ›",
                            onActionClick = { AppNavigator.navigateTo(AppDestination.Exams) }
                        ) {
                            CampusGroup {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { AppNavigator.navigateTo(AppDestination.ExamDetail(currentTerm!!.id, upcoming)) }
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = upcoming.courseName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(colors.examContainer)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (upcoming.arranged) "已排考" else "待排考",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.examForeground
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${upcoming.timeDescription ?: "时间待定"} · ${upcoming.place ?: "地点未提供"}",
                                        fontSize = 13.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                HomeLayoutConfigManager.MODULE_RECENT_TASKS -> {
                    // 待办事项摘要
                    val tasks = tasksSnapshot.data?.items.orEmpty()
                    if (tasksSnapshot.error != null) SafeDataTag(text = "待办更新失败：${tasksSnapshot.error?.message}")
                    else if (tasksSnapshot.isStale) SafeDataTag(text = "待办摘要为上次同步数据")
                    if (tasks.isNotEmpty()) {
                        CampusSection(
                            title = "待办事项 (${tasksSnapshot.data?.total ?: tasks.size})",
                            actionText = "查看列表 ›",
                            onActionClick = { AppNavigator.navigateTo(AppDestination.Tasks) }
                        ) {
                            CampusGroup {
                                tasks.take(2).forEachIndexed { idx, t ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(colors.messageForeground)
                                        )
                                        Text(
                                            text = t.title,
                                            fontSize = 14.sp,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (idx < tasks.take(2).lastIndex) {
                                        CampusGroupDivider(startIndent = 14.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // 冲突课程选择抽屉
    conflictCourses?.let { conflicts ->
        OverlayBottomSheet(
            show = true,
            title = "课程列表 (${conflicts.size} 项)",
            onDismissRequest = { conflictCourses = null },
            endAction = {
                Button(onClick = { conflictCourses = null }) { Text("关闭") }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "请选择具体课程查看详情：",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                conflicts.forEach { c ->
                    QueryCard(
                        title = c.title,
                        subtitle = "第 ${c.beginSection}-${c.endSection} 节 · ${c.place ?: "教室待定"}",
                        onClick = {
                            conflictCourses = null
                            inspectingCourse = c
                        }
                    ) {}
                }
            }
        }
    }

    // 课程详情原生抽屉
    inspectingCourse?.let { c ->
        val others = arrangedCourses.filter { it.title == c.title && it != c }
        CourseDetailBottomSheet(
            course = c,
            otherOccurrences = others,
            lastUpdatedTime = timetableSnapshot?.lastSuccessEpochMillis,
            onDismiss = { inspectingCourse = null }
        )
    }
}

/**
 * 首页无外壳快捷入口单项。
 * 遵循 Section 4.2：图标上、标签下，四等列，触控范围 >= 48dp，完整短标签。
 */
@Composable
private fun QuickActionItem(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    iconContainer: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = title,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}

/**
 * 校园生活轻色余额摘要小卡。
 * 遵循 Section 4.2：等宽轻色卡（校园卡绿、网费青），金额用 TextPrimary，最近同步 HH:mm。
 */
@Composable
private fun BalanceSummaryCard(
    title: String,
    balanceValue: String?,
    serverMasked: Boolean,
    stateError: String?,
    stale: Boolean,
    lastSuccessEpochMillis: Long?,
    isMasked: Boolean,
    cardBg: Color,
    iconColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = colors.textSecondary.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }

            val hasValue = balanceValue != null
            val displayVal = when {
                isMasked -> "¥ ••••"
                serverMasked -> "学校已遮罩"
                hasValue -> "¥ $balanceValue"
                else -> "未同步"
            }

            Text(
                text = displayVal,
                fontSize = if (isMasked || hasValue) 22.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Text(
                text = stateError ?: if (stale) "显示上次同步余额" else "最近同步 ${TimeFormatter.formatTime(lastSuccessEpochMillis)}",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    }
}

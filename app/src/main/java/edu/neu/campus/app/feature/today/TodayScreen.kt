package edu.neu.campus.app.feature.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.CampusDataProvider
import edu.neu.campus.app.config.HomeLayoutConfigManager
import edu.neu.campus.app.feature.balance.BalancePrivacyManager
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.app.navigation.MainTab
import edu.neu.campus.app.registry.FeatureRegistry
import edu.neu.campus.contract.*
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import edu.neu.campus.ui.timetable.CourseDetailBottomSheet
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.ArrowRight
import top.yukonga.miuix.kmp.icon.extended.Alarm
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Share
import top.yukonga.miuix.kmp.icon.extended.Tasks
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
    val hideBalance = BalancePrivacyManager.isBalanceMasked

    val sessionBroken = sessionState.portal == DomainStatus.EXPIRED ||
            sessionState.academic == DomainStatus.EXPIRED ||
            sessionState.portal == DomainStatus.SIGNED_OUT ||
            sessionState.academic == DomainStatus.SIGNED_OUT
    val hasStaleData = timetableSnapshot?.isStale == true || cardSnapshot.isStale

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.screenTop),
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.lg)
    ) {
        // 1. 顶部标题区
        TodayHeader(
            dateText = todayDateStr,
            weekNumber = currentWeek?.number,
            hasUnreadMessage = hasUnreadMessage,
            onMessagesClick = { AppNavigator.navigateTo(AppDestination.Messages) }
        )

        // 2. 状态提示
        AnimatedVisibility(
            visible = sessionBroken,
            enter = fadeIn(tween(CampusMotion.Duration.medium)) +
                expandVertically(tween(CampusMotion.Duration.long, easing = CampusMotion.Easing.emphasizedDecelerate)),
            exit = fadeOut(tween(CampusMotion.Duration.short)) + shrinkVertically(tween(CampusMotion.Duration.medium))
        ) {
            SessionExpiredBanner(onLoginClick = onLoginClick)
        }
        AnimatedVisibility(
            visible = !sessionBroken && hasStaleData,
            enter = fadeIn(tween(CampusMotion.Duration.medium)) +
                expandVertically(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)),
            exit = fadeOut(tween(CampusMotion.Duration.short)) + shrinkVertically(tween(CampusMotion.Duration.short))
        ) {
            StaleDataHint()
        }

        // 3. 品牌主课程卡
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

        // 4. 快捷入口
        QuickActionRow(
            compact = compact,
            onNavigate = { featureId ->
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

        // 5. 可配置摘要模块区
        HomeLayoutConfigManager.modules.filter { it.enabled }.forEachIndexed { moduleIndex, module ->
            when (module.id) {
                HomeLayoutConfigManager.MODULE_TODAY_COURSES -> {
                    StaggeredAppear(index = moduleIndex) {
                        CampusSection(
                            title = "今日课程",
                            actionText = "完整课表",
                            onActionClick = { AppNavigator.navigateToTab(MainTab.TIMETABLE) }
                        ) {
                            if (timetableSnapshot?.data == null) {
                                CampusCard {
                                    LoadStatePanel(
                                        isLoading = timetableSnapshot?.phase == QueryPhase.LOADING,
                                        error = timetableSnapshot?.error ?: weeksSnapshot?.error ?: termsSnapshot.error,
                                        emptyMessage = "尚未获取已确认教学周的课程"
                                    )
                                }
                            } else {
                                CourseTimeline(
                                    courses = todayCourses,
                                    trusted = !timetableSnapshot.isStale,
                                    nowTimeStr = nowTimeStr,
                                    onCourseClick = { inspectingCourse = it },
                                    onSeeAllClick = { AppNavigator.navigateToTab(MainTab.TIMETABLE) }
                                )
                            }
                        }
                    }
                }

                HomeLayoutConfigManager.MODULE_CAMPUS_LIFE -> {
                    StaggeredAppear(index = moduleIndex) {
                        CampusSection(
                            title = "校园生活",
                            actionText = if (hideBalance) "显示余额" else "隐藏余额",
                            onActionClick = { BalancePrivacyManager.toggleMasked() }
                        ) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = if (compact) 1 else 2,
                                verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                            ) {
                                BalanceSummaryCard(
                                    title = "校园卡",
                                    icon = MiuixIcons.Regular.BankCards,
                                    balanceValue = cardSnapshot.data?.rawValue,
                                    serverMasked = cardSnapshot.data?.isMasked == true,
                                    stateError = cardSnapshot.error?.message,
                                    stale = cardSnapshot.isStale,
                                    lastSuccessEpochMillis = cardSnapshot.lastSuccessEpochMillis,
                                    isMasked = hideBalance,
                                    accent = colors.cardForeground,
                                    container = colors.cardContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.CAMPUS_CARD)) }
                                )

                                BalanceSummaryCard(
                                    title = "网费",
                                    icon = MiuixIcons.Regular.Share,
                                    balanceValue = netSnapshot.data?.rawValue,
                                    serverMasked = netSnapshot.data?.isMasked == true,
                                    stateError = netSnapshot.error?.message,
                                    stale = netSnapshot.isStale,
                                    lastSuccessEpochMillis = netSnapshot.lastSuccessEpochMillis,
                                    isMasked = hideBalance,
                                    accent = colors.networkForeground,
                                    container = colors.networkContainer,
                                    modifier = Modifier.weight(1f),
                                    onClick = { AppNavigator.navigateTo(AppDestination.BalanceDetail(BalanceKind.NETWORK)) }
                                )
                            }
                        }
                    }
                }

                HomeLayoutConfigManager.MODULE_RECENT_EXAMS -> {
                    val exams = examsSnapshot?.data.orEmpty()
                    if (exams.isNotEmpty()) {
                        StaggeredAppear(index = moduleIndex) {
                            CampusSection(
                                title = "考试摘要",
                                actionText = "查看全部",
                                onActionClick = { AppNavigator.navigateTo(AppDestination.Exams) }
                            ) {
                                val upcoming = exams.firstOrNull { it.arranged } ?: exams.first()
                                CampusCard(
                                    onClick = { AppNavigator.navigateTo(AppDestination.ExamDetail(currentTerm!!.id, upcoming)) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                                        ) {
                                            CampusIconBadge(
                                                icon = MiuixIcons.Regular.Alarm,
                                                tint = colors.examForeground,
                                                container = colors.examContainer
                                            )
                                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                                Text(
                                                    text = upcoming.courseName,
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = colors.textPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${upcoming.timeDescription ?: "时间待定"} · ${upcoming.place ?: "地点未提供"}",
                                                    fontSize = 12.sp,
                                                    color = colors.textSecondary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        CampusPill(
                                            text = if (upcoming.arranged) "已排考" else "待排考",
                                            contentColor = colors.examForeground,
                                            containerColor = colors.examContainer
                                        )
                                    }
                                }
                                if (examsSnapshot?.error != null) {
                                    Spacer(modifier = Modifier.height(CampusSpacing.xs))
                                    SafeDataTag(text = "考试更新失败：${examsSnapshot.error?.message}")
                                } else if (examsSnapshot?.isStale == true) {
                                    Spacer(modifier = Modifier.height(CampusSpacing.xs))
                                    SafeDataTag(text = "考试摘要为上次同步数据")
                                }
                            }
                        }
                    }
                }

                HomeLayoutConfigManager.MODULE_RECENT_TASKS -> {
                    val tasks = tasksSnapshot.data?.items.orEmpty()
                    if (tasks.isNotEmpty()) {
                        StaggeredAppear(index = moduleIndex) {
                            CampusSection(
                                title = "待办事项 (${tasksSnapshot.data?.total ?: tasks.size})",
                                actionText = "查看列表",
                                onActionClick = { AppNavigator.navigateTo(AppDestination.Tasks) }
                            ) {
                                CampusGroup {
                                    tasks.take(2).forEachIndexed { idx, t ->
                                        CampusRow(
                                            title = t.title,
                                            leading = {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(colors.messageForeground)
                                                )
                                            },
                                            showChevron = true,
                                            onClick = { AppNavigator.navigateTo(AppDestination.Tasks) }
                                        )
                                        if (idx < tasks.take(2).lastIndex) {
                                            CampusGroupDivider(startIndent = 20.dp)
                                        }
                                    }
                                }
                                if (tasksSnapshot.error != null) {
                                    Spacer(modifier = Modifier.height(CampusSpacing.xs))
                                    SafeDataTag(text = "待办更新失败：${tasksSnapshot.error?.message}")
                                } else if (tasksSnapshot.isStale) {
                                    Spacer(modifier = Modifier.height(CampusSpacing.xs))
                                    SafeDataTag(text = "待办摘要为上次同步数据")
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(CampusSpacing.md))
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
                    .padding(CampusSpacing.md),
                verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
            ) {
                Text(
                    text = "请选择具体课程查看详情：",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                conflicts.forEachIndexed { index, c ->
                    StaggeredAppear(index = index, key = conflicts.size) {
                        QueryCard(
                            onClick = {
                                conflictCourses = null
                                inspectingCourse = c
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                            ) {
                                CampusIconBadge(
                                    icon = MiuixIcons.Regular.Notes,
                                    tint = colors.courseAccent(c.title),
                                    container = colors.courseColor(c.title).second
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = c.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "第 ${c.beginSection}-${c.endSection} 节 · ${c.place ?: "教室待定"}",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
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
 * 首页顶部标题区：大标题 + 日期教学周 + 消息按钮（未读圆点带呼吸）。
 */
@Composable
private fun TodayHeader(
    dateText: String,
    weekNumber: Int?,
    hasUnreadMessage: Boolean,
    onMessagesClick: () -> Unit
) {
    val colors = CampusTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "今日",
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            val weekText = if (weekNumber != null) " · 第 $weekNumber 教学周" else ""
            Text(
                text = "$dateText$weekText",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .tapScale(onClick = onMessagesClick, pressedScale = 0.92f, clipShape = CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MiuixIcons.Regular.Messages,
                contentDescription = "消息中心",
                tint = colors.textPrimary,
                modifier = Modifier.size(23.dp)
            )
            UnreadDot(
                visible = hasUnreadMessage,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/** 未读圆点，带出现/消失的淡入淡出。 */
@Composable
private fun UnreadDot(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(CampusMotion.Duration.short)) +
            androidx.compose.animation.scaleIn(initialScale = 0.6f, animationSpec = tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)),
        exit = fadeOut(tween(CampusMotion.Duration.instant)) +
            androidx.compose.animation.scaleOut(targetScale = 0.6f, animationSpec = tween(CampusMotion.Duration.instant)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-9).dp, y = 9.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(colors.error)
        )
    }
}

/** 会话失效提示条。 */
@Composable
private fun SessionExpiredBanner(onLoginClick: () -> Unit) {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CampusShapes.medium))
            .background(colors.errorContainer)
            .padding(CampusSpacing.sm + 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
            ) {
                CampusIconBadge(
                    icon = MiuixIcons.Regular.Alarm,
                    tint = colors.error,
                    container = colors.error.copy(alpha = 0.14f),
                    size = 38.dp,
                    iconSize = 20.dp,
                    cornerRadius = CampusShapes.extraSmall
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "学校账号需要重新认证",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onErrorContainer
                    )
                    Text(
                        text = "登录已过期，请重新登录以获取最新课表",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.width(CampusSpacing.xs))
            Button(onClick = onLoginClick) {
                Text("登录", fontSize = 14.sp)
            }
        }
    }
}

/** 旧缓存提示。 */
@Composable
private fun StaleDataHint() {
    val colors = CampusTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CampusShapes.extraSmall))
            .background(colors.surfaceMuted)
            .padding(horizontal = CampusSpacing.sm + 2.dp, vertical = CampusSpacing.xs)
    ) {
        Text(
            text = "当前包含上次同步的数据，请留意各项同步时间",
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}

private data class QuickAction(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val container: Color
)

/**
 * 快捷入口：无外壳等分列，按索引交错入场，按压缩放。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickActionRow(
    compact: Boolean,
    onNavigate: (String) -> Unit
) {
    val colors = CampusTheme.colors
    val quickIds = HomeLayoutConfigManager.quickActionIds
    val candidates = mapOf(
        FeatureRegistry.ID_GRADES to QuickAction(FeatureRegistry.ID_GRADES, "成绩", MiuixIcons.Regular.Notes, colors.gradeForeground, colors.gradeContainer),
        FeatureRegistry.ID_EXAMS to QuickAction(FeatureRegistry.ID_EXAMS, "考试", MiuixIcons.Regular.Alarm, colors.examForeground, colors.examContainer),
        FeatureRegistry.ID_TASKS to QuickAction(FeatureRegistry.ID_TASKS, "待办", MiuixIcons.Regular.Tasks, colors.messageForeground, colors.messageContainer),
        FeatureRegistry.ID_CAMPUS_CARD to QuickAction(FeatureRegistry.ID_CAMPUS_CARD, "校园卡", MiuixIcons.Regular.BankCards, colors.cardForeground, colors.cardContainer),
        FeatureRegistry.ID_NETWORK to QuickAction(FeatureRegistry.ID_NETWORK, "网费", MiuixIcons.Regular.Share, colors.networkForeground, colors.networkContainer),
        FeatureRegistry.ID_MESSAGES to QuickAction(FeatureRegistry.ID_MESSAGES, "消息", MiuixIcons.Regular.Messages, colors.messageForeground, colors.messageContainer)
    )

    val items = quickIds.take(3).mapNotNull { candidates[it] } + QuickAction(
        id = "__all__",
        label = "全部",
        icon = MiuixIcons.Regular.GridView,
        tint = colors.brand,
        container = colors.brandContainer
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        maxItemsInEachRow = if (compact) 2 else 4,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEachIndexed { index, item ->
            StaggeredAppear(index = index, modifier = Modifier.weight(1f)) {
                QuickActionItem(
                    item = item,
                    onClick = { onNavigate(item.id) }
                )
            }
        }
    }
}

@Composable
private fun QuickActionItem(
    item: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors

    Column(
        modifier = modifier
            .tapScale(onClick = onClick, pressedScale = 0.94f, clipShape = RoundedCornerShape(CampusShapes.small))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(CampusShapes.small + 2.dp))
                .background(item.container),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = item.tint,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = item.label,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}

/**
 * 校园生活余额卡：功能色柔化渐变底 + 数字滚动动画。
 */
@Composable
private fun BalanceSummaryCard(
    title: String,
    icon: ImageVector,
    balanceValue: String?,
    serverMasked: Boolean,
    stateError: String?,
    stale: Boolean,
    lastSuccessEpochMillis: Long?,
    isMasked: Boolean,
    accent: Color,
    container: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = CampusTheme.colors
    val amount = if (isMasked || serverMasked) null else parseAmount(balanceValue)
    val fallback = when {
        isMasked -> "¥ ••••"
        serverMasked -> "学校已遮罩"
        !balanceValue.isNullOrBlank() -> "¥ $balanceValue"
        else -> "未同步"
    }

    Box(
        modifier = modifier
            .tapScale(onClick = onClick, pressedScale = 0.97f, clipShape = RoundedCornerShape(CampusShapes.large))
            .background(
                Brush.linearGradient(
                    listOf(container, container.copy(alpha = if (colors.isDark) 0.55f else 0.7f))
                )
            )
            .padding(CampusSpacing.md)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
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
                        tint = accent,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
                Icon(
                    imageVector = MiuixIcons.Basic.ArrowRight,
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(15.dp)
                )
            }

            AnimatedNumber(
                target = amount,
                fallback = fallback,
                prefix = "¥ ",
                decimals = 2,
                fontSize = if (amount != null) 23.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Text(
                text = stateError ?: if (stale) "显示上次同步余额" else "最近同步 ${TimeFormatter.formatTime(lastSuccessEpochMillis)}",
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** 从学校返回的余额文本中提取可动画的数值，失败时返回 null 以回退原文展示。 */
private fun parseAmount(raw: String?): Float? {
    if (raw.isNullOrBlank()) return null
    val normalized = raw.replace(",", "").trim()
    val match = Regex("[-+]?\\d+(\\.\\d+)?").find(normalized) ?: return null
    return match.value.toFloatOrNull()
}

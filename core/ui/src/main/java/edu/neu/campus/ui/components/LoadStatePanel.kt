package edu.neu.campus.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.QueryError
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.ui.theme.CampusMotion
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Info
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Report
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.icon.extended.WorldClock

private enum class PanelState { Loading, Error, Empty, Idle }

/**
 * 统一的数据加载、异常与空状态呈现面板。
 *
 * 组件约定：
 * - 覆盖加载中、网络异常、认证失效、无权限、数据结构变化与空数据
 * - 状态之间使用淡入 + 轻缩放的转场，避免生硬跳变
 * - 加载态使用骨架屏而非仅转圈，让等待过程有形状预期
 */
@Composable
fun LoadStatePanel(
    isLoading: Boolean,
    error: QueryError? = null,
    emptyMessage: String? = null,
    emptyActionText: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
    onLogin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state = when {
        isLoading -> PanelState.Loading
        error != null -> PanelState.Error
        emptyMessage != null -> PanelState.Empty
        else -> PanelState.Idle
    }

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            (fadeIn(tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)) +
                scaleIn(
                    initialScale = 0.94f,
                    animationSpec = tween(CampusMotion.Duration.medium, easing = CampusMotion.Easing.emphasizedDecelerate)
                ))
                .togetherWith(
                    fadeOut(tween(CampusMotion.Duration.instant)) +
                        scaleOut(
                            targetScale = 1.02f,
                            animationSpec = tween(CampusMotion.Duration.instant)
                        )
                )
                .using(SizeTransform(clip = false))
        },
        label = "loadStatePanel",
        modifier = modifier.fillMaxWidth()
    ) { current ->
        when (current) {
            PanelState.Loading -> LoadingPanel()
            PanelState.Error -> ErrorPanel(error, onRetry, onLogin)
            PanelState.Empty -> EmptyPanel(emptyMessage.orEmpty(), emptyActionText, onEmptyAction)
            PanelState.Idle -> Spacer(modifier = Modifier.height(0.dp))
        }
    }
}

@Composable
private fun LoadingPanel() {
    val colors = CampusTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CampusSpacing.md, vertical = CampusSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
    ) {
        Text(
            text = "正在同步学校数据…",
            color = colors.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        ShimmerLine(height = 46.dp, cornerRadius = CampusShapes.small)
        ShimmerLine(height = 46.dp, cornerRadius = CampusShapes.small)
        ShimmerLine(modifier = Modifier.fillMaxWidth(0.62f), height = 46.dp, cornerRadius = CampusShapes.small)
    }
}

@Composable
private fun ErrorPanel(
    error: QueryError?,
    onRetry: (() -> Unit)?,
    onLogin: (() -> Unit)?
) {
    val colors = CampusTheme.colors
    val kind = error?.kind

    val iconVector: ImageVector
    val iconBg: Color
    val iconTint: Color
    when (kind) {
        QueryErrorKind.AUTH_REQUIRED -> {
            iconVector = MiuixIcons.Regular.Lock
            iconBg = colors.brandContainer
            iconTint = colors.brand
        }
        QueryErrorKind.NETWORK -> {
            iconVector = MiuixIcons.Regular.WorldClock
            iconBg = colors.warningContainer
            iconTint = colors.warning
        }
        QueryErrorKind.SCHEMA_CHANGED -> {
            iconVector = MiuixIcons.Regular.Info
            iconBg = colors.surfaceMuted
            iconTint = colors.textSecondary
        }
        else -> {
            iconVector = MiuixIcons.Regular.Report
            iconBg = colors.errorContainer
            iconTint = colors.error
        }
    }

    val mainTitle = when (kind) {
        QueryErrorKind.AUTH_REQUIRED -> "学校账号需要重新认证"
        QueryErrorKind.NETWORK -> "网络连接异常"
        QueryErrorKind.SCHEMA_CHANGED -> "暂时无法展示学校返回的数据"
        QueryErrorKind.FORBIDDEN -> "当前账号无法查看此项信息"
        else -> error?.message?.takeIf { it.isNotBlank() } ?: "查询遇到异常"
    }

    val subDesc = when (kind) {
        QueryErrorKind.AUTH_REQUIRED -> "会话已过期，请在官方页面重新认证以同步数据"
        QueryErrorKind.NETWORK -> "请检查校园网络或离线缓存，点击重试"
        QueryErrorKind.SCHEMA_CHANGED -> "请稍后再试，或前往学校官方网页查看"
        QueryErrorKind.FORBIDDEN -> "该功能受教务系统权限限制，仅对特定学生开放"
        else -> error?.message?.takeIf { it != mainTitle } ?: "请稍候重新同步"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CampusSpacing.xl, vertical = CampusSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(30.dp)
            )
        }

        Text(
            text = mainTitle,
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = CampusSpacing.xxs)
        )

        Text(
            text = subDesc,
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 19.sp
        )

        Spacer(modifier = Modifier.height(CampusSpacing.xxs))

        if (kind == QueryErrorKind.AUTH_REQUIRED && onLogin != null) {
            CampusButton(text = "登录学校账号", onClick = onLogin, primary = true)
        } else if (error?.retryable == true && onRetry != null) {
            CampusButton(text = "重新同步", onClick = onRetry, primary = true)
        }
    }
}

@Composable
private fun EmptyPanel(
    message: String,
    actionText: String?,
    onAction: (() -> Unit)?
) {
    val colors = CampusTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CampusSpacing.xl, vertical = CampusSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.surfaceMuted),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = MiuixIcons.Regular.Search,
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(28.dp)
            )
        }

        Text(
            text = message,
            color = colors.textSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        if (actionText != null && onAction != null) {
            CampusButton(text = actionText, onClick = onAction)
        }
    }
}

package edu.neu.campus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.QueryError
import edu.neu.campus.contract.QueryErrorKind
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text

/**
 * 统一的数据加载、异常与空状态呈现面板。
 * 遵循 docs/07-UI视觉与布局重设计.md 第 9 节规范：
 * - 覆盖加载中、网络异常、认证失效、无权限、数据结构变化与空数据
 * - 图标与明确文案，严格区分空集合与未获取数据
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
    val colors = CampusTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    InfiniteProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = colors.brand
                    )
                    Text(
                        text = "正在同步学校数据…",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                }
            }
            error != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val (iconVector, iconBg, iconTint) = when (error.kind) {
                        QueryErrorKind.AUTH_REQUIRED -> Triple(Icons.Default.Lock, colors.brandContainer, colors.brand)
                        QueryErrorKind.NETWORK -> Triple(Icons.Default.Warning, colors.warning.copy(alpha = 0.15f), colors.warning)
                        QueryErrorKind.SCHEMA_CHANGED -> Triple(Icons.Default.Info, colors.surfaceMuted, colors.textSecondary)
                        else -> Triple(Icons.Default.Warning, colors.error.copy(alpha = 0.15f), colors.error)
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    val mainTitle = when (error.kind) {
                        QueryErrorKind.AUTH_REQUIRED -> "学校账号需要重新认证"
                        QueryErrorKind.NETWORK -> "网络连接异常"
                        QueryErrorKind.SCHEMA_CHANGED -> "暂时无法展示学校返回的数据"
                        QueryErrorKind.FORBIDDEN -> "当前账号无法查看此项信息"
                        else -> error.message.ifBlank { "查询遇到异常" }
                    }

                    Text(
                        text = mainTitle,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    val subDesc = when (error.kind) {
                        QueryErrorKind.AUTH_REQUIRED -> "会话已过期，请在官方页面重新认证以同步数据"
                        QueryErrorKind.NETWORK -> "请检查校园网络或离线缓存，点击重试"
                        QueryErrorKind.SCHEMA_CHANGED -> "请稍后再试，或前往学校官方网页查看"
                        QueryErrorKind.FORBIDDEN -> "该功能受教务系统权限限制，仅对特定学生开放"
                        else -> error.message.takeIf { it != mainTitle } ?: "请稍候重新同步"
                    }

                    Text(
                        text = subDesc,
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (error.kind == QueryErrorKind.AUTH_REQUIRED && onLogin != null) {
                        Button(
                            onClick = onLogin,
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text("登录学校账号")
                        }
                    } else if (error.retryable && onRetry != null) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text("重新同步")
                        }
                    }
                }
            }
            emptyMessage != null -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = emptyMessage,
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )

                    if (emptyActionText != null && onEmptyAction != null) {
                        Button(
                            onClick = onEmptyAction,
                            colors = ButtonDefaults.buttonColors()
                        ) {
                            Text(emptyActionText)
                        }
                    }
                }
            }
        }
    }
}

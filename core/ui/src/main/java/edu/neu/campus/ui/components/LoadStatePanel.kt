package edu.neu.campus.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.contract.QueryError
import edu.neu.campus.contract.QueryErrorKind
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun LoadStatePanel(
    isLoading: Boolean,
    error: QueryError? = null,
    emptyMessage: String? = null,
    onRetry: (() -> Unit)? = null,
    onLogin: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfiniteProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MiuixTheme.colorScheme.primary
                    )
                    Text(
                        text = "正在同步数据…",
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
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
                    Text(
                        text = error.message.ifBlank { "查询遇到异常" },
                        color = MiuixTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                    if (error.kind == QueryErrorKind.AUTH_REQUIRED) {
                        Text(
                            text = "学校会话已过期或尚未认证，请重新登录",
                            color = MiuixTheme.colorScheme.onSurfaceSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        if (onLogin != null) {
                            Button(
                                onClick = onLogin,
                                colors = ButtonDefaults.buttonColorsPrimary()
                            ) {
                                Text("登录学校账号")
                            }
                        }
                    } else if (error.retryable && onRetry != null) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColorsPrimary()
                        ) {
                            Text("重新尝试")
                        }
                    }
                }
            }
            emptyMessage != null -> {
                Text(
                    text = emptyMessage,
                    color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

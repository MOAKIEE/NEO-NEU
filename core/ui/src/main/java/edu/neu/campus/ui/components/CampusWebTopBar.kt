package edu.neu.campus.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Refresh

/** Compact chrome leaves room for a school's own header and keeps its current host visible. */
@Composable
fun CampusWebTopBar(
    title: String,
    host: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    onRefresh: () -> Unit,
    refreshing: Boolean
) {
    val colors = CampusTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.background)
            .heightIn(min = 64.dp).padding(horizontal = CampusSpacing.sm, vertical = CampusSpacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(MiuixIcons.Regular.Back, contentDescription = "返回", tint = colors.textPrimary)
        }
        Column(Modifier.weight(1f).padding(horizontal = CampusSpacing.xs)) {
            Text(title, color = colors.textPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(host, color = colors.textSecondary, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onRefresh, enabled = !refreshing) {
            Icon(MiuixIcons.Regular.Refresh, contentDescription = "刷新网页",
                tint = if (refreshing) colors.textDisabled else colors.textPrimary)
        }
        IconButton(onClick = onClose) {
            Icon(MiuixIcons.Regular.Close, contentDescription = "关闭网页", tint = colors.textPrimary)
        }
    }
}

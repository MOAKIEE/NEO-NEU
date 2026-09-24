package edu.neu.campus.app.feature.services

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusPill
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.components.tapScale
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.WorldClock

data class CampusServiceItem(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val officialUrl: String
)

@Composable
fun ServicesCatalogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val campusColors = CampusTheme.colors
    var selectedService by remember { mutableStateOf<CampusServiceItem?>(null) }

    val serviceList = remember {
        listOf(
            CampusServiceItem("portal", "智慧东大门户", "学校门户", "在学校官方门户查看服务。", "https://personal.neu.edu.cn/portal"),
            CampusServiceItem("academic", "本科教务系统", "教学教务", "在学校官方教务系统查看信息。", "https://jwxt.neu.edu.cn/")
        )
    }

    val pageScrollBehavior = MiuixScrollBehavior()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(campusColors.background)
            .nestedScroll(pageScrollBehavior.nestedScrollConnection)
    ) {
        CampusTopBar(
            scrollBehavior = pageScrollBehavior,
            title = "学校服务目录",
            subtitle = "学校官方入口（浏览器打开）",
            onBack = onBack
        )

        Row(modifier = Modifier.padding(horizontal = CampusSpacing.screenHorizontal, vertical = CampusSpacing.xs)) {
            SafeDataTag(text = "学校服务将在系统浏览器中打开")
        }

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
            itemsIndexed(serviceList, key = { _, item -> item.id }) { index, item ->
                StaggeredAppear(
                    index = index,
                    key = serviceList.size,
                    modifier = Modifier.animateItem()
                ) {
                    val badgeIcon = if (item.id == "portal") MiuixIcons.Regular.WorldClock else MiuixIcons.Regular.Notes
                    val badgeTint = if (item.id == "portal") campusColors.timetableForeground else campusColors.gradeForeground
                    val badgeContainer = if (item.id == "portal") campusColors.timetableContainer else campusColors.gradeContainer

                    CampusCard(onClick = { selectedService = item }) {
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
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xs)
                                ) {
                                    Text(
                                        text = item.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = campusColors.textPrimary
                                    )

                                    CampusPill(
                                        text = item.category,
                                        contentColor = campusColors.brand,
                                        containerColor = campusColors.brandContainer
                                    )
                                }

                                Text(
                                    text = item.description,
                                    fontSize = 12.sp,
                                    color = campusColors.textSecondary,
                                    maxLines = 1
                                )
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
            }
        }
    }

    // 选中服务详情弹窗
    selectedService?.let { service ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .tapScale(onClick = { selectedService = null })
                .padding(CampusSpacing.xl),
            contentAlignment = Alignment.Center
        ) {
            CampusCard(
                contentPadding = PaddingValues(CampusSpacing.lg)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = service.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = campusColors.textPrimary
                        )

                        CampusPill(
                            text = service.category,
                            contentColor = campusColors.brand,
                            containerColor = campusColors.brandContainer
                        )
                    }

                    Text(
                        text = service.description,
                        fontSize = 13.sp,
                        color = campusColors.textPrimary,
                        lineHeight = 20.sp
                    )

                    CampusGroup {
                        Text(
                            text = "官方系统访问地址：\n${service.officialUrl}",
                            fontSize = 12.sp,
                            color = campusColors.textSecondary,
                            lineHeight = 18.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(service.officialUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "无法启动浏览器", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColorsPrimary(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("在浏览器打开")
                        }

                        Button(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("URL", service.officialUrl))
                                Toast.makeText(context, "网址已复制", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("复制网址")
                        }
                    }
                }
            }
        }
    }
}

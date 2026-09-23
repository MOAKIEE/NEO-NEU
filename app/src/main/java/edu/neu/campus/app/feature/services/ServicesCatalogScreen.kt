package edu.neu.campus.app.feature.services

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.components.SafeDataTag
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

data class CampusServiceItem(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val officialUrl: String
)

/**
 * 学校服务目录页面。
 * 遵循 docs/06-UI页面布局设计.md 第 5 节要求：
 * - 列表项带“官方网页”标签
 * - 点击进入简单服务说明，由用户自主决定“在浏览器打开”或复制网址
 * - 核心查询功能不在此替代，严禁业务 WebView 套壳
 */
@Composable
fun ServicesCatalogScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedService by remember { mutableStateOf<CampusServiceItem?>(null) }

    val serviceList = remember {
        listOf(
            CampusServiceItem(
                id = "portal",
                name = "智慧东大综合门户",
                category = "公共办事",
                description = "全校师生信息门户、校内公文、通知公告与各项校级在线流程填报中心。",
                officialUrl = "http://portal.neu.edu.cn"
            ),
            CampusServiceItem(
                id = "grad",
                name = "研究生管理信息系统",
                category = "教学教务",
                description = "东北大学研究生培养、学籍、学位论文与导师互动系统。",
                officialUrl = "http://gsapp.neu.edu.cn"
            ),
            CampusServiceItem(
                id = "net",
                name = "校园网综合自服务平台",
                category = "网络信息",
                description = "宿舍万兆宽带与无线账号管理、网费明细充值、MAC 终端在线管理与密码重置。",
                officialUrl = "http://ipgw.neu.edu.cn"
            ),
            CampusServiceItem(
                id = "lib",
                name = "东北大学图书馆",
                category = "图书图书",
                description = "馆藏图书检索、续借、学位论文提交与中外文电子学术数据库查阅。",
                officialUrl = "http://lib.neu.edu.cn"
            ),
            CampusServiceItem(
                id = "finance",
                name = "财务综合综合自服务系统",
                category = "财务资产",
                description = "学生学费缴费、学业奖学金发放记录、财务报销进度与学费收据查询。",
                officialUrl = "http://cwzf.neu.edu.cn"
            ),
            CampusServiceItem(
                id = "sports",
                name = "体质健康测试管理中心",
                category = "体育健康",
                description = "全日制本科生国家学生体质健康标准测试预约、免测申请与成绩查询。",
                officialUrl = "http://pe.neu.edu.cn"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = "学校服务目录",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MiuixTheme.colorScheme.onSurface
                    )
                }
            }
        )

        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            SafeDataTag(text = "外部业务由系统浏览器打开，本客户端不设 WebView 嵌套")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(serviceList, key = { it.id }) { item ->
                Card(
                    colors = CardDefaults.defaultColors(),
                    insideMargin = PaddingValues(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedService = item }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MiuixTheme.colorScheme.onSurface
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MiuixTheme.colorScheme.surfaceContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "官方网页",
                                        fontSize = 10.sp,
                                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                                    )
                                }
                            }

                            Text(
                                text = item.description,
                                fontSize = 12.sp,
                                color = MiuixTheme.colorScheme.onSurfaceSecondary,
                                maxLines = 1
                            )
                        }

                        Text(
                            text = "查看 ›",
                            fontSize = 12.sp,
                            color = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
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
                .background(MiuixTheme.colorScheme.background.copy(alpha = 0.6f))
                .clickable { selectedService = null }
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
                        text = service.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "服务类别：${service.category}",
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary
                    )

                    Text(
                        text = service.description,
                        fontSize = 13.sp,
                        color = MiuixTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "官方系统访问地址：\n${service.officialUrl}",
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
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(service.officialUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "无法启动浏览器", Toast.LENGTH_SHORT).show()
                                }
                            },
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

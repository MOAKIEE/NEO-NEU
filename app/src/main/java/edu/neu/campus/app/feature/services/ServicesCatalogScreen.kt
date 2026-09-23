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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.SafeDataTag
import edu.neu.campus.ui.theme.LocalCampusColors
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text

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
    val campusColors = LocalCampusColors.current
    var selectedService by remember { mutableStateOf<CampusServiceItem?>(null) }

    val serviceList = remember {
        listOf(
            CampusServiceItem("portal", "智慧东大门户", "学校门户", "在学校官方门户查看服务。", "https://personal.neu.edu.cn/portal"),
            CampusServiceItem("academic", "本科教务系统", "教学教务", "在学校官方教务系统查看信息。", "https://jwxt.neu.edu.cn/")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(campusColors.background)
    ) {
        CampusTopBar(
            title = "学校服务目录",
            onBack = onBack
        )

        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            SafeDataTag(text = "学校服务将在系统浏览器中打开")
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(serviceList, key = { it.id }) { item ->
                Card(
                    insideMargin = PaddingValues(16.dp),
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
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = campusColors.textPrimary
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(campusColors.surfaceMuted)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                    Text(
                                        text = item.category,
                                        fontSize = 11.sp,
                                        color = campusColors.textSecondary
                                    )
                                }
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
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { selectedService = null }
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                insideMargin = PaddingValues(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(campusColors.brandContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = service.category,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = campusColors.brand
                            )
                        }
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

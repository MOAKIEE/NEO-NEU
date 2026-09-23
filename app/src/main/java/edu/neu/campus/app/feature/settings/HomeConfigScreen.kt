package edu.neu.campus.app.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.neu.campus.app.config.HomeLayoutConfigManager
import edu.neu.campus.app.registry.FeatureRegistry
import edu.neu.campus.ui.components.CampusCheckbox
import edu.neu.campus.ui.components.CampusSwitch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 首页布局可配置管理页面。
 * 落实 docs/06-UI页面布局设计.md 第 15 节扩展规则：
 * - 快捷查询前 3 项自选功能（第 4 项固定“全部”）
 * - 摘要区模块显示/隐藏开关与上下移动排序
 * - 支持恢复默认设置，本地持久化保存
 */
@Composable
fun HomeConfigScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentQuickIds = HomeLayoutConfigManager.quickActionIds
    val modules = HomeLayoutConfigManager.modules

    val availableFeatures = remember {
        FeatureRegistry.allFeatures.filter { it.isNative }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = "首页布局配置",
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

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 快捷查询栏自选（前 3 项）
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "快捷查询栏配置 (最多勾选 3 项)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "选中的前 3 个功能将在首页第一屏快捷栏展示，第 4 项固定为“全部”，点击即可跳转对应功能。",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        lineHeight = 17.sp
                    )

                    availableFeatures.forEach { feat ->
                        val isSelected = currentQuickIds.contains(feat.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val newIds = currentQuickIds.toMutableList()
                                    if (isSelected) {
                                        if (newIds.size > 1) {
                                            newIds.remove(feat.id)
                                            HomeLayoutConfigManager.updateQuickActions(newIds)
                                        } else {
                                            Toast.makeText(context, "请至少保留 1 项快捷查询", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (newIds.size < 3) {
                                            newIds.add(feat.id)
                                            HomeLayoutConfigManager.updateQuickActions(newIds)
                                        } else {
                                            Toast.makeText(context, "最多只能选择 3 项快捷查询", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = feat.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = MiuixTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = feat.description,
                                    fontSize = 11.sp,
                                    color = MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }

                            CampusCheckbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    val newIds = currentQuickIds.toMutableList()
                                    if (checked) {
                                        if (newIds.size < 3) {
                                            newIds.add(feat.id)
                                            HomeLayoutConfigManager.updateQuickActions(newIds)
                                        } else {
                                            Toast.makeText(context, "最多只能选择 3 项快捷查询", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        if (newIds.size > 1) {
                                            newIds.remove(feat.id)
                                            HomeLayoutConfigManager.updateQuickActions(newIds)
                                        } else {
                                            Toast.makeText(context, "请至少保留 1 项快捷查询", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 2. 摘要区模块列表（开关与上下排序）
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "首页摘要模块与展示顺序",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "使用开关控制模块是否在首页展示，点击右侧上下箭头可调整它们在首页的纵向排列顺序。",
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                        lineHeight = 17.sp
                    )

                    modules.forEachIndexed { index, mod ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MiuixTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CampusSwitch(
                                    checked = mod.enabled,
                                    onCheckedChange = { checked ->
                                        HomeLayoutConfigManager.toggleModule(mod.id, checked)
                                    }
                                )

                                Text(
                                    text = mod.title,
                                    fontSize = 14.sp,
                                    fontWeight = if (mod.enabled) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (mod.enabled) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onSurfaceSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { HomeLayoutConfigManager.moveModuleUp(index) },
                                    enabled = index > 0
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "上移",
                                        tint = if (index > 0) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.3f)
                                    )
                                }

                                IconButton(
                                    onClick = { HomeLayoutConfigManager.moveModuleDown(index) },
                                    enabled = index < modules.size - 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "下移",
                                        tint = if (index < modules.size - 1) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceSecondary.copy(alpha = 0.3f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. 恢复默认操作
            Card(
                colors = CardDefaults.defaultColors(),
                insideMargin = PaddingValues(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "重置为默认布局",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MiuixTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "恢复初始的快捷栏选项与模块排序",
                            fontSize = 11.sp,
                            color = MiuixTheme.colorScheme.onSurfaceSecondary
                        )
                    }

                    Button(
                        onClick = {
                            HomeLayoutConfigManager.restoreDefaults()
                            Toast.makeText(context, "已恢复默认首页布局", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("恢复默认")
                    }
                }
            }
        }
    }
}

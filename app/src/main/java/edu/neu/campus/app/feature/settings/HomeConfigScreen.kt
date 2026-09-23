package edu.neu.campus.app.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusSwitch
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.theme.LocalCampusColors
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun HomeConfigScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val campusColors = LocalCampusColors.current
    val currentQuickIds = HomeLayoutConfigManager.quickActionIds
    val modules = HomeLayoutConfigManager.modules

    val availableFeatures = remember {
        FeatureRegistry.allFeatures.filter { it.isNative }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(campusColors.background)
    ) {
        CampusTopBar(
            title = "首页布局配置",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 快捷查询栏自选
            CampusSection(
                title = "快捷入口配置",
                subtitle = "最多选择 3 项，第 4 项固定为“全部”"
            ) {
                CampusGroup {
                    availableFeatures.forEachIndexed { index, feat ->
                        if (index > 0) CampusGroupDivider()
                        val isSelected = currentQuickIds.contains(feat.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
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
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = feat.title,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = campusColors.textPrimary
                                )
                                Text(
                                    text = feat.description,
                                    fontSize = 12.sp,
                                    color = campusColors.textSecondary
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
            CampusSection(
                title = "首页摘要模块与排序",
                subtitle = "使用开关控制显隐，使用箭头调整上下顺序"
            ) {
                CampusGroup {
                    modules.forEachIndexed { index, mod ->
                        if (index > 0) CampusGroupDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
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
                                    fontSize = 15.sp,
                                    fontWeight = if (mod.enabled) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (mod.enabled) campusColors.textPrimary else campusColors.textSecondary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = { HomeLayoutConfigManager.moveModuleUp(index) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "上移",
                                        tint = if (index > 0) campusColors.brand else campusColors.textSecondary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { HomeLayoutConfigManager.moveModuleDown(index) },
                                    enabled = index < modules.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "下移",
                                        tint = if (index < modules.size - 1) campusColors.brand else campusColors.textSecondary.copy(alpha = 0.3f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. 恢复默认操作
            CampusGroup {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "重置为默认布局",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = campusColors.textPrimary
                        )
                        Text(
                            text = "恢复初始快捷入口与默认模块排序",
                            fontSize = 12.sp,
                            color = campusColors.textSecondary
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

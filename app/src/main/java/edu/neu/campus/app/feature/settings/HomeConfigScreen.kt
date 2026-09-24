package edu.neu.campus.app.feature.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import edu.neu.campus.app.config.HomeLayoutConfigManager
import edu.neu.campus.app.registry.FeatureRegistry
import edu.neu.campus.ui.components.CampusButton
import edu.neu.campus.ui.components.CampusCheckbox
import edu.neu.campus.ui.components.CampusGroup
import edu.neu.campus.ui.components.CampusGroupDivider
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.components.CampusSection
import edu.neu.campus.ui.components.CampusSwitch
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.components.StaggeredAppear
import edu.neu.campus.ui.theme.CampusShapes
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.extended.File
import top.yukonga.miuix.kmp.icon.extended.GridView
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Reset
import top.yukonga.miuix.kmp.icon.extended.Stopwatch
import top.yukonga.miuix.kmp.icon.extended.Tasks
import top.yukonga.miuix.kmp.icon.extended.Weeks
import top.yukonga.miuix.kmp.icon.extended.WorldClock

@Composable
fun HomeConfigScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val campusColors = CampusTheme.colors
    val currentQuickIds = HomeLayoutConfigManager.quickActionIds
    val modules = HomeLayoutConfigManager.modules

    val availableFeatures = remember {
        FeatureRegistry.allFeatures.filter { it.isNative }
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
            title = "首页布局配置",
            subtitle = "快捷入口与模块顺序",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = CampusSpacing.screenHorizontal)
                .padding(top = CampusSpacing.xs, bottom = CampusSpacing.screenBottom),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.md)
        ) {
            // 1. 快捷查询栏自选
            StaggeredAppear(index = 0) {
                CampusSection(
                    title = "快捷入口配置",
                    subtitle = "最多选择 3 项，第 4 项固定为“全部”"
                ) {
                    CampusGroup {
                        availableFeatures.forEachIndexed { index, feat ->
                            if (index > 0) CampusGroupDivider(startIndent = 50.dp)
                            val isSelected = currentQuickIds.contains(feat.id)
                            val (badgeTint, badgeContainer) = campusColors.courseColor(feat.id)

                            CampusRow(
                                title = feat.title,
                                subtitle = feat.description,
                                leading = {
                                    CampusIconBadge(
                                        icon = featureIcon(feat.id),
                                        tint = badgeTint,
                                        container = badgeContainer,
                                        size = 38.dp,
                                        iconSize = 20.dp,
                                        cornerRadius = CampusShapes.extraSmall
                                    )
                                },
                                trailingContent = {
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
                                },
                                onClick = {
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
                            )
                        }
                    }
                }
            }

            // 2. 摘要区模块列表（开关与上下排序）
            StaggeredAppear(index = 1) {
                CampusSection(
                    title = "首页摘要模块与排序",
                    subtitle = "使用开关控制显隐，使用箭头调整上下顺序"
                ) {
                    CampusGroup {
                        modules.forEachIndexed { index, mod ->
                            if (index > 0) CampusGroupDivider(startIndent = 50.dp)
                            val (badgeTint, badgeContainer) = campusColors.courseColor(mod.id)

                            CampusRow(
                                title = mod.title,
                                leading = {
                                    CampusIconBadge(
                                        icon = moduleIcon(mod.id),
                                        tint = badgeTint,
                                        container = badgeContainer,
                                        size = 38.dp,
                                        iconSize = 20.dp,
                                        cornerRadius = CampusShapes.extraSmall
                                    )
                                },
                                trailingContent = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(CampusSpacing.xxs)
                                    ) {
                                        CampusSwitch(
                                            checked = mod.enabled,
                                            onCheckedChange = { checked ->
                                                HomeLayoutConfigManager.toggleModule(mod.id, checked)
                                            }
                                        )

                                        IconButton(
                                            onClick = { HomeLayoutConfigManager.moveModuleUp(index) },
                                            enabled = index > 0,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            // Miuix 的 ExpandLess/ExpandMore 是全屏四角图标，这里把前进箭头转成上下箭头。
                                            Icon(
                                                imageVector = MiuixIcons.Regular.ChevronForward,
                                                contentDescription = "上移",
                                                tint = if (index > 0) campusColors.brand else campusColors.textSecondary.copy(alpha = 0.3f),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .rotate(-90f)
                                            )
                                        }

                                        IconButton(
                                            onClick = { HomeLayoutConfigManager.moveModuleDown(index) },
                                            enabled = index < modules.size - 1,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = MiuixIcons.Regular.ChevronForward,
                                                contentDescription = "下移",
                                                tint = if (index < modules.size - 1) campusColors.brand else campusColors.textSecondary.copy(alpha = 0.3f),
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .rotate(90f)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // 3. 恢复默认操作
            StaggeredAppear(index = 2) {
                CampusGroup {
                    CampusRow(
                        title = "重置为默认布局",
                        subtitle = "恢复默认入口与模块顺序",
                        leading = {
                            CampusIconBadge(
                                icon = MiuixIcons.Regular.Reset,
                                tint = campusColors.error,
                                container = campusColors.errorContainer,
                                size = 38.dp,
                                iconSize = 20.dp,
                                cornerRadius = CampusShapes.extraSmall
                            )
                        },
                        trailingContent = {
                            CampusButton(
                                text = "恢复默认",
                                onClick = {
                                    HomeLayoutConfigManager.restoreDefaults()
                                    Toast.makeText(context, "已恢复默认首页布局", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}

private fun featureIcon(id: String): ImageVector = when (id) {
    FeatureRegistry.ID_GRADES -> MiuixIcons.Regular.Notes
    FeatureRegistry.ID_EXAMS -> MiuixIcons.Regular.File
    FeatureRegistry.ID_TIMETABLE -> MiuixIcons.Regular.Weeks
    FeatureRegistry.ID_BELL_SCHEDULE -> MiuixIcons.Regular.Stopwatch
    FeatureRegistry.ID_CAMPUS_CARD -> MiuixIcons.Regular.BankCards
    FeatureRegistry.ID_NETWORK -> MiuixIcons.Regular.WorldClock
    FeatureRegistry.ID_MESSAGES -> MiuixIcons.Regular.Messages
    FeatureRegistry.ID_TASKS -> MiuixIcons.Regular.Tasks
    else -> MiuixIcons.Regular.GridView
}

private fun moduleIcon(id: String): ImageVector = when (id) {
    HomeLayoutConfigManager.MODULE_TODAY_COURSES -> MiuixIcons.Regular.Weeks
    HomeLayoutConfigManager.MODULE_CAMPUS_LIFE -> MiuixIcons.Regular.BankCards
    HomeLayoutConfigManager.MODULE_RECENT_EXAMS -> MiuixIcons.Regular.File
    HomeLayoutConfigManager.MODULE_RECENT_TASKS -> MiuixIcons.Regular.Tasks
    else -> MiuixIcons.Regular.GridView
}

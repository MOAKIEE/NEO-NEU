package edu.neu.campus.app.feature.services

import androidx.compose.runtime.Composable
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.ui.components.CampusIconBadge
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

/** Shared presentation for official web entry points in query, balance and directory pages. */
@Composable
internal fun OfficialServiceRow(service: OfficialService, showWebLabel: Boolean = false) {
    val colors = CampusTheme.colors
    val icon = when (service) {
        OfficialService.STUDENT_MAIL -> MiuixIcons.Regular.Email
        OfficialService.ACADEMIC -> MiuixIcons.Regular.Notes
        OfficialService.CARD_RECHARGE -> MiuixIcons.Regular.BankCards
        OfficialService.NETWORK_RECHARGE -> MiuixIcons.Regular.Share
        OfficialService.ECODE -> MiuixIcons.Regular.Scan
        else -> MiuixIcons.Regular.Store
    }
    val (foreground, container) = when (service) {
        OfficialService.CARD_RECHARGE, OfficialService.ECODE -> colors.cardForeground to colors.cardContainer
        OfficialService.NETWORK_RECHARGE -> colors.networkForeground to colors.networkContainer
        OfficialService.STUDENT_MAIL -> colors.messageForeground to colors.messageContainer
        OfficialService.ACADEMIC -> colors.gradeForeground to colors.gradeContainer
        else -> colors.brand to colors.brandContainer
    }
    CampusRow(
        title = service.title,
        titleMaxLines = 2,
        subtitle = if (showWebLabel) "在学校官方网页办理" else null,
        leading = { CampusIconBadge(icon, foreground, container) },
        showChevron = true,
        onClick = { AppNavigator.navigateTo(AppDestination.OfficialWeb(service)) }
    )
}

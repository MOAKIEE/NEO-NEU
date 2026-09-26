package edu.neu.campus.app.feature.services

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import edu.neu.campus.app.navigation.AppDestination
import edu.neu.campus.app.navigation.AppNavigator
import edu.neu.campus.ui.components.CampusCard
import edu.neu.campus.ui.components.CampusRow
import edu.neu.campus.ui.components.CampusIconBadge
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.BankCards
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Notes
import top.yukonga.miuix.kmp.icon.extended.Store
import edu.neu.campus.ui.components.CampusTopBar
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun ServicesCatalogScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val scroll = MiuixScrollBehavior()
    Column(modifier.fillMaxSize().background(colors.background).nestedScroll(scroll.nestedScrollConnection)) {
        CampusTopBar(title = "学校服务目录", onBack = onBack, scrollBehavior = scroll)
        LazyColumn(
            contentPadding = PaddingValues(
                start = CampusSpacing.screenHorizontal,
                end = CampusSpacing.screenHorizontal,
                top = CampusSpacing.xs,
                bottom = CampusSpacing.screenBottom
            ),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.sm)
        ) {
            items(OfficialService.entries.filter { it != OfficialService.ECODE }, key = { it.name }) { service ->
                CampusCard {
                    CampusRow(
                        title = service.title,
                        leading = {
                            val icon = when (service) {
                                OfficialService.STUDENT_MAIL -> MiuixIcons.Regular.Messages
                                OfficialService.ACADEMIC -> MiuixIcons.Regular.Notes
                                OfficialService.CARD_RECHARGE, OfficialService.NETWORK_RECHARGE, OfficialService.PAYMENT_HALL -> MiuixIcons.Regular.BankCards
                                else -> MiuixIcons.Regular.Store
                            }
                            CampusIconBadge(icon, colors.brand, colors.brandContainer)
                        },
                        showChevron = true,
                        onClick = { AppNavigator.navigateTo(AppDestination.OfficialWeb(service)) }
                    )
                }
            }
        }
    }
}

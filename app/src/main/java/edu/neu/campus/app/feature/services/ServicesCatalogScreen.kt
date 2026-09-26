package edu.neu.campus.app.feature.services

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import edu.neu.campus.ui.components.*
import edu.neu.campus.ui.theme.CampusSpacing
import edu.neu.campus.ui.theme.CampusTheme
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior

@Composable
fun ServicesCatalogScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = CampusTheme.colors
    val scroll = MiuixScrollBehavior()
    Column(modifier.fillMaxSize().background(colors.background).nestedScroll(scroll.nestedScrollConnection)) {
        CampusTopBar(title = "学校服务", onBack = onBack, scrollBehavior = scroll)
        LazyColumn(
            contentPadding = PaddingValues(
                start = CampusSpacing.screenHorizontal, end = CampusSpacing.screenHorizontal,
                top = CampusSpacing.xs, bottom = CampusSpacing.screenBottom
            ),
            verticalArrangement = Arrangement.spacedBy(CampusSpacing.lg)
        ) {
            item {
                ServiceSection("学校平台", listOf(OfficialService.PORTAL, OfficialService.ACADEMIC, OfficialService.STUDENT_MAIL))
            }
            item {
                ServiceSection("充值与缴费", listOf(OfficialService.CARD_RECHARGE, OfficialService.NETWORK_RECHARGE, OfficialService.PAYMENT_HALL))
            }
        }
    }
}

@Composable
private fun ServiceSection(title: String, services: List<OfficialService>) {
    CampusSection(title = title) {
        CampusGroup {
            services.forEachIndexed { index, service ->
                OfficialServiceRow(service)
                if (index < services.lastIndex) CampusGroupDivider()
            }
        }
    }
}

package edu.neu.campus.portal

import edu.neu.campus.contract.BalanceKind
import org.junit.Assert.*
import org.junit.Test

class PortalApiTest {
    private val api = PortalApi()

    @Test fun expiredEnvelopeIsNeverEmptySuccess() {
        assertThrows(PortalAuthException::class.java) { payload("""{"e":10013,"d":{"loginUrl":"redacted"}}""") }
    }

    @Test fun emptyMessagePageIsSuccessfulOnlyWithEnvelope() {
        val page = api.messages("""{"e":0,"d":{"list":[],"count":0,"noReadCount":0}}""")
        assertTrue(page.items.isEmpty())
        assertEquals(0, page.total)
    }

    @Test fun nonEmptyBalanceItemsNeedVerifiedMapping() {
        val items = """{"e":0,"d":{"data":[{"id":"opaque","key":"card.balance","name":"校园卡余额","unit":"元"}]}}"""
        val ref = api.balanceItem(items, BalanceKind.CAMPUS_CARD)
        val balance = api.balance("""{"e":0,"d":{"data":{"value":12.34}}}""", BalanceKind.CAMPUS_CARD, ref)
        assertEquals("12.34", balance.rawValue)
        val masked = api.balance("""{"e":0,"d":{"data":{"value":"***"}}}""", BalanceKind.CAMPUS_CARD, ref)
        assertTrue(masked.isMasked)
        assertNull(masked.rawValue)
    }
}

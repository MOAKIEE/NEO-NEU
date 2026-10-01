package edu.neu.campus.authweb

import org.junit.Assert.*
import org.junit.Test

class InteractiveLoginGateTest {
    @Test fun firstLoginAndChallengeNavigationKeepTheNewSession() {
        val gate = InteractiveLoginGate()
        assertFalse(gate.requiresFreshSession("personal.neu.edu.cn"))
        assertFalse(gate.requiresFreshSession("pass.neu.edu.cn"))
        assertFalse(gate.requiresFreshSession("pass.neu.edu.cn"))
        assertFalse(gate.requiresFreshSession("personal.neu.edu.cn"))
        assertFalse(gate.requiresFreshSession("jwxt.neu.edu.cn"))
    }

    @Test fun enteringCasFromABusinessSiteRequiresIsolationAgain() {
        for (site in listOf("personal.neu.edu.cn", "jwxt.neu.edu.cn", "ecode.neu.edu.cn")) {
            val gate = InteractiveLoginGate()
            assertFalse(gate.requiresFreshSession("pass.neu.edu.cn"))
            assertFalse(gate.requiresFreshSession(site))
            assertTrue(gate.requiresFreshSession("pass.neu.edu.cn"))
        }
    }
}

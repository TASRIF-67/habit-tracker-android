package com.example.habittracker

import com.example.habittracker.data.ActiveSessionNavigationDecision
import com.example.habittracker.data.ActiveSessionNavigationRules
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveSessionNavigationRulesTest {
    @Test fun `cold notification request waits until session state is loaded`() {
        assertEquals(ActiveSessionNavigationDecision.Wait, ActiveSessionNavigationRules.notificationRequest(1, 0, loaded = false, hasSession = false))
    }

    @Test fun `notification opens an available active session`() {
        assertEquals(ActiveSessionNavigationDecision.OpenSession, ActiveSessionNavigationRules.notificationRequest(1, 0, loaded = true, hasSession = true))
    }

    @Test fun `stale notification falls back to today`() {
        assertEquals(ActiveSessionNavigationDecision.FallBackToToday, ActiveSessionNavigationRules.notificationRequest(1, 0, loaded = true, hasSession = false))
    }

    @Test fun `warm intent is consumed only once`() {
        assertEquals(ActiveSessionNavigationDecision.Ignore, ActiveSessionNavigationRules.notificationRequest(2, 2, loaded = true, hasSession = true))
    }
}

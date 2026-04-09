package com.example.earrove.domain.arbitration

import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEventTest {

    @Test
    fun nav_events_are_distinct_sealed_subtypes() {
        val turn: AccessibilityEvent = AccessibilityEvent.Nav.Turn("左转", 50)
        val info: AccessibilityEvent = AccessibilityEvent.Info("提示")
        assertTrue(turn is AccessibilityEvent.Nav)
        assertTrue(info is AccessibilityEvent.Info)
    }
}

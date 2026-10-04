package com.lyrismet.incadent.core.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class BackNavigationTest {
    @Test
    fun `a deeper tab pops before anything else`() {
        assertEquals(BackAction.Pop, resolveBackAction(isOnSessionsTab = true, activeTabDepth = 2))
        assertEquals(BackAction.Pop, resolveBackAction(isOnSessionsTab = false, activeTabDepth = 3))
    }

    @Test
    fun `a non-sessions tab root returns to sessions`() {
        assertEquals(BackAction.SwitchToSessions, resolveBackAction(isOnSessionsTab = false, activeTabDepth = 1))
    }

    @Test
    fun `sessions root asks before exiting`() {
        assertEquals(BackAction.ConfirmExit, resolveBackAction(isOnSessionsTab = true, activeTabDepth = 1))
    }
}

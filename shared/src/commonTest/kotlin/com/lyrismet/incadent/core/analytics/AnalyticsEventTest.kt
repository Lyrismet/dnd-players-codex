package com.lyrismet.incadent.core.analytics

import com.lyrismet.incadent.domain.model.EntityKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsEventTest {
    @Test
    fun `screen view carries the screen name`() {
        val event = AnalyticsEvent.ScreenView("codex")

        assertEquals("screen_view", event.name)
        assertEquals(mapOf("screen_name" to "codex"), event.params)
    }

    @Test
    fun `record created carries the lowercase kind`() {
        assertEquals(mapOf("kind" to "npc"), AnalyticsEvent.RecordCreated(EntityKind.NPC).params)
    }

    @Test
    fun `events without details have no params`() {
        assertTrue(AnalyticsEvent.CombatStarted.params.isEmpty())
        assertEquals("campaign_exported", AnalyticsEvent.CampaignExported.name)
    }
}

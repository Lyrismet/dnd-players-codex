package com.lyrismet.incadent.core.campaign

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CampaignNameTest {
    @Test
    fun `an empty draft is not savable`() {
        assertNull(savableCampaignName(""))
    }

    @Test
    fun `a whitespace-only draft is not savable`() {
        assertNull(savableCampaignName("   \t "))
    }

    @Test
    fun `surrounding whitespace is trimmed before saving`() {
        assertEquals("Тени Блэквуда", savableCampaignName("  Тени Блэквуда \n"))
    }

    @Test
    fun `inner spaces are kept`() {
        assertEquals("Тени  Блэквуда", savableCampaignName("Тени  Блэквуда"))
    }
}

package com.lyrismet.incadent.feature.session.presentation.list

import com.lyrismet.incadent.feature.session.domain.model.Downtime
import com.lyrismet.incadent.feature.session.domain.model.DowntimeItem
import com.lyrismet.incadent.feature.session.presentation.list.downtime.DowntimeDraft
import com.lyrismet.incadent.feature.session.presentation.list.downtime.DowntimeLine
import com.lyrismet.incadent.feature.session.presentation.list.downtime.toCard
import com.lyrismet.incadent.feature.session.presentation.list.downtime.toDowntime
import com.lyrismet.incadent.feature.session.presentation.list.downtime.toDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DowntimeModelsTest {
    private val draft = DowntimeDraft(afterSessionId = 3, afterLabel = "III", nextLabel = "IV")

    @Test
    fun `spent gold becomes a negative delta`() {
        val downtime = draft.copy(shortText = "Пир", goldAmount = 40, goldSpent = true).toDowntime(null)

        assertEquals(-40, downtime.goldDelta)
    }

    @Test
    fun `an item without a name is dropped and the others are trimmed`() {
        val items = listOf(DowntimeLine(" Зелье ", 2), DowntimeLine("  ", 1))

        val downtime = draft.copy(shortText = "a", items = items).toDowntime(null)

        assertEquals(listOf(DowntimeItem("Зелье", 2)), downtime.items)
    }

    @Test
    fun `a draft needs the short text`() {
        assertFalse(draft.copy(shortText = "  ").canSave)
        assertTrue(draft.copy(shortText = "Кузница").canSave)
    }

    @Test
    fun `a card turns back into the same draft`() {
        val downtime = Downtime(5, 3, "Кузница", "долго", "меч", -25, listOf(DowntimeItem("Зелье", 2)))

        val back = downtime.toCard("III", "IV").toDraft().toDowntime(downtime)

        assertEquals(downtime, back)
    }

    @Test
    fun `the same gold and items count as the same loot`() {
        val a = Downtime(1, 1, goldDelta = 10, items = listOf(DowntimeItem("Меч", 1, itemId = 7)))
        val b = Downtime(2, 1, goldDelta = 10, items = listOf(DowntimeItem("Меч", 1)))

        assertTrue(a.sameLootAs(b))
        assertFalse(a.sameLootAs(b.copy(goldDelta = 11)))
        assertFalse(a.sameLootAs(b.copy(items = emptyList())))
    }
}

package com.lyrismet.incadent.feature.codex.domain.model

import com.lyrismet.incadent.domain.model.EntityKind
import kotlin.test.Test
import kotlin.test.assertEquals

class CodexRecordsTest {
    @Test
    fun `an item of a missing owner reads as shared`() {
        val lost = Item(1, "Sword", owner = ItemOwner.Of(EntityKind.NPC, 9))
        val records = CodexRecords(emptyList(), emptyList(), emptyList(), emptyList(), items = listOf(lost))
        assertEquals(ItemOwner.Shared, records.items.single().owner)
    }
}

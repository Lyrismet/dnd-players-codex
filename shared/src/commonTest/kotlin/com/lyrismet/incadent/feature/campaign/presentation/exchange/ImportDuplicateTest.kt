package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeOptions
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import kotlin.test.Test
import kotlin.test.assertEquals

class ImportDuplicateTest {
    private fun scope(
        npcs: Set<Long> = emptySet(),
        places: Set<Long> = emptySet(),
    ) = RecordScope(emptySet(), places, npcs, emptySet(), emptySet(), emptySet())

    private val file =
        ExchangeOptions(
            npcs = listOf(ExchangeItem(1, "Bob"), ExchangeItem(2, "Ann")),
            places = listOf(ExchangeItem(1, "Town")),
        )

    @Test
    fun `the same name and kind is a duplicate whatever the case`() {
        val existing = ExchangeOptions(npcs = listOf(ExchangeItem(9, "  bob ")))
        assertEquals(
            listOf(ImportDuplicate("Bob", ExchangeRef(ExchangeKind.NPC, 1))),
            findDuplicates(scope(npcs = setOf(1, 2)), file, existing),
        )
    }

    @Test
    fun `the same name of another kind is not one`() {
        val existing = ExchangeOptions(places = listOf(ExchangeItem(9, "Bob")))
        assertEquals(emptyList(), findDuplicates(scope(npcs = setOf(1)), file, existing))
    }

    @Test
    fun `a record that is not in the scope is not counted`() {
        val existing = ExchangeOptions(npcs = listOf(ExchangeItem(9, "Bob")))
        assertEquals(emptyList(), findDuplicates(scope(npcs = setOf(2)), file, existing))
    }
}

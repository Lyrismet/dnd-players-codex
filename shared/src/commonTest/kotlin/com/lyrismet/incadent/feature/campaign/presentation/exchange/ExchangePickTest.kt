package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeOptions
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExchangePickTest {
    private val options =
        ExchangeOptions(
            places = listOf(ExchangeItem(1, "Town"), ExchangeItem(2, "Inn", "Town")),
            npcs = listOf(ExchangeItem(10, "Bob"), ExchangeItem(11, "Ann")),
            species = listOf(ExchangeItem(100, "Wolf")),
            party = listOf(ExchangeItem(40, "Hero")),
        )
    private val graph =
        ExchangeGraph(
            placeParent = mapOf(1L to null, 2L to 1L),
            npcSpecies = mapOf(10L to 100L),
            npcPlaces = mapOf(10L to listOf(2L)),
        )
    private val empty = ExchangePick(options, graph, RecordSelection(), tab = ExchangeTab.NPCS)

    private fun marks(pick: ExchangePick) = pick.rows().associate { it.name to it.mark }

    @Test
    fun `a ticked npc pulls its species and place without ticking them`() {
        val pick = empty.toggled(10)
        assertEquals(PickMark.On, marks(pick).getValue("Bob"))
        assertEquals(PickMark.Pulled, marks(pick.withTab(ExchangeTab.SPECIES)).getValue("Wolf"))
        val places = marks(pick.withTab(ExchangeTab.PLACES))
        assertEquals(PickMark.Pulled, places.getValue("Inn"))
        assertEquals(PickMark.Pulled, places.getValue("Town"))
    }

    @Test
    fun `unticking the source lets go of what it pulled`() {
        val pick = empty.toggled(10).toggled(10)
        assertEquals(PickMark.Off, marks(pick.withTab(ExchangeTab.SPECIES)).getValue("Wolf"))
        assertEquals(0, pick.recordCount)
    }

    @Test
    fun `a pulled row cannot be unticked by hand`() {
        val pulled = empty.toggled(10).withTab(ExchangeTab.SPECIES)
        assertEquals(pulled.selection, pulled.toggled(100).selection)
    }

    @Test
    fun `select all ticks what the search shows and a second tap clears it`() {
        val shown = empty.withQuery("an").allToggled()
        assertEquals(setOf(11L), shown.selection.npcs)
        assertTrue(shown.allShownTicked())
        assertEquals(emptySet(), shown.allToggled().selection.npcs)
    }

    @Test
    fun `the whole campaign takes the party and a hand pick starts without it`() {
        assertEquals(setOf(40L), ExchangePick.everything(options, graph).selection.party)
        assertEquals(emptySet(), ExchangePick.handPick(options, graph).selection.party)
        assertEquals(setOf(10L, 11L), ExchangePick.handPick(options, graph).selection.npcs)
    }

    @Test
    fun `tab counts include pulled records`() {
        val counts = empty.toggled(10).tabCounts().associateBy { it.tab }
        assertEquals(1, counts.getValue(ExchangeTab.NPCS).inFile)
        assertEquals(2, counts.getValue(ExchangeTab.PLACES).inFile)
        assertEquals(2, counts.getValue(ExchangeTab.NPCS).total)
    }

    @Test
    fun `a pulled row says who needs it`() {
        val pick = empty.toggled(10)
        val species = pick.withTab(ExchangeTab.SPECIES).rows().single()
        assertEquals(PullReason(PullKind.NPC_SPECIES, "Bob"), species.reason)
        val places = pick.withTab(ExchangeTab.PLACES).rows().associate { it.name to it.reason }
        assertEquals(PullReason(PullKind.NPC_PLACE, "Bob"), places.getValue("Inn"))
        assertEquals(PullReason(PullKind.PLACE_PARENT, "Inn"), places.getValue("Town"))
    }

    @Test
    fun `a ticked row has no reason`() {
        assertEquals(
            null,
            empty
                .toggled(10)
                .rows()
                .first { it.id == 10L }
                .reason,
        )
    }
}

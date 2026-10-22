package com.lyrismet.incadent.feature.campaign

import com.lyrismet.incadent.feature.campaign.domain.exchange.resolve
import com.lyrismet.incadent.feature.campaign.domain.exchange.withKnownMentionsOnly
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeOptions
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import kotlin.test.Test
import kotlin.test.assertEquals

class KnownMentionsTest {
    private val options =
        ExchangeOptions(
            sessions = listOf(ExchangeItem(1, "День")),
            npcs = listOf(ExchangeItem(1, "Кассиан"), ExchangeItem(2, "Мира")),
        )
    private val graph =
        ExchangeGraph(
            sessionMentions =
                mapOf(
                    1L to
                        setOf(
                            ExchangeRef(ExchangeKind.NPC, 1),
                            ExchangeRef(ExchangeKind.NPC, 0),
                            ExchangeRef(ExchangeKind.NPC, 99),
                        ),
                ),
        )

    @Test
    fun `a mention of a record that is not on offer is dropped`() {
        val known = graph.withKnownMentionsOnly(options)

        assertEquals(setOf(ExchangeRef(ExchangeKind.NPC, 1)), known.sessionMentions.getValue(1))
    }

    @Test
    fun `a ticked session does not drag in a record that does not exist`() {
        val scope = RecordSelection(sessions = setOf(1)).resolve(graph.withKnownMentionsOnly(options))

        assertEquals(setOf(1L), scope.npcs)
    }
}

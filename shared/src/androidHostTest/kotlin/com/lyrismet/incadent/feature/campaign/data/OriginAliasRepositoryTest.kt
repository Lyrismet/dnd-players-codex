package com.lyrismet.incadent.feature.campaign.data

import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OriginAliasRepositoryTest {
    private val db = TestDatabase()
    private val aliases = OriginAliasRepositoryImpl(db.database, db.current)

    @Test
    fun `a merged id resolves to the survivor and an unknown id stays as it is`() =
        runTest {
            aliases.add(ExchangeKind.NPC, survivorOriginId = "keep", mergedOriginId = "gone")

            assertEquals("keep", aliases.resolve(ExchangeKind.NPC, "gone"))
            assertEquals("other", aliases.resolve(ExchangeKind.NPC, "other"))
        }

    @Test
    fun `an alias belongs to its kind and its campaign`() =
        runTest {
            aliases.add(ExchangeKind.NPC, "keep", "gone")

            assertEquals("gone", aliases.resolve(ExchangeKind.QUEST, "gone"))
            db.addCampaign(TestDatabase.SECOND_CAMPAIGN)
            db.current.open(TestDatabase.SECOND_CAMPAIGN)
            assertEquals("gone", aliases.resolve(ExchangeKind.NPC, "gone"))
        }

    @Test
    fun `a chain of merges resolves to the last survivor`() =
        runTest {
            aliases.add(ExchangeKind.NPC, survivorOriginId = "b", mergedOriginId = "a")
            aliases.add(ExchangeKind.NPC, survivorOriginId = "c", mergedOriginId = "b")

            assertEquals("c", aliases.resolve(ExchangeKind.NPC, "a"))
            assertEquals("c", aliases.resolve(ExchangeKind.NPC, "b"))
        }
}

package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.CharacterFeat
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.FeatSource
import com.lyrismet.incadent.feature.codex.domain.model.Item
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SheetMeRecordsTest {
    private val db = TestDatabase()
    private val party = PartyRepositoryImpl(db.database, db.current)
    private val items = ItemRepositoryImpl(db.database, db.current)
    private val feats = CharacterFeatRepositoryImpl(db.database, db.current)

    private suspend fun lira(portrait: Photo? = null): Long =
        party.upsert(
            PartyMember(
                id = 0,
                name = "Lira",
                characterClass = "Wizard",
                race = "",
                level = 5,
                playerName = "",
                isPlayerCharacter = true,
                presence = PartyPresence.IN,
                hpMax = 28,
                hpCurrent = 22,
                description = "",
                portrait = portrait,
            ),
        )

    @Test
    fun `a worn item stays worn when it is saved again and comes back`() =
        runTest {
            val owner = ItemOwner.Of(EntityKind.PARTY, lira())
            val id = items.upsert(Item(id = 0, name = "Cloak", owner = owner, worn = true))
            items.upsert(items.getById(id)!!.copy(quantity = 3))
            assertTrue(items.getById(id)!!.worn)
            assertTrue(
                items
                    .observeAll()
                    .first()
                    .single()
                    .worn,
            )
            items.upsert(items.getById(id)!!.copy(worn = false))
            assertEquals(false, items.getById(id)!!.worn)
        }

    @Test
    fun `an item put back after a delete is still worn`() =
        runTest {
            val id = items.upsert(Item(id = 0, name = "Cloak", worn = true))
            val gone = items.getById(id)!!
            items.delete(id)
            items.upsert(gone)
            assertTrue(items.getById(id)!!.worn)
        }

    @Test
    fun `the shields of a member are written on their own and a form save keeps them`() =
        runTest {
            val id = lira()
            party.setVulnerabilities(id, mapOf(DamageType.FIRE to DamageState.RESISTANCE))
            party.upsert(party.getById(id)!!.copy(name = "Lira B").copy(vulnerabilities = emptyMap()))
            assertEquals(mapOf(DamageType.FIRE to DamageState.RESISTANCE), party.getById(id)!!.vulnerabilities)
            assertEquals(
                mapOf(DamageType.FIRE to DamageState.RESISTANCE),
                party
                    .observeAll()
                    .first()
                    .single()
                    .vulnerabilities,
            )
        }

    @Test
    fun `a member deleted and put back keeps the shields`() =
        runTest {
            val id = lira()
            party.setVulnerabilities(id, mapOf(DamageType.COLD to DamageState.IMMUNITY))
            val gone = party.getById(id)!!
            party.delete(id)
            party.upsert(gone)
            assertEquals(mapOf(DamageType.COLD to DamageState.IMMUNITY), party.getById(id)!!.vulnerabilities)
        }

    @Test
    fun `feats keep their order and fields and take an edit`() =
        runTest {
            val member = lira()
            feats.upsert(member, CharacterFeat(name = "Lucky", source = FeatSource.FEAT, description = "reroll"))
            val second = feats.upsert(member, CharacterFeat(name = "Darkvision"))
            assertEquals(listOf("Lucky", "Darkvision"), feats.observe(member).first().map { it.name })
            assertEquals(
                FeatSource.FEAT,
                feats
                    .observe(member)
                    .first()
                    .first()
                    .source,
            )
            assertNull(feats.getById(second)!!.source)
            feats.upsert(member, feats.getById(second)!!.copy(name = "Superior Darkvision", source = FeatSource.RACE))
            assertEquals("Superior Darkvision", feats.getById(second)!!.name)
            assertEquals(FeatSource.RACE, feats.getById(second)!!.source)
            assertEquals(2, feats.observe(member).first().size)
        }

    @Test
    fun `a deleted feat comes back under its id and place`() =
        runTest {
            val member = lira()
            val first = feats.upsert(member, CharacterFeat(name = "Lucky"))
            feats.upsert(member, CharacterFeat(name = "Alert"))
            val gone = feats.getById(first)!!
            feats.delete(first)
            feats.upsert(member, gone)
            assertEquals(listOf("Lucky", "Alert"), feats.observe(member).first().map { it.name })
            assertEquals(
                first,
                feats
                    .observe(member)
                    .first()
                    .first()
                    .id,
            )
        }

    @Test
    fun `feats go with the member`() =
        runTest {
            val member = lira()
            feats.upsert(member, CharacterFeat(name = "Lucky"))
            party.delete(member)
            assertEquals(
                0,
                db.database.characterFeatQueries
                    .selectByMember(member)
                    .executeAsList()
                    .size,
            )
        }

    @Test
    fun `a cropped portrait keeps its source and a list row leaves it out`() =
        runTest {
            val id = lira(Photo(thumb = byteArrayOf(1), full = byteArrayOf(2, 2), source = byteArrayOf(3, 3, 3)))
            val loaded = party.getById(id)!!.portrait!!
            assertContentEquals(byteArrayOf(2, 2), loaded.full)
            assertContentEquals(byteArrayOf(3, 3, 3), loaded.source)
            assertNull(
                party
                    .observeAll()
                    .first()
                    .single()
                    .portrait!!
                    .source,
            )
        }

    @Test
    fun `a new portrait without a source clears the old source`() =
        runTest {
            val id = lira(Photo(thumb = byteArrayOf(1), full = byteArrayOf(2), source = byteArrayOf(3)))
            party.upsert(party.getById(id)!!.copy(portrait = Photo(thumb = byteArrayOf(9), full = byteArrayOf(8))))
            val loaded = party.getById(id)!!.portrait!!
            assertContentEquals(byteArrayOf(8), loaded.full)
            assertNull(loaded.source)
        }

    @Test
    fun `a recrop from the source writes the new cut and keeps the source`() =
        runTest {
            val id = lira(Photo(thumb = byteArrayOf(1), full = byteArrayOf(2), source = byteArrayOf(3, 3)))
            val stored = party.getById(id)!!.portrait!!
            party.upsert(
                party.getById(id)!!.copy(portrait = Photo(byteArrayOf(5), byteArrayOf(6), stored.source)),
            )
            val loaded = party.getById(id)!!.portrait!!
            assertContentEquals(byteArrayOf(6), loaded.full)
            assertContentEquals(byteArrayOf(3, 3), loaded.source)
        }

    @Test
    fun `a photo made before the source was kept loads without one and saves unchanged`() =
        runTest {
            val id = lira(Photo(thumb = byteArrayOf(1), full = byteArrayOf(2)))
            assertNull(party.getById(id)!!.portrait!!.source)
            party.upsert(party.getById(id)!!.copy(name = "Lira B"))
            assertContentEquals(byteArrayOf(2), party.getById(id)!!.portrait!!.full)
        }

    @Test
    fun `an item and an npc photo keep their source too`() =
        runTest {
            val itemId =
                items.upsert(
                    Item(id = 0, name = "Ring", photo = Photo(byteArrayOf(1), byteArrayOf(2), byteArrayOf(4))),
                )
            assertContentEquals(byteArrayOf(4), items.getById(itemId)!!.photo!!.source)
        }
}

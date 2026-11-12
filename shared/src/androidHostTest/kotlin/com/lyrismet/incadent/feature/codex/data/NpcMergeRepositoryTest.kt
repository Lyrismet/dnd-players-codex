package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.campaign.data.OriginAliasRepositoryImpl
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.combat.data.CombatRepositoryImpl
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.session.data.SessionEntryRepositoryImpl
import com.lyrismet.incadent.feature.session.data.SessionNoteRepositoryImpl
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NpcMergeRepositoryTest {
    private val db = TestDatabase()
    private val npcs = NpcRepositoryImpl(db.database, db.current)
    private val quests = QuestRepositoryImpl(db.database, db.current)
    private val places = LocationRepositoryImpl(db.database, db.current)
    private val notes = SessionNoteRepositoryImpl(db.database, db.current)
    private val entries = SessionEntryRepositoryImpl(db.database, db.current)
    private val combats = CombatRepositoryImpl(db.database, db.current)
    private val aliases = OriginAliasRepositoryImpl(db.database, db.current)
    private val merge = NpcMergeRepositoryImpl(db.database, db.current, npcs)

    private fun npc(
        name: String,
        description: String = "",
        race: String = "",
        locations: List<Long> = emptyList(),
        origin: String = "",
    ) = Npc(0, name, NpcStatus.NEUTRAL, NpcLifeState.ALIVE, description, locations, race, originId = origin)

    private fun mention(id: Long) = "@[Вазгар|n:$id]"

    @Test
    fun `the fighters of the loser fight as the survivor`() =
        runTest {
            val survivor = npcs.upsert(npc("Вазгар"))
            val loser = npcs.upsert(npc("Вазгар"))
            val combat = combats.create(null, LocalDateTime(2026, 1, 1, 12, 0))
            val fighter =
                Combatant(0, "Вазгар", CombatSide.ENEMY, loser, null, null, true, CombatState.WHOLE, 1, 0, 0)
            val member = combats.addCombatant(combat, fighter)

            val trace = assertNotNull(merge.merge(survivor, loser))

            assertEquals(
                survivor,
                combats
                    .observe(combat)
                    .first()!!
                    .combatants
                    .single { it.id == member }
                    .npcId,
            )
            assertNull(npcs.getById(loser))
            merge.undo(trace)
            assertEquals(
                loser,
                combats
                    .observe(combat)
                    .first()!!
                    .combatants
                    .single { it.id == member }
                    .npcId,
            )
        }

    @Test
    fun `a mention of the loser in the description of the survivor points at the survivor`() =
        runTest {
            val survivor = npcs.upsert(npc("Вазгар"))
            val loser = npcs.upsert(npc("Вазгар"))
            npcs.upsert(npcs.getById(survivor)!!.copy(description = "брат ${mention(loser)}"))

            val trace = assertNotNull(merge.merge(survivor, loser))

            assertEquals("брат ${mention(survivor)}", npcs.getById(survivor)!!.description)
            merge.undo(trace)
            assertEquals("брат ${mention(loser)}", npcs.getById(survivor)!!.description)
        }

    @Test
    fun `the stored name of a fighter follows the rename of its npc`() =
        runTest {
            val id = npcs.upsert(npc("Вазгар"))
            val combat = combats.create(null, LocalDateTime(2026, 1, 1, 12, 0))
            combats.addCombatant(
                combat,
                Combatant(0, "Вазгар", CombatSide.ENEMY, id, null, null, true, CombatState.WHOLE, 1, 0, 0),
            )

            npcs.upsert(npcs.getById(id)!!.copy(name = "Рагнар"))

            assertEquals(
                "Рагнар",
                db.database.combatantQueries
                    .selectByCombat(combat)
                    .executeAsList()
                    .single()
                    .name,
            )
        }

    @Test
    fun `places join and the places of the survivor stay`() =
        runTest {
            val town = places.upsert(Location(0, "Город", ""))
            val inn = places.upsert(Location(0, "Трактир", ""))
            val survivor = npcs.upsert(npc("Вазгар", locations = listOf(town)))
            val loser = npcs.upsert(npc("Вазгар", locations = listOf(inn)))

            val trace = assertNotNull(merge.merge(survivor, loser))

            assertEquals(setOf(town, inn), npcs.getById(survivor)!!.locationIds.toSet())
            merge.undo(trace)
            assertEquals(listOf(town), npcs.getById(survivor)!!.locationIds)
            assertEquals(listOf(inn), npcs.getById(loser)!!.locationIds)
        }

    @Test
    fun `a quest given by the loser is given by the survivor`() =
        runTest {
            val survivor = npcs.upsert(npc("Вазгар"))
            val loser = npcs.upsert(npc("Вазгар"))
            val quest = quests.upsert(Quest(0, "Найти", QuestStatus.ACTIVE, "", loser, null))

            val trace = assertNotNull(merge.merge(survivor, loser))

            assertEquals(survivor, quests.getById(quest)!!.givenByNpcId)
            merge.undo(trace)
            assertEquals(loser, quests.getById(quest)!!.givenByNpcId)
        }

    @Test
    fun `mentions in notes and descriptions move and come back with the undo`() =
        runTest {
            val survivor = npcs.upsert(npc("Вазгар"))
            val loser = npcs.upsert(npc("Вазгар"))
            val session = notes.upsert(SessionNote(0, "С", LocalDateTime(2026, 1, 1, 12, 0), null))
            val entry = entries.add(session, "встретили ${mention(loser)} у моста")
            val counted = entries.add(session, "нашли @[Вазгар|n:$loser|3]")
            val quest = quests.upsert(Quest(0, "Найти", QuestStatus.ACTIVE, "", null, null, "ищет ${mention(loser)}"))
            val other = npcs.upsert(npc("Кира", description = "друг ${mention(loser)}"))
            val untouched = entries.add(session, "вот ${mention(loser + 100)}")

            val trace = assertNotNull(merge.merge(survivor, loser))

            val bodies = entries.observeAll().first().associate { it.id to it.body }
            assertEquals("встретили ${mention(survivor)} у моста", bodies[entry])
            assertEquals("нашли @[Вазгар|n:$survivor|3]", bodies[counted])
            assertEquals("вот ${mention(loser + 100)}", bodies[untouched])
            assertEquals("ищет ${mention(survivor)}", quests.getById(quest)!!.description)
            assertEquals("друг ${mention(survivor)}", npcs.getById(other)!!.description)

            merge.undo(trace)
            val back = entries.observeAll().first().associate { it.id to it.body }
            assertEquals("встретили ${mention(loser)} у моста", back[entry])
            assertEquals("нашли @[Вазгар|n:$loser|3]", back[counted])
            assertEquals("ищет ${mention(loser)}", quests.getById(quest)!!.description)
            assertEquals("друг ${mention(loser)}", npcs.getById(other)!!.description)
        }

    @Test
    fun `empty fields are filled and filled ones are kept`() =
        runTest {
            val survivor = npcs.upsert(npc("Вазгар", description = "старый", race = ""))
            val loser = npcs.upsert(npc("Вазгар", description = "другой", race = "Орк"))

            merge.merge(survivor, loser)

            val merged = npcs.getById(survivor)!!
            assertEquals("старый", merged.description)
            assertEquals("Орк", merged.race)
        }

    @Test
    fun `the origin id of the loser leads to the survivor and the aliases that led to it follow`() =
        runTest {
            val survivor = npcs.upsert(npc("Вазгар", origin = "keep"))
            val loser = npcs.upsert(npc("Вазгар", origin = "gone"))
            aliases.add(ExchangeKind.NPC, "gone", "older")

            val trace = assertNotNull(merge.merge(survivor, loser))

            assertEquals("keep", aliases.resolve(ExchangeKind.NPC, "gone"))
            assertEquals("keep", aliases.resolve(ExchangeKind.NPC, "older"))
            merge.undo(trace)
            assertEquals("gone", aliases.resolve(ExchangeKind.NPC, "gone"))
            assertEquals("gone", aliases.resolve(ExchangeKind.NPC, "older"))
            assertEquals("gone", npcs.getById(loser)!!.originId)
        }

    @Test
    fun `the loser comes back under its old id with its fields`() =
        runTest {
            val survivor = npcs.upsert(npc("Вазгар", description = "", race = ""))
            val loser = npcs.upsert(npc("Вазгар", description = "другой", race = "Орк"))

            val trace = assertNotNull(merge.merge(survivor, loser))
            merge.undo(trace)

            assertEquals("другой", npcs.getById(loser)!!.description)
            assertEquals("Орк", npcs.getById(loser)!!.race)
            assertEquals("", npcs.getById(survivor)!!.description)
            assertEquals("", npcs.getById(survivor)!!.race)
        }

    @Test
    fun `a record is not merged into itself or into a missing one`() =
        runTest {
            val one = npcs.upsert(npc("Вазгар"))
            assertNull(merge.merge(one, one))
            assertNull(merge.merge(one, one + 50))
        }
}

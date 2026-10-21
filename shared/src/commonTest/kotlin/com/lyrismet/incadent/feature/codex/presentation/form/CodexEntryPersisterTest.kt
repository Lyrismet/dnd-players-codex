package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeLocationRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeNpcRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakePartyRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeQuestRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.FakeSpeciesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CodexEntryPersisterTest {
    private val npcs = FakeNpcRepository()
    private val party = FakePartyRepository()
    private val quests = FakeQuestRepository()
    private val locations = FakeLocationRepository()
    private val species = FakeSpeciesRepository()
    private val persister = CodexEntryPersister(npcs, party, quests, locations, species)

    private fun form(
        type: EntityKind,
        name: String = "  громм  ",
    ) = CodexEntryFormFields(editingRef = null, type = type, name = name)

    @Test
    fun `a new npc is trimmed capitalized and written to the npc store only`() =
        runTest {
            persister.save(
                form(EntityKind.NPC).copy(
                    race = " дварф ",
                    description = "  кузнец ",
                    npcStatus = NpcStatus.FRIEND,
                    npcLife = NpcLifeState.ALIVE,
                    npcLocationIds = listOf(4),
                ),
            )

            val saved = npcs.all.single()
            assertEquals("Громм", saved.name)
            assertEquals("Дварф", saved.race)
            assertEquals("кузнец", saved.description)
            assertEquals(NpcStatus.FRIEND, saved.status)
            assertEquals(listOf(4L), saved.locationIds)
            assertEquals(0, party.all.size + quests.all.size + locations.all.size)
        }

    @Test
    fun `the id of the record being edited wins over a revive id`() =
        runTest {
            val existing = Npc(5, "Старый", NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), "")
            val withNpc = FakeNpcRepository(existing)
            val edit = form(EntityKind.NPC, "новый").copy(editingRef = EntityRef.Npc(5), reviveId = 99)

            CodexEntryPersister(withNpc, party, quests, locations, species).save(edit)

            assertEquals(listOf(5L), withNpc.all.map { it.id })
            assertEquals("Новый", withNpc.stored(5)?.name)
        }

    @Test
    fun `a revive id creates the record under the old id`() =
        runTest {
            val id = persister.save(form(EntityKind.LOCATION, "таверна").copy(reviveId = 42))

            assertEquals(42L, id)
            assertEquals("Таверна", locations.stored(42)?.name)
        }

    @Test
    fun `a player character keeps no player name and starts at full hp within the ranges`() =
        runTest {
            persister.save(
                form(EntityKind.PARTY, "бран").copy(
                    party =
                        PartyFormFields(
                            isPlayerCharacter = true,
                            player = "Аня",
                            characterClass = "воин",
                            presence = PartyPresence.IN,
                            level = 99,
                            hpMax = 30,
                        ),
                ),
            )

            val saved = party.all.single()
            assertEquals("", saved.playerName)
            assertEquals(20, saved.level)
            assertEquals(30, saved.hpCurrent)
            assertEquals("Воин", saved.characterClass)
        }

    @Test
    fun `a quest carries its giver place and status`() =
        runTest {
            persister.save(
                form(EntityKind.QUEST, "ключ").copy(
                    reward = " 200 зм ",
                    questGiverId = 7,
                    questLocationId = 8,
                    questStatus = QuestStatus.COMPLETED,
                ),
            )

            val saved = quests.all.single()
            assertEquals("Ключ", saved.title)
            assertEquals("200 зм", saved.reward)
            assertEquals(7L, saved.givenByNpcId)
            assertEquals(8L, saved.locationId)
            assertEquals(QuestStatus.COMPLETED, saved.status)
        }

    @Test
    fun `a record read into the form and written back is unchanged`() =
        runTest {
            val original =
                Npc(3, "Громм", NpcStatus.ENEMY, NpcLifeState.DEAD, "кузнец", listOf(1, 2), "Дварф")

            assertEquals(original, original.toFields(EntityRef.Npc(3)).toNpc())
        }

    @Test
    fun `a species is trimmed capitalized and keeps its type and nicknames`() =
        runTest {
            persister.save(
                form(EntityKind.SPECIES, " гоблин ").copy(
                    speciesType = CreatureType.FEY,
                    nicknames = "зелёные, мелкие",
                    description = " мерзкие ",
                ),
            )

            val saved = species.all.single()
            assertEquals("Гоблин", saved.name)
            assertEquals(CreatureType.FEY, saved.creatureType)
            assertEquals(listOf("зелёные", "мелкие"), saved.nicknames)
            assertEquals("мерзкие", saved.notes)
            assertEquals(0, npcs.all.size)
        }

    @Test
    fun `editing a species leaves the damage the card set`() =
        runTest {
            val damage = mapOf(DamageType.FIRE to DamageState.VULNERABILITY)
            val existing = Species(3, "Скелет", CreatureType.UNDEAD, damage)
            val edit =
                form(EntityKind.SPECIES, "скелеты").copy(
                    editingRef = EntityRef.Species(3),
                    speciesType = CreatureType.UNDEAD,
                    speciesDamage = existing.damage,
                )

            persister.save(edit)

            assertEquals(damage, species.stored(3)?.damage)
            assertEquals("Скелеты", species.stored(3)?.name)
        }

    @Test
    fun `an npc keeps the species it was linked to`() =
        runTest {
            persister.save(form(EntityKind.NPC, "грикк").copy(npcSpeciesId = 3))

            assertEquals(3L, npcs.all.single().speciesId)
        }
}

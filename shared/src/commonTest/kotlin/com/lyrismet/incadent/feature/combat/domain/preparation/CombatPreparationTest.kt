package com.lyrismet.incadent.feature.combat.domain.preparation

import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class CombatPreparationTest {
    private val repository = FakeCombatRepository()
    private val preparation = CombatPreparation(repository)
    private val startedAt = LocalDateTime.parse("2026-10-22T20:15")
    private val skeleton = MemberSource(npcId = null, speciesId = 5, baseName = "Скелет-лучник")

    private fun namesOf(
        combatId: Long,
        side: CombatSide,
    ) = repository
        .get(combatId)
        .combatants
        .filter { it.side == side }
        .map { it.name }

    @Test
    fun `a new preparation seats the party`() =
        runTest {
            val id = preparation.start(startedAt, listOf(PartySeat(1, "Аэлин"), PartySeat(2, "Борк")))
            assertEquals(listOf("Аэлин", "Борк"), namesOf(id, CombatSide.PARTY))
        }

    @Test
    fun `a party member can leave and come back`() =
        runTest {
            val seat = PartySeat(1, "Аэлин")
            val id = preparation.start(startedAt, listOf(seat))
            preparation.setPartyIncluded(repository.get(id), seat, included = false)
            assertEquals(emptyList(), namesOf(id, CombatSide.PARTY))
            preparation.setPartyIncluded(repository.get(id), seat, included = true)
            assertEquals(listOf("Аэлин"), namesOf(id, CombatSide.PARTY))
        }

    @Test
    fun `a second skeleton numbers both`() =
        runTest {
            val id = preparation.start(startedAt, emptyList())
            preparation.add(repository.get(id), CombatSide.ENEMY, skeleton)
            assertEquals(listOf("Скелет-лучник"), namesOf(id, CombatSide.ENEMY))
            preparation.add(repository.get(id), CombatSide.ENEMY, skeleton)
            assertEquals(listOf("Скелет-лучник I", "Скелет-лучник II"), namesOf(id, CombatSide.ENEMY))
        }

    @Test
    fun `removing one leaves the last one without a number`() =
        runTest {
            val id = preparation.start(startedAt, emptyList())
            repeat(2) { preparation.add(repository.get(id), CombatSide.ENEMY, skeleton) }
            preparation.removeOne(repository.get(id), CombatSide.ENEMY, skeleton)
            assertEquals(listOf("Скелет-лучник"), namesOf(id, CombatSide.ENEMY))
        }

    @Test
    fun `a named npc is added only once`() =
        runTest {
            val id = preparation.start(startedAt, emptyList())
            val npc = MemberSource(npcId = 9, speciesId = null, baseName = "Мортис")
            repeat(2) { preparation.add(repository.get(id), CombatSide.ENEMY, npc) }
            assertEquals(listOf("Мортис"), namesOf(id, CombatSide.ENEMY))
        }

    @Test
    fun `removing all takes the whole group out`() =
        runTest {
            val id = preparation.start(startedAt, emptyList())
            repeat(3) { preparation.add(repository.get(id), CombatSide.ENEMY, skeleton) }
            preparation.removeAll(repository.get(id), CombatSide.ENEMY, skeleton)
            assertEquals(emptyList(), namesOf(id, CombatSide.ENEMY))
        }

    @Test
    fun `a summon without a record is a group by its name`() =
        runTest {
            val id = preparation.start(startedAt, emptyList())
            val wolf = MemberSource(npcId = null, speciesId = null, baseName = "Призрачный волк")
            repeat(2) { preparation.add(repository.get(id), CombatSide.ALLY, wolf) }
            assertEquals(listOf("Призрачный волк I", "Призрачный волк II"), namesOf(id, CombatSide.ALLY))
        }
}

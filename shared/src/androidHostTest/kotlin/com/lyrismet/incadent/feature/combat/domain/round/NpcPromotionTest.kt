package com.lyrismet.incadent.feature.combat.domain.round

import com.lyrismet.incadent.feature.codex.data.NpcRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.SpeciesRepositoryImpl
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.combat.data.CombatRepositoryImpl
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.LearnedVulnerability
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class NpcPromotionTest {
    private val db = TestDatabase()
    private val combats = CombatRepositoryImpl(db.database, db.current)
    private val npcs = NpcRepositoryImpl(db.database, db.current)
    private val species = SpeciesRepositoryImpl(db.database, db.current)
    private val promotion = NpcPromotion(combats, npcs, species)

    private suspend fun setup(): Triple<Long, Long, Combatant> {
        val speciesId =
            species.upsert(
                Species(0, "Культист", CreatureType.HUMANOID, mapOf(DamageType.FIRE to DamageState.VULNERABILITY)),
            )
        val combatId = combats.create(null, LocalDateTime.parse("2026-10-23T20:00"))
        val member =
            Combatant(
                id = 0,
                name = "Культист III",
                side = CombatSide.ENEMY,
                npcId = null,
                speciesId = speciesId,
                partyMemberId = null,
                enteredWhole = true,
                state = CombatState.WHOLE,
                joinedRound = 1,
                deathSuccesses = 0,
                deathFailures = 0,
                stance = CombatStance.NEUTRAL,
                note = " с кинжалом ",
            )
        val id = combats.addCombatant(combatId, member)
        val stored = assertNotNull(combats.observe(combatId).first()).combatants.single { it.id == id }
        return Triple(speciesId, combatId, stored)
    }

    @Test
    fun `a new name makes an npc of the species and links the fighter to it`() =
        runTest {
            val (speciesId, combatId, member) = setup()
            val trace = assertNotNull(promotion.promote(member, NameChoice.New("Вазгар")))

            val npc = assertNotNull(npcs.getById(trace.npcId))
            assertEquals("Вазгар", npc.name)
            assertEquals(speciesId, npc.speciesId)
            assertEquals("Культист", npc.race)
            assertEquals(NpcStatus.NEUTRAL, npc.status)
            assertEquals("с кинжалом", npc.description)
            val linked = assertNotNull(combats.observe(combatId).first()).combatants.single()
            assertEquals(trace.npcId, linked.npcId)
            assertEquals("Вазгар", linked.name)
            assertEquals(speciesId, linked.speciesId)
        }

    @Test
    fun `taking the naming back removes the made npc and brings the name back`() =
        runTest {
            val (_, combatId, member) = setup()
            val trace = assertNotNull(promotion.promote(member, NameChoice.New("Вазгар")))
            promotion.undo(trace)

            assertNull(npcs.getById(trace.npcId))
            val back = assertNotNull(combats.observe(combatId).first()).combatants.single()
            assertEquals("Культист III", back.name)
            assertNull(back.npcId)
        }

    @Test
    fun `a caption only renames the fighter`() =
        runTest {
            val (_, combatId, member) = setup()
            assertNull(promotion.promote(member, NameChoice.Plain("Слепой")))
            val renamed = assertNotNull(combats.observe(combatId).first()).combatants.single()
            assertEquals("Слепой", renamed.name)
            assertNull(renamed.npcId)
        }

    @Test
    fun `what the fight learned moves to the npc and the species gets its old state back`() =
        runTest {
            val (speciesId, combatId, member) = setup()
            val learned = LearnedVulnerability(speciesId, DamageType.FIRE, null)
            combats.addLearned(combatId, learned)
            val trace = assertNotNull(promotion.promote(member, NameChoice.New("Вазгар")))

            val back = promotion.moveLearned(combatId, trace, listOf(learned), setOf(DamageType.FIRE))

            assertEquals(
                mapOf(DamageType.FIRE to DamageState.VULNERABILITY),
                assertNotNull(npcs.getById(trace.npcId)).vulnerabilities,
            )
            assertEquals(emptyMap(), assertNotNull(species.getById(speciesId)).vulnerabilities)
            assertEquals(emptyList(), combats.observeLearned(combatId).first())

            back()
            assertEquals(emptyMap(), assertNotNull(npcs.getById(trace.npcId)).vulnerabilities)
            assertEquals(
                mapOf(DamageType.FIRE to DamageState.VULNERABILITY),
                assertNotNull(species.getById(speciesId)).vulnerabilities,
            )
            assertEquals(listOf(learned), combats.observeLearned(combatId).first())
        }
}

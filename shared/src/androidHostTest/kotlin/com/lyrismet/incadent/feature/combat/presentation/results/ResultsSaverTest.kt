package com.lyrismet.incadent.feature.combat.presentation.results

import com.lyrismet.incadent.feature.codex.data.LocationRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.NpcRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.PartyRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.QuestRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.SpeciesRepositoryImpl
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Species
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.combat.data.CombatRepositoryImpl
import com.lyrismet.incadent.feature.combat.data.HitObservationRepositoryImpl
import com.lyrismet.incadent.feature.combat.domain.model.CombatEvent
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.feature.combat.domain.results.CodexItem
import com.lyrismet.incadent.feature.combat.domain.results.HitObservationDraft
import com.lyrismet.incadent.feature.combat.domain.results.ResistanceGuess
import com.lyrismet.incadent.feature.session.data.SessionNoteRepositoryImpl
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultsSaverTest {
    private val db = TestDatabase()
    private val combats = CombatRepositoryImpl(db.database, db.current)
    private val notes = SessionNoteRepositoryImpl(db.database, db.current)
    private val observations = HitObservationRepositoryImpl(db.database, db.current)
    private val codex =
        CodexRepositories(
            PartyRepositoryImpl(db.database, db.current),
            NpcRepositoryImpl(db.database, db.current),
            QuestRepositoryImpl(db.database, db.current),
            LocationRepositoryImpl(db.database, db.current),
            SpeciesRepositoryImpl(db.database, db.current),
        )
    private val saver = ResultsSaver(combats, notes, observations, codex)
    private val now = LocalDateTime.parse("2026-10-22T21:00")

    private suspend fun fight(
        speciesId: Long?,
        npcId: Long? = null,
    ): Pair<Long, Long> {
        val combatId = combats.create(null, now)
        val member =
            Combatant(
                id = 0,
                name = "Гоблин",
                side = CombatSide.ENEMY,
                npcId = npcId,
                speciesId = speciesId,
                partyMemberId = null,
                enteredWhole = true,
                state = CombatState.DOWN,
                joinedRound = 1,
                deathSuccesses = 0,
                deathFailures = 0,
            )
        val memberId = combats.addCombatant(combatId, member)
        combats.addEvent(
            combatId,
            CombatEvent(0, 1, CombatEventKind.DAMAGE, null, memberId, 9, DamageType.FIRE, HitEffect.HALF, null),
        )
        return combatId to memberId
    }

    @Test
    fun `saving without a running session makes one and closes the fight in it`() =
        runTest {
            val (combatId, _) = fight(null)
            val combat = assertNotNull(combats.observe(combatId).first())
            val saved = saver.save(combat, CombatOutcome.VICTORY, emptyList(), "Новая", now)

            val stored = assertNotNull(combats.observe(combatId).first())
            assertEquals(CombatPhase.FINISHED, stored.phase)
            assertEquals(CombatOutcome.VICTORY, stored.outcome)
            assertEquals(now, stored.finishedAt)
            assertEquals(saved.sessionId, stored.sessionNoteId)
            val session = assertNotNull(notes.getById(saved.sessionId))
            assertTrue(session.isLive)
        }

    @Test
    fun `saving goes under the running session when there is one`() =
        runTest {
            val live =
                notes.upsert(
                    com.lyrismet.incadent.feature.session.domain.model
                        .SessionNote(0, "Вечер", now, null),
                )
            val (combatId, _) = fight(null)
            val combat = assertNotNull(combats.observe(combatId).first())
            val saved = saver.save(combat, CombatOutcome.PEACE, emptyList(), "Новая", now)
            assertEquals(live, saved.sessionId)
            assertEquals(1, notes.observeAll().first().size)
        }

    @Test
    fun `only the ticked items are written and one undo takes them all back`() =
        runTest {
            val speciesId =
                codex.speciesRepository.upsert(Species(0, "Гоблин", CreatureType.HUMANOID))
            val npcId =
                codex.npcRepository.upsert(Npc(0, "Вожак", NpcStatus.ENEMY, NpcLifeState.ALIVE, "", emptyList(), ""))
            val (combatId, memberId) = fight(speciesId)
            val combat = assertNotNull(combats.observe(combatId).first())
            val ticked =
                listOf(
                    CodexItem.HitObserved(HitObservationDraft(memberId, "Гоблин", speciesId, 9, true)),
                    CodexItem.Resistance(
                        ResistanceGuess(speciesId, "Гоблин", DamageType.FIRE, DamageState.RESISTANCE, true),
                    ),
                    CodexItem.MarkDead(npcId, "Вожак"),
                )
            val saved = saver.save(combat, CombatOutcome.VICTORY, ticked, "Новая", now)

            assertEquals(
                1,
                observations
                    .observeAll()
                    .first()
                    .getValue(speciesId)
                    .size,
            )
            assertEquals(
                DamageState.RESISTANCE,
                codex.speciesRepository
                    .getById(speciesId)
                    ?.damage
                    ?.get(DamageType.FIRE),
            )
            assertEquals(NpcLifeState.DEAD, codex.npcRepository.getById(npcId)?.lifeState)

            saved.undo()

            assertTrue(observations.observeAll().first().isEmpty())
            assertNull(
                codex.speciesRepository
                    .getById(speciesId)
                    ?.damage
                    ?.get(DamageType.FIRE),
            )
            assertEquals(NpcLifeState.ALIVE, codex.npcRepository.getById(npcId)?.lifeState)
        }

    @Test
    fun `an ally kept as an npc is a friend and undo removes it`() =
        runTest {
            val (combatId, _) = fight(null)
            val combat = assertNotNull(combats.observe(combatId).first())
            val saved =
                saver.save(
                    combat,
                    CombatOutcome.VICTORY,
                    listOf(CodexItem.KeepAlly(1, "Стражник")),
                    "Новая",
                    now,
                )

            val npc =
                codex.npcRepository
                    .observeAll()
                    .first()
                    .single()
            assertEquals(NpcStatus.FRIEND, npc.status)
            saved.undo()
            assertTrue(
                codex.npcRepository
                    .observeAll()
                    .first()
                    .isEmpty(),
            )
        }

    @Test
    fun `deleting the fight takes its observations with it`() =
        runTest {
            val speciesId = codex.speciesRepository.upsert(Species(0, "Гоблин", CreatureType.HUMANOID))
            val (combatId, memberId) = fight(speciesId)
            val combat = assertNotNull(combats.observe(combatId).first())
            saver.save(
                combat,
                CombatOutcome.VICTORY,
                listOf(CodexItem.HitObserved(HitObservationDraft(memberId, "Гоблин", speciesId, 9, true))),
                "Новая",
                now,
            )
            combats.delete(combatId)
            assertTrue(observations.observeAll().first().isEmpty())
        }

    @Test
    fun `a fallen party member leaves the party and undo brings them back`() =
        runTest {
            val memberId =
                codex.partyRepository.upsert(
                    PartyMember(0, "Кира", "", "", null, "", false, PartyPresence.IN, null, null, ""),
                )
            val (combatId, _) = fight(null)
            val hero =
                Combatant(
                    0,
                    "Кира",
                    CombatSide.PARTY,
                    null,
                    null,
                    memberId,
                    true,
                    CombatState.DOWN,
                    1,
                    0,
                    3,
                    dead = true,
                )
            combats.addCombatant(combatId, hero)
            val combat = assertNotNull(combats.observe(combatId).first())
            val saved =
                saver.save(
                    combat,
                    CombatOutcome.RETREAT,
                    listOf(CodexItem.MarkPartyFallen(memberId, "Кира")),
                    "Новая",
                    now,
                )

            val fallen = assertNotNull(codex.partyRepository.getById(memberId))
            assertTrue(fallen.fallen)
            assertEquals(combatId, fallen.fellCombatId)

            saved.undo()

            assertEquals(false, codex.partyRepository.getById(memberId)?.fallen)
        }
}

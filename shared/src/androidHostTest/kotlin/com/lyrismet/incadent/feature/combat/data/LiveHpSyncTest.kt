package com.lyrismet.incadent.feature.combat.data

import com.lyrismet.incadent.feature.codex.data.CharacterSheetRepositoryImpl
import com.lyrismet.incadent.feature.codex.data.PartyRepositoryImpl
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.Combatant
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveHpSyncTest {
    private val db = TestDatabase()
    private val combats = CombatRepositoryImpl(db.database, db.current)
    private val party = PartyRepositoryImpl(db.database, db.current)
    private val sheets = CharacterSheetRepositoryImpl(db.database, db.current)
    private val startedAt = LocalDateTime.parse("2026-10-22T20:15")

    private suspend fun fight(): Triple<Long, Long, Long> {
        val member =
            party.upsert(
                PartyMember(
                    0,
                    "Ari",
                    "",
                    "",
                    3,
                    "",
                    true,
                    PartyPresence.IN,
                    hpMax = 30,
                    hpCurrent = 30,
                    description = "",
                ),
            )
        val combatId = combats.create(null, startedAt)
        val seat =
            combats.addCombatant(
                combatId,
                Combatant(
                    0,
                    "Ari",
                    CombatSide.PARTY,
                    null,
                    null,
                    member,
                    true,
                    CombatState.WHOLE,
                    1,
                    0,
                    0,
                    hpCurrent = 30,
                    hpMax = 30,
                ),
            )
        combats.start(combatId, startedAt)
        return Triple(member, combatId, seat)
    }

    private suspend fun seat(combatId: Long) = checkNotNull(combats.observe(combatId).first()).combatants.single()

    @Test
    fun `a hit taken on the sheet reaches the fight going on`() =
        runTest {
            val (member, combatId, _) = fight()
            sheets.updateVitals(member) { it.copy(hpCurrent = 12, tempHp = 4) }
            assertEquals(12, seat(combatId).hpCurrent)
            assertEquals(4, seat(combatId).tempHp)
        }

    @Test
    fun `a hit taken in the fight reaches the sheet`() =
        runTest {
            val (member, combatId, _) = fight()
            combats.updateCombatant(seat(combatId).copy(hpCurrent = 9, tempHp = 2))
            assertEquals(9, checkNotNull(party.getById(member)).hpCurrent)
            assertEquals(2, sheets.observeVitals(member).first()?.tempHp)
        }

    @Test
    fun `a new maximum on the sheet reaches the fight`() =
        runTest {
            val (member, combatId, _) = fight()
            party.upsert(checkNotNull(party.getById(member)).copy(hpMax = 40, hpCurrent = 30))
            assertEquals(40, seat(combatId).hpMax)
        }
}

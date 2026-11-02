package com.lyrismet.incadent.feature.campaign.data

import com.lyrismet.incadent.feature.campaign.data.file.FileRecord
import com.lyrismet.incadent.feature.campaign.data.file.forMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class FriendCopyTest {
    private val hero =
        FileRecord.Party(
            id = 1,
            name = "Lira",
            characterClass = "Rogue",
            race = "elf",
            level = 4,
            playerName = "Ana",
            isPlayerCharacter = true,
            hpMax = 20,
            hpCurrent = 12,
            description = "afraid of fire",
            fallen = true,
        )

    @Test
    fun `a friend copy keeps who the character is and drops the rest`() {
        val copy = hero.forMode(RecordSelection(mode = ExportMode.FRIEND))

        assertEquals("Lira", copy.name)
        assertEquals("Rogue", copy.characterClass)
        assertEquals("elf", copy.race)
        assertEquals(4, copy.level)
        assertEquals("Ana", copy.playerName)
        assertEquals(true, copy.fallen)
        assertNull(copy.hpMax)
        assertNull(copy.hpCurrent)
        assertEquals("", copy.description)
        assertFalse(copy.isPlayerCharacter)
    }

    @Test
    fun `the notes about the party go only when asked`() {
        val copy = hero.forMode(RecordSelection(mode = ExportMode.FRIEND, partyNotes = true))

        assertEquals("afraid of fire", copy.description)
        assertNull(copy.hpMax)
    }

    @Test
    fun `a personal copy is the full record`() {
        assertEquals(hero, hero.forMode(RecordSelection(mode = ExportMode.SELF)))
    }

    @Test
    fun `a friend copy takes the hit points off a party fighter only`() {
        val fighter =
            FileRecord.Fighter(
                combatId = 1,
                id = 1,
                name = "Lira",
                side = CombatSide.PARTY,
                partyId = 1,
                hpCurrent = 5,
                hpMax = 20,
            )
        val enemy = fighter.copy(partyId = null)
        val friend = RecordSelection(mode = ExportMode.FRIEND)

        assertNull(fighter.forMode(friend).hpMax)
        assertEquals(20, enemy.forMode(friend).hpMax)
    }
}

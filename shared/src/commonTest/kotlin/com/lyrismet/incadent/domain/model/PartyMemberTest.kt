package com.lyrismet.incadent.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PartyMemberTest {
    private val member =
        PartyMember(
            id = 1,
            name = "Торин",
            characterClass = "Паладин",
            race = "Человек",
            level = 5,
            playerName = "Максим",
            isPlayerCharacter = false,
            presence = PartyPresence.IN,
            hpMax = 52,
            hpCurrent = 43,
            armorClass = 18,
            initiativeBonus = 0,
            description = "",
            portraitUri = null,
        )

    @Test
    fun `values inside their ranges are left untouched`() {
        assertEquals(member, member.clampedToRanges())
    }

    @Test
    fun `out of range stats are pinned to their bounds`() {
        val clamped =
            member
                .copy(level = 25, hpMax = 1500, armorClass = 0, initiativeBonus = 40)
                .clampedToRanges()

        assertEquals(20, clamped.level)
        assertEquals(999, clamped.hpMax)
        assertEquals(1, clamped.armorClass)
        assertEquals(15, clamped.initiativeBonus)
    }

    @Test
    fun `current hp never exceeds the max after clamping`() {
        val clamped = member.copy(hpMax = 30, hpCurrent = 45).clampedToRanges()
        assertEquals(30, clamped.hpCurrent)
    }

    @Test
    fun `current hp cannot drop below zero`() {
        assertEquals(0, member.copy(hpCurrent = -3).clampedToRanges().hpCurrent)
    }
}

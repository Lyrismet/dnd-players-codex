package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartyLevelUpTest {
    @Test
    fun `everyone below the new level catches up and the others stay`() {
        val party = listOf(member(1, 6), member(2, 4), member(3, 6), member(4, 8), member(5, 5))

        assertEquals(listOf(2L, 5L), levelFollowers(party, ownerId = 1, newLevel = 6).map { it.id })
    }

    @Test
    fun `fallen and levelless ones stay`() {
        val party = listOf(member(1, 6), member(2, 4, fallen = true), member(3, null))

        assertTrue(levelFollowers(party, ownerId = 1, newLevel = 6).isEmpty())
    }

    private fun member(
        id: Long,
        level: Int?,
        fallen: Boolean = false,
    ) = PartyMember(
        id = id,
        name = "m$id",
        characterClass = "",
        race = "",
        level = level,
        playerName = "",
        isPlayerCharacter = id == 1L,
        presence = PartyPresence.IN,
        hpMax = null,
        hpCurrent = null,
        description = "",
        fallen = fallen,
    )
}

package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlayerCharacterPickTest {
    @Test
    fun `a party with nobody marked asks for a pick`() {
        assertTrue(needsPlayerCharacterPick(listOf(member(1), member(2))))
    }

    @Test
    fun `a marked character ends the ask`() {
        assertFalse(needsPlayerCharacterPick(listOf(member(1), member(2, isOwn = true))))
    }

    @Test
    fun `an empty party or one of the fallen has nobody to pick`() {
        assertFalse(needsPlayerCharacterPick(emptyList()))
        assertFalse(needsPlayerCharacterPick(listOf(member(1, fallen = true))))
    }

    private fun member(
        id: Long,
        isOwn: Boolean = false,
        fallen: Boolean = false,
    ) = PartyMember(
        id = id,
        name = "m$id",
        characterClass = "",
        race = "",
        level = null,
        playerName = "",
        isPlayerCharacter = isOwn,
        presence = PartyPresence.IN,
        hpMax = null,
        hpCurrent = null,
        description = "",
        fallen = fallen,
    )
}

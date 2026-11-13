package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CharacterSpellRepositoryTest {
    private val db = TestDatabase()
    private val party = PartyRepositoryImpl(db.database, db.current)
    private val spells = CharacterSpellRepositoryImpl(db.database, db.current)
    private val rest = CharacterRestRepositoryImpl(db.database)

    private suspend fun lira(): Long =
        party.upsert(
            PartyMember(
                id = 0,
                name = "Lira",
                characterClass = "Wizard",
                race = "",
                level = 5,
                playerName = "",
                isPlayerCharacter = true,
                presence = PartyPresence.IN,
                hpMax = 28,
                hpCurrent = 22,
                description = "",
            ),
        )

    @Test
    fun `a saved spell comes back with every field`() =
        runTest {
            val member = lira()
            spells.upsert(
                member,
                CharacterSpell(
                    id = 0,
                    name = "Hold Person",
                    level = 2,
                    school = "Enchantment",
                    verbal = true,
                    material = "iron",
                    concentration = true,
                    text = "text",
                ),
            )
            val spell = spells.observe(member).first().single()
            assertEquals("Hold Person", spell.name)
            assertEquals(2, spell.level)
            assertEquals(true, spell.verbal)
            assertEquals(false, spell.somatic)
            assertEquals(true, spell.concentration)
            assertEquals("iron", spell.material)
        }

    @Test
    fun `free casts count down to zero and no further`() =
        runTest {
            val member = lira()
            val id = spells.upsert(member, CharacterSpell(0, "Misty Step", 2, freeMax = 1, freeLeft = 1))
            spells.changeFree(id, -1)
            spells.changeFree(id, -1)
            assertEquals(
                0,
                spells
                    .observe(member)
                    .first()
                    .single()
                    .freeLeft,
            )
        }

    @Test
    fun `a long rest brings the free casts back`() =
        runTest {
            val member = lira()
            val id = spells.upsert(member, CharacterSpell(0, "Misty Step", 2, freeMax = 2, freeLeft = 2))
            spells.changeFree(id, -2)
            rest.longRest(member)
            assertEquals(
                2,
                spells
                    .observe(member)
                    .first()
                    .single()
                    .freeLeft,
            )
        }

    @Test
    fun `lowering the free casts cuts what is left`() =
        runTest {
            val member = lira()
            val id = spells.upsert(member, CharacterSpell(0, "Misty Step", 2, freeMax = 3, freeLeft = 3))
            spells.upsert(member, CharacterSpell(id, "Misty Step", 2, freeMax = 1, freeLeft = 3))
            assertEquals(
                1,
                spells
                    .observe(member)
                    .first()
                    .single()
                    .freeLeft,
            )
        }

    @Test
    fun `a slot spent in one place is spent in the other and never goes below zero`() =
        runTest {
            val member = lira()
            val resources = CharacterResourceRepositoryImpl(db.database, db.current)
            val id = resources.upsert(member, CharacterResource(0, "III", 2, 2, slotLevel = 3))
            resources.change(id, -1)
            assertEquals(
                1,
                resources
                    .observe(member)
                    .first()
                    .single()
                    .current,
            )
            resources.change(id, -5)
            assertEquals(
                0,
                resources
                    .observe(member)
                    .first()
                    .single()
                    .current,
            )
            resources.change(id, 1)
            assertEquals(
                1,
                resources
                    .observe(member)
                    .first()
                    .single()
                    .current,
            )
        }
}

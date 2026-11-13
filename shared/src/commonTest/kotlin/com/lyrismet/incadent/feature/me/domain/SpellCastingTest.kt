package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.CharacterResource
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSpellRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpellCastingTest {
    private val resources = FakeResources(mutableMapOf(10L to 2, 30L to 0))
    private val spells = FakeSpells(mutableMapOf(1L to 1))
    private val casting = SpellCasting(resources, spells)
    private val columns =
        listOf(
            SlotColumn(1, CharacterResource(10, "I", 3, 2, slotLevel = 1)),
            SlotColumn(3, CharacterResource(30, "III", 2, 0, slotLevel = 3)),
        )

    private fun spell(
        level: Int,
        freeLeft: Int = 0,
    ) = CharacterSpell(1, "Shield", level, freeMax = freeLeft, freeLeft = freeLeft)

    @Test
    fun `a cantrip costs nothing`() {
        assertEquals(listOf(SpellPay.None), payOptions(spell(0), columns))
    }

    @Test
    fun `a spell with free casts is paid only with them`() {
        assertEquals(listOf(SpellPay.Free), payOptions(spell(1, freeLeft = 1), columns))
    }

    @Test
    fun `a level with no slots left is not offered`() {
        assertEquals(listOf(SpellPay.Slot(1)), payOptions(spell(1), columns))
        assertNull(defaultPay(spell(2), columns))
    }

    @Test
    fun `a slot cast takes one cell and its undo gives it back`() =
        runTest {
            val cast = casting.cast(spell(1), SpellPay.Slot(1), columns)
            assertEquals(1, resources.counts[10L])
            cast?.undo()
            assertEquals(2, resources.counts[10L])
        }

    @Test
    fun `an empty level spends nothing`() =
        runTest {
            assertNull(casting.cast(spell(3), SpellPay.Slot(3), columns))
            assertEquals(0, resources.counts[30L])
        }

    @Test
    fun `a free cast counts down until the rest and undoes`() =
        runTest {
            val cast = casting.cast(spell(1, freeLeft = 1), SpellPay.Free, columns)
            assertEquals(0, spells.free[1L])
            cast?.undo()
            assertEquals(1, spells.free[1L])
        }

    @Test
    fun `a spent free cast cannot be cast again`() =
        runTest {
            assertNull(casting.cast(spell(1, freeLeft = 0), SpellPay.Free, columns))
        }

    @Test
    fun `levels with slots or spells get a tile in order`() {
        val list = listOf(spell(0), spell(5))
        assertEquals(listOf(0, 1, 3, 5), spellLevels(list, columns))
    }
}

private class FakeResources(
    val counts: MutableMap<Long, Int>,
) : CharacterResourceRepository {
    override fun observe(memberId: Long): Flow<List<CharacterResource>> = emptyFlow()

    override suspend fun upsert(
        memberId: Long,
        resource: CharacterResource,
    ): Long = resource.id

    override suspend fun setCurrent(
        id: Long,
        value: Int,
    ) = Unit

    override suspend fun pressCell(
        id: Long,
        index: Int,
    ) = Unit

    override suspend fun change(
        id: Long,
        delta: Int,
    ) {
        counts[id] = (counts[id] ?: 0) + delta
    }

    override suspend fun edit(
        id: Long,
        name: String,
        max: Int,
    ) = Unit

    override suspend fun delete(id: Long) = Unit
}

private class FakeSpells(
    val free: MutableMap<Long, Int>,
) : CharacterSpellRepository {
    override fun observe(memberId: Long): Flow<List<CharacterSpell>> = emptyFlow()

    override suspend fun upsert(
        memberId: Long,
        spell: CharacterSpell,
    ): Long = spell.id

    override suspend fun changeFree(
        id: Long,
        delta: Int,
    ) {
        free[id] = (free[id] ?: 0) + delta
    }

    override suspend fun delete(id: Long) = Unit
}

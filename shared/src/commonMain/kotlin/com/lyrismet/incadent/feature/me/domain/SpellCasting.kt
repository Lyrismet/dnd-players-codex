package com.lyrismet.incadent.feature.me.domain

import com.lyrismet.incadent.feature.codex.domain.model.CANTRIP_LEVEL
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSpell
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterResourceRepository
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterSpellRepository

/** what a cast spends: a slot of a level, one of the free casts, or nothing for a cantrip */
sealed interface SpellPay {
    data class Slot(
        val level: Int,
    ) : SpellPay

    data object Free : SpellPay

    data object None : SpellPay
}

/** the ways to pay for [spell] now: its free casts when it has them, else every level from its own up with a slot */
fun payOptions(
    spell: CharacterSpell,
    columns: List<SlotColumn>,
): List<SpellPay> =
    when {
        spell.isCantrip -> listOf(SpellPay.None)
        spell.hasFreeCasts -> if (spell.freeLeft > 0) listOf(SpellPay.Free) else emptyList()
        else -> columns.filter { it.level >= spell.level && it.resource.current > 0 }.map { SpellPay.Slot(it.level) }
    }

/** the one press of the row button pays with: a free cast, else the lowest level from the spell's own with a slot */
fun defaultPay(
    spell: CharacterSpell,
    columns: List<SlotColumn>,
): SpellPay? = payOptions(spell, columns).firstOrNull()

/** the levels that get a tile: cantrips always first, then every level with slots or spells */
fun spellLevels(
    spells: List<CharacterSpell>,
    columns: List<SlotColumn>,
): List<Int> = (listOf(CANTRIP_LEVEL) + columns.map { it.level } + spells.map { it.level }).distinct().sorted()

fun spellsOfLevel(
    spells: List<CharacterSpell>,
    level: Int,
): List<CharacterSpell> = spells.filter { it.level == level }.sortedWith(compareBy({ it.position }, { it.id }))

/** a cast already paid, [undo] puts back exactly what it took */
class SpellCast(
    val pay: SpellPay,
    private val undoAction: suspend () -> Unit,
) {
    suspend fun undo() = undoAction()
}

/** spends the slot or the free cast of a spell, the one place both the sheet and the fight cast through */
class SpellCasting(
    private val resources: CharacterResourceRepository,
    private val spells: CharacterSpellRepository,
) {
    /** pays [pay] for [spell], the slot is found among [columns], nothing is spent when it is not there */
    suspend fun cast(
        spell: CharacterSpell,
        pay: SpellPay,
        columns: List<SlotColumn>,
    ): SpellCast? =
        when (pay) {
            SpellPay.None -> SpellCast(pay) {}
            SpellPay.Free ->
                if (spell.freeLeft <= 0) {
                    null
                } else {
                    spells.changeFree(spell.id, -1)
                    SpellCast(pay) { spells.changeFree(spell.id, 1) }
                }
            is SpellPay.Slot ->
                columns.firstOrNull { it.level == pay.level && it.resource.current > 0 }?.let { column ->
                    if (resources.spend(
                            column.resource.id,
                        )
                    ) {
                        SpellCast(pay) { resources.change(column.resource.id, 1) }
                    } else {
                        null
                    }
                }
        }
}

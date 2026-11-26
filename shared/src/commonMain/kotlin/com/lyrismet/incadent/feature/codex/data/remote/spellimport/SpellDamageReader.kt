package com.lyrismet.incadent.feature.codex.data.remote.spellimport

import com.lyrismet.incadent.feature.codex.domain.model.CANTRIP_TIERS
import com.lyrismet.incadent.feature.codex.domain.model.CantripGrowth
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.GrowthMode
import com.lyrismet.incadent.feature.codex.domain.model.GrowthRule
import com.lyrismet.incadent.feature.codex.domain.model.GrowthStep
import com.lyrismet.incadent.feature.codex.domain.model.GrowthTarget
import com.lyrismet.incadent.feature.codex.domain.model.MAX_DAMAGE_ROWS
import com.lyrismet.incadent.feature.codex.domain.model.SPELL_LEVELS
import com.lyrismet.incadent.feature.codex.domain.model.SpellDamage
import com.lyrismet.incadent.feature.codex.domain.model.SpellEffect
import com.lyrismet.incadent.feature.codex.domain.model.SpellGrowth
import com.lyrismet.incadent.feature.codex.domain.model.addDice
import com.lyrismet.incadent.feature.codex.domain.model.isValidDice
import com.lyrismet.incadent.feature.codex.domain.model.multiplyDice
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private val CountOfDice = Regex("^(\\d{1,3})d(\\d{1,3})$", RegexOption.IGNORE_CASE)

/** one damage or healing row of a source, the dice by slot circle and by character level */
internal class DamageTable(
    val effect: SpellEffect?,
    val bySlot: Map<Int, String>,
    val byCharacter: Map<Int, String>,
)

/** the damage rows and the growth of a spell, empty when the source has no tables or its dice do not fit the form */
internal class DamageRead(
    val damage: List<SpellDamage>,
    val growth: SpellGrowth,
)

internal val NoDamage = DamageRead(emptyList(), SpellGrowth())

internal fun damageTypeOf(name: String): DamageType? =
    DamageType.entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

private fun diceOf(text: String?): String? =
    text?.filterNot { it.isWhitespace() }?.takeIf {
        it.isNotEmpty() &&
            isValidDice(it)
    }

internal fun readDamage(
    level: Int,
    tables: List<DamageTable>,
): DamageRead {
    val rows = tables.take(MAX_DAMAGE_ROWS).mapNotNull { rowOf(level, it) }
    val damage = rows.map { SpellDamage(it.dice, it.effect) }
    val rules = rows.mapIndexedNotNull { index, row -> row.rule?.copy(damageIndex = index) }
    val scales = rows.any { it.cantripScales }
    return when {
        rows.isEmpty() -> NoDamage
        level == 0 -> DamageRead(damage, SpellGrowth(cantrip = if (scales) CantripGrowth.DICE else CantripGrowth.NONE))
        else -> DamageRead(damage, SpellGrowth(enabled = rules.isNotEmpty(), rules = rules))
    }
}

private class Row(
    val dice: String,
    val effect: SpellEffect?,
    val cantripScales: Boolean,
    val rule: GrowthRule?,
)

private fun rowOf(
    level: Int,
    table: DamageTable,
): Row? {
    val slots = table.bySlot.mapNotNull { (circle, text) -> diceOf(text)?.let { circle to it } }.toMap()
    val levels = table.byCharacter.mapNotNull { (at, text) -> diceOf(text)?.let { at to it } }.toMap()
    val base =
        if (level ==
            0
        ) {
            levels.minByOrNull { it.key }?.value ?: slots.minByOrNull { it.key }?.value
        } else {
            slots[level]
        }
    return base?.let { Row(it, table.effect, level == 0 && scalesByTier(base, levels), growthRule(level, base, slots)) }
}

// a cantrip grows by dice when the first tier is the base and each next one has one more of the same dice
private fun scalesByTier(
    base: String,
    byCharacter: Map<Int, String>,
): Boolean =
    CountOfDice.matches(base) &&
        CANTRIP_TIERS.withIndex().all { (tier, from) -> byCharacter[from] == multiplyDice(base, tier + 2) }

private fun growthRule(
    level: Int,
    base: String,
    bySlot: Map<Int, String>,
): GrowthRule? {
    val higher = bySlot.filterKeys { it > level }.entries.sortedBy { it.key }
    val add = if (level == 0) null else evenStep(base, higher.firstOrNull()?.value)
    val even =
        add != null &&
            higher.size == SPELL_LEVELS.last - level &&
            higher.all { addDice(base, add, it.key - level) == it.value }
    return when {
        level == 0 || higher.isEmpty() -> null
        even -> GrowthRule(GrowthTarget.DICE, GrowthMode.EVERY, add = add)
        else -> GrowthRule(GrowthTarget.DICE, GrowthMode.STEPS, steps = higher.map { GrowthStep(it.key, it.value) })
    }
}

// the dice one circle adds, "1d6" when 8d6 becomes 9d6, null when the dice are not of the same kind
private fun evenStep(
    base: String,
    next: String?,
): String? {
    val from = CountOfDice.find(base)?.groupValues
    val to = next?.let { CountOfDice.find(it)?.groupValues }
    val more = if (from != null && to != null && from[2] == to[2]) to[1].toInt() - from[1].toInt() else 0
    return if (more > 0) "${more}d${to?.get(2)}" else null
}

/** the tables of the damage and the healing of dnd5eapi.co, the circle or the character level is the key of a table */
internal fun JsonObject.apiDamageTables(): List<DamageTable> {
    val damage =
        (this["damage"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty().map {
            DamageTable(
                effect =
                    it.obj("damage_type")?.text("index")?.let(::damageTypeOf)?.let { type ->
                        SpellEffect.Damage(type)
                    },
                bySlot = it.obj("damage_at_slot_level").levels(),
                byCharacter = it.obj("damage_at_character_level").levels(),
            )
        }
    val healing =
        obj("heal_at_slot_level")
            ?.let {
                listOf(DamageTable(SpellEffect.Healing, it.levels(), emptyMap()))
            }.orEmpty()
    return damage + healing
}

private fun JsonObject?.levels(): Map<Int, String> =
    this
        ?.entries
        ?.mapNotNull { (key, value) ->
            key.toIntOrNull()?.let { at ->
                (value as? JsonPrimitive)?.content?.let {
                    at to
                        it
                }
            }
        }.orEmpty()
        .toMap()

/** the table of Open5e v2: the dice of the spell, then the ones of the higher slots and of the character levels */
internal fun JsonObject.open5eDamageTables(): List<DamageTable> {
    val base = text("damage_roll").orEmpty()
    val level = number("level")
    val options = (this["casting_options"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    val type = (this["damage_types"] as? JsonArray)?.firstOrNull()?.let { (it as? JsonPrimitive)?.content }
    val table =
        DamageTable(
            effect = type?.let(::damageTypeOf)?.let { SpellEffect.Damage(it) },
            bySlot = options.rollsBy("slot_level_") + listOfNotNull(level?.let { it to base }),
            byCharacter = options.rollsBy("player_level_") + (1 to base),
        )
    return if (base.isBlank() || level == null) emptyList() else listOf(table)
}

private fun List<JsonObject>.rollsBy(prefix: String): Map<Int, String> =
    mapNotNull { option ->
        val type = option.text("type").orEmpty()
        val roll = option.text("damage_roll")
        val at = type.takeIf { it.startsWith(prefix) }?.removePrefix(prefix)?.toIntOrNull()
        if (at != null && roll != null) at to roll else null
    }.toMap()

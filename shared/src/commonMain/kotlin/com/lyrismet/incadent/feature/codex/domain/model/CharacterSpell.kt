package com.lyrismet.incadent.feature.codex.domain.model

/** a spell of a character, level 0 is a cantrip, [freeMax] casts a rest can bring back are paid without a slot */
data class CharacterSpell(
    val id: Long,
    val name: String,
    val level: Int,
    val school: String = "",
    val castKind: CastKind = CastKind.ACTION,
    /** the time of a [CastKind.LONG] spell, "1 мин" or the user's own words, empty for the other kinds */
    val castingTime: String = "",
    val range: String = "",
    val verbal: Boolean = false,
    val somatic: Boolean = false,
    val hasMaterial: Boolean = false,
    val material: String = "",
    /** the words of a duration written before the picker, shown while [durationUnit] is [DurationUnit.NONE] */
    val duration: String = "",
    val durationUnit: DurationUnit = DurationUnit.NONE,
    val durationValue: Int = 0,
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val text: String = "",
    val damage: List<SpellDamage> = emptyList(),
    val projectiles: Int = 1,
    val growth: SpellGrowth = SpellGrowth(),
    val freeMax: Int = 0,
    val freeLeft: Int = 0,
    val position: Int = 0,
    val originId: String = "",
) {
    val isCantrip: Boolean get() = level == CANTRIP_LEVEL
    val hasFreeCasts: Boolean get() = freeMax > 0

    /** the components as the card writes them: "В, С, М (a pinch of sand)", empty when none is set */
    val hasComponents: Boolean get() = verbal || somatic || hasMaterial

    /** how many rounds a fight counts the spell for, null when it is instant, unset or lasts over ten minutes */
    val combatRounds: Int? get() = combatRoundsOf(durationUnit, durationValue)

    fun clamped(): CharacterSpell {
        val cap = freeMax.coerceIn(FREE_CASTS_RANGE)
        return copy(
            name = name.trim(),
            level = level.coerceIn(CANTRIP_LEVEL, SPELL_LEVELS.last),
            damage = damage.take(MAX_DAMAGE_ROWS),
            projectiles = projectiles.coerceIn(PROJECTILES_RANGE),
            growth = growth.clamped(),
            durationValue = durationValue.coerceIn(0, MAX_DURATION_VALUE),
            freeMax = cap,
            freeLeft = freeLeft.coerceIn(0, cap),
        )
    }
}

const val CANTRIP_LEVEL = 0
val FREE_CASTS_RANGE = 0..9
val SPELL_POSITIONS = 0..9_999
const val ROUNDS_PER_MINUTE = 10
const val MAX_COMBAT_ROUNDS = 100
const val MAX_DAMAGE_ROWS = 6
const val MAX_DURATION_VALUE = 9_999
val PROJECTILES_RANGE = 1..20

/** how long a spell takes to cast, [LONG] keeps its own time in [CharacterSpell.castingTime] */
enum class CastKind { ACTION, BONUS, REACTION, LONG }

/** the unit of a picked duration, [NONE] is a spell whose duration is only the old free text or nothing */
enum class DurationUnit { NONE, INSTANT, ROUND, MINUTE, HOUR, DAY }

/** what a damage row of a spell does: a damage of a type, healing, or hit points that are temporary */
sealed interface SpellEffect {
    data class Damage(
        val type: DamageType,
    ) : SpellEffect

    data object Healing : SpellEffect

    data object TempHp : SpellEffect
}

/** one row of a spell's damage, the type may be left out */
data class SpellDamage(
    val dice: String,
    val effect: SpellEffect? = null,
)

fun combatRoundsOf(
    unit: DurationUnit,
    value: Int,
): Int? =
    when (unit) {
        DurationUnit.ROUND -> value
        DurationUnit.MINUTE -> value * ROUNDS_PER_MINUTE
        else -> null
    }?.takeIf { it in 1..MAX_COMBAT_ROUNDS }

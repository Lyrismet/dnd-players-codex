package com.lyrismet.incadent.feature.codex.domain.model

enum class Ability { STRENGTH, DEXTERITY, CONSTITUTION, INTELLIGENCE, WISDOM, CHARISMA }

/** the 18 skills of the SRD, each one tied to a single ability */
enum class Skill(
    val ability: Ability,
) {
    ACROBATICS(Ability.DEXTERITY),
    ANIMAL_HANDLING(Ability.WISDOM),
    ARCANA(Ability.INTELLIGENCE),
    ATHLETICS(Ability.STRENGTH),
    DECEPTION(Ability.CHARISMA),
    HISTORY(Ability.INTELLIGENCE),
    INSIGHT(Ability.WISDOM),
    INTIMIDATION(Ability.CHARISMA),
    INVESTIGATION(Ability.INTELLIGENCE),
    MEDICINE(Ability.WISDOM),
    NATURE(Ability.INTELLIGENCE),
    PERCEPTION(Ability.WISDOM),
    PERFORMANCE(Ability.CHARISMA),
    PERSUASION(Ability.CHARISMA),
    RELIGION(Ability.INTELLIGENCE),
    SLEIGHT_OF_HAND(Ability.DEXTERITY),
    STEALTH(Ability.DEXTERITY),
    SURVIVAL(Ability.WISDOM),
}

/** a sheet number that takes a typed adjustment on top of what the rules calculate */
enum class SheetAdjustment { INITIATIVE, PASSIVE_PERCEPTION, SPELL_DC, SPELL_ATTACK }

/** how much of the proficiency bonus a skill gets, the multiplier is what the bonus is multiplied by */
enum class Proficiency(
    val multiplier: Int,
) {
    NONE(0),
    PROFICIENT(1),
    EXPERTISE(2),
}

/** the numbers and texts of a character sheet, the party record keeps the level and the hit points */
data class CharacterSheet(
    // an ability that was never entered is missing, so its bonus is a dash instead of a made up zero
    val abilities: Map<Ability, Int> = emptyMap(),
    val saves: Set<Ability> = emptySet(),
    val skillMarks: Map<Skill, Proficiency> = emptyMap(),
    val skillAdjustments: Map<Skill, Int> = emptyMap(),
    val initiativeAdjustment: Int = 0,
    val passivePerceptionAdjustment: Int = 0,
    val spellDcAdjustment: Int = 0,
    val spellAttackAdjustment: Int = 0,
    // notes for the player only about where an adjustment comes from, nothing is calculated from them
    val skillNotes: Map<Skill, String> = emptyMap(),
    val adjustmentNotes: Map<SheetAdjustment, String> = emptyMap(),
    val spellAbility: Ability? = null,
    val speed: Int? = null,
    val inspiration: Boolean = false,
    // the player has no spells and hid the tab, it is shown to everyone else
    val spellsHidden: Boolean = false,
    val background: String = "",
    val alignment: String = "",
    val personality: String = "",
    val languages: String = "",
    val features: String = "",
    val notes: String = "",
    val purse: CoinPurse = CoinPurse(),
    val originId: String = "",
) {
    fun mark(skill: Skill): Proficiency = skillMarks[skill] ?: Proficiency.NONE

    fun adjustment(skill: Skill): Int = skillAdjustments[skill] ?: 0

    fun adjustment(target: SheetAdjustment): Int =
        when (target) {
            SheetAdjustment.INITIATIVE -> initiativeAdjustment
            SheetAdjustment.PASSIVE_PERCEPTION -> passivePerceptionAdjustment
            SheetAdjustment.SPELL_DC -> spellDcAdjustment
            SheetAdjustment.SPELL_ATTACK -> spellAttackAdjustment
        }

    fun note(skill: Skill): String = skillNotes[skill].orEmpty()

    fun note(target: SheetAdjustment): String = adjustmentNotes[target].orEmpty()

    /** the sheet with the adjustment of [target] set and its note kept when it is not blank */
    fun withAdjustment(
        target: SheetAdjustment,
        value: Int,
        note: String,
    ): CharacterSheet =
        when (target) {
            SheetAdjustment.INITIATIVE -> copy(initiativeAdjustment = value)
            SheetAdjustment.PASSIVE_PERCEPTION -> copy(passivePerceptionAdjustment = value)
            SheetAdjustment.SPELL_DC -> copy(spellDcAdjustment = value)
            SheetAdjustment.SPELL_ATTACK -> copy(spellAttackAdjustment = value)
        }.copy(adjustmentNotes = adjustmentNotes.withNote(target, note))
}

/** the map with the note of [key] replaced, dropped when it is blank */
fun <K> Map<K, String>.withNote(
    key: K,
    note: String,
): Map<K, String> = this - key + listOfNotNull(note.trim().takeIf { it.isNotEmpty() }?.let { key to it })

object SheetRanges {
    val ability = 1..30
    val speed = 0..999
    val armorClass = 0..99
    val adjustment = -20..20
    val tempHp = 0..999
    val deathSaves = 0..3
}

/** pins every set number to its range, a missing one stays missing and a zero adjustment is not kept */
fun CharacterSheet.clampedToRanges(): CharacterSheet =
    copy(
        abilities = abilities.mapValues { it.value.coerceIn(SheetRanges.ability) },
        skillAdjustments =
            skillAdjustments
                .mapValues { it.value.coerceIn(SheetRanges.adjustment) }
                .filterValues { it != 0 },
        skillMarks = skillMarks.filterValues { it != Proficiency.NONE },
        initiativeAdjustment = initiativeAdjustment.coerceIn(SheetRanges.adjustment),
        passivePerceptionAdjustment = passivePerceptionAdjustment.coerceIn(SheetRanges.adjustment),
        spellDcAdjustment = spellDcAdjustment.coerceIn(SheetRanges.adjustment),
        spellAttackAdjustment = spellAttackAdjustment.coerceIn(SheetRanges.adjustment),
        speed = speed?.coerceIn(SheetRanges.speed),
        purse = purse.clamped(),
    )

/** what the party record keeps for a sheet - the hit points beside the max, armor class and the death saves */
data class CharacterVitals(
    val hpMax: Int?,
    val hpCurrent: Int?,
    val tempHp: Int = 0,
    val armorClass: Int? = null,
    // a note for the player only, nothing is calculated from it
    val armorClassFrom: String = "",
    val deathSavesSuccess: Int = 0,
    val deathSavesFailure: Int = 0,
)

fun CharacterVitals.clampedToRanges(): CharacterVitals {
    val max = hpMax?.coerceIn(PartyStatRanges.hpMax)
    return copy(
        hpMax = max,
        hpCurrent = if (max != null) hpCurrent?.coerceIn(0, max) else hpCurrent?.coerceAtLeast(0),
        tempHp = tempHp.coerceIn(SheetRanges.tempHp),
        armorClass = armorClass?.coerceIn(SheetRanges.armorClass),
        deathSavesSuccess = deathSavesSuccess.coerceIn(SheetRanges.deathSaves),
        deathSavesFailure = deathSavesFailure.coerceIn(SheetRanges.deathSaves),
    )
}

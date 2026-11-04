package com.lyrismet.incadent.feature.codex.domain.model

/** a counter of a character - uses of a feature or the spell slots of one level, [slotLevel] is null for a feature */
data class CharacterResource(
    val id: Long,
    val name: String,
    val max: Int,
    val current: Int,
    val slotLevel: Int? = null,
    val position: Int = 0,
    val originId: String = "",
) {
    val isSpellSlot: Boolean get() = slotLevel != null

    fun clamped(): CharacterResource {
        val limit = max.coerceIn(RESOURCE_MAX_RANGE)
        return copy(
            max = limit,
            current = current.coerceIn(0, limit),
            slotLevel = slotLevel?.coerceIn(SPELL_LEVELS),
        )
    }
}

val RESOURCE_MAX_RANGE = 0..99
val SPELL_LEVELS = 1..9

/** the spell tab shows only for a caster, one that chose a spell ability or has slots */
fun isSpellTabVisible(
    sheet: CharacterSheet,
    resources: List<CharacterResource>,
): Boolean = sheet.spellAbility != null || resources.any { it.isSpellSlot }

/** a ready made name from the SRD 5.1, the maximum is always typed by the player */
enum class ResourceTemplate {
    SPELL_SLOTS,
    RAGE,
    BARDIC_INSPIRATION,
    CHANNEL_DIVINITY,
    WILD_SHAPE,
    SECOND_WIND,
    ACTION_SURGE,
    INDOMITABLE,
    KI_POINTS,
    SORCERY_POINTS,
    LAY_ON_HANDS,
    DIVINE_SENSE,
    ARCANE_RECOVERY,
}

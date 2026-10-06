package com.lyrismet.incadent.core.quickedit

import com.lyrismet.incadent.domain.model.PartyStatRanges

/** how a field takes its value - text and numbers are typed in, links and choices are tapped */
sealed interface QuickEditKind {
    data object Text : QuickEditKind

    data class Number(
        val range: IntRange,
    ) : QuickEditKind

    data object Link : QuickEditKind

    data object Choice : QuickEditKind
}

/** every card field a quick edit can change - each entity type only uses its own subset */
enum class QuickEditField(
    val kind: QuickEditKind,
    val isRequired: Boolean = false,
) {
    NAME(QuickEditKind.Text, isRequired = true),
    DESCRIPTION(QuickEditKind.Text),
    RACE(QuickEditKind.Text),
    FACTION(QuickEditKind.Text),
    CHARACTER_CLASS(QuickEditKind.Text),
    PLAYER_NAME(QuickEditKind.Text),
    LOCATION_TYPE(QuickEditKind.Text),
    LOCATION_REGION(QuickEditKind.Text),
    REWARD(QuickEditKind.Text),
    LEVEL(QuickEditKind.Number(PartyStatRanges.level)),
    ARMOR_CLASS(QuickEditKind.Number(PartyStatRanges.armorClass)),
    HP_MAX(QuickEditKind.Number(PartyStatRanges.hpMax)),
    INITIATIVE_BONUS(QuickEditKind.Number(PartyStatRanges.initiativeBonus)),
    GIVER(QuickEditKind.Link),
    PLACE(QuickEditKind.Link),
    RELATION(QuickEditKind.Choice),
    LIFE(QuickEditKind.Choice),
    QUEST_STATUS(QuickEditKind.Choice),
    PRESENCE(QuickEditKind.Choice),
}

/** the typed-in or tapped value of one [QuickEditField], always matching that field's [QuickEditKind] */
sealed interface QuickEditValue {
    data class Text(
        val text: String,
    ) : QuickEditValue

    data class Number(
        val number: Int,
    ) : QuickEditValue

    /** null unlinks the field - "Не указано" */
    data class Link(
        val id: Long?,
    ) : QuickEditValue
}

/** the value a typed draft would commit, or null when it must not be saved - a number is clamped into range */
fun quickEditValueFromDraft(
    field: QuickEditField,
    draft: String,
): QuickEditValue? {
    val trimmed = draft.trim()
    return when (val kind = field.kind) {
        QuickEditKind.Text -> if (trimmed.isEmpty() && field.isRequired) null else QuickEditValue.Text(trimmed)
        is QuickEditKind.Number -> trimmed.toIntOrNull()?.let { QuickEditValue.Number(it.coerceIn(kind.range)) }
        QuickEditKind.Link, QuickEditKind.Choice -> null
    }
}

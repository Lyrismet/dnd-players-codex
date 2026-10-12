package com.lyrismet.incadent.core.quickedit

import com.lyrismet.incadent.domain.model.PartyStatRanges

/** how a field takes its value - text and numbers are typed in, links and choices are tapped */
sealed interface QuickEditKind {
    data object Text : QuickEditKind

    data class Number(
        val range: IntRange,
    ) : QuickEditKind

    data object Link : QuickEditKind

    /** one or several places picked in the select sheet - whether it is several depends on the entity */
    data object Places : QuickEditKind

    data object Choice : QuickEditKind

    /** a picked photo - it has no typed draft, the picker hands over the finished value */
    data object Portrait : QuickEditKind
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
    REWARD(QuickEditKind.Text),
    LEVEL(QuickEditKind.Number(PartyStatRanges.level)),
    HP_MAX(QuickEditKind.Number(PartyStatRanges.hpMax)),
    GIVER(QuickEditKind.Link),
    PLACE(QuickEditKind.Places),
    PARENT(QuickEditKind.Places),
    RELATION(QuickEditKind.Choice),
    LIFE(QuickEditKind.Choice),
    QUEST_STATUS(QuickEditKind.Choice),
    PRESENCE(QuickEditKind.Choice),
    PORTRAIT(QuickEditKind.Portrait),
}

/** the typed-in or tapped value of one [QuickEditField], always matching that field's [QuickEditKind] */
sealed interface QuickEditValue {
    data class Text(
        val text: String,
    ) : QuickEditValue

    /** null clears the number, the card shows a dash again */
    data class Number(
        val number: Int?,
    ) : QuickEditValue

    /** the chosen places, empty clears the field */
    data class Places(
        val ids: List<Long>,
    ) : QuickEditValue

    /** null unlinks the field - "Не указано" */
    data class Link(
        val id: Long?,
    ) : QuickEditValue

    data class Portrait(
        val base64: String,
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
        is QuickEditKind.Number ->
            if (trimmed.isEmpty()) {
                QuickEditValue.Number(null)
            } else {
                trimmed.toIntOrNull()?.let { QuickEditValue.Number(it.coerceIn(kind.range)) }
            }
        QuickEditKind.Link, QuickEditKind.Places, QuickEditKind.Choice, QuickEditKind.Portrait -> null
    }
}

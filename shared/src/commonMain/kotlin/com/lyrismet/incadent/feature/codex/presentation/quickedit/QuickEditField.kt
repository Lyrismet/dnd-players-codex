package com.lyrismet.incadent.feature.codex.presentation.quickedit

import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges
import com.lyrismet.incadent.feature.codex.domain.model.SPECIES_HIT_POINTS_RANGE

/** how a field takes its value - text and numbers are typed in, links and choices are tapped */
sealed interface QuickEditKind {
    data object Text : QuickEditKind

    data class Number(
        val range: IntRange,
    ) : QuickEditKind

    data object Link : QuickEditKind

    /** one species picked in the select sheet over the card, the answer is a link to it */
    data object Species : QuickEditKind

    /** one or several places picked in the select sheet - whether it is several depends on the entity */
    data object Places : QuickEditKind

    data object Choice : QuickEditKind

    /** a group of linked entities on the card - every tap on a candidate toggles its link at once */
    data object Relations : QuickEditKind

    /** a picked and cropped photo - it has no typed draft, the crop window hands over the finished value */
    data object Photo : QuickEditKind
}

/** every card field a quick edit can change - each entity type only uses its own subset */
enum class QuickEditField(
    val kind: QuickEditKind,
    val isRequired: Boolean = false,
) {
    NAME(QuickEditKind.Text, isRequired = true),
    DESCRIPTION(QuickEditKind.Text),
    NICKNAMES(QuickEditKind.Text),
    RACE(QuickEditKind.Text),
    SPECIES(QuickEditKind.Species),
    CHARACTER_CLASS(QuickEditKind.Text),
    PLAYER_NAME(QuickEditKind.Text),
    REWARD(QuickEditKind.Text),
    LEVEL(QuickEditKind.Number(PartyStatRanges.level)),
    HP_MAX(QuickEditKind.Number(PartyStatRanges.hpMax)),
    HIT_POINTS(QuickEditKind.Number(SPECIES_HIT_POINTS_RANGE)),
    GIVER(QuickEditKind.Link),
    PLACE(QuickEditKind.Places),
    PARENT(QuickEditKind.Places),

    /** the places inside a place - picking writes the parent of those places, not of the card's own place */
    INSIDE(QuickEditKind.Places),

    /** "Переместить в…" on a place card - the same parent as [PARENT], with its own sheet title and undo bar */
    MOVE(QuickEditKind.Places),
    RELATION(QuickEditKind.Choice),
    LIFE(QuickEditKind.Choice),
    VULNERABILITIES(QuickEditKind.Choice),
    QUEST_STATUS(QuickEditKind.Choice),
    PRESENCE(QuickEditKind.Choice),
    PORTRAIT(QuickEditKind.Photo),

    /** the 5e creature type of a species, picked from the list of types */
    CREATURE_TYPE(QuickEditKind.Choice),

    /** the wide picture at the top of a place's card */
    COVER(QuickEditKind.Photo),

    /** the quests an npc gave - each one is a quest whose giver is this npc */
    GIVEN_QUESTS(QuickEditKind.Relations),

    /** the npcs met at a place - each one is an npc that lists this place */
    RESIDENTS(QuickEditKind.Relations),

    /** the quests set at a place - each one is a quest whose place is this one */
    PLACE_QUESTS(QuickEditKind.Relations),
}

/** true for a field that is edited in a select sheet over the card, the line itself stays as it is meanwhile */
val QuickEditField.editsInSheet: Boolean get() = kind is QuickEditKind.Places || kind == QuickEditKind.Species

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

    data class Creature(
        val type: CreatureType,
    ) : QuickEditValue

    /** null removes the photo */
    data class Picture(
        val photo: Photo?,
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
        QuickEditKind.Link,
        QuickEditKind.Species,
        QuickEditKind.Places,
        QuickEditKind.Choice,
        QuickEditKind.Photo,
        QuickEditKind.Relations,
        -> null
    }
}

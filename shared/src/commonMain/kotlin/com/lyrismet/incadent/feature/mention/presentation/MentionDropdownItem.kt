package com.lyrismet.incadent.feature.mention.presentation

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.domain.model.EntityKind

data class MentionSuggestion(
    val token: String,
    val glyph: MentionGlyph,
    val tint: Color,
    val name: String,
    val typeLabel: String,
    // the grey second line - race and place, who gave the quest, the path of a place
    val detail: String = "",
    // the nickname the query hit, shown ahead of the detail
    val nickname: String = "",
)

/** one line of the "@" dropdown */
sealed interface MentionDropdownItem {
    data class Header(
        val title: String,
    ) : MentionDropdownItem

    data class Entry(
        val suggestion: MentionSuggestion,
    ) : MentionDropdownItem

    /** "+ Создать «X»…" - [name] is what was typed after the "@" */
    data class Create(
        val name: String,
    ) : MentionDropdownItem

    /** the row that is always last - [withHint] adds the line telling a new player that a record can be made here */
    data class CreateRecord(
        val withHint: Boolean,
    ) : MentionDropdownItem

    /** shown instead of the list while the codex has no records at all */
    data object EmptyCodex : MentionDropdownItem
}

/** the sheet that makes a record out of [name] - the record kind is picked from [kinds] */
data class MentionCreateSheetState(
    val name: String,
    val type: EntityKind,
    val kinds: List<EntityKind>,
    // how many notes carry the "?" tag this record is made for, null when the name comes from the draft
    val linkedNotes: Int?,
) {
    val canCreate: Boolean get() = name.isNotBlank()
}

/** what the create sheet reports - the host decides what a name change or a confirm does */
internal class MentionCreateActions(
    val onNameChanged: (String) -> Unit,
    val onTypeChanged: (EntityKind) -> Unit,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

/** the rows to scroll and the create row that stays in view under them, null when the list ends with none */
fun List<MentionDropdownItem>.withPinnedCreate(): Pair<List<MentionDropdownItem>, MentionDropdownItem?> {
    val last = lastOrNull()
    val pinned = last?.takeIf { it is MentionDropdownItem.Create || it is MentionDropdownItem.CreateRecord }
    return (if (pinned == null) this else dropLast(1)) to pinned
}

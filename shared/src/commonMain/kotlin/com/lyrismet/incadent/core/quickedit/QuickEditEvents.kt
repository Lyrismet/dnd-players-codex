package com.lyrismet.incadent.core.quickedit

/** the field whose editor is open and what it currently holds - at most one editor is open at a time */
data class InlineEdit(
    val field: QuickEditField,
    val draft: String,
)

/** what the card's in-place editors send to the presenter */
sealed interface QuickEditUiEvent {
    data class FieldHeld(
        val field: QuickEditField,
        val currentText: String,
    ) : QuickEditUiEvent

    data class DraftChanged(
        val draft: String,
    ) : QuickEditUiEvent

    data object EditSaved : QuickEditUiEvent

    data object EditCancelled : QuickEditUiEvent

    /** null picks "Не указано" */
    data class LinkChosen(
        val id: Long?,
    ) : QuickEditUiEvent

    data object HoldHintDismissed : QuickEditUiEvent

    /** the raw picked image - the presenter compresses it before it is stored */
    data class PortraitPicked(
        val bytes: ByteArray,
    ) : QuickEditUiEvent
}

/** a committed change and the way back - the codex turns it into an undo toast */
data class EntityChange(
    val field: QuickEditField,
    val entityName: String,
    val restore: suspend () -> Unit,
)

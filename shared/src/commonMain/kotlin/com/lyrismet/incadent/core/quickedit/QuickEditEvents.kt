package com.lyrismet.incadent.core.quickedit

import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.PhotoSlot

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

    /** the select sheet's answer - an empty list clears the field */
    data class PlacesChosen(
        val ids: List<Long>,
    ) : QuickEditUiEvent

    /** a candidate of the open relation editor was tapped - the editor stays open for the next one */
    data class RelationToggled(
        val id: Long,
    ) : QuickEditUiEvent

    data object HoldHintDismissed : QuickEditUiEvent

    /** the raw picked image - the presenter shrinks it and opens the crop window on it */
    class PhotoPicked(
        val slot: PhotoSlot,
        val bytes: ByteArray,
    ) : QuickEditPhotoEvent

    /** "Кадрировать" on the stored photo - the crop window opens on it, narrowing only */
    class PhotoRecropRequested(
        val slot: PhotoSlot,
        val stored: ByteArray,
    ) : QuickEditPhotoEvent

    /** "Готово" in the crop window - the presenter cuts, compresses and saves */
    data class PhotoCropConfirmed(
        val crop: CropRect,
    ) : QuickEditPhotoEvent

    data object PhotoCropCancelled : QuickEditPhotoEvent

    data class PhotoRemoved(
        val slot: PhotoSlot,
    ) : QuickEditPhotoEvent
}

/** the photo steps of a card - pick, crop window, remove - kept apart so the presenter delegates them as one */
sealed interface QuickEditPhotoEvent : QuickEditUiEvent

/** a committed change and the way back - the codex turns it into an undo toast */
data class EntityChange(
    val field: QuickEditField,
    val entityName: String,
    val restore: suspend () -> Unit,
)

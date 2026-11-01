package com.lyrismet.incadent.feature.codex.presentation.quickedit

import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.SpeciesType

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

    /** a type of the open creature type editor was tapped */
    data class CreatureTypeChosen(
        val type: SpeciesType,
    ) : QuickEditUiEvent

    /** a type made in the creature type sheet, the card's species takes it */
    data class CustomTypeCreated(
        val name: String,
    ) : QuickEditUiEvent

    data class CustomTypeRenamed(
        val id: Long,
        val name: String,
    ) : QuickEditUiEvent

    data class CustomTypeDeleted(
        val id: Long,
    ) : QuickEditUiEvent

    /** a candidate of the open relation editor was tapped - the editor stays open for the next one */
    data class RelationToggled(
        val id: Long,
    ) : QuickEditUiEvent

    data object HoldHintDismissed : QuickEditUiEvent

    /** the place select sheet's "+ create" row, [checked] are the ids ticked so far */
    data class PlaceCreated(
        val name: String,
        val checked: List<Long>,
    ) : QuickEditUiEvent

    /** a species made on the spot in the select sheet, the card's species points to it */
    data class SpeciesCreated(
        val name: String,
        val type: CreatureType,
    ) : QuickEditUiEvent

    /** the undo bar the card carries over its bottom edge */
    data object UndoClicked : QuickEditUiEvent

    data object UndoFollowUpClicked : QuickEditUiEvent

    data object UndoDismissed : QuickEditUiEvent

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
    val moved: MovedPlaces? = null,
    val restore: suspend () -> Unit,
)

/** the places a change moved into the card's place, [fromName] is the place they all came out of when it is one */
data class MovedPlaces(
    val count: Int,
    val fromName: String?,
)

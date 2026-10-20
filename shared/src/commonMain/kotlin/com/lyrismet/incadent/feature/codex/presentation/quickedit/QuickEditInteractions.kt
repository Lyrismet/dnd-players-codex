package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.encodeAsync
import com.lyrismet.incadent.core.portrait.prepareCropDraftAsync
import com.lyrismet.incadent.core.portrait.recropDraft
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** a photo in the crop window and the card it will be saved to - the ref is read once, the sheet may close meanwhile */
class QuickPhotoCrop(
    val draft: PhotoCropDraft,
    val ref: EntityRef,
)

private fun PhotoSlot.field(): QuickEditField =
    when (this) {
        PhotoSlot.PORTRAIT -> QuickEditField.PORTRAIT
        PhotoSlot.COVER -> QuickEditField.COVER
    }

/** the open-editor flow every card that supports quick edits delegates to - the presenter owns the state */
class QuickEditInteractions(
    private val inlineEdit: MutableState<InlineEdit?>,
    private val photoCrop: MutableState<QuickPhotoCrop?>,
    private val selectedRef: MutableState<EntityRef?>,
    private val entitySheet: EntitySheetInteractions,
    private val imageCompressor: ImageCompressor,
    private val onHoldHintSeen: () -> Unit,
) {
    fun onEvent(
        scope: CoroutineScope,
        event: QuickEditUiEvent,
    ) {
        when (event) {
            is QuickEditUiEvent.FieldHeld -> {
                inlineEdit.value = InlineEdit(event.field, event.currentText)
                // the move button is tapped, not held
                if (event.field != QuickEditField.MOVE) onHoldHintSeen()
            }
            is QuickEditUiEvent.DraftChanged -> inlineEdit.value = inlineEdit.value?.copy(draft = event.draft)
            QuickEditUiEvent.EditCancelled -> inlineEdit.value = null
            QuickEditUiEvent.EditSaved -> saveDraft(scope)
            is QuickEditUiEvent.LinkChosen ->
                inlineEdit.value?.let { edit ->
                    inlineEdit.value = null
                    commit(scope, edit.field, QuickEditValue.Link(event.id))
                }
            is QuickEditUiEvent.PlacesChosen ->
                inlineEdit.value?.let { edit ->
                    inlineEdit.value = null
                    commit(scope, edit.field, QuickEditValue.Places(event.ids))
                }
            is QuickEditUiEvent.RelationToggled -> toggleRelation(scope, event.id)
            QuickEditUiEvent.HoldHintDismissed -> onHoldHintSeen()
            is QuickEditPhotoEvent -> onPhotoEvent(scope, event)
        }
    }

    private fun onPhotoEvent(
        scope: CoroutineScope,
        event: QuickEditPhotoEvent,
    ) {
        when (event) {
            is QuickEditUiEvent.PhotoPicked -> openCrop(scope, event.slot, event.bytes)
            is QuickEditUiEvent.PhotoRecropRequested -> openRecrop(event.slot, event.stored)
            is QuickEditUiEvent.PhotoCropConfirmed -> saveCrop(scope, event.crop)
            QuickEditUiEvent.PhotoCropCancelled -> photoCrop.value = null
            is QuickEditUiEvent.PhotoRemoved -> commit(scope, event.slot.field(), QuickEditValue.Picture(null))
        }
    }

    /** drops any open editor - called when the card closes or switches to another entity */
    fun onSheetClosed() {
        inlineEdit.value = null
        photoCrop.value = null
    }

    // shrinking a big pick runs off the main thread, a second pick while one is working simply wins the race
    private fun openCrop(
        scope: CoroutineScope,
        slot: PhotoSlot,
        rawBytes: ByteArray,
    ) {
        val ref = selectedRef.value ?: return
        scope.launch {
            val draft = prepareCropDraftAsync(rawBytes, slot, imageCompressor)
            photoCrop.value = QuickPhotoCrop(draft, ref)
        }
    }

    private fun openRecrop(
        slot: PhotoSlot,
        stored: ByteArray,
    ) {
        val ref = selectedRef.value ?: return
        photoCrop.value = QuickPhotoCrop(recropDraft(stored, slot), ref)
    }

    private fun saveCrop(
        scope: CoroutineScope,
        crop: CropRect,
    ) {
        val pending = photoCrop.value ?: return
        photoCrop.value = null
        if (pending.draft.keepsStored(crop)) return
        scope.launch {
            val photo = pending.draft.encodeAsync(crop, imageCompressor)
            entitySheet.onQuickEdit(scope, pending.ref, pending.draft.slot.field(), QuickEditValue.Picture(photo))
        }
    }

    // the editor stays open so several candidates can be ticked one after another
    private fun toggleRelation(
        scope: CoroutineScope,
        targetId: Long,
    ) {
        val field = inlineEdit.value?.field ?: return
        val ref = selectedRef.value ?: return
        entitySheet.onRelationToggled(scope, ref, field, targetId)
    }

    private fun saveDraft(scope: CoroutineScope) {
        val edit = inlineEdit.value ?: return
        inlineEdit.value = null
        // a rejected draft just closes the editor, the field keeps its old value
        val value = quickEditValueFromDraft(edit.field, edit.draft) ?: return
        commit(scope, edit.field, value)
    }

    private fun commit(
        scope: CoroutineScope,
        field: QuickEditField,
        value: QuickEditValue,
    ) {
        val ref = selectedRef.value ?: return
        entitySheet.onQuickEdit(scope, ref, field, value)
    }
}

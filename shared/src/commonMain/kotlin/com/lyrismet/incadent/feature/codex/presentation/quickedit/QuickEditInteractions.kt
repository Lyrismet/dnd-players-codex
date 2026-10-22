package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.core.localization.currentAppLanguage
import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.encodeAsync
import com.lyrismet.incadent.core.portrait.prepareCropDraftAsync
import com.lyrismet.incadent.core.portrait.recropDraft
import com.lyrismet.incadent.core.preferences.AppLanguage
import com.lyrismet.incadent.core.undo.UndoAction
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.grouping.withRecentPlaces
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.quickSpecies
import com.lyrismet.incadent.feature.codex.presentation.form.PlaceSelectState
import com.lyrismet.incadent.feature.codex.presentation.form.SpeciesSelectState
import com.lyrismet.incadent.feature.codex.presentation.form.editingPlace
import com.lyrismet.incadent.feature.codex.presentation.form.placeFieldTitle
import com.lyrismet.incadent.feature.codex.presentation.form.placeSelectState
import com.lyrismet.incadent.feature.codex.presentation.form.quickPlaceField
import com.lyrismet.incadent.feature.codex.presentation.form.quickPlaceTarget
import com.lyrismet.incadent.feature.codex.presentation.form.speciesSelectState
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.entity_sheet_move_into
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

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
@Suppress("LongParameterList")
class QuickEditInteractions(
    private val inlineEdit: MutableState<InlineEdit?>,
    private val photoCrop: MutableState<QuickPhotoCrop?>,
    private val selectedRef: MutableState<EntityRef?>,
    private val entitySheet: EntitySheetInteractions,
    private val imageCompressor: ImageCompressor,
    // the places picked or created last, newest first - the select sheet lists them above the rest
    private val recentPlaceIds: MutableState<List<Long>>,
    repositories: CodexRepositories,
    private val undoController: UndoController,
    private val onHoldHintSeen: () -> Unit,
) {
    private val locationRepository = repositories.locationRepository
    private val speciesRepository = repositories.speciesRepository

    /** what the screen draws of the open editors - [holdTipAllowed] is the quick mode with the hint still unseen */
    @Composable
    fun state(
        records: CodexRecords,
        holdTipAllowed: Boolean,
        undoAction: UndoAction?,
    ): QuickEditState {
        val edit = inlineEdit.value
        val ref = selectedRef.value
        return QuickEditState(
            inlineEdit = edit,
            photoCrop = photoCrop.value?.draft,
            placeSelect = placeSelectOf(edit, ref, records),
            speciesSelect = speciesSelectOf(edit, ref, records),
            holdTipVisible = holdTipAllowed && edit == null,
            undoAction = undoAction,
        )
    }

    // null when no place field is being edited
    @Composable
    private fun placeSelectOf(
        edit: InlineEdit?,
        ref: EntityRef?,
        records: CodexRecords,
    ): PlaceSelectState? {
        val moveTitle = stringResource(Res.string.entity_sheet_move_into)
        val target =
            if (edit != null &&
                edit.field.kind is QuickEditKind.Places
            ) {
                quickPlaceTarget(ref, edit.field, records)
            } else {
                null
            }
        return target?.let { (field, selectedIds) ->
            placeSelectState(
                field = field,
                title = moveTitle.takeIf { edit?.field == QuickEditField.MOVE } ?: placeFieldTitle(field),
                locations = records.locations,
                selectedIds = selectedIds,
                editing = editingPlace(ref, records.locations),
                recentIds = recentPlaceIds.value,
            )
        }
    }

    // null unless the species of an npc card is being edited
    @Composable
    private fun speciesSelectOf(
        edit: InlineEdit?,
        ref: EntityRef?,
        records: CodexRecords,
    ): SpeciesSelectState? {
        val isRussian = currentAppLanguage() == AppLanguage.RUSSIAN
        val npc = (ref as? EntityRef.Npc)?.let { held -> records.npcs.firstOrNull { it.id == held.id } }
        return if (edit?.field == QuickEditField.SPECIES && npc != null) {
            speciesSelectState(records.species, npc.speciesId, isRussian)
        } else {
            null
        }
    }

    fun onEvent(
        scope: CoroutineScope,
        event: QuickEditUiEvent,
    ) {
        when (event) {
            is QuickEditUiEvent.FieldHeld -> onFieldHeld(event)
            is QuickEditUiEvent.DraftChanged -> inlineEdit.value = inlineEdit.value?.copy(draft = event.draft)
            QuickEditUiEvent.EditCancelled -> inlineEdit.value = null
            QuickEditUiEvent.EditSaved -> saveDraft(scope)
            is QuickEditUiEvent.LinkChosen -> commitChosen(scope, QuickEditValue.Link(event.id))
            is QuickEditUiEvent.PlacesChosen -> {
                recentPlaceIds.value = recentPlaceIds.value.withRecentPlaces(event.ids)
                commitChosen(scope, QuickEditValue.Places(event.ids))
            }
            is QuickEditUiEvent.PlaceCreated -> createPlace(scope, event)
            is QuickEditUiEvent.SpeciesCreated -> createSpecies(scope, event)
            QuickEditUiEvent.UndoClicked -> undoController.undo()
            QuickEditUiEvent.UndoFollowUpClicked -> undoController.followUp()
            is QuickEditUiEvent.RelationToggled -> toggleRelation(scope, event.id)
            QuickEditUiEvent.HoldHintDismissed -> onHoldHintSeen()
            is QuickEditPhotoEvent -> onPhotoEvent(scope, event)
        }
    }

    private fun onFieldHeld(event: QuickEditUiEvent.FieldHeld) {
        inlineEdit.value = InlineEdit(event.field, event.currentText)
        // the move button is tapped, not held
        if (event.field != QuickEditField.MOVE) onHoldHintSeen()
    }

    // a select sheet answered, the editor closes and the held field takes the answer
    private fun commitChosen(
        scope: CoroutineScope,
        value: QuickEditValue,
    ) {
        val edit = inlineEdit.value ?: return
        inlineEdit.value = null
        commit(scope, edit.field, value)
    }

    // the new place joins what was ticked in the select sheet, a single field takes just the new place
    private fun createPlace(
        scope: CoroutineScope,
        event: QuickEditUiEvent.PlaceCreated,
    ) {
        val name = event.name.trim().capitalizeFirst()
        if (name.isBlank()) return
        val held = inlineEdit.value?.field ?: return
        val isMulti = quickPlaceField(selectedRef.value, held)?.isMulti == true
        scope.launch {
            val id = locationRepository.upsert(Location(id = 0, name = name, description = ""))
            recentPlaceIds.value = recentPlaceIds.value.withRecentPlaces(listOf(id))
            commitChosen(scope, QuickEditValue.Places(if (isMulti) event.checked + id else listOf(id)))
        }
    }

    private fun createSpecies(
        scope: CoroutineScope,
        event: QuickEditUiEvent.SpeciesCreated,
    ) {
        val name = event.name.trim().capitalizeFirst()
        val edit = inlineEdit.value
        val ref = selectedRef.value
        if (name.isBlank() || edit == null || ref == null) return
        inlineEdit.value = null
        scope.launch {
            val id = speciesRepository.upsert(quickSpecies(name).copy(creatureType = event.type))
            entitySheet.onQuickEdit(scope, ref, edit.field, QuickEditValue.Link(id))
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

package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexGroupingSelection
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormEvent
import com.lyrismet.incadent.feature.codex.presentation.party.PlayerCharacterPicker
import com.lyrismet.incadent.feature.codex.presentation.quickedit.InlineEdit
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditInteractions
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickPhotoCrop
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetInteractions
import com.lyrismet.incadent.feature.codex.presentation.summary.onEvent
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_quick_add_toast_format
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** the presenter's editable-in-place state, bundled so [onEvent] doesn't take one param per field */
@Suppress("LongParameterList")
internal class CodexFields(
    val activeTab: MutableState<CodexTab>,
    val searchQuery: MutableState<String>,
    val grouping: MutableState<CodexGroupingSelection>,
    val selectedEntityRef: MutableState<EntityRef?>,
    val entitySheet: EntitySheetInteractions,
    val inlineEdit: MutableState<InlineEdit?>,
    // the photo waiting in the crop window of the open card, quick mode only
    val photoCrop: MutableState<QuickPhotoCrop?>,
    val quickEdit: QuickEditInteractions,
    val toast: MutableState<ToastMessage?>,
    // the places picked or created last, newest first - the select sheet lists them above the rest
    val recentPlaceIds: MutableState<List<Long>>,
    // the places whose tree section is folded on the places tab
    val collapsedPlaceIds: MutableState<Set<Long>>,
    // the record a delete is waiting on - set while the "notes still mention it" warning is up
    val pendingDelete: MutableState<EntityRef?>,
    // which list the npc tab shows
    val npcMode: MutableState<NpcListMode>,
    // true while the sheet to pick the player's own character is open
    val playerPickOpen: MutableState<Boolean>,
) {
    val showsSpecies: Boolean get() = activeTab.value == CodexTab.NPC && npcMode.value == NpcListMode.SPECIES
}

/** turns every [CodexEvent] into the matching controller call or repository write */
internal class CodexEventHandler(
    private val imageCompressor: ImageCompressor,
    private val undoController: UndoController,
    private val deletionPlanner: CodexDeletionPlanner,
    private val playerPicker: PlayerCharacterPicker,
) {
    // flat circuit event-dispatch table, grows one branch per event variant - not real branching complexity
    @Suppress("CyclomaticComplexMethod", "LongMethod", "LongParameterList")
    fun handle(
        event: CodexEvent,
        fields: CodexFields,
        formController: CodexEntryFormController,
        records: CodexRecords,
        labels: CodexLabels,
        scope: CoroutineScope,
        campaignId: String?,
    ) {
        when (event) {
            CodexEvent.PlayerBannerPickClicked -> fields.playerPickOpen.value = true
            CodexEvent.PlayerBannerHidden -> playerPicker.hideBanner(campaignId)
            CodexEvent.PlayerPickDismissed -> fields.playerPickOpen.value = false
            is CodexEvent.PlayerPicked -> {
                fields.playerPickOpen.value = false
                playerPicker.pick(scope, records.parties, event.id)
            }
            is CodexEvent.TabSelected -> fields.activeTab.value = event.tab
            is CodexEvent.NpcModeSelected -> fields.npcMode.value = event.mode
            is CodexEvent.SearchQueryChanged -> fields.searchQuery.value = event.query
            is CodexEvent.GroupBySelected -> fields.grouping.value = fields.grouping.value.select(event.choice)
            is CodexEvent.EntitySheet -> {
                // a card that opens or closes drops the half typed value of the one underneath
                if (event.event is EntitySheetEvent.EntityClicked || event.event == EntitySheetEvent.Dismissed) {
                    fields.quickEdit.onSheetClosed()
                }
                fields.entitySheet.onEvent(scope, event.event)
            }
            is CodexEvent.QuickEdit -> fields.quickEdit.onEvent(scope, event.event)
            CodexEvent.AddEntryClicked -> {
                val type = if (fields.showsSpecies) EntityKind.SPECIES else fields.activeTab.value.toEntryType()
                formController.onAddEntryClicked(type)
            }
            is CodexEvent.EmptyActionClicked -> formController.onAddEntryClicked(event.entryType, event.name)
            is CodexEvent.EditEntryRequested -> formController.onEditEntryRequested(event.ref, scope)
            is CodexEvent.PlaceSectionToggled -> {
                val folded = fields.collapsedPlaceIds.value
                fields.collapsedPlaceIds.value =
                    if (event.placeId in folded) folded - event.placeId else folded + event.placeId
            }
            is CodexEvent.EntryForm -> {
                val created =
                    formController.creatingType().takeIf {
                        event.event ==
                            CodexEntryFormEvent.EntryFormSaveClicked
                    }
                val closed =
                    formController.onEvent(event.event, scope, imageCompressor) { name ->
                        fields.toast.value =
                            ToastMessage(getString(Res.string.codex_entry_quick_add_toast_format, name))
                    }
                if (closed) fields.toast.value = null
                created?.let { fields.showTabOf(it) }
            }
            is CodexEvent.EntityDeleteRequested -> onEntityDeleteRequested(event.ref, fields, records, labels, scope)
            CodexEvent.EntityDeleteConfirmed ->
                fields.pendingDelete.value?.let { ref ->
                    fields.pendingDelete.value = null
                    fields.closeSheetOf(ref)
                    deleteNow(ref, records, labels, scope)
                }
            CodexEvent.EntityDeleteCancelled -> fields.pendingDelete.value = null
        }
    }

    // a record made here is shown where it was filed, so the user sees it appear
    private fun CodexFields.showTabOf(type: EntityKind) {
        activeTab.value =
            when (type) {
                EntityKind.PARTY -> CodexTab.PARTY
                EntityKind.NPC, EntityKind.SPECIES -> CodexTab.NPC
                EntityKind.QUEST -> CodexTab.QUEST
                EntityKind.LOCATION -> CodexTab.LOCATION
            }
        if (type == EntityKind.NPC) npcMode.value = NpcListMode.NAMED
        if (type == EntityKind.SPECIES) npcMode.value = NpcListMode.SPECIES
    }

    // a record notes still mention asks first, anything else goes straight away
    private fun onEntityDeleteRequested(
        ref: EntityRef,
        fields: CodexFields,
        records: CodexRecords,
        labels: CodexLabels,
        scope: CoroutineScope,
    ) {
        val plan = deletionPlanner.planFor(ref, records, labels.entityType) ?: return
        if (plan.mentionHits.notes > 0) {
            fields.pendingDelete.value = ref
        } else {
            fields.closeSheetOf(ref)
            deleteNow(ref, records, labels, scope)
        }
    }

    // deletes immediately and offers undo - restoring also re-links whatever else referenced the deleted entity
    private fun deleteNow(
        ref: EntityRef,
        records: CodexRecords,
        labels: CodexLabels,
        scope: CoroutineScope,
    ) {
        val plan = deletionPlanner.planFor(ref, records, labels.entityType) ?: return
        scope.launch {
            plan.delete()
            undoController.show(labels.undoDeletedTitle, plan.subtitle, plan.restore)
        }
    }
}

// a deleted card must not come back by itself when the delete is undone
private fun CodexFields.closeSheetOf(ref: EntityRef) {
    if (selectedEntityRef.value == ref) quickEdit.onSheetClosed()
    entitySheet.onEntityRemoved(ref)
}

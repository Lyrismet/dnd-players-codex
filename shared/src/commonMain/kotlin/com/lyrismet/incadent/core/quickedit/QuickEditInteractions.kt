package com.lyrismet.incadent.core.quickedit

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySheetInteractions
import kotlinx.coroutines.CoroutineScope

/** the open-editor flow every card that supports quick edits delegates to - the presenter owns the state */
class QuickEditInteractions(
    private val inlineEdit: MutableState<InlineEdit?>,
    private val selectedRef: MutableState<EntityRef?>,
    private val entitySheet: EntitySheetInteractions,
    private val onHoldHintSeen: () -> Unit,
) {
    fun onEvent(
        scope: CoroutineScope,
        event: QuickEditUiEvent,
    ) {
        when (event) {
            is QuickEditUiEvent.FieldHeld -> {
                inlineEdit.value = InlineEdit(event.field, event.currentText)
                onHoldHintSeen()
            }
            is QuickEditUiEvent.DraftChanged -> inlineEdit.value = inlineEdit.value?.copy(draft = event.draft)
            QuickEditUiEvent.EditCancelled -> inlineEdit.value = null
            QuickEditUiEvent.EditSaved -> saveDraft(scope)
            is QuickEditUiEvent.LinkChosen ->
                inlineEdit.value?.let { edit ->
                    inlineEdit.value = null
                    commit(scope, edit.field, QuickEditValue.Link(event.id))
                }
            QuickEditUiEvent.HoldHintDismissed -> onHoldHintSeen()
        }
    }

    /** drops any open editor - called when the card closes or switches to another entity */
    fun onSheetClosed() {
        inlineEdit.value = null
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

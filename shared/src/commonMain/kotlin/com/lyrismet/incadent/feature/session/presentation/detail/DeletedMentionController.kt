package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.mention.domain.DeletedMention
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import kotlinx.coroutines.CoroutineScope

/** the "?" tag sheet that is open - [isPicking] swaps its actions for the list of records to link to */
internal data class OpenDeletedSheet(
    val deleted: DeletedMention,
    val isPicking: Boolean = false,
    val query: String = "",
)

/** the "?" tag sheet of a deleted record - opening it, picking a record to link to and what each choice rewrites */
internal class DeletedMentionController(
    private val open: MutableState<OpenDeletedSheet?>,
    private val actions: SessionMentionActions,
    private val formController: CodexEntryFormController,
) {
    fun show(deleted: DeletedMention) {
        open.value = OpenDeletedSheet(deleted)
    }

    fun dismiss() {
        open.value = null
    }

    fun setPicking(isPicking: Boolean) {
        open.value = open.value?.copy(isPicking = isPicking, query = "")
    }

    fun setQuery(query: String) {
        open.value = open.value?.copy(query = query)
    }

    /** the sheet closes and the codex form opens over the screen, named and typed like the tag */
    fun createRecord(startCreate: (DeletedMention) -> Unit) {
        val deleted = open.value?.deleted ?: return
        open.value = null
        if (deleted.isUnlinked) {
            // a name nobody made a record for has no type yet, so it asks for one first
            startCreate(deleted)
        } else {
            // created under the old id, so the tokens already in the notes point at it again
            formController.onAddEntryClicked(deleted.type, deleted.name, reviveId = deleted.id)
        }
    }

    /** a null [key] keeps the name as plain words, otherwise the mention points at the picked record */
    fun rewrite(
        scope: CoroutineScope,
        key: String?,
        data: MentionEventData,
    ) {
        val deleted = open.value?.deleted ?: return
        val target = key?.let { id -> data.linkTargets.firstOrNull { it.id == id }?.entity }
        // a picked record that is gone by now leaves the tag alone instead of turning it into words
        if (key == null || target != null) {
            open.value = null
            actions.rewriteDeleted(scope, deleted, target, data)
        }
    }

    /** the sheet only exists while the tag does, so one that was linked, turned into words or revived closes it */
    fun state(data: MentionEventData): DeletedMentionSheetState? {
        val current = open.value ?: return null
        val deleted = current.deleted
        val hits = data.hitsOf(deleted)
        val tagStillThere = hits.notes > 0 && !data.candidates.has(MentionRef(deleted.type, deleted.id))
        return DeletedMentionSheetState(
            name = deleted.name,
            type = deleted.type,
            notes = hits.notes,
            sessions = hits.sessions,
            isUnlinked = deleted.isUnlinked,
            isPicking = current.isPicking,
            query = current.query,
            linkOptions = if (current.isPicking) data.linkOptionsFor(current.query) else emptyList(),
        ).takeIf { tagStillThere }
    }
}

package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lyrismet.incadent.core.format.toDisplayTime
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.insertMention
import com.lyrismet.incadent.feature.mention.domain.linkTypedMentions
import com.lyrismet.incadent.feature.mention.domain.plainMentionText
import com.lyrismet.incadent.feature.phrases.domain.insertPhrase
import com.lyrismet.incadent.feature.session.domain.model.SessionEntry
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import com.lyrismet.incadent.feature.mention.domain.withoutBrokenTokens as stripBrokenTokens

private const val ENTRY_PREVIEW_LENGTH = 60

/** the note composer's draft, the entry being edited and the entry whose action row is open, with what they do */
internal class SessionDraftController(
    private val sessionNoteId: Long,
    private val entryRepository: SessionEntryRepository,
    private val undoController: UndoController,
    val draft: MutableState<TextFieldValue>,
    val editingEntryId: MutableState<Long?>,
    val selectedEntryId: MutableState<Long?>,
) {
    private var sending = false

    /** a cut token goes whole, the caret lands where the same text position now is */
    fun onDraftChanged(
        text: String,
        selection: TextRange,
    ) {
        val clean = stripBrokenTokens(text)
        draft.value =
            if (clean == text) {
                TextFieldValue(text, selection)
            } else {
                val caret = stripBrokenTokens(text.take(selection.end)).length
                TextFieldValue(clean, TextRange(caret.coerceAtMost(clean.length)))
            }
    }

    /** the cursor always lands at the end of the new text, not stuck at the caret's old position */
    fun insertMentionTrigger() {
        val current = draft.value.text
        setDraft(if (current.isEmpty() || current.endsWith(" ")) "$current@" else "$current @")
    }

    fun pickMention(token: String) {
        setDraft(insertMention(draft.value.text, token))
    }

    /** the cursor lands after the inserted phrase, same as a picked mention */
    fun insertPhrase(
        phrase: String,
        replacing: String?,
    ) {
        setDraft(insertPhrase(draft.value.text, phrase, replacing))
    }

    /** sends the draft as a new note or as the edit of the open one, an unchanged edit writes nothing */
    fun submit(
        scope: CoroutineScope,
        entries: List<SessionEntry>,
        index: MentionIndex,
    ) {
        // a name typed after "@" without picking it from the list still turns into the token of its record
        val body = linkTypedMentions(draft.value.text.trim(), index)
        val editingId = editingEntryId.value
        // an unchanged save must not flip the "изм." mark, so it only writes when the body actually differs
        val unchanged = editingId != null && entries.find { it.id == editingId }?.body == body
        when {
            body.isEmpty() || sending -> Unit
            unchanged -> cancelEdit()
            else -> write(scope, editingId, body)
        }
    }

    private fun write(
        scope: CoroutineScope,
        editingId: Long?,
        body: String,
    ) {
        sending = true
        val sent = draft.value
        scope.launch {
            try {
                if (editingId != null) {
                    entryRepository.update(editingId, body)
                } else {
                    entryRepository.add(sessionNoteId, body)
                }
                // a failed write throws before this, so the text stays in the composer, and a newer draft is kept
                if (draft.value == sent) cancelEdit()
            } finally {
                sending = false
            }
        }
    }

    fun edit(
        id: Long,
        entries: List<SessionEntry>,
    ) {
        val entry = entries.find { it.id == id } ?: return
        selectedEntryId.value = null
        editingEntryId.value = id
        setDraft(entry.body)
    }

    fun cancelEdit() {
        editingEntryId.value = null
        draft.value = TextFieldValue("")
    }

    /** tapping the already selected note deselects it */
    fun toggleSelected(id: Long) {
        selectedEntryId.value = if (selectedEntryId.value == id) null else id
    }

    /** deletes at once and offers undo, drops the edit in progress when the edited entry is the one removed */
    fun delete(
        scope: CoroutineScope,
        id: Long,
        entries: List<SessionEntry>,
        undoTitle: String,
    ) {
        val entry = entries.find { it.id == id } ?: return
        selectedEntryId.value = null
        if (editingEntryId.value == id) cancelEdit()
        scope.launch {
            entryRepository.delete(id)
            val plain = plainMentionText(entry.body)
            val preview = plain.take(ENTRY_PREVIEW_LENGTH)
            val suffix = if (plain.length > ENTRY_PREVIEW_LENGTH) "…" else ""
            undoController.show(undoTitle, "${entry.createdAt.toDisplayTime()} · $preview$suffix") {
                entryRepository.restore(entry)
            }
        }
    }

    private fun setDraft(text: String) {
        draft.value = TextFieldValue(text, TextRange(text.length))
    }
}

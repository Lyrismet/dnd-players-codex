package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.MutableState
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.preferences.FirstRecordHintState
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.core.undo.UndoFollowUp
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.createNamed
import com.lyrismet.incadent.feature.codex.domain.repository.deleteRecord
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormController
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.mention.domain.DeletedMention
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.insertMentionAt
import com.lyrismet.incadent.feature.mention.domain.mentionToken
import com.lyrismet.incadent.feature.mention.domain.unlinkedMentionRef
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** the create sheet that is open - [unlinked] is set when it was opened from the "no record yet" tag sheet */
internal data class OpenMentionCreate(
    val name: String,
    val type: EntityKind,
    val unlinked: DeletedMention? = null,
)

private val CREATE_KINDS =
    listOf(EntityKind.NPC, EntityKind.QUEST, EntityKind.LOCATION, EntityKind.PARTY, EntityKind.SPECIES)

/** turns a typed name into a codex record and a mention of it, with an Undo that takes both back */
@Suppress("LongParameterList")
internal class MentionCreateController(
    private val open: MutableState<OpenMentionCreate?>,
    private val draft: MutableState<TextFieldValue>,
    private val codexRepositories: CodexRepositories,
    private val undoController: UndoController,
    private val appPreferences: AppPreferencesRepository,
    private val mentionActions: SessionMentionActions,
    private val formController: CodexEntryFormController,
    private val openCard: (EntityRef) -> Unit,
) {
    fun start(name: String) {
        open.value = OpenMentionCreate(name.trim(), EntityKind.NPC)
    }

    fun startFor(unlinked: DeletedMention) {
        open.value = OpenMentionCreate(unlinked.name, EntityKind.NPC, unlinked)
    }

    fun setName(name: String) {
        open.value = open.value?.copy(name = name.take(TextLimits.LINE))
    }

    fun setType(type: EntityKind) {
        open.value = open.value?.copy(type = type)
    }

    fun dismiss() {
        open.value = null
    }

    fun state(data: MentionEventData): MentionCreateSheetState? =
        open.value?.let {
            MentionCreateSheetState(
                it.name,
                it.type,
                CREATE_KINDS,
                it.unlinked?.let { u ->
                    data.hitsOf(u).notes
                },
            )
        }

    fun confirm(
        scope: CoroutineScope,
        data: MentionEventData,
    ) {
        val current = open.value ?: return
        val name = current.name.trim().capitalizeFirst()
        if (name.isEmpty()) return
        open.value = null
        scope.launch {
            val id = codexRepositories.createNamed(current.type, name)
            val ref = MentionRef(current.type, id)
            val revert = placeMention(current.unlinked, ref, name, data)
            val isFirst = appPreferences.observeFirstRecordHintState().first() == FirstRecordHintState.PENDING
            if (isFirst) appPreferences.setFirstRecordHintState(FirstRecordHintState.SEEN)
            val entityRef = ref.toEntityRef()
            val followUp =
                if (isFirst) {
                    UndoFollowUp(data.titles.created.open) { openCard(entityRef) }
                } else {
                    UndoFollowUp(data.titles.created.fill) { formController.onEditEntryRequested(entityRef, scope) }
                }
            val title = if (isFirst) data.titles.created.firstTitle else data.titles.created.title
            undoController.showMade(title, name, followUp) {
                revert()
                codexRepositories.deleteRecord(current.type, id)
            }
        }
    }

    // the draft takes the new token, or the notes' tokens of the unlinked name are pointed at the record
    private suspend fun placeMention(
        unlinked: DeletedMention?,
        ref: MentionRef,
        name: String,
        data: MentionEventData,
    ): suspend () -> Unit {
        if (unlinked != null) {
            return mentionActions.relinkAll(MentionRef(unlinked.type, unlinked.id), ref, name, data.allEntries)
        }
        val before = draft.value
        val inserted = insertMentionAt(before.text, mentionToken(ref, name), before.selection.end)
        val text = inserted.text
        draft.value = TextFieldValue(text, TextRange(inserted.caret))
        // older notes that already hold this name as a "no record yet" tag now point at the record too
        val relinkOlder = mentionActions.relinkAll(unlinkedMentionRef(name), ref, name, data.allEntries)
        // a draft the player kept typing in after the creation is left as it is
        return {
            relinkOlder()
            if (draft.value.text == text) draft.value = before
        }
    }
}

private fun MentionRef.toEntityRef(): EntityRef =
    when (type) {
        EntityKind.NPC -> EntityRef.Npc(id)
        EntityKind.PARTY -> EntityRef.Party(id)
        EntityKind.QUEST -> EntityRef.Quest(id)
        EntityKind.LOCATION -> EntityRef.Location(id)
        EntityKind.SPECIES -> EntityRef.Species(id)
    }

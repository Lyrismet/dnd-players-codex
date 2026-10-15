package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.runtime.MutableState
import com.lyrismet.incadent.feature.session.domain.repository.TagRepository
import com.lyrismet.incadent.feature.session.domain.tags.normalizeCustomTagName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** the session's tag sheet - whether it is open, the name being typed, and every change it makes to the tags */
internal class SessionTagsController(
    private val sessionNoteId: Long,
    private val repository: TagRepository,
    val isOpen: MutableState<Boolean>,
    val draft: MutableState<String>,
) {
    fun open() {
        isOpen.value = true
    }

    fun dismiss() {
        isOpen.value = false
    }

    fun onDraftChanged(value: String) {
        draft.value = value
    }

    /** assigns [name], or unassigns it when the session already carries it */
    fun toggle(
        scope: CoroutineScope,
        name: String,
        assigned: List<String>,
    ) {
        val isAssigned = assigned.any { it.equals(name, ignoreCase = true) }
        scope.launch {
            if (isAssigned) repository.removeTag(sessionNoteId, name) else repository.addTag(sessionNoteId, name)
        }
    }

    fun add(
        scope: CoroutineScope,
        tag: String,
    ) {
        scope.launch { repository.addTag(sessionNoteId, tag) }
    }

    fun addAll(
        scope: CoroutineScope,
        tags: List<String>,
    ) {
        scope.launch { tags.forEach { repository.addTag(sessionNoteId, it) } }
    }

    /** adds the typed name as a custom tag and clears the field, a blank or unusable name does nothing */
    fun submitDraft(scope: CoroutineScope) {
        val name = normalizeCustomTagName(draft.value) ?: return
        draft.value = ""
        add(scope, name)
    }
}

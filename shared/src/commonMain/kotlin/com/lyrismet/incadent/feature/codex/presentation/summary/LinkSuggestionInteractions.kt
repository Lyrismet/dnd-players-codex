package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.link.LinkField
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.LinkSuggestionDismissalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock

/** what the "Link" and "No thanks" of an offer under a card's notes do, Undo takes either back */
class LinkSuggestionInteractions(
    repositories: CodexRepositories,
    private val dismissals: LinkSuggestionDismissalRepository,
    private val undoController: UndoController,
) {
    private val npcRepository = repositories.npcRepository
    private val questRepository = repositories.questRepository

    fun apply(
        scope: CoroutineScope,
        offer: LinkOffer,
    ) {
        scope.launch {
            val restore = entityWriteLock.withLock { link(offer) } ?: return@launch
            undoController.show(offer.doneTitle, offer.doneSubtitle, restore)
        }
    }

    fun dismiss(
        scope: CoroutineScope,
        offer: LinkOffer,
    ) {
        val owner = offer.suggestion.owner
        val target = offer.suggestion.target.ref
        scope.launch {
            dismissals.dismiss(owner, target)
            undoController.show(offer.dismissTitle, offer.dismissSubtitle) { dismissals.restore(owner, target) }
        }
    }

    // fills the field and returns the way back, null when the record already holds the link or is gone
    private suspend fun link(offer: LinkOffer): (suspend () -> Unit)? {
        val suggestion = offer.suggestion
        val targetId = suggestion.target.ref.id
        return when (suggestion.field) {
            LinkField.MEET_AT -> changeNpc(suggestion.owner.id) { it.withPlace(targetId) }
            LinkField.WHO_HERE -> changeNpc(targetId) { it.withPlace(suggestion.owner.id) }
            LinkField.GIVER ->
                changeQuest(suggestion.owner.id) { quest ->
                    quest
                        .takeIf {
                            it.givenByNpcId == null
                        }?.copy(givenByNpcId = targetId)
                }
        }
    }

    private fun Npc.withPlace(placeId: Long): Npc? =
        takeIf { placeId !in locationIds }?.copy(
            locationIds =
                locationIds + placeId,
        )

    private suspend fun changeNpc(
        id: Long,
        change: (Npc) -> Npc?,
    ): (suspend () -> Unit)? = changeRecord({ npcRepository.getById(id) }, change) { npcRepository.upsert(it) }

    private suspend fun changeQuest(
        id: Long,
        change: (Quest) -> Quest?,
    ): (suspend () -> Unit)? = changeRecord({ questRepository.getById(id) }, change) { questRepository.upsert(it) }

    // writes the changed record and returns the way back, a record edited again since keeps its newer state
    private suspend fun <T> changeRecord(
        read: suspend () -> T?,
        change: (T) -> T?,
        write: suspend (T) -> Unit,
    ): (suspend () -> Unit)? {
        val before = read()
        val after = before?.let(change)
        if (before == null || after == null) return null
        write(after)
        return { entityWriteLock.withLock { if (read() == after) write(before) } }
    }
}

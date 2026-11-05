package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterRestRepository
import com.lyrismet.incadent.feature.codex.domain.repository.ItemRepository
import com.lyrismet.incadent.feature.session.domain.repository.SessionEntryRepository
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.items_deleted_title
import dndplayerscodex.shared.generated.resources.me_rest_done_title
import dndplayerscodex.shared.generated.resources.me_rest_note
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** the long rest of the character with its note in the session, the undo takes back only what the rest wrote */
internal class MeRestActions(
    private val scope: CoroutineScope,
    private val rests: CharacterRestRepository,
    private val entries: SessionEntryRepository,
    private val undoController: UndoController,
) {
    fun rest(
        member: PartyMember,
        liveSessionId: Long?,
        log: Boolean,
    ) {
        scope.launch {
            val plan = rests.longRest(member.id) ?: return@launch
            // a session that ended meanwhile must not cost the user the undo
            val entryId =
                liveSessionId?.takeIf { log }?.let {
                    runCatching { entries.add(it, getString(Res.string.me_rest_note)) }.getOrNull()
                }
            undoController.show(getString(Res.string.me_rest_done_title), member.name) {
                rests.undo(member.id, plan.undo)
                entryId?.let { entries.delete(it) }
            }
        }
    }
}

/** the deletes of the things tab, an item goes with an undo that puts it back under its old id */
internal class MeItemActions(
    private val scope: CoroutineScope,
    private val items: ItemRepository,
    private val undoController: UndoController,
) {
    fun delete(id: Long) {
        scope.launch {
            val item = items.getById(id) ?: return@launch
            items.delete(id)
            undoController.show(getString(Res.string.items_deleted_title), item.name) { items.upsert(item) }
        }
    }
}

package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.grouping.duplicateNpcOf
import com.lyrismet.incadent.feature.codex.domain.repository.NpcMergeRepository
import com.lyrismet.incadent.feature.codex.domain.repository.NpcRepository
import com.lyrismet.incadent.feature.codex.presentation.summary.entityWriteLock
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.npc_merge_done
import incadent.shared.generated.resources.npc_merge_done_sub
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.getString

/** makes the npc being edited one with the record that has the typed name, and offers to take it back */
internal class NpcFormMerge(
    private val npcs: NpcRepository,
    private val merge: NpcMergeRepository,
    private val undo: UndoController,
    // the title and the subtitle of the bar for the survivor's name
    private val texts: suspend (String) -> Pair<String, String>,
) {
    /** true when the two became one */
    suspend fun run(
        loserId: Long,
        typedName: String,
    ): Boolean {
        val survivor = duplicateNpcOf(typedName, npcs.observeAll().first(), loserId) ?: return false
        val trace = entityWriteLock.withLock { merge.merge(survivor.id, loserId) }
        if (trace != null) {
            val (title, subtitle) = texts(survivor.name)
            undo.show(title, subtitle) {
                entityWriteLock.withLock { merge.undo(trace) }
            }
        }
        return trace != null
    }
}

/** the bar a merge leaves behind, the survivor's name in the title */
internal suspend fun npcMergeTexts(survivorName: String): Pair<String, String> =
    getString(Res.string.npc_merge_done, survivorName) to getString(Res.string.npc_merge_done_sub)

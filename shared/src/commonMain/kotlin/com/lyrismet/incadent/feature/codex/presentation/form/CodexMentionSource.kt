package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.domain.repository.createNamed
import com.lyrismet.incadent.feature.codex.domain.repository.deleteRecord
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.presentation.MentionSource
import com.lyrismet.incadent.feature.mention.presentation.rememberMentionLookups
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.mention_created_title
import org.jetbrains.compose.resources.stringResource

/** the records a notes field offers after "@", a new one made from there can be taken back with Undo */
@Composable
fun rememberMentionSource(
    records: CodexRecords,
    repositories: CodexRepositories,
    undoController: UndoController,
): MentionSource {
    val title = stringResource(Res.string.mention_created_title)
    val entities = records.mentionIndex.entities
    val lookups = rememberMentionLookups(records.npcs, records.locations, records.quests, records.parties)
    return remember(entities, lookups, repositories, undoController, title) {
        MentionSource(entities, lookups) { kind, name ->
            val id = repositories.createNamed(kind, name)
            undoController.show(title, name) { repositories.deleteRecord(kind, id) }
            MentionRef(kind, id)
        }
    }
}

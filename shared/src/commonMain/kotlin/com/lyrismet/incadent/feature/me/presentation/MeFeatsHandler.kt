package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.CharacterFeat
import com.lyrismet.incadent.feature.codex.domain.repository.CharacterFeatRepository
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_feats_deleted_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** reacts to the presses of the feats block and its sheet, a delete leaves a toast that puts the feat back */
internal class MeFeatsHandler(
    private val ui: MeUiState,
    private val feats: CharacterFeatRepository,
    private val undoController: UndoController,
    private val scope: CoroutineScope,
) {
    fun handle(
        event: FeatEvent,
        memberId: Long,
        stored: List<CharacterFeat>,
    ) {
        val draft = ui.featDraft
        when (event) {
            FeatEvent.AddClicked -> ui.featDraft = CharacterFeat(name = "")
            is FeatEvent.Opened -> stored.find { it.id == event.id }?.let { ui.featDraft = it }
            is FeatEvent.NameChanged -> ui.featDraft = draft?.copy(name = event.name)
            is FeatEvent.SourcePicked ->
                ui.featDraft = draft?.copy(source = event.source.takeIf { it != draft.source })
            is FeatEvent.DescriptionChanged -> ui.featDraft = draft?.copy(description = event.text)
            FeatEvent.Dismissed -> ui.featDraft = null
            FeatEvent.Saved -> {
                val saved = draft?.takeIf { it.canSave } ?: return
                ui.featDraft = null
                scope.launch { feats.upsert(memberId, saved) }
            }
            FeatEvent.Deleted -> {
                val gone = stored.find { it.id == draft?.id } ?: return
                ui.featDraft = null
                scope.launch {
                    feats.delete(gone.id)
                    undoController.show(getString(Res.string.me_feats_deleted_title), gone.name) {
                        feats.upsert(memberId, gone)
                    }
                }
            }
        }
    }
}

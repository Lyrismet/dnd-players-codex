package com.lyrismet.incadent.feature.codex.presentation.party

import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_pick_done_title
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString

/** marks one character of the party as the player's own with an undo, the banner and the "me" screen both use it */
class PlayerCharacterPicker(
    private val partyRepository: PartyRepository,
    private val undoController: UndoController,
    private val preferences: AppPreferencesRepository,
) {
    fun pick(
        scope: CoroutineScope,
        party: List<PartyMember>,
        id: Long,
    ) {
        val chosen = party.find { it.id == id } ?: return
        val previous = party.find { it.isPlayerCharacter }
        scope.launch {
            partyRepository.upsert(chosen.copy(isPlayerCharacter = true))
            undoController.show(getString(Res.string.me_pick_done_title), chosen.name) {
                // a character marked since keeps the mark
                if (partyRepository.getById(id)?.isPlayerCharacter == true) {
                    setMark(id, false)
                    previous?.let { setMark(it.id, true) }
                }
            }
        }
    }

    private suspend fun setMark(
        id: Long,
        marked: Boolean,
    ) {
        partyRepository.getById(id)?.let { partyRepository.upsert(it.copy(isPlayerCharacter = marked)) }
    }

    fun hideBanner(campaignId: String?) {
        if (campaignId != null) preferences.setMeBannerHidden(campaignId, true)
    }
}

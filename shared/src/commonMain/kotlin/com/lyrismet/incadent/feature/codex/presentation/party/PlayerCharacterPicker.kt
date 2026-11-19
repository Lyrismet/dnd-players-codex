package com.lyrismet.incadent.feature.codex.presentation.party

import com.lyrismet.incadent.core.preferences.AppPreferencesRepository
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** marks one character of the party as the player's own, the banner and the "me" screen both use it */
class PlayerCharacterPicker(
    private val partyRepository: PartyRepository,
    private val preferences: AppPreferencesRepository,
) {
    fun pick(
        scope: CoroutineScope,
        party: List<PartyMember>,
        id: Long,
    ) {
        val chosen = party.find { it.id == id } ?: return
        scope.launch { partyRepository.upsert(chosen.copy(isPlayerCharacter = true)) }
    }

    fun hideBanner(campaignId: String?) {
        if (campaignId != null) preferences.setMeBannerHidden(campaignId, true)
    }
}

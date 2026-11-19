package com.lyrismet.incadent.feature.me.presentation

import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.encodeAsync
import com.lyrismet.incadent.core.portrait.prepareCropDraftAsync
import com.lyrismet.incadent.core.portrait.recropDraft
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** reacts to the header portrait, the photo is the one of the party card */
internal class MePortraitHandler(
    private val ui: MeUiState,
    private val party: PartyRepository,
    private val compressor: ImageCompressor,
    private val scope: CoroutineScope,
) {
    fun handle(
        event: PortraitEvent,
        memberId: Long,
    ) {
        when (event) {
            is PortraitEvent.Picked ->
                scope.launch { ui.photoCrop = prepareCropDraftAsync(event.bytes, PhotoSlot.PORTRAIT, compressor) }
            // the list only holds the thumbnail, so the full picture and the kept source are read for the crop
            PortraitEvent.RecropRequested ->
                scope.launch {
                    party.getById(memberId)?.portrait?.takeIf { it.full != null }?.let {
                        ui.photoCrop = recropDraft(it, PhotoSlot.PORTRAIT)
                    }
                }
            is PortraitEvent.CropConfirmed -> confirm(event, memberId)
            PortraitEvent.CropCancelled -> ui.photoCrop = null
            PortraitEvent.Removed -> scope.launch { setPortrait(memberId, null) }
        }
    }

    private fun confirm(
        event: PortraitEvent.CropConfirmed,
        memberId: Long,
    ) {
        val draft = ui.photoCrop ?: return
        ui.photoCrop = null
        if (draft.keepsStored(event.crop)) return
        scope.launch {
            val photo = draft.encodeAsync(event.crop, compressor)
            setPortrait(memberId, photo)
        }
    }

    private suspend fun setPortrait(
        memberId: Long,
        photo: Photo?,
    ) {
        party.getById(memberId)?.let { party.upsert(it.copy(portrait = photo)) }
    }
}

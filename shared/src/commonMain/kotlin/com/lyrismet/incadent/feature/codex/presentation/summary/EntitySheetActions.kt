package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.presentation.quickedit.InlineEdit
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent

/** actions the sheet's interactive pieces dispatch - one bundle instead of one lambda param per feature */
data class EntitySummarySheetActions(
    val onEntityRefClicked: (EntityRef) -> Unit,
    val onNpcStatusSelected: (npcId: Long, status: NpcStatus) -> Unit,
    val onNpcLifeSelected: (npcId: Long, lifeState: NpcLifeState) -> Unit,
    val onQuestStatusSelected: (questId: Long, status: QuestStatus) -> Unit,
    val onRelatedNoteClicked: (sessionNoteId: Long, entryId: Long) -> Unit,
    val onEditClicked: ((EntityRef) -> Unit)? = null,
    // null hides the presence picker - only the codex manages party membership
    val onPartyPresenceSelected: ((partyId: Long, presence: PartyPresence) -> Unit)? = null,
    // true shows the status pickers as plain badges - the form-mode card is read-only
    val statusesReadOnly: Boolean = false,
    // null keeps the card read-only - in-place editing only exists in the codex
    val quickEdit: QuickEditSheet? = null,
)

/** the in-place editing state a card renders - the presenter owns it, the card only shows it and forwards events */
data class QuickEditSheet(
    val inlineEdit: InlineEdit?,
    // the photo waiting in the crop window - drawn over the card until it is confirmed or cancelled
    val photoCrop: PhotoCropDraft?,
    val holdTipVisible: Boolean,
    val onEvent: (QuickEditUiEvent) -> Unit,
)

package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
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
    // null hides the delete button - only an editable codex card offers it
    val onDeleteClicked: ((EntityRef) -> Unit)? = null,
    // null hides the presence picker - only the codex manages party membership
    val onPartyPresenceSelected: ((partyId: Long, presence: PartyPresence) -> Unit)? = null,
    // null keeps a species' damage read-only - only the codex marks what the party has noticed
    val onSpeciesDamageChanged: ((speciesId: Long, type: DamageType, state: DamageState) -> Unit)? = null,
    // steps the count of the chip the card was opened from, the row only shows when the card knows that count
    val onSpeciesNoteQuantityStep: ((delta: Int) -> Unit)? = null,
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

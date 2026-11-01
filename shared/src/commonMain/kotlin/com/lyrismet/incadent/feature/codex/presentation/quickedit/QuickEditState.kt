package com.lyrismet.incadent.feature.codex.presentation.quickedit

import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.undo.UndoAction
import com.lyrismet.incadent.feature.codex.presentation.form.CreatureTypeSelectState
import com.lyrismet.incadent.feature.codex.presentation.form.PlaceSelectState
import com.lyrismet.incadent.feature.codex.presentation.form.SpeciesSelectState
import com.lyrismet.incadent.feature.codex.presentation.summary.QuickEditSheet
import com.lyrismet.incadent.feature.mention.presentation.MentionField

/** everything a screen shows of the in-place editing of the open card, built by [QuickEditInteractions.state] */
data class QuickEditState(
    // the field editor open inside the card, if any
    val inlineEdit: InlineEdit? = null,
    // the photo waiting in the crop window of the open card
    val photoCrop: PhotoCropDraft? = null,
    // the select sheet of a place field being held on the card
    val placeSelect: PlaceSelectState? = null,
    // the select sheet of the species field being held on an npc card
    val speciesSelect: SpeciesSelectState? = null,
    // the creature type select sheet of the species field being held on a species card
    val creatureTypeSelect: CreatureTypeSelectState? = null,
    val holdTipVisible: Boolean = false,
    // the bar the card carries itself, the sheet covers the app-level one
    val undoAction: UndoAction? = null,
    // what "@" offers in the description editor of the open card
    val mention: MentionField? = null,
)

/** the card's editing hooks, [onEvent] reaches the screen's presenter */
fun QuickEditState.asSheet(onEvent: (QuickEditUiEvent) -> Unit): QuickEditSheet =
    QuickEditSheet(
        inlineEdit = inlineEdit,
        photoCrop = photoCrop,
        holdTipVisible = holdTipVisible,
        onEvent = onEvent,
        mention = mention,
    )

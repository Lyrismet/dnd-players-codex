package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef

/** the party-only part of the form, nested so the other entry types never see these fields */
internal data class PartyFormFields(
    val isPlayerCharacter: Boolean = false,
    val player: String = "",
    val characterClass: String = "",
    val presence: PartyPresence = PartyPresence.IN,
    // a number the user has not filled in stays null and shows as a dash
    val level: Int? = null,
    val hpMax: Int? = null,
    val hpCurrent: Int? = null,
)

/** the form's own editable-in-place fields - kept separate from [CodexEntryFormState] so typing survives rebuilds */
internal data class CodexEntryFormFields(
    val editingRef: EntityRef?,
    // the id a new record is created under - a deleted record's, so the tokens in notes find it again
    val reviveId: Long = 0,
    val originalName: String = "",
    val type: EntityKind,
    val name: String = "",
    val description: String = "",
    // shared between npc and party - the only two types the mockup gives a portrait frame to
    val portrait: Photo? = null,
    val race: String = "",
    val faction: String = "",
    val npcStatus: NpcStatus = NpcStatus.NEUTRAL,
    val npcLife: NpcLifeState = NpcLifeState.ALIVE,
    val npcLocationIds: List<Long> = emptyList(),
    val reward: String = "",
    val questGiverId: Long? = null,
    val questLocationId: Long? = null,
    val questStatus: QuestStatus = QuestStatus.ACTIVE,
    val locationParentId: Long? = null,
    // the wide picture of a place - the only photo a location has
    val cover: Photo? = null,
    val party: PartyFormFields = PartyFormFields(),
    // null means the select sheet is closed - see onPlaceFieldOpened
    val placeSelectField: CodexEntryPlaceField? = null,
    // null means no "+ New X" quick-add input is open - see onQuickAddOpened
    val quickAddField: CodexEntryChipField? = null,
    val quickAddDraft: String = "",
    // the photo waiting in the crop window, transient like the select sheet
    val photoCrop: PhotoCropDraft? = null,
) {
    // the select sheet and quick-add are transient panels, only the values the user typed or picked count as a change
    fun hasUnsavedChanges(baseline: CodexEntryFormFields?): Boolean =
        baseline != null &&
            copy(
                placeSelectField = baseline.placeSelectField,
                quickAddField = baseline.quickAddField,
                quickAddDraft = baseline.quickAddDraft,
                photoCrop = baseline.photoCrop,
            ) != baseline
}

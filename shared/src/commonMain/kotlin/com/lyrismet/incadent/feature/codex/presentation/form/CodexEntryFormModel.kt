package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem

/** pre-formatted create/edit sheet state - null fields/lists are simply unused for the current [type] */
data class CodexEntryFormState(
    val overline: String,
    val heading: String,
    val isEditing: Boolean,
    val type: EntityKind,
    val name: String = "",
    val description: String = "",
    // only rendered for party/npc types - see PortraitField in CodexEntryFormUi
    val portrait: Photo? = null,
    // the wide picture of a place, only rendered for the location type
    val cover: Photo? = null,
    // the photo waiting in the crop window over the form
    val photoCrop: PhotoCropDraft? = null,
    val race: String = "",
    val nicknames: String = "",
    val reward: String = "",
    val npcStatusOptions: List<FormChipOption<NpcStatus>> = emptyList(),
    val npcLifeOptions: List<FormChipOption<NpcLifeState>> = emptyList(),
    val questStatusOptions: List<FormChipOption<QuestStatus>> = emptyList(),
    // the places picked in the select-sheet fields, each shown on its field row
    val npcPlaces: List<MentionChipItem> = emptyList(),
    val questPlace: SelectedPlace? = null,
    val locationParent: SelectedPlace? = null,
    // false while no other place exists, so the parent field has nothing to pick from and stays hidden
    val hasParentCandidates: Boolean = false,
    // non-null while the select sheet is open over the form
    val placeSelect: PlaceSelectState? = null,
    val npcOptions: List<FormChipOption<Long>> = emptyList(),
    val partyClass: String = "",
    val partyPlayer: String = "",
    val partyIsPlayerCharacter: Boolean = false,
    val partyOwnerOptions: List<FormChipOption<Boolean>> = emptyList(),
    val partyLevel: Int? = null,
    val partyHpMax: Int? = null,
    // the player character's level when setting it here would change something, null hides the shortcut
    val partyLevelFromPlayer: Int? = null,
    val partyPresenceOptions: List<FormChipOption<PartyPresence>> = emptyList(),
    val canSave: Boolean = false,
    val saveLabel: String = "",
    // the chip field whose "+ New X" quick-add input is open, if any - at most one at a time, see FormChipQuickAdd
    val quickAddField: CodexEntryChipField? = null,
    val quickAddDraft: String = "",
    // set while the "save changes?" prompt asks what to do with an unsaved form being closed
    val isDiscardPromptVisible: Boolean = false,
    // true once something was typed or picked, closing the sheet then asks before it goes away
    val hasUnsavedChanges: Boolean = false,
)

enum class CodexEntryField {
    NAME,
    DESCRIPTION,
    RACE,
    NICKNAMES,
    REWARD,
    PARTY_CLASS,
    PARTY_PLAYER,
}

enum class CodexEntryNumberField { PARTY_LEVEL, PARTY_HP_MAX }

/** a place picked on a form field, shown with the path above it */
data class SelectedPlace(
    val name: String,
    val path: String,
)

/** the form fields that open the place select sheet - the same sheet also serves the card's quick edit */
enum class CodexEntryPlaceField { NPC_PLACES, QUEST_PLACE, LOCATION_PARENT }

/** the select sheet's content - [multi] is true for the npc's several places, the rest pick exactly one */
data class PlaceSelectState(
    val field: CodexEntryPlaceField,
    val title: String,
    val options: List<SelectOption>,
    val selectedIds: Set<Long>,
    val multi: Boolean,
    val excludedIds: Set<Long>,
    val recentIds: List<Long>,
)

enum class CodexEntryChipField { QUEST_GIVER }

/** the events of the create/edit form sheet - the codex and the session screen both host that sheet */
sealed interface CodexEntryFormEvent {
    data class EntryTypeChanged(
        val type: EntityKind,
    ) : CodexEntryFormEvent

    data class EntryFieldChanged(
        val field: CodexEntryField,
        val text: String,
    ) : CodexEntryFormEvent

    class EntryPhotoPicked(
        val slot: PhotoSlot,
        val bytes: ByteArray,
    ) : CodexEntryFormEvent

    data class EntryPhotoRecropRequested(
        val slot: PhotoSlot,
    ) : CodexEntryFormEvent

    data class EntryPhotoCropConfirmed(
        val crop: CropRect,
    ) : CodexEntryFormEvent

    data object EntryPhotoCropCancelled : CodexEntryFormEvent

    data class EntryPhotoRemoved(
        val slot: PhotoSlot,
    ) : CodexEntryFormEvent

    data class EntryNpcStatusChanged(
        val status: NpcStatus,
    ) : CodexEntryFormEvent

    data class EntryNpcLifeChanged(
        val lifeState: NpcLifeState,
    ) : CodexEntryFormEvent

    data class EntryPartyOwnerChanged(
        val isPlayerCharacter: Boolean,
    ) : CodexEntryFormEvent

    data class EntryPartyPresenceChanged(
        val presence: PartyPresence,
    ) : CodexEntryFormEvent

    data class EntryNumberChanged(
        val field: CodexEntryNumberField,
        val value: Int?,
    ) : CodexEntryFormEvent

    data object EntryPartyLevelFromPlayerClicked : CodexEntryFormEvent

    data class EntryPlaceFieldOpened(
        val field: CodexEntryPlaceField,
    ) : CodexEntryFormEvent

    data class EntryPlacesChosen(
        val ids: List<Long>,
    ) : CodexEntryFormEvent

    data class EntryPlaceCreated(
        val name: String,
        val checked: List<Long>,
    ) : CodexEntryFormEvent

    data object EntryPlaceSelectClosed : CodexEntryFormEvent

    data class EntryQuestStatusChanged(
        val status: QuestStatus,
    ) : CodexEntryFormEvent

    data class EntryChipToggled(
        val field: CodexEntryChipField,
        val id: Long,
    ) : CodexEntryFormEvent

    data object EntryFormSaveClicked : CodexEntryFormEvent

    data object EntryFormClosed : CodexEntryFormEvent

    data object EntryFormCloseRequested : CodexEntryFormEvent

    data object EntryFormDiscardPromptDismissed : CodexEntryFormEvent

    data class EntryQuickAddOpened(
        val field: CodexEntryChipField,
    ) : CodexEntryFormEvent

    data class EntryQuickAddDraftChanged(
        val text: String,
    ) : CodexEntryFormEvent

    data object EntryQuickAddSaveClicked : CodexEntryFormEvent

    data object EntryQuickAddCancelled : CodexEntryFormEvent
}

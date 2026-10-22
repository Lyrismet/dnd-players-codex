package com.lyrismet.incadent.feature.codex.presentation.form

import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.selectlist.SelectOption
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageState
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
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
    val locationChildren: List<MentionChipItem> = emptyList(),
    val questPlace: SelectedPlace? = null,
    val locationParent: SelectedPlace? = null,
    // false while no other place exists, so the parent field has nothing to pick from and stays hidden
    val hasParentCandidates: Boolean = false,
    // non-null while the select sheet is open over the form
    val placeSelect: PlaceSelectState? = null,
    val npcOptions: List<FormChipOption<Long>> = emptyList(),
    val speciesDamage: Map<DamageType, DamageState> = emptyMap(),
    val speciesHitPoints: Int? = null,
    val creatureTypeOptions: List<FormChipOption<CreatureType>> = emptyList(),
    // the species the npc is linked to, shown on its field row
    val npcSpecies: SelectedSpecies? = null,
    // non-null while the species select sheet is open over the form
    val speciesSelect: SpeciesSelectState? = null,
    // the SRD names that match the species name typed so far
    val srdSuggestions: List<SrdSuggestion> = emptyList(),
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

enum class CodexEntryNumberField { PARTY_LEVEL, PARTY_HP_MAX, SPECIES_HIT_POINTS }

/** the species picked on the npc form, shown with its creature type under the name */
data class SelectedSpecies(
    val name: String,
    val typeLabel: String,
)

/** a place picked on a form field, shown with the path above it */
data class SelectedPlace(
    val name: String,
    val path: String,
)

/** the form fields that open the place select sheet - the same sheet also serves the card's quick edit */
enum class CodexEntryPlaceField {
    NPC_PLACES,
    QUEST_PLACE,
    LOCATION_PARENT,
    LOCATION_CHILDREN,
    ;

    /** the npc's places and a place's inside take several, the rest pick exactly one */
    val isMulti: Boolean get() = this == NPC_PLACES || this == LOCATION_CHILDREN
}

/** the select sheet's content - [multi] is true for a field that takes several places */
data class PlaceSelectState(
    val field: CodexEntryPlaceField,
    val title: String,
    val options: List<SelectOption>,
    val selectedIds: Set<Long>,
    val multi: Boolean,
    val recentIds: List<Long>,
)

enum class CodexEntryChipField { QUEST_GIVER }

/** the species select sheet's content, one species at most */
data class SpeciesSelectState(
    val options: List<SelectOption>,
    val selectedIds: Set<Long>,
    // the built-in names among the options, by their negative option id
    val srd: Map<Long, SrdSuggestion> = emptyMap(),
)

/** an SRD name offered under the species name field */
data class SrdSuggestion(
    val name: String,
    val type: CreatureType,
)

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

    data class EntryCreatureTypeChanged(
        val type: CreatureType,
    ) : CodexEntryFormEvent

    data class EntryDamageChanged(
        val type: DamageType,
        val state: DamageState,
    ) : CodexEntryFormEvent

    data class EntrySrdPicked(
        val name: String,
        val type: CreatureType,
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

    data object EntrySpeciesSelectOpened : CodexEntryFormEvent

    data class EntrySpeciesChosen(
        val ids: List<Long>,
    ) : CodexEntryFormEvent

    data class EntrySpeciesSrdChosen(
        val name: String,
        val type: CreatureType,
    ) : CodexEntryFormEvent

    data class EntrySpeciesCreated(
        val name: String,
    ) : CodexEntryFormEvent

    data object EntrySpeciesSelectClosed : CodexEntryFormEvent

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

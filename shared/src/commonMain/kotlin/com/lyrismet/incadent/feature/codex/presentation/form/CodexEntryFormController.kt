package com.lyrismet.incadent.feature.codex.presentation.form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.core.portrait.CropRect
import com.lyrismet.incadent.core.portrait.ImageCompressor
import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.core.portrait.encodeAsync
import com.lyrismet.incadent.core.portrait.prepareCropDraftAsync
import com.lyrismet.incadent.core.portrait.recropDraft
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.grouping.withRecentPlaces
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.PartyStatRanges
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.repository.LocationRepository
import com.lyrismet.incadent.feature.codex.domain.repository.NpcRepository
import com.lyrismet.incadent.feature.codex.domain.repository.PartyRepository
import com.lyrismet.incadent.feature.codex.domain.repository.QuestRepository
import com.lyrismet.incadent.feature.codex.domain.repository.quickLocation
import com.lyrismet.incadent.feature.codex.domain.repository.quickNpc
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusColor
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.npcLifeLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.npcStatusLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.partyPresenceLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.questStatusLabels
import com.lyrismet.incadent.feature.mention.presentation.toMentionChip
import com.slack.circuit.retained.rememberRetained
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_location
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_npc
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_party
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_quest
import dndplayerscodex.shared.generated.resources.codex_entry_overline_create
import dndplayerscodex.shared.generated.resources.codex_entry_overline_edit
import dndplayerscodex.shared.generated.resources.codex_entry_owner_me
import dndplayerscodex.shared.generated.resources.codex_entry_owner_other
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_create
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_edit
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_invalid
import dndplayerscodex.shared.generated.resources.codex_place_top_level_caption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// one entry point per form action - splitting them across classes would only forward the same calls
@Suppress("TooManyFunctions", "LongParameterList")
class CodexEntryFormController private constructor(
    private val fields: MutableState<CodexEntryFormFields?>,
    private val baseline: MutableState<CodexEntryFormFields?>,
    private val discardPrompt: MutableState<Boolean>,
    private val npcRepository: NpcRepository,
    private val partyRepository: PartyRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
    private val recentPlaceIds: MutableState<List<Long>>,
    // the level a new party member starts with and the "same as yours" shortcut offers, null without a level
    private val playerCharacterLevel: Int?,
) {
    private val persister = CodexEntryPersister(npcRepository, partyRepository, questRepository, locationRepository)

    companion object {
        @Composable
        fun rememberController(
            npcRepository: NpcRepository,
            partyRepository: PartyRepository,
            questRepository: QuestRepository,
            locationRepository: LocationRepository,
            recentPlaceIds: MutableState<List<Long>>,
            playerCharacterLevel: Int?,
        ): CodexEntryFormController {
            val fields = rememberRetained { mutableStateOf<CodexEntryFormFields?>(null) }
            val baseline = rememberRetained { mutableStateOf<CodexEntryFormFields?>(null) }
            val discardPrompt = rememberRetained { mutableStateOf(false) }
            return CodexEntryFormController(
                fields,
                baseline,
                discardPrompt,
                npcRepository,
                partyRepository,
                questRepository,
                locationRepository,
                recentPlaceIds,
                playerCharacterLevel,
            )
        }
    }

    /**
     * handles one event of the form sheet. [onQuickAdded] gets the name of a record the quick-add created. Returns
     * true when the form is gone afterwards, so a host can drop what it showed for it.
     */
    @Suppress("CyclomaticComplexMethod")
    fun onEvent(
        event: CodexEntryFormEvent,
        scope: CoroutineScope,
        imageCompressor: ImageCompressor,
        onQuickAdded: suspend (String) -> Unit,
    ): Boolean {
        var isClosed = false
        when (event) {
            is CodexEntryFormEvent.EntryTypeChanged -> onTypeChanged(event.type)
            is CodexEntryFormEvent.EntryFieldChanged -> onFieldChanged(event.field, event.text)
            is CodexEntryFormEvent.EntryPhotoPicked -> onPhotoPicked(scope, imageCompressor, event.slot, event.bytes)
            is CodexEntryFormEvent.EntryPhotoRecropRequested -> onPhotoRecropRequested(event.slot)
            is CodexEntryFormEvent.EntryPhotoCropConfirmed -> onPhotoCropConfirmed(scope, imageCompressor, event.crop)
            CodexEntryFormEvent.EntryPhotoCropCancelled -> onPhotoCropCancelled()
            is CodexEntryFormEvent.EntryPhotoRemoved -> onPhotoRemoved(event.slot)
            is CodexEntryFormEvent.EntryNumberChanged -> onNumberChanged(event.field, event.value)
            is CodexEntryFormEvent.EntryNpcStatusChanged -> onNpcStatusChanged(event.status)
            is CodexEntryFormEvent.EntryNpcLifeChanged -> onNpcLifeChanged(event.lifeState)
            is CodexEntryFormEvent.EntryPartyOwnerChanged -> onPartyOwnerChanged(event.isPlayerCharacter)
            is CodexEntryFormEvent.EntryPartyPresenceChanged -> onPartyPresenceChanged(event.presence)
            is CodexEntryFormEvent.EntryQuestStatusChanged -> onQuestStatusChanged(event.status)
            is CodexEntryFormEvent.EntryChipToggled -> onChipToggled(event.field, event.id)
            CodexEntryFormEvent.EntryFormSaveClicked -> onSaveClicked(scope)
            CodexEntryFormEvent.EntryFormClosed -> {
                onClosed()
                isClosed = true
            }
            CodexEntryFormEvent.EntryFormCloseRequested -> isClosed = onCloseRequested()
            CodexEntryFormEvent.EntryFormDiscardPromptDismissed -> onDiscardPromptDismissed()
            CodexEntryFormEvent.EntryPartyLevelFromPlayerClicked -> onPartyLevelFromPlayerClicked()
            is CodexEntryFormEvent.EntryPlaceFieldOpened -> onPlaceFieldOpened(event.field)
            is CodexEntryFormEvent.EntryPlacesChosen -> onPlacesChosen(event.ids)
            is CodexEntryFormEvent.EntryPlaceCreated -> onPlaceCreated(scope, event.name, event.checked)
            CodexEntryFormEvent.EntryPlaceSelectClosed -> onPlaceSelectClosed()
            is CodexEntryFormEvent.EntryQuickAddOpened -> onQuickAddOpened(event.field)
            is CodexEntryFormEvent.EntryQuickAddDraftChanged -> onQuickAddDraftChanged(event.text)
            CodexEntryFormEvent.EntryQuickAddSaveClicked -> onQuickAddSaveClicked(scope, onQuickAdded)
            CodexEntryFormEvent.EntryQuickAddCancelled -> onQuickAddCancelled()
        }
        return isClosed
    }

    // defaultType mirrors the mockup's openNew() - the active codex tab pre-selects the form's type
    fun onAddEntryClicked(
        defaultType: EntityKind,
        initialName: String = "",
        reviveId: Long = 0,
    ) {
        open(
            withDefaults(
                CodexEntryFormFields(editingRef = null, type = defaultType, name = initialName, reviveId = reviveId),
            ),
        )
    }

    // a new party member starts at the player character's level, when that is known
    private fun withDefaults(form: CodexEntryFormFields): CodexEntryFormFields =
        if (form.type == EntityKind.PARTY && form.editingRef == null && form.party.level == null) {
            form.copy(party = form.party.copy(level = playerCharacterLevel))
        } else {
            form
        }

    private fun open(opened: CodexEntryFormFields) {
        fields.value = opened
        baseline.value = opened
        discardPrompt.value = false
    }

    fun onEditEntryRequested(
        ref: EntityRef,
        scope: CoroutineScope,
    ) {
        scope.launch {
            val loaded =
                when (ref) {
                    is EntityRef.Party -> partyRepository.getById(ref.id)?.let { it.toFields(ref) }
                    is EntityRef.Npc -> npcRepository.getById(ref.id)?.let { it.toFields(ref) }
                    is EntityRef.Quest -> questRepository.getById(ref.id)?.let { it.toFields(ref) }
                    is EntityRef.Location ->
                        locationRepository.getById(ref.id)?.let { place ->
                            val inside = locationRepository.observeAll().first().filter { it.parentId == ref.id }
                            place.toFields(ref).copy(locationChildIds = inside.map { it.id })
                        }
                }
            if (loaded != null) open(loaded)
        }
    }

    fun onTypeChanged(type: EntityKind) {
        val current = fields.value ?: return
        fields.value = withDefaults(current.copy(type = type))
        // a default the form fills in by itself is not something the user changed
        baseline.value = baseline.value?.let { withDefaults(it.copy(type = type)).copy(type = it.type) }
    }

    fun onFieldChanged(
        field: CodexEntryField,
        text: String,
    ) {
        val current = fields.value ?: return
        fields.value =
            when (field) {
                CodexEntryField.NAME -> current.copy(name = text)
                CodexEntryField.DESCRIPTION -> current.copy(description = text)
                CodexEntryField.RACE -> current.copy(race = text)
                CodexEntryField.NICKNAMES -> current.copy(nicknames = text)
                CodexEntryField.REWARD -> current.copy(reward = text)
                CodexEntryField.PARTY_CLASS -> current.copy(party = current.party.copy(characterClass = text))
                CodexEntryField.PARTY_PLAYER -> current.copy(party = current.party.copy(player = text))
            }
    }

    // the number fields send what they hold, null for an emptied field - the lower bound waits for blur and save
    fun onNumberChanged(
        field: CodexEntryNumberField,
        value: Int?,
    ) {
        val current = fields.value ?: return
        val party = current.party
        val clamped = value?.coerceAtMost(field.range().last)
        val updated =
            when (field) {
                CodexEntryNumberField.PARTY_LEVEL -> party.copy(level = clamped)
                CodexEntryNumberField.PARTY_HP_MAX -> party.copy(hpMax = clamped)
            }
        fields.value = current.copy(party = updated)
    }

    fun onPartyLevelFromPlayerClicked() {
        val current = fields.value ?: return
        fields.value = current.copy(party = current.party.copy(level = playerCharacterLevel))
    }

    fun onNpcStatusChanged(status: NpcStatus) {
        fields.value = fields.value?.copy(npcStatus = status)
    }

    fun onNpcLifeChanged(lifeState: NpcLifeState) {
        fields.value = fields.value?.copy(npcLife = lifeState)
    }

    // only one party member can be "вы" - the repository clears the flag from the others on save
    fun onPartyOwnerChanged(isPlayerCharacter: Boolean) {
        val current = fields.value ?: return
        fields.value = current.copy(party = current.party.copy(isPlayerCharacter = isPlayerCharacter))
    }

    fun onPartyPresenceChanged(presence: PartyPresence) {
        val current = fields.value ?: return
        fields.value = current.copy(party = current.party.copy(presence = presence))
    }

    fun onQuestStatusChanged(status: QuestStatus) {
        fields.value = fields.value?.copy(questStatus = status)
    }

    fun onChipToggled(
        field: CodexEntryChipField,
        id: Long,
    ) {
        val current = fields.value ?: return
        fields.value =
            when (field) {
                CodexEntryChipField.QUEST_GIVER -> current.copy(questGiverId = current.questGiverId.toggled(id))
            }
    }

    fun onQuickAddOpened(field: CodexEntryChipField) {
        val current = fields.value ?: return
        fields.value = current.copy(quickAddField = field, quickAddDraft = "")
    }

    fun onQuickAddDraftChanged(text: String) {
        val current = fields.value ?: return
        if (current.quickAddField == null) return
        fields.value = current.copy(quickAddDraft = text)
    }

    fun onQuickAddCancelled() {
        val current = fields.value ?: return
        fields.value = current.copy(quickAddField = null, quickAddDraft = "")
    }

    // creates the minimal stub entity directly through the repository and selects it where the quick-add opened
    fun onQuickAddSaveClicked(
        scope: CoroutineScope,
        onCreated: suspend (String) -> Unit,
    ) {
        val current = fields.value ?: return
        val field = current.quickAddField
        val name = current.quickAddDraft.trim().capitalizeFirst()
        if (field == null || name.isBlank()) return
        // cleared synchronously, same as onSaveClicked - otherwise a second tap re-reads this still-open state
        fields.value = current.copy(quickAddField = null, quickAddDraft = "")
        scope.launch {
            when (field) {
                CodexEntryChipField.QUEST_GIVER -> {
                    val id = npcRepository.upsert(quickNpc(name))
                    fields.value = fields.value?.copy(questGiverId = id)
                }
            }
            onCreated(name)
        }
    }

    fun onPlaceFieldOpened(field: CodexEntryPlaceField) {
        fields.value = fields.value?.copy(placeSelectField = field)
    }

    fun onPlaceSelectClosed() {
        fields.value = fields.value?.copy(placeSelectField = null)
    }

    // a single field keeps the first id, the npc keeps them all - an empty list clears the field
    fun onPlacesChosen(ids: List<Long>) {
        val current = fields.value ?: return
        val field = current.placeSelectField ?: return
        fields.value = current.withPlaces(field, ids).copy(placeSelectField = null)
        recentPlaceIds.value = recentPlaceIds.value.withRecentPlaces(ids)
    }

    // the typed name becomes a new top level place that joins whatever was already checked
    fun onPlaceCreated(
        scope: CoroutineScope,
        name: String,
        checked: List<Long>,
    ) {
        val current = fields.value ?: return
        val field = current.placeSelectField
        val trimmed = name.trim().capitalizeFirst()
        if (field == null || trimmed.isBlank()) return
        fields.value = current.copy(placeSelectField = null)
        scope.launch {
            val id = locationRepository.upsert(quickLocation(trimmed))
            val ids = if (field.isMulti) checked + id else listOf(id)
            fields.value = fields.value?.withPlaces(field, ids)
            recentPlaceIds.value = recentPlaceIds.value.withRecentPlaces(listOf(id))
        }
    }

    // shrinking a big pick runs off the main thread - a second pick while one is working simply wins the race
    fun onPhotoPicked(
        scope: CoroutineScope,
        compressor: ImageCompressor,
        slot: PhotoSlot,
        rawBytes: ByteArray,
    ) {
        if (fields.value == null) return
        val session = baseline.value
        scope.launch {
            val draft = prepareCropDraftAsync(rawBytes, slot, compressor)
            // the form this pick came from may be closed or replaced by now
            if (baseline.value !== session) return@launch
            fields.value = fields.value?.copy(photoCrop = draft)
        }
    }

    fun onPhotoRecropRequested(slot: PhotoSlot) {
        val current = fields.value ?: return
        val stored = if (slot == PhotoSlot.COVER) current.cover else current.portrait
        fields.value = current.copy(photoCrop = stored?.let { recropDraft(it.display, slot) })
    }

    fun onPhotoCropConfirmed(
        scope: CoroutineScope,
        compressor: ImageCompressor,
        crop: CropRect,
    ) {
        val draft = fields.value?.photoCrop ?: return
        fields.value = fields.value?.copy(photoCrop = null)
        // an untouched window over the stored picture keeps it as it is instead of re-encoding it
        if (draft.keepsStored(crop)) return
        val session = baseline.value
        scope.launch {
            val photo = draft.encodeAsync(crop, compressor)
            if (baseline.value !== session) return@launch
            fields.value =
                fields.value?.let {
                    if (draft.slot == PhotoSlot.COVER) it.copy(cover = photo) else it.copy(portrait = photo)
                }
        }
    }

    fun onPhotoCropCancelled() {
        fields.value = fields.value?.copy(photoCrop = null)
    }

    fun onPhotoRemoved(slot: PhotoSlot) {
        fields.value =
            fields.value?.let {
                if (slot == PhotoSlot.COVER) it.copy(cover = null) else it.copy(portrait = null)
            }
    }

    fun onClosed() {
        fields.value = null
        baseline.value = null
        discardPrompt.value = false
    }

    /** closes right away when nothing changed, otherwise asks first - returns true when the form is gone */
    fun onCloseRequested(): Boolean {
        val current = fields.value
        val canClose = current == null || !current.hasUnsavedChanges(baseline.value)
        if (canClose) onClosed() else discardPrompt.value = true
        return canClose
    }

    fun onDiscardPromptDismissed() {
        discardPrompt.value = false
    }

    fun onSaveClicked(scope: CoroutineScope) {
        discardPrompt.value = false
        val current = fields.value ?: return
        if (current.name.isBlank()) return
        onClosed()
        scope.launch { persister.save(current) }
    }

    // one assignment per form-state field - a flat mapping, not real complexity
    @Suppress("LongMethod")
    @Composable
    fun buildState(
        npcs: List<Npc>,
        locations: List<Location>,
    ): CodexEntryFormState? {
        val current = fields.value ?: return null
        val isEditing = current.editingRef != null
        val npcLabels = npcStatusLabels()
        val lifeLabels = npcLifeLabels()
        val questLabels = questStatusLabels()
        val partyLabels = partyPresenceLabels()
        val party = current.party
        val places = PlaceTree(locations)
        val topLevelLabel = stringResource(Res.string.codex_place_top_level_caption)
        return CodexEntryFormState(
            overline =
                stringResource(
                    if (isEditing) Res.string.codex_entry_overline_edit else Res.string.codex_entry_overline_create,
                ),
            heading = if (isEditing) current.originalName else newEntryHeading(current.type),
            isEditing = isEditing,
            type = current.type,
            name = current.name,
            description = current.description,
            portrait = current.portrait,
            cover = current.cover,
            photoCrop = current.photoCrop,
            race = current.race,
            nicknames = current.nicknames,
            reward = current.reward,
            npcStatusOptions =
                NpcStatus.entries.map { status ->
                    FormChipOption(
                        status,
                        npcLabels.getValue(status),
                        status == current.npcStatus,
                        status.toStatusColor(),
                    )
                },
            npcLifeOptions =
                NpcLifeState.entries.map { life ->
                    FormChipOption(life, lifeLabels.getValue(life), life == current.npcLife, life.toStatusColor())
                },
            questStatusOptions =
                QuestStatus.entries.map { status ->
                    FormChipOption(
                        status,
                        questLabels.getValue(status),
                        status == current.questStatus,
                        status.toStatusColor(),
                    )
                },
            npcPlaces = current.npcLocationIds.mapNotNull { id -> places.find(id)?.toMentionChip() },
            locationChildren = current.locationChildIds.mapNotNull { id -> places.find(id)?.toMentionChip() },
            questPlace = places.find(current.questLocationId)?.toSelectedPlace(places, topLevelLabel),
            locationParent = places.find(current.locationParentId)?.toSelectedPlace(places, topLevelLabel),
            hasParentCandidates =
                locations.any { it.id != (current.editingRef as? EntityRef.Location)?.id },
            placeSelect =
                current.placeSelectField?.let { field ->
                    placeSelectState(
                        field = field,
                        title = placeFieldTitle(field),
                        locations = locations,
                        selectedIds = current.placeIds(field),
                        editing = current.placeEditing(),
                        recentIds = recentPlaceIds.value,
                    )
                },
            npcOptions =
                npcs.map { npc ->
                    FormChipOption(npc.id, npc.name, npc.id == current.questGiverId, npc.status.toStatusColor())
                },
            partyClass = party.characterClass,
            partyPlayer = party.player,
            partyIsPlayerCharacter = party.isPlayerCharacter,
            partyOwnerOptions =
                listOf(
                    FormChipOption(true, stringResource(Res.string.codex_entry_owner_me), party.isPlayerCharacter),
                    FormChipOption(false, stringResource(Res.string.codex_entry_owner_other), !party.isPlayerCharacter),
                ),
            partyLevel = party.level,
            partyHpMax = party.hpMax,
            // offered only for a member that is not the player character, and only when it would change the level
            partyLevelFromPlayer =
                playerCharacterLevel?.takeIf { !party.isPlayerCharacter && it != party.level },
            partyPresenceOptions =
                PartyPresence.entries.map { presence ->
                    FormChipOption(
                        presence,
                        partyLabels.getValue(presence),
                        presence == party.presence,
                        presence.toStatusColor(),
                    )
                },
            // blocked while a quick-add panel is open - otherwise Save can race its still-pending repository write
            canSave = current.name.isNotBlank() && current.quickAddField == null,
            saveLabel = saveLabel(current.name.isNotBlank(), isEditing),
            quickAddField = current.quickAddField,
            quickAddDraft = current.quickAddDraft,
            isDiscardPromptVisible = discardPrompt.value,
            hasUnsavedChanges = current.hasUnsavedChanges(baseline.value),
        )
    }
}

private fun CodexEntryNumberField.range(): IntRange =
    when (this) {
        CodexEntryNumberField.PARTY_LEVEL -> PartyStatRanges.level
        CodexEntryNumberField.PARTY_HP_MAX -> PartyStatRanges.hpMax
    }

@Composable
private fun newEntryHeading(type: EntityKind): String =
    stringResource(
        when (type) {
            EntityKind.PARTY -> Res.string.codex_entry_heading_new_party
            EntityKind.NPC -> Res.string.codex_entry_heading_new_npc
            EntityKind.QUEST -> Res.string.codex_entry_heading_new_quest
            EntityKind.LOCATION -> Res.string.codex_entry_heading_new_location
        },
    )

@Composable
private fun saveLabel(
    isValid: Boolean,
    isEditing: Boolean,
): String =
    stringResource(
        when {
            !isValid -> Res.string.codex_entry_save_label_invalid
            isEditing -> Res.string.codex_entry_save_label_edit
            else -> Res.string.codex_entry_save_label_create
        },
    )

private fun Long?.toggled(id: Long): Long? = if (this == id) null else id

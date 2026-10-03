package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.designsystem.LocationMentionColor
import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.npcStatusLabels
import com.lyrismet.incadent.core.entitysummary.questStatusLabels
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.slack.circuit.retained.rememberRetained
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_location
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_npc
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_quest
import dndplayerscodex.shared.generated.resources.codex_entry_overline_create
import dndplayerscodex.shared.generated.resources.codex_entry_overline_edit
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_create
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_edit
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_invalid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** the form's own editable-in-place fields - kept separate from [CodexEntryFormState] so typing survives rebuilds */
private data class CodexEntryFormFields(
    val editingRef: EntityRef?,
    val originalName: String = "",
    val type: CodexEntryType,
    val name: String = "",
    val description: String = "",
    val race: String = "",
    val faction: String = "",
    val npcStatus: NpcStatus = NpcStatus.NEUTRAL,
    val npcLocationId: Long? = null,
    val reward: String = "",
    val questGiverId: Long? = null,
    val questLocationId: Long? = null,
    val questStatus: QuestStatus = QuestStatus.ACTIVE,
    val locationType: String = "",
    val locationRegion: String = "",
)

class CodexEntryFormController private constructor(
    private val fields: MutableState<CodexEntryFormFields?>,
    private val npcRepository: NpcRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
) {
    companion object {
        @Composable
        fun rememberController(
            npcRepository: NpcRepository,
            questRepository: QuestRepository,
            locationRepository: LocationRepository,
        ): CodexEntryFormController {
            val fields = rememberRetained { mutableStateOf<CodexEntryFormFields?>(null) }
            return CodexEntryFormController(fields, npcRepository, questRepository, locationRepository)
        }
    }

    // defaultType mirrors the mockup's openNew(): the active codex tab pre-selects the form's type
    fun onAddEntryClicked(defaultType: CodexEntryType) {
        fields.value = CodexEntryFormFields(editingRef = null, type = defaultType)
    }

    fun onEditEntryRequested(
        ref: EntityRef,
        scope: CoroutineScope,
    ) {
        scope.launch {
            val loaded =
                when (ref) {
                    is EntityRef.Npc -> npcRepository.getById(ref.id)?.let { it.toFields(ref) }
                    is EntityRef.Quest -> questRepository.getById(ref.id)?.let { it.toFields(ref) }
                    is EntityRef.Location -> locationRepository.getById(ref.id)?.let { it.toFields(ref) }
                }
            if (loaded != null) fields.value = loaded
        }
    }

    fun onTypeChanged(type: CodexEntryType) {
        fields.value = fields.value?.copy(type = type)
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
                CodexEntryField.FACTION -> current.copy(faction = text)
                CodexEntryField.REWARD -> current.copy(reward = text)
                CodexEntryField.LOCATION_TYPE -> current.copy(locationType = text)
                CodexEntryField.LOCATION_REGION -> current.copy(locationRegion = text)
            }
    }

    fun onNpcStatusChanged(status: NpcStatus) {
        fields.value = fields.value?.copy(npcStatus = status)
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
                CodexEntryChipField.NPC_LOCATION -> current.copy(npcLocationId = current.npcLocationId.toggled(id))
                CodexEntryChipField.QUEST_GIVER -> current.copy(questGiverId = current.questGiverId.toggled(id))
                CodexEntryChipField.QUEST_LOCATION ->
                    current.copy(questLocationId = current.questLocationId.toggled(id))
            }
    }

    fun onClosed() {
        fields.value = null
    }

    fun onSaveClicked(scope: CoroutineScope) {
        val current = fields.value ?: return
        if (current.name.isBlank()) return
        fields.value = null
        scope.launch {
            when (current.type) {
                CodexEntryType.NPC ->
                    npcRepository.upsert(
                        Npc(
                            id = (current.editingRef as? EntityRef.Npc)?.id ?: 0,
                            name = current.name.trim().capitalizeFirst(),
                            status = current.npcStatus,
                            description = current.description.trim(),
                            locationId = current.npcLocationId,
                            race = current.race.trim().capitalizeFirst(),
                            faction = current.faction.trim().capitalizeFirst(),
                        ),
                    )

                CodexEntryType.QUEST ->
                    questRepository.upsert(
                        Quest(
                            id = (current.editingRef as? EntityRef.Quest)?.id ?: 0,
                            title = current.name.trim().capitalizeFirst(),
                            status = current.questStatus,
                            reward = current.reward.trim(),
                            givenByNpcId = current.questGiverId,
                            locationId = current.questLocationId,
                            description = current.description.trim(),
                        ),
                    )

                CodexEntryType.LOCATION ->
                    locationRepository.upsert(
                        Location(
                            id = (current.editingRef as? EntityRef.Location)?.id ?: 0,
                            name = current.name.trim().capitalizeFirst(),
                            type = current.locationType.trim().capitalizeFirst(),
                            description = current.description.trim(),
                            region = current.locationRegion.trim().capitalizeFirst(),
                        ),
                    )
            }
        }
    }

    @Composable
    fun buildState(
        npcs: List<Npc>,
        locations: List<Location>,
    ): CodexEntryFormState? {
        val current = fields.value ?: return null
        val isEditing = current.editingRef != null
        val npcLabels = npcStatusLabels()
        val questLabels = questStatusLabels()
        val selectedLocationId =
            if (current.type == CodexEntryType.NPC) current.npcLocationId else current.questLocationId
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
            race = current.race,
            faction = current.faction,
            reward = current.reward,
            locationType = current.locationType,
            locationRegion = current.locationRegion,
            npcStatusOptions =
                NpcStatus.entries.map { status ->
                    FormChipOption(
                        status,
                        npcLabels.getValue(status),
                        status == current.npcStatus,
                        status.toStatusColor(),
                    )
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
            locationOptions =
                locations.map { location ->
                    FormChipOption(
                        location.id,
                        location.name,
                        location.id == selectedLocationId,
                        LocationMentionColor,
                    )
                },
            npcOptions =
                npcs.map { npc ->
                    FormChipOption(npc.id, npc.name, npc.id == current.questGiverId, npc.status.toStatusColor())
                },
            canSave = current.name.isNotBlank(),
            saveLabel = saveLabel(current.name.isNotBlank(), isEditing),
        )
    }
}

@Composable
private fun newEntryHeading(type: CodexEntryType): String =
    stringResource(
        when (type) {
            CodexEntryType.NPC -> Res.string.codex_entry_heading_new_npc
            CodexEntryType.QUEST -> Res.string.codex_entry_heading_new_quest
            CodexEntryType.LOCATION -> Res.string.codex_entry_heading_new_location
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

private fun Npc.toFields(ref: EntityRef.Npc) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = name,
        type = CodexEntryType.NPC,
        name = name,
        description = description,
        race = race,
        faction = faction,
        npcStatus = status,
        npcLocationId = locationId,
    )

private fun Quest.toFields(ref: EntityRef.Quest) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = title,
        type = CodexEntryType.QUEST,
        name = title,
        description = description,
        reward = reward,
        questGiverId = givenByNpcId,
        questLocationId = locationId,
        questStatus = status,
    )

private fun Location.toFields(ref: EntityRef.Location) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = name,
        type = CodexEntryType.LOCATION,
        name = name,
        description = description,
        locationType = type,
        locationRegion = region,
    )

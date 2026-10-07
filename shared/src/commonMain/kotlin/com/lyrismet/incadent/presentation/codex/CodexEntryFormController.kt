package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.lyrismet.incadent.core.calc.CalcExpression
import com.lyrismet.incadent.core.designsystem.LocationMentionColor
import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.npcLifeLabels
import com.lyrismet.incadent.core.entitysummary.npcStatusLabels
import com.lyrismet.incadent.core.entitysummary.partyPresenceLabels
import com.lyrismet.incadent.core.entitysummary.questStatusLabels
import com.lyrismet.incadent.core.format.capitalizeFirst
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.PartyStatRanges
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.model.clampedToRanges
import com.lyrismet.incadent.domain.repository.LocationRepository
import com.lyrismet.incadent.domain.repository.NpcRepository
import com.lyrismet.incadent.domain.repository.PartyRepository
import com.lyrismet.incadent.domain.repository.QuestRepository
import com.slack.circuit.retained.rememberRetained
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_calculator_apply
import dndplayerscodex.shared.generated.resources.codex_calculator_overline
import dndplayerscodex.shared.generated.resources.codex_calculator_range_format
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_location
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_npc
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_party
import dndplayerscodex.shared.generated.resources.codex_entry_heading_new_quest
import dndplayerscodex.shared.generated.resources.codex_entry_label_armor_class
import dndplayerscodex.shared.generated.resources.codex_entry_label_hp_max
import dndplayerscodex.shared.generated.resources.codex_entry_label_initiative_bonus
import dndplayerscodex.shared.generated.resources.codex_entry_label_level
import dndplayerscodex.shared.generated.resources.codex_entry_overline_create
import dndplayerscodex.shared.generated.resources.codex_entry_overline_edit
import dndplayerscodex.shared.generated.resources.codex_entry_owner_me
import dndplayerscodex.shared.generated.resources.codex_entry_owner_other
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_create
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_edit
import dndplayerscodex.shared.generated.resources.codex_entry_save_label_invalid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

// defaults for a freshly added party member - the steppers start here, not at the range's floor
private const val NEW_PARTY_LEVEL = 1
private const val NEW_PARTY_HP_MAX = 10
private const val NEW_PARTY_ARMOR_CLASS = 10
private const val NEW_PARTY_INITIATIVE_BONUS = 0

/** the party-only part of the form, nested so the other entry types never see these fields */
private data class PartyFormFields(
    val isPlayerCharacter: Boolean = false,
    val player: String = "",
    val characterClass: String = "",
    val presence: PartyPresence = PartyPresence.IN,
    val level: Int = NEW_PARTY_LEVEL,
    val hpMax: Int = NEW_PARTY_HP_MAX,
    val hpCurrent: Int? = null,
    val armorClass: Int = NEW_PARTY_ARMOR_CLASS,
    val initiativeBonus: Int = NEW_PARTY_INITIATIVE_BONUS,
    val portraitUri: String? = null,
)

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
    val npcLife: NpcLifeState = NpcLifeState.ALIVE,
    val npcLocationId: Long? = null,
    val reward: String = "",
    val questGiverId: Long? = null,
    val questLocationId: Long? = null,
    val questStatus: QuestStatus = QuestStatus.ACTIVE,
    val locationType: String = "",
    val locationRegion: String = "",
    val party: PartyFormFields = PartyFormFields(),
    // null means no stepper's calculator is open - see onCalculatorOpened
    val calculatorField: CodexEntryNumberField? = null,
    val calculatorExpr: CalcExpression = CalcExpression(),
)

// one entry point per form action - splitting them across classes would only forward the same calls
@Suppress("TooManyFunctions")
class CodexEntryFormController private constructor(
    private val fields: MutableState<CodexEntryFormFields?>,
    private val npcRepository: NpcRepository,
    private val partyRepository: PartyRepository,
    private val questRepository: QuestRepository,
    private val locationRepository: LocationRepository,
) {
    companion object {
        @Composable
        fun rememberController(
            npcRepository: NpcRepository,
            partyRepository: PartyRepository,
            questRepository: QuestRepository,
            locationRepository: LocationRepository,
        ): CodexEntryFormController {
            val fields = rememberRetained { mutableStateOf<CodexEntryFormFields?>(null) }
            return CodexEntryFormController(fields, npcRepository, partyRepository, questRepository, locationRepository)
        }
    }

    // defaultType mirrors the mockup's openNew() - the active codex tab pre-selects the form's type
    fun onAddEntryClicked(
        defaultType: CodexEntryType,
        initialName: String = "",
    ) {
        fields.value = CodexEntryFormFields(editingRef = null, type = defaultType, name = initialName)
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
                CodexEntryField.PARTY_CLASS -> current.copy(party = current.party.copy(characterClass = text))
                CodexEntryField.PARTY_PLAYER -> current.copy(party = current.party.copy(player = text))
            }
    }

    // steppers send the value they show - the range is enforced here too, so a bad value never reaches the repository
    fun onNumberChanged(
        field: CodexEntryNumberField,
        value: Int,
    ) {
        val current = fields.value ?: return
        val party = current.party
        val clamped = value.coerceIn(field.range())
        val updated =
            when (field) {
                CodexEntryNumberField.PARTY_LEVEL -> party.copy(level = clamped)
                CodexEntryNumberField.PARTY_HP_MAX -> party.copy(hpMax = clamped)
                CodexEntryNumberField.PARTY_ARMOR_CLASS -> party.copy(armorClass = clamped)
                CodexEntryNumberField.PARTY_INITIATIVE_BONUS -> party.copy(initiativeBonus = clamped)
            }
        fields.value = current.copy(party = updated)
    }

    // only wired from fields whose range has no negative minimum - see FormStepper's onOpenCalculator
    fun onCalculatorOpened(field: CodexEntryNumberField) {
        val current = fields.value ?: return
        fields.value = current.copy(calculatorField = field, calculatorExpr = CalcExpression())
    }

    fun onCalculatorDigitPressed(digit: Int) {
        val current = fields.value ?: return
        if (current.calculatorField == null) return
        fields.value = current.copy(calculatorExpr = current.calculatorExpr.digit(digit))
    }

    fun onCalculatorBackspacePressed() {
        val current = fields.value ?: return
        if (current.calculatorField == null) return
        fields.value = current.copy(calculatorExpr = current.calculatorExpr.backspace())
    }

    fun onCalculatorClearPressed() {
        val current = fields.value ?: return
        if (current.calculatorField == null) return
        fields.value = current.copy(calculatorExpr = current.calculatorExpr.clear())
    }

    // reuses onNumberChanged's own clamping instead of duplicating the Math.max(min, Math.min(max, v)) here
    fun onCalculatorApplyClicked() {
        val current = fields.value ?: return
        val field = current.calculatorField
        if (field == null || current.calculatorExpr.raw.isBlank()) return
        onNumberChanged(field, current.calculatorExpr.value())
        fields.value = fields.value?.copy(calculatorField = null, calculatorExpr = CalcExpression())
    }

    fun onCalculatorClosed() {
        val current = fields.value ?: return
        fields.value = current.copy(calculatorField = null, calculatorExpr = CalcExpression())
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
                CodexEntryType.PARTY ->
                    partyRepository.upsert(current.toPartyMember().clampedToRanges())

                CodexEntryType.NPC ->
                    npcRepository.upsert(
                        Npc(
                            id = (current.editingRef as? EntityRef.Npc)?.id ?: 0,
                            name = current.name.trim().capitalizeFirst(),
                            status = current.npcStatus,
                            lifeState = current.npcLife,
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
        val selectedLocationId =
            if (current.type == CodexEntryType.NPC) current.npcLocationId else current.questLocationId
        val party = current.party
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
            partyArmorClass = party.armorClass,
            partyInitiativeBonus = party.initiativeBonus,
            partyPresenceOptions =
                PartyPresence.entries.map { presence ->
                    FormChipOption(
                        presence,
                        partyLabels.getValue(presence),
                        presence == party.presence,
                        presence.toStatusColor(),
                    )
                },
            canSave = current.name.isNotBlank(),
            saveLabel = saveLabel(current.name.isNotBlank(), isEditing),
            calculator = current.calculatorField?.let { field -> calculatorPadState(field, current.calculatorExpr) },
        )
    }
}

@Composable
private fun calculatorPadState(
    field: CodexEntryNumberField,
    expr: CalcExpression,
): CalculatorPadState {
    val range = field.range()
    return CalculatorPadState(
        field = field,
        overline = stringResource(Res.string.codex_calculator_overline),
        title = field.label(),
        expr = expr.display(),
        result = if (expr.raw.isBlank()) "0" else expr.value().toString(),
        rangeHint = stringResource(Res.string.codex_calculator_range_format, range.first, range.last),
        applyLabel = stringResource(Res.string.codex_calculator_apply),
        applyEnabled = expr.raw.isNotBlank(),
    )
}

private fun CodexEntryNumberField.range(): IntRange =
    when (this) {
        CodexEntryNumberField.PARTY_LEVEL -> PartyStatRanges.level
        CodexEntryNumberField.PARTY_HP_MAX -> PartyStatRanges.hpMax
        CodexEntryNumberField.PARTY_ARMOR_CLASS -> PartyStatRanges.armorClass
        CodexEntryNumberField.PARTY_INITIATIVE_BONUS -> PartyStatRanges.initiativeBonus
    }

@Composable
private fun CodexEntryNumberField.label(): String =
    stringResource(
        when (this) {
            CodexEntryNumberField.PARTY_LEVEL -> Res.string.codex_entry_label_level
            CodexEntryNumberField.PARTY_HP_MAX -> Res.string.codex_entry_label_hp_max
            CodexEntryNumberField.PARTY_ARMOR_CLASS -> Res.string.codex_entry_label_armor_class
            CodexEntryNumberField.PARTY_INITIATIVE_BONUS -> Res.string.codex_entry_label_initiative_bonus
        },
    )

@Composable
private fun newEntryHeading(type: CodexEntryType): String =
    stringResource(
        when (type) {
            CodexEntryType.PARTY -> Res.string.codex_entry_heading_new_party
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

private fun CodexEntryFormFields.toPartyMember(): PartyMember {
    val party = party
    return PartyMember(
        id = (editingRef as? EntityRef.Party)?.id ?: 0,
        name = name.trim().capitalizeFirst(),
        characterClass = party.characterClass.trim().capitalizeFirst(),
        race = race.trim().capitalizeFirst(),
        level = party.level,
        // the "вы" flag replaces the player name, so a player character never keeps one
        playerName = if (party.isPlayerCharacter) "" else party.player.trim().capitalizeFirst(),
        isPlayerCharacter = party.isPlayerCharacter,
        presence = party.presence,
        hpMax = party.hpMax,
        hpCurrent = party.hpCurrent ?: party.hpMax,
        armorClass = party.armorClass,
        initiativeBonus = party.initiativeBonus,
        description = description.trim(),
        portraitUri = party.portraitUri,
    )
}

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
        npcLife = lifeState,
        npcLocationId = locationId,
    )

private fun PartyMember.toFields(ref: EntityRef.Party) =
    CodexEntryFormFields(
        editingRef = ref,
        originalName = name,
        type = CodexEntryType.PARTY,
        name = name,
        description = description,
        race = race,
        party =
            PartyFormFields(
                isPlayerCharacter = isPlayerCharacter,
                player = playerName,
                characterClass = characterClass,
                presence = presence,
                level = level,
                hpMax = hpMax,
                hpCurrent = hpCurrent,
                armorClass = armorClass,
                initiativeBonus = initiativeBonus,
                portraitUri = portraitUri,
            ),
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

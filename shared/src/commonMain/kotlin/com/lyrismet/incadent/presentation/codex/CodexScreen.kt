package com.lyrismet.incadent.presentation.codex

import androidx.compose.ui.graphics.Color
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.PartyStatRanges
import com.lyrismet.incadent.domain.model.QuestStatus
import com.slack.circuit.runtime.CircuitUiEvent
import com.slack.circuit.runtime.CircuitUiState
import com.slack.circuit.runtime.screen.Screen
import kotlinx.serialization.Serializable

@Serializable
data object CodexScreen : Screen

enum class CodexTab {
    ALL,
    PARTY,
    NPC,
    QUEST,
    LOCATION,
}

data class CodexTabCounts(
    val party: Int = 0,
    val npc: Int = 0,
    val quest: Int = 0,
    val location: Int = 0,
)

/** pre-formatted for direct rendering - the Ui never touches a domain model or does formatting itself */
data class NpcCodexItem(
    val id: Long,
    val name: String,
    val initial: String,
    val isDead: Boolean,
    val lifeBadge: NpcLifeBadge?,
    val statusLabel: String,
    val statusColor: StatusColor,
    val subtitle: String,
)

/** the tag a dead npc's card shows next to its name - null for a living npc */
data class NpcLifeBadge(
    val label: String,
    val color: StatusColor,
)

/** the party-tab labels that the mapping composes into a card's subtitle and meta line */
data class PartyCardLabels(
    val presence: Map<PartyPresence, String>,
    val you: String,
    val playerPrefix: String,
    val armorClassShort: String,
    val hpShort: String,
)

data class PartyCodexItem(
    val id: Long,
    val name: String,
    val initial: String,
    val color: StatusColor,
    val presenceLabel: String,
    val presenceColor: StatusColor,
    val subtitle: String,
    val meta: String,
)

data class QuestGiverItem(
    val npcId: Long,
    val name: String,
    val isDead: Boolean,
    val color: StatusColor,
)

data class QuestCodexItem(
    val id: Long,
    val title: String,
    val statusLabel: String,
    val statusColor: StatusColor,
    val giver: QuestGiverItem?,
    val reward: String,
)

data class LocationCodexItem(
    val id: Long,
    val name: String,
    val subtitle: String,
)

data class CodexFilterOption<T>(
    val value: T?,
    val label: String,
    val count: Int,
    val dotColor: Color?,
)

/** the NPC tab's filter: a relation to the party, or the dead shortcut - the two axes are independent */
sealed interface NpcListFilter {
    data class Relation(
        val status: NpcStatus,
    ) : NpcListFilter

    data object Dead : NpcListFilter
}

enum class CodexEntryType { PARTY, NPC, QUEST, LOCATION }

/** pre-formatted create/edit sheet state - null fields/lists are simply unused for the current [type] */
data class CodexEntryFormState(
    val overline: String,
    val heading: String,
    val isEditing: Boolean,
    val type: CodexEntryType,
    val name: String = "",
    val description: String = "",
    val race: String = "",
    val faction: String = "",
    val reward: String = "",
    val locationType: String = "",
    val locationRegion: String = "",
    val npcStatusOptions: List<FormChipOption<NpcStatus>> = emptyList(),
    val npcLifeOptions: List<FormChipOption<NpcLifeState>> = emptyList(),
    val questStatusOptions: List<FormChipOption<QuestStatus>> = emptyList(),
    val locationOptions: List<FormChipOption<Long>> = emptyList(),
    val npcOptions: List<FormChipOption<Long>> = emptyList(),
    val partyClass: String = "",
    val partyPlayer: String = "",
    val partyIsPlayerCharacter: Boolean = false,
    val partyOwnerOptions: List<FormChipOption<Boolean>> = emptyList(),
    val partyLevel: Int = PartyStatRanges.level.first,
    val partyHpMax: Int = PartyStatRanges.hpMax.first,
    val partyArmorClass: Int = PartyStatRanges.armorClass.first,
    val partyInitiativeBonus: Int = 0,
    val partyPresenceOptions: List<FormChipOption<PartyPresence>> = emptyList(),
    val canSave: Boolean = false,
    val saveLabel: String = "",
)

enum class CodexEntryField {
    NAME,
    DESCRIPTION,
    RACE,
    FACTION,
    REWARD,
    LOCATION_TYPE,
    LOCATION_REGION,
    PARTY_CLASS,
    PARTY_PLAYER,
}

enum class CodexEntryNumberField { PARTY_LEVEL, PARTY_HP_MAX, PARTY_ARMOR_CLASS, PARTY_INITIATIVE_BONUS }

enum class CodexEntryChipField { NPC_LOCATION, QUEST_GIVER, QUEST_LOCATION }

/** the Codex screen shows at most one bottom sheet at a time - the form always wins over the entity view */
sealed interface CodexSheet {
    data class EntityView(
        val entity: EntitySummaryItem,
    ) : CodexSheet

    data class EntryForm(
        val form: CodexEntryFormState,
    ) : CodexSheet
}

data class CodexState(
    val activeTab: CodexTab = CodexTab.PARTY,
    val searchQuery: String = "",
    val tabCounts: CodexTabCounts = CodexTabCounts(),
    val party: List<PartyCodexItem> = emptyList(),
    val partyFilter: PartyPresence? = null,
    val partyFilterOptions: List<CodexFilterOption<PartyPresence>> = emptyList(),
    val npcs: List<NpcCodexItem> = emptyList(),
    val npcFilter: NpcListFilter? = null,
    val npcFilterOptions: List<CodexFilterOption<NpcListFilter>> = emptyList(),
    val quests: List<QuestCodexItem> = emptyList(),
    val questStatusFilter: QuestStatus? = null,
    val questFilterOptions: List<CodexFilterOption<QuestStatus>> = emptyList(),
    val locations: List<LocationCodexItem> = emptyList(),
    val activeSheet: CodexSheet? = null,
    val eventSink: (CodexEvent) -> Unit = {},
) : CircuitUiState

sealed interface CodexEvent : CircuitUiEvent {
    data class TabSelected(
        val tab: CodexTab,
    ) : CodexEvent

    data class SearchQueryChanged(
        val query: String,
    ) : CodexEvent

    data class PartyFilterSelected(
        val presence: PartyPresence?,
    ) : CodexEvent

    data class NpcFilterSelected(
        val filter: NpcListFilter?,
    ) : CodexEvent

    data class QuestStatusFilterSelected(
        val status: QuestStatus?,
    ) : CodexEvent

    data class EntityClicked(
        val ref: EntityRef,
    ) : CodexEvent

    data class NpcStatusSelected(
        val npcId: Long,
        val status: NpcStatus,
    ) : CodexEvent

    data class NpcLifeSelected(
        val npcId: Long,
        val lifeState: NpcLifeState,
    ) : CodexEvent

    data class PartyPresenceSelected(
        val partyId: Long,
        val presence: PartyPresence,
    ) : CodexEvent

    data class QuestStatusSelected(
        val questId: Long,
        val status: QuestStatus,
    ) : CodexEvent

    data class RelatedNoteClicked(
        val sessionNoteId: Long,
    ) : CodexEvent

    data object SheetDismissed : CodexEvent

    data object AddEntryClicked : CodexEvent

    data class EditEntryRequested(
        val ref: EntityRef,
    ) : CodexEvent

    data class EntityDeleteRequested(
        val ref: EntityRef,
    ) : CodexEvent

    data class EntryTypeChanged(
        val type: CodexEntryType,
    ) : CodexEvent

    data class EntryFieldChanged(
        val field: CodexEntryField,
        val text: String,
    ) : CodexEvent

    data class EntryNpcStatusChanged(
        val status: NpcStatus,
    ) : CodexEvent

    data class EntryNpcLifeChanged(
        val lifeState: NpcLifeState,
    ) : CodexEvent

    data class EntryPartyOwnerChanged(
        val isPlayerCharacter: Boolean,
    ) : CodexEvent

    data class EntryPartyPresenceChanged(
        val presence: PartyPresence,
    ) : CodexEvent

    data class EntryNumberChanged(
        val field: CodexEntryNumberField,
        val value: Int,
    ) : CodexEvent

    data class EntryQuestStatusChanged(
        val status: QuestStatus,
    ) : CodexEvent

    data class EntryChipToggled(
        val field: CodexEntryChipField,
        val id: Long,
    ) : CodexEvent

    data object EntryFormSaveClicked : CodexEvent

    data object EntryFormClosed : CodexEvent
}

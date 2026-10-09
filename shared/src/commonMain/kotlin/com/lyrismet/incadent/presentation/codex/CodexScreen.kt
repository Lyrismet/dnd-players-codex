package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.codexgroup.CodexGroup
import com.lyrismet.incadent.core.codexgroup.CodexGroupBy
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.FormChipOption
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySummaryItem
import com.lyrismet.incadent.core.quickedit.InlineEdit
import com.lyrismet.incadent.core.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.core.swipehint.SwipeHintDirection
import com.lyrismet.incadent.core.swipehint.SwipeHintTarget
import com.lyrismet.incadent.core.undo.UndoAction
import com.lyrismet.incadent.domain.model.EntityEditMode
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
    val portraitBase64: String?,
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
    val portraitBase64: String?,
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

/** one chip of the codex group row */
data class CodexFilterOption<T>(
    val value: T,
    val label: String,
)

enum class CodexEntryType { PARTY, NPC, QUEST, LOCATION }

/** pre-formatted create/edit sheet state - null fields/lists are simply unused for the current [type] */
data class CodexEntryFormState(
    val overline: String,
    val heading: String,
    val isEditing: Boolean,
    val type: CodexEntryType,
    val name: String = "",
    val description: String = "",
    // only rendered for party/npc types - see PortraitField in CodexEntryFormUi
    val portraitBase64: String? = null,
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
    // null when no stepper's calculator is open - the Ui overlays NumberPadSheet on the form when non-null
    val calculator: CalculatorPadState? = null,
    // the chip field whose "+ New X" quick-add input is open, if any - at most one at a time, see FormChipQuickAdd
    val quickAddField: CodexEntryChipField? = null,
    val quickAddDraft: String = "",
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

/**
 * the quick number-pad shown over a stepper (P2.9) - pre-formatted for [NumberPadSheet], see
 * Players Codex v6.dc.html's `pad` for the `purpose === 'form'` branch this mirrors
 */
data class CalculatorPadState(
    val field: CodexEntryNumberField,
    val overline: String,
    val title: String,
    val expr: String,
    val result: String,
    val rangeHint: String,
    val applyLabel: String,
    val applyEnabled: Boolean,
)

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

/** a button of the empty state that opens the entry form of [entryType], with [name] prefilled */
data class CodexEmptyAction(
    val label: String,
    val entryType: CodexEntryType,
    val name: String = "",
)

/** pre-resolved text of the list area's empty state - the Ui only renders it */
sealed interface CodexEmptyState {
    data object Hidden : CodexEmptyState

    data class Blank(
        val title: String,
        val text: String,
        val action: CodexEmptyAction,
    ) : CodexEmptyState

    data class NoMatch(
        val title: String,
        val text: String,
        val resetLabel: String,
        val create: CodexEmptyAction?,
    ) : CodexEmptyState
}

data class CodexState(
    val activeTab: CodexTab = CodexTab.PARTY,
    val searchQuery: String = "",
    val tabCounts: CodexTabCounts = CodexTabCounts(),
    val party: List<PartyCodexItem> = emptyList(),
    val npcs: List<CodexGroup<NpcCodexItem>> = emptyList(),
    val quests: List<CodexGroup<QuestCodexItem>> = emptyList(),
    val locations: List<CodexGroup<LocationCodexItem>> = emptyList(),
    val groupBy: CodexGroupBy? = null,
    val groupOptions: List<CodexFilterOption<CodexGroupBy>> = emptyList(),
    val emptyState: CodexEmptyState = CodexEmptyState.Hidden,
    val activeSheet: CodexSheet? = null,
    val editMode: EntityEditMode = EntityEditMode.QUICK,
    // the field editor open inside the entity card, if any - quick mode only
    val inlineEdit: InlineEdit? = null,
    val holdTipVisible: Boolean = false,
    val undoAction: UndoAction? = null,
    // the row the one-time swipe hint peeks on, and the direction captured when it started - see core/swipehint
    val swipeHintTarget: SwipeHintTarget<EntityRef>? = null,
    val swipeHintDirection: SwipeHintDirection = SwipeHintDirection.DELETE_ONLY,
    val toast: CodexToast? = null,
    val eventSink: (CodexEvent) -> Unit = {},
) : CircuitUiState

/** compared by identity, so repeating the same message restarts its timer - the quick-add "added" confirmation */
class CodexToast(
    val text: String,
)

sealed interface CodexEvent : CircuitUiEvent {
    data class TabSelected(
        val tab: CodexTab,
    ) : CodexEvent

    data class SearchQueryChanged(
        val query: String,
    ) : CodexEvent

    data class GroupBySelected(
        val choice: CodexGroupBy,
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
        val entryId: Long,
    ) : CodexEvent

    data object SheetDismissed : CodexEvent

    data object AddEntryClicked : CodexEvent

    data class EmptyActionClicked(
        val entryType: CodexEntryType,
        val name: String,
    ) : CodexEvent

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

    data class EntryPortraitPicked(
        val bytes: ByteArray,
    ) : CodexEvent

    data object EntryPortraitRemoved : CodexEvent

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

    data class CalculatorOpened(
        val field: CodexEntryNumberField,
    ) : CodexEvent

    data class CalculatorDigitPressed(
        val digit: Int,
    ) : CodexEvent

    data object CalculatorBackspacePressed : CodexEvent

    data object CalculatorClearPressed : CodexEvent

    data object CalculatorApplyClicked : CodexEvent

    data object CalculatorClosed : CodexEvent

    data class EntryQuickAddOpened(
        val field: CodexEntryChipField,
    ) : CodexEvent

    data class EntryQuickAddDraftChanged(
        val text: String,
    ) : CodexEvent

    data object EntryQuickAddSaveClicked : CodexEvent

    data object EntryQuickAddCancelled : CodexEvent

    data class QuickEdit(
        val event: QuickEditUiEvent,
    ) : CodexEvent

    data object UndoClicked : CodexEvent
}

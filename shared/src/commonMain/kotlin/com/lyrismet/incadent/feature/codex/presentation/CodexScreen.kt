package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.portrait.PhotoCropDraft
import com.lyrismet.incadent.core.preferences.EntityEditMode
import com.lyrismet.incadent.core.swipehint.SwipeHintDirection
import com.lyrismet.incadent.core.swipehint.SwipeHintTarget
import com.lyrismet.incadent.core.toast.ToastMessage
import com.lyrismet.incadent.core.undo.UndoAction
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.domain.model.Photo
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexGroup
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexGroupBy
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormEvent
import com.lyrismet.incadent.feature.codex.presentation.form.CodexEntryFormState
import com.lyrismet.incadent.feature.codex.presentation.form.PlaceSelectState
import com.lyrismet.incadent.feature.codex.presentation.quickedit.InlineEdit
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySummaryItem
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
    val portrait: Photo?,
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
    val hpShort: String,
)

data class PartyCodexItem(
    val id: Long,
    val name: String,
    val initial: String,
    val portrait: Photo?,
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
    // the path above the place, null when there is nothing to show
    val subtitle: String?,
    // the cover fills the list badge when the place has one
    val cover: Photo? = null,
)

/** one chip of the codex group row */
data class CodexFilterOption<T>(
    val value: T,
    val label: String,
)

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
    val entryType: EntityKind,
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

    /** the tab found nothing but other tabs did - a hint over the list of what they found */
    data class OtherSections(
        val message: String,
        val foundLabel: String,
        val count: Int,
        val create: CodexEmptyAction?,
    ) : CodexEmptyState
}

data class CodexState(
    // a finished campaign is read-only, so nothing in the header offers a new record
    val readOnly: Boolean = false,
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
    // the photo waiting in the crop window of the open card, quick mode only
    val photoCrop: PhotoCropDraft? = null,
    // the select sheet of a place field being held on the card, quick mode only
    val quickPlaceSelect: PlaceSelectState? = null,
    val holdTipVisible: Boolean = false,
    val undoAction: UndoAction? = null,
    // the row the one-time swipe hint peeks on, and the direction captured when it started - see core/swipehint
    val swipeHintTarget: SwipeHintTarget<EntityRef>? = null,
    val swipeHintDirection: SwipeHintDirection = SwipeHintDirection.DELETE_ONLY,
    val toast: ToastMessage? = null,
    // set while a delete waits on the "notes still mention this record" warning
    val deleteWarning: CodexDeleteWarning? = null,
    val eventSink: (CodexEvent) -> Unit = {},
) : CircuitUiState

/** the record a pending delete is about and how many notes in how many sessions still mention it */
data class CodexDeleteWarning(
    val ref: EntityRef,
    val name: String,
    val notes: Int,
    val sessions: Int,
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

    /** a tap or change on the entity card that opens over this screen */
    data class EntitySheet(
        val event: EntitySheetEvent,
    ) : CodexEvent

    data object AddEntryClicked : CodexEvent

    data class EmptyActionClicked(
        val entryType: EntityKind,
        val name: String,
    ) : CodexEvent

    data class EditEntryRequested(
        val ref: EntityRef,
    ) : CodexEvent

    data object EntityDeleteConfirmed : CodexEvent

    data object EntityDeleteCancelled : CodexEvent

    data class EntityDeleteRequested(
        val ref: EntityRef,
    ) : CodexEvent

    /** the card's own place select sheet asked for a new place, [checked] are the ids ticked so far */
    data class QuickPlaceCreated(
        val name: String,
        val checked: List<Long>,
    ) : CodexEvent

    /** a place section's header was tapped - the section folds or unfolds */
    data class PlaceSectionToggled(
        val placeId: Long,
    ) : CodexEvent

    data class QuickEdit(
        val event: QuickEditUiEvent,
    ) : CodexEvent

    data object UndoClicked : CodexEvent

    data object UndoFollowUpClicked : CodexEvent

    /** a change inside the create/edit form sheet that opens over this screen */
    data class EntryForm(
        val event: CodexEntryFormEvent,
    ) : CodexEvent
}

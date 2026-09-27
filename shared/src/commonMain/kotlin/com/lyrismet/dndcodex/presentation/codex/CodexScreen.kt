package com.lyrismet.dndcodex.presentation.codex

import androidx.compose.ui.graphics.Color
import com.lyrismet.dndcodex.core.designsystem.StatusColor
import com.lyrismet.dndcodex.core.designsystem.component.EntityRef
import com.lyrismet.dndcodex.core.designsystem.component.EntitySummaryItem
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.QuestStatus
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
    // todo stays 0 until the party domain model lands, see FEATURES.md section 4
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
    val statusLabel: String,
    val statusColor: StatusColor,
    val subtitle: String,
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

data class CodexState(
    val activeTab: CodexTab = CodexTab.PARTY,
    val searchQuery: String = "",
    val tabCounts: CodexTabCounts = CodexTabCounts(),
    val npcs: List<NpcCodexItem> = emptyList(),
    val npcStatusFilter: NpcStatus? = null,
    val npcFilterOptions: List<CodexFilterOption<NpcStatus>> = emptyList(),
    val quests: List<QuestCodexItem> = emptyList(),
    val questStatusFilter: QuestStatus? = null,
    val questFilterOptions: List<CodexFilterOption<QuestStatus>> = emptyList(),
    val locations: List<LocationCodexItem> = emptyList(),
    val locationFilterOptions: List<CodexFilterOption<Nothing>> = emptyList(),
    val selectedEntity: EntitySummaryItem? = null,
    val eventSink: (CodexEvent) -> Unit = {},
) : CircuitUiState

sealed interface CodexEvent : CircuitUiEvent {
    data class TabSelected(
        val tab: CodexTab,
    ) : CodexEvent

    data class SearchQueryChanged(
        val query: String,
    ) : CodexEvent

    data class NpcStatusFilterSelected(
        val status: NpcStatus?,
    ) : CodexEvent

    data class QuestStatusFilterSelected(
        val status: QuestStatus?,
    ) : CodexEvent

    data class EntityClicked(
        val ref: EntityRef,
    ) : CodexEvent

    data object SheetDismissed : CodexEvent

    data object AddEntryClicked : CodexEvent
}

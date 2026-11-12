package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.EmptyState
import com.lyrismet.incadent.core.designsystem.component.EmptyStateAction
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import com.lyrismet.incadent.core.designsystem.component.icons.entity.icon
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.presentation.item.EquipmentSheet
import com.lyrismet.incadent.feature.codex.presentation.item.ItemCodexItem
import com.lyrismet.incadent.feature.codex.presentation.item.ItemEvent
import com.lyrismet.incadent.feature.codex.presentation.item.ItemSheet
import com.lyrismet.incadent.feature.codex.presentation.item.PartyListMode
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.codex_search_empty_reset
import incadent.shared.generated.resources.codex_search_empty_text
import incadent.shared.generated.resources.codex_search_empty_title
import incadent.shared.generated.resources.items_add
import incadent.shared.generated.resources.items_empty
import incadent.shared.generated.resources.items_empty_text
import incadent.shared.generated.resources.items_segment_characters
import incadent.shared.generated.resources.items_segment_items
import org.jetbrains.compose.resources.stringResource

/** the characters or items switch at the top of the party tab */
@Composable
internal fun CodexPartyModeSwitch(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    AppSegmentedTabs(
        tabs =
            listOf(
                SegmentedTab(
                    PartyListMode.CHARACTERS,
                    stringResource(Res.string.items_segment_characters),
                    state.tabCounts.party,
                    EntityKind.PARTY.icon(),
                ),
                SegmentedTab(
                    PartyListMode.ITEMS,
                    stringResource(Res.string.items_segment_items),
                    state.items.total,
                    EntityKind.ITEM.icon(),
                ),
            ),
        selected = state.partyMode,
        onSelected = { state.eventSink(CodexEvent.PartyModeSelected(it)) },
        modifier = modifier,
        itemHeight = 34.dp,
    )
}

/** the items of the campaign, or what to do when there are none */
@Composable
internal fun CodexItemsList(
    state: CodexState,
    modifier: Modifier = Modifier,
) {
    val items = state.items
    when {
        items.rows.isNotEmpty() ->
            CodexLazyColumn(modifier) {
                items(items.rows, key = { it.id }) { row ->
                    ItemRow(row, state)
                }
            }
        items.total == 0 ->
            EmptyState(
                text = stringResource(Res.string.items_empty_text),
                title = stringResource(Res.string.items_empty),
                modifier = modifier,
                primaryAction =
                    if (state.readOnly) {
                        null
                    } else {
                        EmptyStateAction(stringResource(Res.string.items_add)) {
                            state.eventSink(CodexEvent.Items(ItemEvent.AddClicked))
                        }
                    },
            )
        else ->
            EmptyState(
                title = stringResource(Res.string.codex_search_empty_title),
                text = stringResource(Res.string.codex_search_empty_text, state.searchQuery.trim()),
                modifier = modifier,
                primaryAction =
                    EmptyStateAction(stringResource(Res.string.codex_search_empty_reset)) {
                        state.eventSink(CodexEvent.SearchQueryChanged(""))
                    },
            )
    }
}

@Composable
private fun ItemRow(
    item: ItemCodexItem,
    state: CodexState,
) {
    val ref = EntityRef.Item(item.id)
    CodexDeletableRow(
        ref = ref,
        eventSink = state.eventSink,
        swipeEditEnabled = state.swipeEditEnabled && !state.readOnly,
    ) {
        ItemCodexCard(
            item = item,
            // an archived campaign only reads, so the row opens the card instead of the editing sheet
            onClick = {
                state.eventSink(
                    if (state.readOnly) {
                        CodexEvent.EntitySheet(EntitySheetEvent.EntityClicked(ref))
                    } else {
                        CodexEvent.Items(ItemEvent.Opened(item.id))
                    },
                )
            },
        )
    }
}

/** the sheets of the items part of the screen - the one that adds and edits an item and the gear pick */
@Composable
internal fun CodexItemSheets(state: CodexState) {
    val send = { event: ItemEvent -> state.eventSink(CodexEvent.Items(event)) }
    state.items.sheet?.let { sheet ->
        ItemSheet(
            state = sheet,
            onEvent = send,
            onDelete = {
                state.eventSink(CodexEvent.EntityDeleteRequested(EntityRef.Item(sheet.draft.id)))
                send(ItemEvent.SheetDismissed)
            },
        )
    }
    state.items.equipment?.let { EquipmentSheet(it, send) }
}

package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.me_tab_items
import dndplayerscodex.shared.generated.resources.me_tab_resources
import dndplayerscodex.shared.generated.resources.me_tab_sheet
import dndplayerscodex.shared.generated.resources.me_tab_spells
import org.jetbrains.compose.resources.stringResource

private const val COLLAPSE_AFTER_PX = 120
private const val EXPAND_BELOW_PX = 12

/** the pinned header over the tab of the character, the header folds up while the sheet scrolls */
@Composable
internal fun MeCharacterUi(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    val folded = rememberFolded(listState)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp).animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedVisibility(!folded) { MeNameRow(character, onEvent) }
                MeHitPointsCard(character, onEvent)
                AnimatedVisibility(!folded) { MeTiles(character, onEvent) }
                MeTabs(character, onEvent)
            }
            MeTabContent(character, listState, onEvent, Modifier.weight(1f))
        }
        MeOverlays(character, onEvent)
    }
}

@Composable
private fun rememberFolded(listState: LazyListState): Boolean {
    var folded by remember { mutableStateOf(false) }
    // two thresholds, so the list growing when the header folds does not fold it back at once
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                if (index > 0 || offset > COLLAPSE_AFTER_PX) {
                    folded = true
                } else if (offset < EXPAND_BELOW_PX) {
                    folded = false
                }
            }
    }
    return folded
}

@Composable
private fun MeTabs(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    AppSegmentedTabs(
        tabs = character.tabs.map { SegmentedTab(it, tabLabel(it)) },
        selected = character.tab,
        onSelected = { onEvent(MeEvent.TabSelected(it)) },
        modifier = Modifier.fillMaxWidth(),
        itemHeight = 40.dp,
    )
}

@Composable
private fun tabLabel(tab: MeTab): String =
    stringResource(
        when (tab) {
            MeTab.SHEET -> Res.string.me_tab_sheet
            MeTab.SPELLS -> Res.string.me_tab_spells
            MeTab.RESOURCES -> Res.string.me_tab_resources
            MeTab.ITEMS -> Res.string.me_tab_items
        },
    )

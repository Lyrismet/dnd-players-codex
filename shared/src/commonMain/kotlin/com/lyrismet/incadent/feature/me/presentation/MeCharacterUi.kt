package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.component.AppSegmentedTabs
import com.lyrismet.incadent.core.designsystem.component.SegmentedTab
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.me_tab_items
import incadent.shared.generated.resources.me_tab_resources
import incadent.shared.generated.resources.me_tab_sheet
import incadent.shared.generated.resources.me_tab_spells
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val FOLD_DISTANCE = 150.dp
private val BLOCK_GAP = 12.dp

/** the header over the tab, its name and tiles fold up with the scroll gesture before the list moves */
@Composable
internal fun MeCharacterUi(
    character: MeCharacter,
    onEvent: (MeEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    val distance = with(LocalDensity.current) { FOLD_DISTANCE.toPx() }
    var folded by remember { mutableFloatStateOf(0f) }
    val connection = remember(distance) { FoldConnection(distance, { folded }, { folded = it }) }
    val fraction = (folded / distance).coerceIn(0f, 1f)
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().nestedScroll(connection)) {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 6.dp)) {
                Box(Modifier.foldable(fraction)) { MeNameRow(character, onEvent) }
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MeHitPointsCard(character, onEvent, Modifier.weight(1f))
                    if (!character.readOnly) {
                        MeRestButton({ onEvent(MeEvent.Rest(RestEvent.Opened)) }, Modifier.fillMaxHeight())
                    }
                }
                Box(Modifier.foldable(fraction)) { MeTiles(character, onEvent) }
                MeTabs(character, onEvent)
            }
            MeTabContent(character, listState, onEvent, Modifier.weight(1f))
        }
        MeOverlays(character, onEvent)
    }
}

/** takes the scroll up first while folding and gives it back last while unfolding, so the list never decides it */
private class FoldConnection(
    private val distance: Float,
    private val read: () -> Float,
    private val write: (Float) -> Unit,
) : NestedScrollConnection {
    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val before = read()
        if (available.y >= 0f || before >= distance) return Offset.Zero
        val after = (before - available.y).coerceAtMost(distance)
        write(after)
        return Offset(0f, before - after)
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val before = read()
        if (available.y <= 0f || before <= 0f) return Offset.Zero
        val after = (before - available.y).coerceAtLeast(0f)
        write(after)
        return Offset(0f, before - after)
    }
}

/** shrinks to nothing as [fraction] goes to 1, the gap under the block shrinks with it */
private fun Modifier.foldable(fraction: Float): Modifier =
    this.then(
        Modifier
            .clipToBounds()
            .alpha(1f - fraction)
            .layout { measurable, constraints ->
                val gap = BLOCK_GAP.roundToPx()
                val placeable = measurable.measure(constraints)
                val height = ((placeable.height + gap) * (1f - fraction)).roundToInt()
                layout(placeable.width, height) { placeable.placeRelative(0, 0) }
            },
    )

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

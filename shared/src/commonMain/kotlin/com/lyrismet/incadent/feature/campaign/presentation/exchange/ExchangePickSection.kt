package com.lyrismet.incadent.feature.campaign.presentation.exchange

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppSearchField
import com.lyrismet.incadent.core.designsystem.component.PillSize
import com.lyrismet.incadent.core.designsystem.component.SelectCheckbox
import com.lyrismet.incadent.core.designsystem.component.SelectablePill
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.exchange_items_all
import dndplayerscodex.shared.generated.resources.exchange_items_empty
import dndplayerscodex.shared.generated.resources.exchange_items_none
import dndplayerscodex.shared.generated.resources.exchange_items_search
import dndplayerscodex.shared.generated.resources.exchange_kind_npc
import dndplayerscodex.shared.generated.resources.exchange_kind_place
import dndplayerscodex.shared.generated.resources.exchange_kind_quest
import dndplayerscodex.shared.generated.resources.exchange_kind_species
import dndplayerscodex.shared.generated.resources.exchange_pick_footnote
import dndplayerscodex.shared.generated.resources.exchange_pull_npc_place
import dndplayerscodex.shared.generated.resources.exchange_pull_npc_species
import dndplayerscodex.shared.generated.resources.exchange_pull_parent
import dndplayerscodex.shared.generated.resources.exchange_pull_quest
import dndplayerscodex.shared.generated.resources.exchange_pull_session
import dndplayerscodex.shared.generated.resources.exchange_tab_format
import dndplayerscodex.shared.generated.resources.exchange_tab_npcs
import dndplayerscodex.shared.generated.resources.exchange_tab_places
import dndplayerscodex.shared.generated.resources.exchange_tab_quests
import dndplayerscodex.shared.generated.resources.exchange_tab_sessions
import dndplayerscodex.shared.generated.resources.exchange_tab_species
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** what the pick section draws, the same for an export and for an import */
data class ExchangePickView(
    val tabs: List<PickTabCount>,
    val tab: ExchangeTab,
    val query: String,
    val rows: List<PickRow>,
    val allShownTicked: Boolean,
)

fun ExchangePick.toView(): ExchangePickView {
    val rows = rows()
    return ExchangePickView(tabCounts(), tab, query, rows, rows.isNotEmpty() && rows.all { it.mark == PickMark.On })
}

sealed interface ExchangePickEvent {
    data class TabSelected(
        val tab: ExchangeTab,
    ) : ExchangePickEvent

    data class QueryChanged(
        val value: String,
    ) : ExchangePickEvent

    data class RowToggled(
        val id: Long,
    ) : ExchangePickEvent

    data object AllToggled : ExchangePickEvent
}

fun ExchangePick.reduce(event: ExchangePickEvent): ExchangePick =
    when (event) {
        is ExchangePickEvent.TabSelected -> withTab(event.tab)
        is ExchangePickEvent.QueryChanged -> withQuery(event.value)
        is ExchangePickEvent.RowToggled -> toggled(event.id)
        ExchangePickEvent.AllToggled -> allToggled()
    }

private fun ExchangeTab.label(): StringResource =
    when (this) {
        ExchangeTab.PLACES -> Res.string.exchange_tab_places
        ExchangeTab.NPCS -> Res.string.exchange_tab_npcs
        ExchangeTab.QUESTS -> Res.string.exchange_tab_quests
        ExchangeTab.SPECIES -> Res.string.exchange_tab_species
        ExchangeTab.SESSIONS -> Res.string.exchange_tab_sessions
    }

/** the tabs, the search with "select all", the rows card and the dashed-row footnote - shared by export and import */
@Composable
fun ExchangePickSection(
    view: ExchangePickView,
    onEvent: (ExchangePickEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PickTabs(view.tabs, view.tab, onEvent)
        AppSearchField(
            query = view.query,
            onQueryChange = { onEvent(ExchangePickEvent.QueryChanged(it)) },
            placeholder = stringResource(Res.string.exchange_items_search),
            modifier = Modifier.fillMaxWidth(),
            height = 44.dp,
            background = AppPalette.Background,
            trailingContent = {
                Text(
                    text =
                        stringResource(
                            if (view.allShownTicked) Res.string.exchange_items_none else Res.string.exchange_items_all,
                        ),
                    modifier =
                        Modifier
                            .clickable { onEvent(ExchangePickEvent.AllToggled) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                    color = AppPalette.GoldBright,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = AppFontSize.SmallPlus),
                )
            },
        )
        if (view.rows.isEmpty()) {
            Text(
                text = stringResource(Res.string.exchange_items_empty),
                color = AppPalette.TextTertiary,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .appCard(AppShapes.Card, AppPalette.Background, AppPalette.BorderSubtle)
                        .padding(horizontal = 12.dp),
            ) {
                view.rows.forEachIndexed { index, row ->
                    if (index > 0) AppDivider(color = AppPalette.SurfacePopover)
                    PickRowItem(row) { onEvent(ExchangePickEvent.RowToggled(row.id)) }
                }
            }
        }
        Text(
            text = stringResource(Res.string.exchange_pick_footnote),
            color = AppPalette.TextSecondary,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.CaptionPlus, lineHeight = 17.sp),
        )
    }
}

@Composable
private fun PickTabs(
    tabs: List<PickTabCount>,
    selected: ExchangeTab,
    onEvent: (ExchangePickEvent) -> Unit,
) {
    // the pills run to the sheet's edges, the first one still starts at the sheet's inset
    Row(
        modifier =
            Modifier
                .bleedHorizontally(
                    SHEET_INSET,
                ).horizontalScroll(rememberScrollState())
                .padding(horizontal = SHEET_INSET),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tabs.forEach { count ->
            SelectablePill(
                text =
                    stringResource(
                        Res.string.exchange_tab_format,
                        stringResource(count.tab.label()),
                        count.inFile,
                        count.total,
                    ),
                selected = count.tab == selected,
                onClick = { onEvent(ExchangePickEvent.TabSelected(count.tab)) },
                size = PillSize.Small,
                background = AppPalette.Surface,
                unselectedContent = AppPalette.TextDescription,
            )
        }
    }
}

private val SHEET_INSET = 20.dp

private fun Modifier.bleedHorizontally(inset: Dp): Modifier =
    layout { measurable, constraints ->
        val extra = inset.roundToPx()
        val placeable = measurable.measure(constraints.copy(minWidth = 0, maxWidth = constraints.maxWidth + extra * 2))
        layout(constraints.maxWidth, placeable.height) { placeable.place(-extra, 0) }
    }

@Composable
private fun PickRowItem(
    row: PickRow,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .clickable(onClick = onClick)
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PickCheckbox(row.mark)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.name,
                color = AppPalette.TextPrimary,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = AppFontSize.MediumPlus),
            )
            Text(
                text = rowSubtitle(row),
                color = AppPalette.TextSecondary,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.CaptionPlus),
            )
        }
    }
}

// a ticked record, a record that comes along (dashed ring and a grey tick) or an empty box
@Composable
private fun PickCheckbox(mark: PickMark) {
    when (mark) {
        PickMark.On -> SelectCheckbox(checked = true)
        PickMark.Off -> SelectCheckbox(checked = false)
        PickMark.Pulled ->
            Box(
                modifier =
                    Modifier
                        .size(
                            20.dp,
                        ).dashedBorder(AppPalette.TextSecondary, width = 1.5.dp, cornerRadius = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "✓",
                    fontSize = AppFontSize.Caption,
                    fontWeight = FontWeight.Bold,
                    color = AppPalette.TextSecondary,
                )
            }
    }
}

@Composable
private fun rowSubtitle(row: PickRow): String {
    row.reason?.let { return stringResource(it.kind.label(), it.source) }
    val kind =
        when (row.tab) {
            ExchangeTab.PLACES -> stringResource(Res.string.exchange_kind_place)
            ExchangeTab.NPCS -> stringResource(Res.string.exchange_kind_npc)
            ExchangeTab.QUESTS -> stringResource(Res.string.exchange_kind_quest)
            ExchangeTab.SPECIES -> stringResource(Res.string.exchange_kind_species)
            ExchangeTab.SESSIONS -> ""
        }
    return listOf(kind, row.subtitle).filter { it.isNotEmpty() }.joinToString(" · ")
}

private fun PullKind.label(): StringResource =
    when (this) {
        PullKind.NPC_PLACE -> Res.string.exchange_pull_npc_place
        PullKind.NPC_SPECIES -> Res.string.exchange_pull_npc_species
        PullKind.QUEST -> Res.string.exchange_pull_quest
        PullKind.SESSION -> Res.string.exchange_pull_session
        PullKind.PLACE_PARENT -> Res.string.exchange_pull_parent
    }

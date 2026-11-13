package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.rememberSparkClock
import com.lyrismet.incadent.core.designsystem.component.sparkFrame
import com.lyrismet.incadent.feature.combat.presentation.prep.AddButton
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_round_add_ally
import incadent.shared.generated.resources.combat_round_add_enemy
import org.jetbrains.compose.resources.stringResource
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

private const val DIMMED_ALPHA = 0.5f
private val FIELD_RADIUS = 12.dp
private const val SAVE_PIPS = 3
private const val HELD_TILT = -1f

/** the items in front of the rows in the list, the library counts places by them */
private const val HEADER_ITEMS = 1
private const val CURRENT_TINT = 0.1f
private const val CURRENT_EDGE = 0.5f

@Composable
internal fun RoundList(
    state: CombatRoundState,
    modifier: Modifier = Modifier,
) {
    val send = state.eventSink
    val listState = rememberLazyListState()
    val drag = rememberQueueDrag(state.rows.map { it.id })
    drag.rows = state.rows
    val reorderState =
        rememberReorderableLazyListState(listState) { from, to ->
            drag.move(
                from.index - HEADER_ITEMS,
                to.index - HEADER_ITEMS,
            )
        }
    val clock = if (state.rows.any { it.current }) rememberSparkClock() else null
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 12.dp, top = 2.dp, end = 12.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "queue-header") { QueueHeader(state) }
        items(drag.shown(state.rows), key = { it.id }) { row ->
            ReorderableItem(reorderState, key = row.id, enabled = state.queueEditing) { held ->
                RoundRowCard(
                    row = row,
                    editing = state.queueEditing,
                    held = held,
                    clock = clock,
                    send = send,
                    handle =
                        Modifier.draggableHandle(
                            onDragStarted = { drag.start(row.id) },
                            onDragStopped = { drag.end { id, index -> send(CombatRoundEvent.QueueMoved(id, index)) } },
                        ),
                    modifier = Modifier.graphicsLayer { rotationZ = if (held) HELD_TILT else 0f },
                )
            }
        }
        if (state.queueEditing) item(key = "queue-tip") { QueueEditTip() }
        if (!state.queueEditing) item(key = "queue-add") { ReinforcementButtons(send) }
    }
}

@Composable
private fun ReinforcementButtons(send: (CombatRoundEvent) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AddButton(
            text = stringResource(Res.string.combat_round_add_enemy),
            color = AppPalette.MaroonBright,
            modifier = Modifier.weight(1f),
        ) { send(CombatRoundEvent.ReinforcementOpened(ally = false)) }
        AddButton(
            text = stringResource(Res.string.combat_round_add_ally),
            color = AppPalette.EmeraldBright,
            modifier = Modifier.weight(1f),
        ) { send(CombatRoundEvent.ReinforcementOpened(ally = true)) }
    }
}

@Composable
@Suppress("LongParameterList")
private fun RoundRowCard(
    row: RoundRow,
    editing: Boolean,
    held: Boolean,
    clock: State<Float>?,
    send: (CombatRoundEvent) -> Unit,
    handle: Modifier,
    modifier: Modifier = Modifier,
) {
    val silver = AppPalette.TextSecondary
    val inner = if (held || row.current) null else silver.takeIf { row.selected }
    val spark = row.current && !editing
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .alpha(if (row.dimmed) DIMMED_ALPHA else 1f)
                .appCard(
                    shape = AppShapes.Field,
                    background = rowBackground(row, held),
                    border = rowBorder(row, held, editing),
                    onClick = { send(CombatRoundEvent.RowTapped(row.id)) },
                ).sparkFrame(FIELD_RADIUS, gold = spark, inner = inner, clock = clock.takeIf { spark })
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TurnBadge(row, editing) { send(CombatRoundEvent.QueueTileTapped(row.id)) }
        Column(modifier = Modifier.weight(1f)) {
            RoundRowTitle(row)
            if (!editing) RowBars(row)
            if (!editing && row.chips.isNotEmpty()) RowChips(row.chips)
        }
        if (editing) DragHandle(handle, held) else RowEnd(row, Modifier.width(84.dp))
    }
}

private fun rowBackground(
    row: RoundRow,
    held: Boolean,
): Color =
    when {
        row.current -> AppPalette.Gold.copy(alpha = CURRENT_TINT).compositeOver(AppPalette.SurfaceVariant)
        row.selected || held -> AppPalette.SurfacePopover
        else -> AppPalette.SurfaceVariant
    }

private fun rowBorder(
    row: RoundRow,
    held: Boolean,
    editing: Boolean,
): Color? =
    when {
        held -> AppPalette.TextSecondary
        row.current && editing -> AppPalette.Gold.copy(alpha = CURRENT_EDGE)
        row.current -> null
        else -> AppPalette.Border
    }

@Composable
private fun RowBars(row: RoundRow) {
    val hp = row.hp
    val wound = row.wound
    if (hp != null) {
        Bar(hp.ratio, RoundColors.hp(hp.level), Modifier.padding(top = 7.dp))
    }
    if (wound != null) {
        if (wound.successes != null && wound.failures != null) {
            SaveDots(wound.successes, wound.failures, Modifier.padding(top = 7.dp))
        } else {
            WoundPips(wound.pips, RoundColors.wound(wound.tone), Modifier.padding(top = 7.dp))
        }
    }
}

@Composable
private fun Bar(
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth().height(5.dp).appCard(AppShapes.Badge, AppPalette.SurfaceElevated, null)) {
        if (ratio > 0f) {
            Column(modifier = Modifier.weight(ratio).height(5.dp).background(color)) {}
        }
        if (ratio < 1f) Column(modifier = Modifier.weight(1f - ratio).height(5.dp)) {}
    }
}

@Composable
private fun WoundPips(
    filled: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(WoundScale.size) { index ->
            Column(
                modifier =
                    Modifier
                        .weight(1f)
                        .height(5.dp)
                        .appCard(AppShapes.Badge, if (index < filled) color else AppPalette.SurfaceElevated, null),
            ) {}
        }
    }
}

@Composable
private fun SaveDots(
    successes: Int,
    failures: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(SAVE_PIPS) {
            Dot(
                size = 9.dp,
                color =
                    if (it <
                        successes
                    ) {
                        RoundColors.Success
                    } else {
                        AppPalette.SurfaceElevated
                    },
            )
        }
        Column(modifier = Modifier.size(width = 8.dp, height = 1.dp)) {}
        repeat(SAVE_PIPS) {
            Dot(
                size = 9.dp,
                color =
                    if (it <
                        failures
                    ) {
                        RoundColors.Failure
                    } else {
                        AppPalette.SurfaceElevated
                    },
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun RowChips(chips: List<RowChip>) {
    FlowRow(
        modifier = Modifier.padding(top = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        chips.forEach { chip ->
            val (fg, bg) = RoundColors.chip(chip.kind)
            Text(
                text = chip.label,
                color = fg,
                fontSize = AppFontSize.Tiny,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.appCard(AppShapes.Badge, bg, null).padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun RowEnd(
    row: RoundRow,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
        row.hp?.let { hp ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hp.temp > 0) {
                    Text(
                        "+${hp.temp}",
                        color = AppPalette.Azure,
                        fontSize = AppFontSize.Small,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                }
                Text(
                    hp.current,
                    color = if (hp.level == HpLevel.ZERO) AppPalette.TextTertiary else AppPalette.TextPrimary,
                    fontSize = AppFontSize.Heading,
                    fontWeight = FontWeight.Bold,
                )
                Text("/${hp.max}", color = AppPalette.TextTertiary, fontSize = AppFontSize.Body)
            }
        }
        row.wound?.let {
            Text(
                it.label,
                color = RoundColors.wound(it.tone),
                fontSize = AppFontSize.SmallPlus,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
        if (row.caption.isNotEmpty()) {
            Text(row.caption, color = AppPalette.TextSecondary, fontSize = AppFontSize.Caption, maxLines = 1)
        }
    }
}

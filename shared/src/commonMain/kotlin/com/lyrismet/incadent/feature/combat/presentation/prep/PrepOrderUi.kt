package com.lyrismet.incadent.feature.combat.presentation.prep

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppLineHeight
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.centeredLine
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppHelpHint
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.ButtonVariant
import com.lyrismet.incadent.core.designsystem.component.Dot
import com.lyrismet.incadent.core.designsystem.component.HelpMarker
import com.lyrismet.incadent.core.designsystem.component.HelpRow
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.feature.combat.domain.round.QueueSlot
import com.lyrismet.incadent.feature.combat.domain.round.QueueStatus
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_list_reset
import incadent.shared.generated.resources.combat_prep_order_header
import incadent.shared.generated.resources.combat_prep_order_hint
import incadent.shared.generated.resources.combat_prep_queue_goto
import incadent.shared.generated.resources.combat_prep_queue_help_body
import incadent.shared.generated.resources.combat_prep_queue_help_clear
import incadent.shared.generated.resources.combat_prep_queue_help_first
import incadent.shared.generated.resources.combat_prep_queue_help_rest
import incadent.shared.generated.resources.combat_prep_queue_help_title
import incadent.shared.generated.resources.combat_prep_queue_partial
import incadent.shared.generated.resources.combat_prep_queue_ready
import incadent.shared.generated.resources.combat_prep_queue_unnamed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private val NumberShape = CircleShape
private val HintArea = 32.dp
private val HINT_PULL = 8.dp

/** how far under the header the scroll goes, enough to show the hint and a few rows */
private val ORDER_REACH = 300.dp
private const val LIT_FILL = 0.14f
private const val LIT_EDGE = 0.55f
private const val LIT_MILLIS = 400

/** the row takes [by] less room below it than it draws, so the next row sits closer */
private fun Modifier.pullUp(by: Dp): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, (placeable.height - by.roundToPx()).coerceAtLeast(0)) { placeable.place(0, 0) }
    }

/** where the third step is on the screen: the status line scrolls to it and the header lights up for a moment */
@OptIn(ExperimentalFoundationApi::class)
internal class OrderFocus(
    private val scope: CoroutineScope,
    private val reach: Float,
) {
    val requester = BringIntoViewRequester()
    var size by mutableStateOf(IntSize.Zero)
    var highlighted by mutableStateOf(false)
        private set

    fun show() {
        scope.launch {
            requester.bringIntoView(Rect(0f, 0f, size.width.toFloat(), size.height + reach))
            highlighted = true
            delay(HIGHLIGHT_MILLIS)
            highlighted = false
        }
    }
}

private const val HIGHLIGHT_MILLIS = 1000L

@Composable
internal fun rememberOrderFocus(): OrderFocus {
    val scope = rememberCoroutineScope()
    val reach = with(LocalDensity.current) { ORDER_REACH.toPx() }
    return remember(scope) { OrderFocus(scope, reach) }
}

/** the "?" that explains how the queue is set, it sits in the third step and in the status line */
@Composable
internal fun QueueHelpHint(
    modifier: Modifier = Modifier,
    tapArea: Dp = HintArea,
    mark: (@Composable () -> Unit)? = null,
) {
    AppHelpHint(
        title = stringResource(Res.string.combat_prep_queue_help_title),
        body = stringResource(Res.string.combat_prep_queue_help_body),
        modifier = modifier,
        tapArea = tapArea,
        mark = mark,
        rows =
            listOf(
                HelpRow(HelpMarker.Filled, stringResource(Res.string.combat_prep_queue_help_first)),
                HelpRow(HelpMarker.Empty, stringResource(Res.string.combat_prep_queue_help_clear)),
                HelpRow(HelpMarker.Double, stringResource(Res.string.combat_prep_queue_help_rest)),
            ),
    )
}

/** the third step: tap the participants in the order the master names them */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun PrepOrderSection(
    rows: List<PrepOrderRow>,
    focus: OrderFocus,
    onEvent: (CombatPrepEvent) -> Unit,
) {
    val fill by animateColorAsState(
        if (focus.highlighted) AppPalette.Gold.copy(alpha = LIT_FILL) else Color.Transparent,
        tween(LIT_MILLIS),
    )
    val edge by animateColorAsState(
        if (focus.highlighted) AppPalette.Gold.copy(alpha = LIT_EDGE) else Color.Transparent,
        tween(LIT_MILLIS),
    )
    val litShape = RoundedCornerShape(8.dp)
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .bringIntoViewRequester(focus.requester)
                .onSizeChanged { focus.size = it }
                .defaultMinSize(minHeight = 32.dp)
                .background(fill, litShape)
                .border(1.dp, edge, litShape)
                .padding(start = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionOverline(
            text = stringResource(Res.string.combat_prep_order_header),
            color = AppPalette.TextTertiary,
            style = OverlineStyle.Field,
            modifier = Modifier.weight(1f),
        )
        AppButton(
            text = stringResource(Res.string.combat_list_reset),
            onClick = { onEvent(CombatPrepEvent.OrderReset) },
            tone = ButtonTone.Neutral,
            variant = ButtonVariant.Ghost,
        )
    }
    Row(modifier = Modifier.fillMaxWidth().pullUp(HINT_PULL), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.combat_prep_order_hint),
            color = AppPalette.TextSecondary,
            fontSize = AppFontSize.SmallPlus,
            lineHeight = AppLineHeight.Row,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        QueueHelpHint()
    }
    rows.forEach { row -> OrderRow(row) { onEvent(CombatPrepEvent.OrderTapped(row.memberId)) } }
}

@Composable
private fun OrderRow(
    row: PrepOrderRow,
    onClick: () -> Unit,
) {
    val has = row.slot is QueueSlot.Named
    val dot =
        when (row.side) {
            OrderSide.PARTY -> AppPalette.Azure
            OrderSide.ALLY -> AppPalette.Emerald
            OrderSide.ENEMY -> AppPalette.Enemy
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Field,
                    background = AppPalette.SurfaceVariant,
                    border = if (has) AppPalette.Gold.copy(alpha = 0.35f) else dot.copy(alpha = 0.25f),
                    onClick = onClick,
                ).defaultMinSize(minHeight = 60.dp)
                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) { Dot(size = 8.dp, color = dot) }
        PrepRowTitle(row.name, row.sub, Modifier.weight(1f))
        OrderNumber(row.slot)
    }
}

@Composable
private fun OrderNumber(slot: QueueSlot) {
    val base = Modifier.size(44.dp)
    val named = slot is QueueSlot.Named
    Box(
        modifier =
            if (named) {
                base.background(AppPalette.Gold, NumberShape).border(1.5.dp, AppPalette.Gold, NumberShape)
            } else {
                base
                    .dashedBorder(
                        AppPalette.RadioIdle,
                        width = 1.5.dp,
                        cornerRadius = 22.dp,
                        dashLength = 3.dp,
                        dashGap = 3.dp,
                    )
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (named) slot.number.toString() else "?${slot.number}",
            color = if (named) AppPalette.Background else AppPalette.TextTertiary,
            fontSize = if (named) AppFontSize.Heading else AppFontSize.Medium,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** the bar over the tab bar: the state of the queue and the way into the fight */
@Composable
internal fun PrepStartBar(
    start: PrepStart,
    queue: QueueStatus?,
    total: Int,
    onQueueClick: () -> Unit,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().background(AppPalette.SurfaceFooter)) {
        AppDivider(color = AppPalette.BorderSubtle)
        Column(modifier = Modifier.padding(start = 16.dp, top = 10.dp, end = 16.dp, bottom = 12.dp)) {
            if (queue != null) QueueLine(queue, total, onQueueClick)
            AppButton(
                text = start.label,
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                variant = ButtonVariant.Solid,
                size = ButtonSize.Primary,
                enabled = start.ready,
            )
        }
    }
}

/** one line of fixed height, so the button never jumps when the queue gets its names */
@Composable
private fun QueueLine(
    queue: QueueStatus,
    total: Int,
    onClick: () -> Unit,
) {
    val done = queue == QueueStatus.Complete
    val text =
        when (queue) {
            QueueStatus.Unnamed -> stringResource(Res.string.combat_prep_queue_unnamed)
            is QueueStatus.Partial ->
                stringResource(
                    Res.string.combat_prep_queue_partial,
                    total - queue.unplaced,
                    total,
                )
            QueueStatus.Complete -> stringResource(Res.string.combat_prep_queue_ready)
        }
    Row(
        modifier = Modifier.fillMaxWidth().height(QUEUE_LINE_HEIGHT).padding(start = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f).fillMaxHeight().clickable(enabled = !done, onClick = onClick),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            QueueMark(done)
            Text(
                text = text,
                color = AppPalette.TextDescription,
                fontSize = AppFontSize.BodyPlus,
                modifier = Modifier.weight(1f),
            )
            if (!done) {
                Text(
                    text = stringResource(Res.string.combat_prep_queue_goto),
                    color = AppPalette.GoldBright,
                    fontSize = AppFontSize.SmallPlus,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        QueueHelpHint(tapArea = HELP_CIRCLE) { HelpCircle() }
    }
}

/** the dashed "?" of a queue that is not complete, a gold check once it is */
@Composable
private fun QueueMark(done: Boolean) {
    val shape = RoundedCornerShape(6.dp)
    Box(
        modifier =
            Modifier
                .defaultMinSize(minWidth = 18.dp, minHeight = 18.dp)
                .then(
                    if (done) {
                        Modifier
                            .background(
                                AppPalette.Gold.copy(alpha = MARK_FILL),
                                shape,
                            ).border(1.5.dp, AppPalette.Gold.copy(alpha = MARK_EDGE), shape)
                    } else {
                        Modifier.dashedBorder(
                            AppPalette.TextSecondary,
                            width = 1.5.dp,
                            cornerRadius = 6.dp,
                            dashLength = 3.dp,
                            dashGap = 2.dp,
                        )
                    },
                ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (done) "✓" else "?",
            color = if (done) AppPalette.GoldBright else AppPalette.TextDescription,
            fontSize = AppFontSize.Tiny,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall.centeredLine(),
        )
    }
}

/** the round "?" of the status line, bigger than the one in the third step */
@Composable
private fun HelpCircle() {
    Box(
        modifier = Modifier.size(HELP_CIRCLE).border(1.dp, AppPalette.BorderHover, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "?",
            color = AppPalette.TextDescription,
            fontSize = AppFontSize.Body,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall.centeredLine(),
        )
    }
}

private val QUEUE_LINE_HEIGHT = 52.dp
private val HELP_CIRCLE = 36.dp
private const val MARK_FILL = 0.12f
private const val MARK_EDGE = 0.55f

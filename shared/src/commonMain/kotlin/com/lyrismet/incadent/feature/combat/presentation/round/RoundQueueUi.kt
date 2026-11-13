package com.lyrismet.incadent.feature.combat.presentation.round

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.dashedBorder
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.combat_queue_edit
import incadent.shared.generated.resources.combat_queue_edit_done
import incadent.shared.generated.resources.combat_queue_edit_handle
import incadent.shared.generated.resources.combat_queue_edit_hint
import incadent.shared.generated.resources.combat_queue_edit_tip
import incadent.shared.generated.resources.combat_queue_edit_title
import incadent.shared.generated.resources.combat_queue_title
import incadent.shared.generated.resources.combat_queue_title_unnamed
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val SETTLE_TIMEOUT_MILLIS = 1000L
private const val TILE_TINT = 0.12f
private val EDIT_ICON_GAP = 8.dp
private val EDIT_ICON_SIZE = 16.dp
private const val HANDLE_BARS = 3
private val HANDLE_SIZE = 44.dp
private val HANDLE_GAP = 4.dp
private val HANDLE_BAR_WIDTH = 18.dp
private val HANDLE_BAR_HEIGHT = 2.dp

/** numerals and labels in a fixed box sit on its middle instead of leaving room for the descent of the font */
internal val CenteredLine =
    LineHeightStyle(alignment = LineHeightStyle.Alignment.Center, trim = LineHeightStyle.Trim.Both)

/** the drag of the queue and what keeps it going: the shown order until the saved one comes back */
@Composable
internal fun rememberQueueDrag(rowIds: List<Long>): QueueDrag {
    val drag = remember { QueueDrag() }
    LaunchedEffect(drag.settling, rowIds) {
        drag.settle(rowIds)
        if (drag.settling) {
            delay(SETTLE_TIMEOUT_MILLIS)
            drag.cancel()
        }
    }
    return drag
}

/** the title of the queue and the way in and out of its edit */
@Composable
internal fun QueueHeader(state: CombatRoundState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionOverline(
            text =
                if (state.queueUnnamed) {
                    stringResource(Res.string.combat_queue_title_unnamed)
                } else {
                    stringResource(Res.string.combat_queue_title, state.rows.size)
                },
            color = AppPalette.TextTertiary,
            style = OverlineStyle.Field,
            modifier = Modifier.weight(1f),
        )
        if (!state.queueEditing) {
            Row(
                modifier =
                    Modifier
                        .clickable {
                            state.eventSink(CombatRoundEvent.QueueEditStarted)
                        }.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(EDIT_ICON_GAP),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    AppIcons.Pencil,
                    contentDescription = null,
                    tint = AppPalette.Gold,
                    modifier = Modifier.size(EDIT_ICON_SIZE),
                )
                Text(
                    text = stringResource(Res.string.combat_queue_edit),
                    color = AppPalette.Gold,
                    fontSize = AppFontSize.MediumPlus,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/** the three bars a row is held by in the edit of the queue, a 44dp touch target like the mockup */
@Composable
internal fun DragHandle(
    modifier: Modifier,
    held: Boolean,
) {
    val description = stringResource(Res.string.combat_queue_edit_handle)
    Column(
        modifier = modifier.size(HANDLE_SIZE).semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(HANDLE_GAP, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(HANDLE_BARS) {
            Box(
                Modifier
                    .size(width = HANDLE_BAR_WIDTH, height = HANDLE_BAR_HEIGHT)
                    .background(
                        if (held) AppPalette.TextDescription else AppPalette.TextTertiary,
                        RoundedCornerShape(1.dp),
                    ),
            )
        }
    }
}

/** the number of the place of a row: a tap passes the turn in the edit or opens the edit on a "?N" */
@Composable
internal fun TurnBadge(
    row: RoundRow,
    editing: Boolean,
    onClick: () -> Unit,
) {
    PlaceTile(row.badge, row.current, row.pending, if (editing || row.pending) onClick else null)
}

/** the tile of a place, shared by the rows and the turn card: a temporary place is dashed, the mover's is gold */
@Composable
internal fun PlaceTile(
    badge: String,
    current: Boolean,
    pending: Boolean,
    onClick: (() -> Unit)?,
) {
    val shape = AppShapes.Control
    val look =
        when {
            current && pending ->
                Modifier
                    .background(AppPalette.Gold.copy(alpha = TILE_TINT), shape)
                    .dashedBorder(
                        AppPalette.Gold,
                        width = 1.5.dp,
                        cornerRadius = 10.dp,
                        dashLength = 3.dp,
                        dashGap = 3.dp,
                    )
            pending ->
                Modifier.dashedBorder(
                    AppPalette.RadioIdle,
                    cornerRadius = 10.dp,
                    dashLength = 3.dp,
                    dashGap = 3.dp,
                )
            current -> Modifier.background(AppPalette.Gold, shape)
            else -> Modifier.background(AppPalette.BadgeFill, shape)
        }
    Box(
        modifier =
            Modifier.size(36.dp).then(look).then(
                if (onClick !=
                    null
                ) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = badge,
            color =
                when {
                    current && pending -> AppPalette.Gold
                    current -> AppPalette.Background
                    pending -> AppPalette.TextTertiary
                    else -> AppPalette.TextDescription
                },
            fontSize = AppFontSize.MediumPlus,
            fontWeight = FontWeight.Bold,
            lineHeight = AppFontSize.MediumPlus,
            style = TextStyle(lineHeightStyle = CenteredLine),
        )
    }
}

/** stands in for the turn card while the queue is edited: says so and holds the way out */
@Composable
internal fun QueueEditCard(
    state: CombatRoundState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(AppShapes.Card, AppPalette.Gold.copy(alpha = 0.08f), AppPalette.Gold.copy(alpha = 0.5f))
                .padding(start = 14.dp, top = 6.dp, end = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.combat_queue_edit_title),
                color = AppPalette.TextHeading,
                fontSize = AppFontSize.MediumPlus,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(Res.string.combat_queue_edit_hint, state.currentName),
                color = AppPalette.TextSecondary,
                fontSize = AppFontSize.Small,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            modifier =
                Modifier
                    .height(44.dp)
                    .background(AppPalette.Gold, RoundedCornerShape(11.dp))
                    .clickable { state.eventSink(CombatRoundEvent.QueueEditFinished) }
                    .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.combat_queue_edit_done),
                color = AppPalette.Background,
                fontSize = AppFontSize.Medium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/** the line under the edited queue that says how to move a row */
@Composable
internal fun QueueEditTip() {
    Text(
        text = stringResource(Res.string.combat_queue_edit_tip),
        color = AppPalette.TextTertiary,
        fontSize = AppFontSize.Small,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    )
}

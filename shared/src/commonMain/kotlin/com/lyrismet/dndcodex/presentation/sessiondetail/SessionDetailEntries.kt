package com.lyrismet.dndcodex.presentation.sessiondetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.MentionChip
import com.lyrismet.dndcodex.core.designsystem.component.appCard
import com.lyrismet.dndcodex.core.designsystem.component.icons.AppIcons
import com.lyrismet.dndcodex.core.entitysummary.EntityRef
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_delete
import dndplayerscodex.shared.generated.resources.action_edit
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

private const val ENTRY_COLLAPSE_ANIMATION_DURATION_MS = 220
private const val ENTRY_REMOVAL_GRACE_MS = 1000L

// top and bottom edges converge on the center as the row collapses, Gmail-delete-style, not a one-sided slide
private fun entryCollapseExit() =
    shrinkVertically(
        animationSpec = tween(ENTRY_COLLAPSE_ANIMATION_DURATION_MS),
        shrinkTowards = Alignment.CenterVertically,
    ) + fadeOut(animationSpec = tween(ENTRY_COLLAPSE_ANIMATION_DURATION_MS))

// tapping a note selects it and reveals its actions underneath
@Composable
internal fun SessionEntryRow(
    entry: SessionEntryItem,
    isSelected: Boolean,
    isEditing: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMentionClick: (EntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    // collapses in place first, Gmail-style, instead of just vanishing once the entry is actually deleted
    var isRemoving by remember { mutableStateOf(false) }
    LaunchedEffect(isRemoving) {
        if (isRemoving) {
            delay(ENTRY_COLLAPSE_ANIMATION_DURATION_MS.toLong())
            onDeleteClick()
            // still composed after the grace period means nothing was deleted, so bring the entry back
            delay(ENTRY_REMOVAL_GRACE_MS)
            isRemoving = false
        }
    }

    AnimatedVisibility(
        visible = !isRemoving,
        enter = EnterTransition.None,
        exit = entryCollapseExit(),
        modifier = modifier,
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                entry.timeLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
                color = AppPalette.TextTertiary,
                modifier = Modifier.width(38.dp).padding(top = 4.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SessionEntryBody(
                    segments = entry.segments,
                    onMentionClick = onMentionClick,
                    modifier = Modifier.selectionHighlight(isSelected, isEditing).clickable(onClick = onClick),
                )
                if (isSelected) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        EntryActionButton(
                            glyph = null,
                            label = stringResource(Res.string.action_edit),
                            foreground = AppPalette.GoldBright,
                            background = AppPalette.Gold.copy(alpha = 0.1f),
                            border = AppPalette.Gold.copy(alpha = 0.4f),
                            onClick = onEditClick,
                        )
                        EntryActionButton(
                            glyph = "✕",
                            label = stringResource(Res.string.action_delete),
                            foreground = AppPalette.MaroonBright,
                            background = AppPalette.Maroon.copy(alpha = 0.14f),
                            border = AppPalette.Maroon.copy(alpha = 0.6f),
                            onClick = { isRemoving = true },
                        )
                    }
                }
            }
        }
    }
}

/** the note's rounded plate reaches 8dp left/right and 4dp above/below its text, like the design's negative margin */
private fun Modifier.selectionHighlight(
    isSelected: Boolean,
    isEditing: Boolean,
): Modifier =
    drawBehind {
        if (!isSelected && !isEditing) return@drawBehind
        val topLeft = Offset(-8.dp.toPx(), -4.dp.toPx())
        val plateSize = Size(size.width + 16.dp.toPx(), size.height + 8.dp.toPx())
        val radius = CornerRadius(10.dp.toPx())
        val borderColor = if (isEditing) AppPalette.Gold.copy(alpha = 0.6f) else AppPalette.BorderPopover
        drawRoundRect(AppPalette.Surface, topLeft, plateSize, radius)
        drawRoundRect(borderColor, topLeft, plateSize, radius, style = Stroke(width = 1.dp.toPx()))
    }

@Composable
private fun EntryActionButton(
    glyph: String?,
    label: String,
    foreground: Color,
    background: Color,
    border: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .height(36.dp)
                .appCard(shape = RoundedCornerShape(10.dp), background = background, border = border, onClick = onClick)
                .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph == null) {
            Icon(AppIcons.Edit, contentDescription = null, tint = foreground, modifier = Modifier.size(14.dp))
        } else {
            Text(glyph, fontSize = 12.sp, color = foreground)
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = foreground)
    }
}

// wraps word-by-word so a chip never gets stranded on its own line, with the design's 1.65 paragraph line height
@Composable
private fun SessionEntryBody(
    segments: List<SessionEntrySegment>,
    onMentionClick: (EntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.75.sp)
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEach { segment ->
            when (segment) {
                is SessionEntrySegment.Text -> {
                    segment.text.split('\n').forEachIndexed { lineIndex, line ->
                        // a full-width empty item forces the flow onto a fresh line for each newline in the note
                        if (lineIndex > 0) Spacer(Modifier.fillMaxWidth())
                        line.split(' ').forEach { word ->
                            if (word.isNotEmpty()) Text(word, style = textStyle, color = AppPalette.TextPrimary)
                        }
                    }
                }

                is SessionEntrySegment.Mention -> {
                    MentionChip(
                        item = segment.chip,
                        onClick = segment.chip.entityRef?.let { ref -> { onMentionClick(ref) } },
                        fontSize = 14.sp,
                        glyphSize = 7.sp,
                        glyphGap = 4.dp,
                        lineHeightFactor = 1.5f,
                    )
                }
            }
        }
    }
}

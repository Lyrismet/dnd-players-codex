package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.ButtonSize
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.designsystem.component.tapOrQuickHold
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.mention.domain.DeletedMention
import com.lyrismet.incadent.feature.mention.presentation.MentionChip
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_delete
import dndplayerscodex.shared.generated.resources.action_edit
import dndplayerscodex.shared.generated.resources.auto_mention_dismiss
import dndplayerscodex.shared.generated.resources.auto_mention_link
import dndplayerscodex.shared.generated.resources.auto_mention_matches
import dndplayerscodex.shared.generated.resources.session_detail_entry_save_phrase
import dndplayerscodex.shared.generated.resources.session_entry_edited_label
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val ENTRY_COLLAPSE_ANIMATION_DURATION_MS = 220
private const val ENTRY_REMOVAL_GRACE_MS = 1000L
private const val FOCUS_FADE_DURATION_MS = 600

// top and bottom edges converge on the center as the row collapses, Gmail-delete-style, not a one-sided slide
private fun entryCollapseExit() =
    shrinkVertically(
        animationSpec = tween(ENTRY_COLLAPSE_ANIMATION_DURATION_MS),
        shrinkTowards = Alignment.CenterVertically,
    ) + fadeOut(animationSpec = tween(ENTRY_COLLAPSE_ANIMATION_DURATION_MS))

// tapping or holding a note selects it and reveals its actions underneath
@Composable
internal fun SessionEntryRow(
    entry: SessionEntryItem,
    isSelected: Boolean,
    isEditing: Boolean,
    isHighlighted: Boolean,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onSavePhraseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onMentionClick: (EntityRef) -> Unit,
    mentionActions: EntryMentionActions,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
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
        Row(
            modifier = Modifier.fillMaxWidth().focusFlash(isHighlighted),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            EntryTimeColumn(timeLabel = entry.timeLabel, edited = entry.edited)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SessionEntryBody(
                    segments = entry.segments,
                    onMentionClick = onMentionClick,
                    mentionActions = mentionActions,
                    modifier =
                        Modifier.selectionHighlight(isSelected, isEditing).tapOrQuickHold(
                            onTap = onClick,
                            // a hold only reveals the actions, a second hold on an open note must not close it again
                            onHold = {
                                if (!isSelected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClick()
                                }
                            },
                        ),
                )
                if (entry.autoMatchCount > 0) {
                    AutoMentionBar(
                        count = entry.autoMatchCount,
                        onLink = mentionActions.onAutoLink,
                        onDismiss = mentionActions.onAutoDismiss,
                    )
                }
                if (isSelected) {
                    EntryActions(
                        onEditClick = onEditClick,
                        onSavePhraseClick = onSavePhraseClick,
                        onDeleteClick = { isRemoving = true },
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryActions(
    onEditClick: () -> Unit,
    onSavePhraseClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppButton(
            text = stringResource(Res.string.action_edit),
            onClick = onEditClick,
            size = ButtonSize.Medium,
            leading = { color ->
                Icon(AppIcons.Edit, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
            },
        )
        AppButton(
            text = stringResource(Res.string.session_detail_entry_save_phrase),
            onClick = onSavePhraseClick,
            tone = ButtonTone.Neutral,
            size = ButtonSize.Medium,
            contentColor = AppPalette.TextMuted,
            leading = { color -> Text("★", fontSize = AppFontSize.Small, color = color) },
        )
        AppButton(
            text = stringResource(Res.string.action_delete),
            onClick = onDeleteClick,
            tone = ButtonTone.Maroon,
            size = ButtonSize.Medium,
            leading = { color -> Text("✕", fontSize = AppFontSize.Small, color = color) },
        )
    }
}

// the related-note jump flashes a gold plate that reaches 6dp past the note, then fades out over 600 ms
// fill and border are the same gold at two fixed alphas, so one progress value drives both instead of
// animating two independent Color states for what is visually a single fade
@Composable
private fun Modifier.focusFlash(isHighlighted: Boolean): Modifier {
    val progress by animateFloatAsState(
        targetValue = if (isHighlighted) 1f else 0f,
        animationSpec = tween(FOCUS_FADE_DURATION_MS),
    )
    return drawBehind {
        val topLeft = Offset(-6.dp.toPx(), -6.dp.toPx())
        val plateSize = Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx())
        val radius = CornerRadius(12.dp.toPx())
        drawRoundRect(AppPalette.Gold.copy(alpha = 0.13f * progress), topLeft, plateSize, radius)
        drawRoundRect(
            AppPalette.Gold.copy(alpha = 0.55f * progress),
            topLeft,
            plateSize,
            radius,
            style = Stroke(width = 1.dp.toPx()),
        )
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

private const val EDITED_LABEL_GAP_DP = 2

// the "изм." mark is placed below the time with Modifier.offset, which shifts drawing only and isn't
// counted in this Box's measured height - so the mark never changes this column's height or the row's,
// and the gap to the next message in the feed stays the same whether a note is edited or not
@Composable
private fun EntryTimeColumn(
    timeLabel: String,
    edited: Boolean,
) {
    var timeHeightPx by remember { mutableStateOf(0) }
    val gapPx = with(LocalDensity.current) { EDITED_LABEL_GAP_DP.dp.roundToPx() }
    Box(modifier = Modifier.width(38.dp).padding(top = 4.dp)) {
        Text(
            timeLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum"),
            color = AppPalette.TextTertiary,
            modifier = Modifier.onSizeChanged { timeHeightPx = it.height },
        )
        if (edited) {
            Text(
                stringResource(Res.string.session_entry_edited_label),
                fontSize = AppFontSize.Tiny,
                color = AppPalette.TextSecondary,
                modifier = Modifier.offset { IntOffset(0, timeHeightPx + gapPx) },
            )
        }
    }
}

/** what a note's mention parts can ask for - a "?" tag, the "link them" row and a single dotted word */
internal class EntryMentionActions(
    val onDeletedClick: (DeletedMention) -> Unit,
    val onAutoLink: () -> Unit,
    val onAutoDismiss: () -> Unit,
    val onAutoWordClick: (entityKey: String) -> Unit,
    val onSpeciesClick: (occurrence: Int, speciesId: Long) -> Unit,
)

// the "N matches with the codex · Link · No" row under a note - nothing changes until one of the two is tapped
@Composable
private fun AutoMentionBar(
    count: Int,
    onLink: () -> Unit,
    onDismiss: () -> Unit,
) {
    val label = pluralStringResource(Res.plurals.auto_mention_matches, count, count)
    // the leading number is the gold one, the rest of the sentence stays quiet
    val numberEnd = label.indexOf(' ').let { if (it < 0) label.length else it }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text =
                buildAnnotatedString {
                    withStyle(SpanStyle(color = AppPalette.GoldBright, fontWeight = FontWeight.SemiBold)) {
                        append(label.substring(0, numberEnd))
                    }
                    append(label.substring(numberEnd))
                },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Small),
            color = AppPalette.TextSecondary,
        )
        Text("·", style = MaterialTheme.typography.bodySmall, color = AppPalette.TextTertiary)
        AutoMentionTextButton(stringResource(Res.string.auto_mention_link), AppPalette.GoldBright, onLink)
        Text("·", style = MaterialTheme.typography.bodySmall, color = AppPalette.TextTertiary)
        AutoMentionTextButton(stringResource(Res.string.auto_mention_dismiss), AppPalette.TextMuted, onDismiss)
    }
}

@Composable
private fun AutoMentionTextButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    Text(
        text,
        style =
            MaterialTheme.typography.labelLarge.copy(
                fontSize = AppFontSize.Small,
                fontWeight = FontWeight.SemiBold,
            ),
        color = color,
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 8.dp),
    )
}

// a word the codex knows, written without "@" - dotted gold so it reads as a suggestion and not as a tag
private fun Modifier.dottedUnderline(): Modifier =
    drawBehind {
        val strokePx = 1.5.dp.toPx()
        val y = size.height - strokePx / 2
        drawLine(
            color = AppPalette.GoldBright,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokePx,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(strokePx, strokePx * 2)),
        )
    }

// wraps word-by-word so a chip never gets stranded on its own line, with the design's 1.65 paragraph line height
@Composable
private fun SessionEntryBody(
    segments: List<SessionEntrySegment>,
    onMentionClick: (EntityRef) -> Unit,
    mentionActions: EntryMentionActions,
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

                is SessionEntrySegment.AutoMatch ->
                    Text(
                        segment.text,
                        style = textStyle,
                        color = AppPalette.TextPrimary,
                        modifier =
                            Modifier
                                .dottedUnderline()
                                .clickable { mentionActions.onAutoWordClick(segment.entityKey) },
                    )

                is SessionEntrySegment.Mention -> {
                    MentionChip(
                        item = segment.chip,
                        onClick =
                            segment.chip.entityRef?.let { ref ->
                                if (ref is EntityRef.Species) {
                                    { mentionActions.onSpeciesClick(segment.occurrence, ref.id) }
                                } else {
                                    { onMentionClick(ref) }
                                }
                            }
                                ?: segment.chip.deleted?.let { deleted -> { mentionActions.onDeletedClick(deleted) } },
                        fontSize = AppFontSize.Medium,
                        glyphSize = 13.dp,
                        glyphGap = 4.dp,
                        lineHeightFactor = 1.5f,
                    )
                }
            }
        }
    }
}

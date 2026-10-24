package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppButton
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.ButtonTone
import com.lyrismet.incadent.core.designsystem.component.GlowingDot
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import com.lyrismet.incadent.feature.mention.presentation.MentionChip
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem
import com.lyrismet.incadent.feature.session.domain.tags.sessionTagLabel
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_back
import dndplayerscodex.shared.generated.resources.session_detail_end_button
import dndplayerscodex.shared.generated.resources.session_detail_ended_badge
import dndplayerscodex.shared.generated.resources.session_detail_live_badge
import dndplayerscodex.shared.generated.resources.session_detail_resume_button
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailHeader(
    header: SessionHeaderState,
    eventSink: (SessionDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // with the keyboard up the header shrinks to the title row, so the notes keep their room
    val ime = WindowInsets.ime
    val density = LocalDensity.current
    val compact by remember { derivedStateOf { ime.getBottom(density) > 0 } }
    Column(modifier = modifier) {
        SessionDetailHeaderContent(header, eventSink, compact)
        AppDivider()
    }
}

@Composable
private fun SessionDetailHeaderContent(
    header: SessionHeaderState,
    eventSink: (SessionDetailEvent) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SessionHeaderTopRow(header, eventSink)
            Text(
                header.overline,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.14.em),
                color = AppPalette.Gold,
                modifier = Modifier.padding(top = 4.dp),
            )
            SessionTitle(
                title = header.title,
                maxLines = if (compact) 1 else TITLE_MAX_LINES,
                isEditing = header.isTitleEditing,
                onEditStarted = { eventSink(SessionDetailEvent.TitleEditStarted) },
                onEditFinished = { eventSink(SessionDetailEvent.TitleEditFinished) },
                onTitleChanged = { eventSink(SessionDetailEvent.TitleChanged(it)) },
                modifier = Modifier.padding(top = 2.dp),
            )
            if (!compact) {
                Text(
                    header.dateLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppPalette.TextSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
                SessionTagsRow(header, eventSink, modifier = Modifier.padding(top = 10.dp))
            }
        }
        if (!compact && header.headerMentions.isNotEmpty()) {
            HeaderMentionsRow(header.headerMentions, eventSink, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun SessionHeaderTopRow(
    header: SessionHeaderState,
    eventSink: (SessionDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(38.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // plain clickable text, not a TextButton, whose 12dp inner padding pushes the label off the title edge
        Text(
            stringResource(Res.string.session_detail_back),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = AppPalette.GoldBright,
            modifier =
                Modifier
                    .clickable { eventSink(SessionDetailEvent.BackClicked) }
                    .padding(top = 6.dp, end = 4.dp, bottom = 6.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionStatusBadge(isLive = header.isLive)
            if (header.isLive) {
                AppButton(
                    text = stringResource(Res.string.session_detail_end_button),
                    onClick = { eventSink(SessionDetailEvent.EndSessionClicked) },
                    tone = ButtonTone.Maroon,
                )
            } else {
                AppButton(
                    text = stringResource(Res.string.session_detail_resume_button),
                    onClick = { eventSink(SessionDetailEvent.ResumeSessionClicked) },
                )
            }
        }
    }
}

// static wrapping title with a pen button, tapping either swaps in the gold-underlined field and a confirm button
@Composable
private fun SessionTitle(
    title: String,
    maxLines: Int,
    isEditing: Boolean,
    onEditStarted: () -> Unit,
    onEditFinished: () -> Unit,
    onTitleChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleStyle = MaterialTheme.typography.headlineLarge.copy(color = AppPalette.TextHeading)
    if (isEditing) {
        SessionTitleEditor(title, titleStyle, onTitleChanged, onDone = onEditFinished, modifier = modifier)
    } else {
        Row(
            modifier = modifier.fillMaxWidth().clickable(onClick = onEditStarted),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                title,
                style = titleStyle,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconBadge(
                modifier = Modifier.padding(top = 1.dp),
                size = 36.dp,
                shape = AppShapes.Control,
                background = AppPalette.SurfaceInput,
                border = AppPalette.Border,
            ) {
                Icon(
                    AppIcons.Edit,
                    contentDescription = null,
                    tint = AppPalette.TextMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionTitleEditor(
    title: String,
    titleStyle: TextStyle,
    onTitleChanged: (String) -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // a plain String value would start the caret at 0, so the selection is kept here and begins at the end
    var field by remember { mutableStateOf(TextFieldValue(title, TextRange(title.length))) }
    Row(
        modifier = modifier.fillMaxWidth().consumeTaps(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppTextInput(
            value = field,
            onValueChange = {
                field = it
                onTitleChanged(it.text)
            },
            style =
                AppTextInputStyle(
                    textStyle = titleStyle,
                    textColor = Color.Unspecified,
                    chrome = null,
                    height = 40.dp,
                    contentPadding = PaddingValues(),
                ),
            maxLength = TextLimits.SESSION_TITLE,
            autoFocus = true,
            modifier =
                Modifier
                    .weight(1f)
                    .drawBehind {
                        val strokeWidth = 1.5.dp.toPx()
                        drawLine(
                            color = AppPalette.Gold,
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth,
                        )
                    },
        )
        IconBadge(
            size = 36.dp,
            shape = AppShapes.Control,
            background = AppPalette.Gold,
            onClick = onDone,
        ) {
            Text("✓", style = MaterialTheme.typography.titleSmall, color = AppPalette.Background)
        }
    }
}

// scrolls edge to edge: the row carries its own 16dp inset instead of sitting inside the padded header column
@Composable
private fun HeaderMentionsRow(
    mentions: List<MentionChipItem>,
    eventSink: (SessionDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        mentions.forEach { chip ->
            MentionChip(
                item = chip,
                onClick =
                    chip.entityRef?.let { ref ->
                        { eventSink(SessionDetailEvent.EntitySheet(EntitySheetEvent.EntityClicked(ref))) }
                    }
                        ?: chip.deleted?.let { deleted ->
                            { eventSink(SessionDetailEvent.DeletedMentionClicked(deleted)) }
                        },
                fontSize = AppFontSize.Small,
                lineHeightFactor = 1.7f,
            )
        }
    }
}

// the session's own tag pills plus the dashed "+ Тег"/"+ Добавить тег" button that opens the tags bottom sheet
@Composable
private fun SessionTagsRow(
    header: SessionHeaderState,
    eventSink: (SessionDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        header.tags.forEach { tag ->
            TagChip(
                text = sessionTagLabel(tag),
                foreground = AppPalette.TextDescription,
                background = AppPalette.SurfacePopover,
                border = AppPalette.Border,
                height = 32.dp,
            )
        }
        TagButton(
            label = header.tagButtonLabel,
            suggestionCount = header.tagSuggestionCount,
            onClick = { eventSink(SessionDetailEvent.TagButtonClicked) },
        )
    }
}

private val SUGGESTION_BADGE_SIZE = 18.dp

private const val TITLE_MAX_LINES = 3

@Composable
private fun TagButton(
    label: String,
    suggestionCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasSuggestions = suggestionCount > 0
    // the mockup's button border is dashed - approximated here with a plain solid border, the one visual
    // simplification this feature makes, since compose has no built-in dashed-stroke border primitive
    val border = if (hasSuggestions) AppPalette.Gold.copy(alpha = 0.6f) else AppPalette.BorderHover
    Row(
        modifier =
            modifier
                .height(32.dp)
                .appCard(
                    shape = AppShapes.Small,
                    background = Color.Transparent,
                    border = border,
                    onClick = onClick,
                ).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AppPalette.GoldBright)
        if (hasSuggestions) {
            // the count sits in a gold circle, the same suggestion cue as the sheet's "по записям" header
            Text(
                suggestionCount.toString(),
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = AppFontSize.TinyPlus,
                        fontWeight = FontWeight.Bold,
                    ),
                color = AppPalette.Background,
                textAlign = TextAlign.Center,
                modifier =
                    Modifier
                        .defaultMinSize(minWidth = SUGGESTION_BADGE_SIZE, minHeight = SUGGESTION_BADGE_SIZE)
                        .appCard(
                            shape = CircleShape,
                            background = AppPalette.Gold,
                            border = null,
                        ).padding(horizontal = 5.dp)
                        .wrapContentHeight(Alignment.CenterVertically),
            )
        }
    }
}

@Composable
private fun SessionStatusBadge(
    isLive: Boolean,
    modifier: Modifier = Modifier,
) {
    if (isLive) {
        Row(
            modifier =
                modifier
                    .appCard(
                        shape = RoundedCornerShape(7.dp),
                        background = AppPalette.Emerald.copy(alpha = 0.14f),
                        border = null,
                    ).padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlowingDot(dotSize = 6.dp)
            Text(
                stringResource(Res.string.session_detail_live_badge),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.12.em),
                color = AppPalette.EmeraldBright,
            )
        }
    } else {
        Text(
            stringResource(Res.string.session_detail_ended_badge),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.12.em),
            color = AppPalette.TextTertiary,
            modifier = modifier,
        )
    }
}

// a tap inside the title editor must not read as a tap outside it, which closes the editor
private fun Modifier.consumeTaps(): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            waitForUpOrCancellation()?.consume()
        }
    }

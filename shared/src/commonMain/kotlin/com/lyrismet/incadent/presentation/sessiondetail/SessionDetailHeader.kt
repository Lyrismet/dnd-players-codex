package com.lyrismet.incadent.presentation.sessiondetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.GoldCursorBrush
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.GlowingDot
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.MentionChip
import com.lyrismet.incadent.core.designsystem.component.MentionChipItem
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.tags.sessionTagLabel
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_back
import dndplayerscodex.shared.generated.resources.session_detail_end_button
import dndplayerscodex.shared.generated.resources.session_detail_ended_badge
import dndplayerscodex.shared.generated.resources.session_detail_live_badge
import dndplayerscodex.shared.generated.resources.session_detail_resume_button
import dndplayerscodex.shared.generated.resources.session_detail_tag_suggestion_badge_format
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionDetailHeader(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SessionDetailHeaderContent(state)
        AppDivider()
    }
}

@Composable
private fun SessionDetailHeaderContent(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(bottom = 12.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SessionHeaderTopRow(state)
            Text(
                state.overline,
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.14.em),
                color = AppPalette.Gold,
                modifier = Modifier.padding(top = 4.dp),
            )
            SessionTitle(
                title = state.title,
                onTitleChanged = { state.eventSink(SessionDetailEvent.TitleChanged(it)) },
                modifier = Modifier.padding(top = 2.dp),
            )
            Text(
                state.dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = AppPalette.TextSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
            SessionTagsRow(state, modifier = Modifier.padding(top = 10.dp))
        }
        if (state.headerMentions.isNotEmpty()) {
            HeaderMentionsRow(state.headerMentions, state.eventSink, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun SessionHeaderTopRow(
    state: SessionDetailState,
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
                    .clickable { state.eventSink(SessionDetailEvent.BackClicked) }
                    .padding(top = 6.dp, end = 4.dp, bottom = 6.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SessionStatusBadge(isLive = state.isLive)
            if (state.isLive) {
                HeaderActionButton(
                    text = stringResource(Res.string.session_detail_end_button),
                    onClick = { state.eventSink(SessionDetailEvent.EndSessionClicked) },
                    foreground = AppPalette.MaroonBright,
                    background = AppPalette.Maroon.copy(alpha = 0.16f),
                    border = AppPalette.Maroon.copy(alpha = 0.7f),
                )
            } else {
                HeaderActionButton(
                    text = stringResource(Res.string.session_detail_resume_button),
                    onClick = { state.eventSink(SessionDetailEvent.ResumeSessionClicked) },
                )
            }
        }
    }
}

// static wrapping title with a pen button, tapping either swaps in the gold-underlined field and a confirm button
@Composable
private fun SessionTitle(
    title: String,
    onTitleChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isEditing by remember { mutableStateOf(false) }
    val titleStyle = MaterialTheme.typography.headlineLarge.copy(color = AppPalette.TextHeading)
    if (isEditing) {
        SessionTitleEditor(title, titleStyle, onTitleChanged, onDone = { isEditing = false }, modifier = modifier)
    } else {
        Row(
            modifier = modifier.fillMaxWidth().clickable { isEditing = true },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(title, style = titleStyle, modifier = Modifier.weight(1f))
            IconBadge(
                modifier = Modifier.padding(top = 1.dp),
                size = 36.dp,
                shape = RoundedCornerShape(10.dp),
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
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    // a plain String value would start the caret at 0, so the selection is kept here and begins at the end
    var field by remember { mutableStateOf(TextFieldValue(title, TextRange(title.length))) }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // BasicTextField, not Material3's TextField, whose built-in padding and minimum height don't match
        BasicTextField(
            value = field,
            onValueChange = {
                field = it
                onTitleChanged(it.text)
            },
            textStyle = titleStyle,
            singleLine = true,
            cursorBrush = GoldCursorBrush,
            keyboardOptions = SentenceKeyboardOptions,
            modifier =
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .focusRequester(focusRequester)
                    .drawBehind {
                        val strokeWidth = 1.5.dp.toPx()
                        drawLine(
                            color = AppPalette.Gold,
                            start = Offset(0f, size.height - strokeWidth / 2),
                            end = Offset(size.width, size.height - strokeWidth / 2),
                            strokeWidth = strokeWidth,
                        )
                    },
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    innerTextField()
                }
            },
        )
        IconBadge(
            size = 36.dp,
            shape = RoundedCornerShape(10.dp),
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
                onClick = chip.entityRef?.let { ref -> { eventSink(SessionDetailEvent.MentionChipClicked(ref)) } },
                fontSize = 12.sp,
                lineHeightFactor = 1.7f,
            )
        }
    }
}

// the session's own tag pills plus the dashed "+ Тег"/"+ Добавить тег" button that opens the tags bottom sheet
@Composable
private fun SessionTagsRow(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        state.tags.forEach { tag ->
            TagChip(
                text = sessionTagLabel(tag),
                foreground = AppPalette.TextDescription,
                background = AppPalette.SurfacePopover,
                border = AppPalette.Border,
                height = 32.dp,
            )
        }
        TagButton(
            label = state.tagButtonLabel,
            suggestionCount = state.tagSuggestionCount,
            onClick = { state.eventSink(SessionDetailEvent.TagButtonClicked) },
        )
    }
}

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
                    shape = RoundedCornerShape(8.dp),
                    background = Color.Transparent,
                    border = border,
                    onClick = onClick,
                ).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = AppPalette.GoldBright)
        if (hasSuggestions) {
            Text(
                stringResource(Res.string.session_detail_tag_suggestion_badge_format, suggestionCount),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = AppPalette.Background,
                modifier =
                    Modifier
                        .appCard(shape = RoundedCornerShape(6.dp), background = AppPalette.Gold, border = null)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
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

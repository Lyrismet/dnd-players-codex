package com.lyrismet.incadent.feature.session.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.AppText
import com.lyrismet.incadent.core.designsystem.SentenceKeyboardOptions
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppTextInput
import com.lyrismet.incadent.core.designsystem.component.AppTextInputCounter
import com.lyrismet.incadent.core.designsystem.component.AppTextInputStyle
import com.lyrismet.incadent.core.designsystem.component.IconBadge
import com.lyrismet.incadent.core.designsystem.component.TextLimits
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.icons.AppIcons
import com.lyrismet.incadent.core.navigation.LocalTabSelected
import com.lyrismet.incadent.feature.mention.presentation.MentionVisualTransformation
import com.lyrismet.incadent.feature.mention.presentation.mentionDisplayLength
import com.lyrismet.incadent.feature.mention.presentation.takeMentionDisplay
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_cancel
import dndplayerscodex.shared.generated.resources.session_detail_composer_hint
import dndplayerscodex.shared.generated.resources.session_detail_composer_placeholder
import dndplayerscodex.shared.generated.resources.session_detail_editing_label_format
import dndplayerscodex.shared.generated.resources.session_detail_save_button
import dndplayerscodex.shared.generated.resources.session_detail_submit_button
import org.jetbrains.compose.resources.stringResource

internal val COMPOSER_SIDE_PADDING = 12.dp
private val COMPOSER_TOP_PADDING = 10.dp

// the popup is anchored to the padded content, so the 1dp divider and top padding sit between it and the footer edge
// tall enough for the seven rows and three headers of an empty "@", anything more scrolls
private val POPOVER_MAX_HEIGHT = 360.dp
private val POPOVER_OFFSET_ABOVE_CONTENT = 6.dp + COMPOSER_TOP_PADDING + 1.dp

@Composable
internal fun SessionComposer(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    // gating suggestions on focus too (not just draft text) is what makes an outside tap dismiss the popover
    var isDraftFocused by remember { mutableStateOf(false) }
    var composerWidthPx by remember { mutableIntStateOf(0) }
    val draftFocusRequester = remember { FocusRequester() }
    // an ended session mounts the composer only for the edit, so focus it right away
    LaunchedEffect(state.editingEntryId) {
        if (state.editingEntryId != null) draftFocusRequester.requestFocus()
    }

    Column(modifier = modifier.fillMaxWidth().background(AppPalette.SurfaceFooter)) {
        // the design's footer sits below a divider that separates it from the feed above
        AppDivider(color = AppPalette.BorderSubtle)
        Column(
            modifier =
                Modifier
                    .padding(
                        start = COMPOSER_SIDE_PADDING,
                        top = COMPOSER_TOP_PADDING,
                        end = COMPOSER_SIDE_PADDING,
                        bottom = 12.dp,
                    ).onSizeChanged { composerWidthPx = it.width },
        ) {
            // the popovers float over the strip's spot, so the strip only shows while neither is open
            val mentionsOpen = isDraftFocused && state.mentionSuggestions.isNotEmpty()
            val completionsOpen = isDraftFocused && !mentionsOpen && state.phraseCompletions.isNotEmpty()
            if (mentionsOpen) MentionSuggestionsPopover(state, composerWidthPx)
            if (completionsOpen) PhraseCompletionsPopover(state, composerWidthPx)
            state.editingEntryTimeLabel?.let { EditingEntryBanner(it, state.eventSink) }
            if (!mentionsOpen && !completionsOpen) {
                PhraseStrip(
                    items = state.phraseStrip,
                    onPick = {
                        draftFocusRequester.requestFocus()
                        state.eventSink(SessionDetailEvent.PhraseStripPicked(it))
                    },
                    onOpenSheet = { state.eventSink(SessionDetailEvent.PhraseSheetOpened) },
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            ComposerInputCard(
                state = state,
                onDraftFocusChanged = { isDraftFocused = it },
                draftFocusRequester = draftFocusRequester,
            )
        }
    }
}

@Composable
private fun ComposerInputCard(
    state: SessionDetailState,
    onDraftFocusChanged: (Boolean) -> Unit,
    draftFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = AppShapes.Card,
                    background = AppPalette.SurfaceInput,
                    border = AppPalette.Border,
                ).padding(start = 12.dp, top = 10.dp, end = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ComposerDraftField(state, onDraftFocusChanged, draftFocusRequester)
        ComposerActionRow(state, draftFocusRequester)
    }
}

@Composable
private fun ComposerDraftField(
    state: SessionDetailState,
    onDraftFocusChanged: (Boolean) -> Unit,
    draftFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    AppTextInput(
        value = TextFieldValue(state.draft, state.draftSelection),
        onValueChange = {
            val capped = it.copy(text = takeMentionDisplay(it.text, TextLimits.NOTE))
            state.eventSink(SessionDetailEvent.DraftChanged(capped.text, capped.selection))
        },
        placeholder = stringResource(Res.string.session_detail_composer_placeholder),
        // the card around the field is the composer's own, so the input draws none and counts what the user sees
        style =
            AppTextInputStyle(
                textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.5.sp),
                chrome = null,
                contentPadding = PaddingValues(),
            ),
        singleLine = false,
        minLines = 2,
        counter = AppTextInputCounter(TextLimits.NOTE, count = mentionDisplayLength(state.draft)),
        keyboardOptions = SentenceKeyboardOptions.copy(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { state.eventSink(SessionDetailEvent.SubmitEntryClicked) }),
        visualTransformation = remember { MentionVisualTransformation(SpanStyle(color = AppPalette.GoldBright)) },
        focusRequester = draftFocusRequester,
        modifier =
            modifier
                .fillMaxWidth()
                .onFocusChanged { onDraftFocusChanged(it.isFocused) }
                .onPreviewKeyEvent { event -> acceptFirstCompletionOnTab(event, state) },
    )
}

// a hardware keyboard's Tab takes the first suggested continuation, like the design's TAB hint
private fun acceptFirstCompletionOnTab(
    event: KeyEvent,
    state: SessionDetailState,
): Boolean {
    val first = state.phraseCompletions.firstOrNull()
    val isTab = event.key == Key.Tab
    if (first != null && isTab && event.type == KeyEventType.KeyDown) {
        state.eventSink(SessionDetailEvent.PhraseCompletionPicked(first.text))
    }
    return first != null && isTab
}

@Composable
private fun ComposerActionRow(
    state: SessionDetailState,
    draftFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MentionTriggerButton(
            onClick = {
                draftFocusRequester.requestFocus()
                state.eventSink(SessionDetailEvent.InsertMentionTriggerClicked)
            },
        )
        Text(
            stringResource(Res.string.session_detail_composer_hint),
            style = AppText.caption,
            color = AppPalette.TextTertiary,
            modifier = Modifier.weight(1f),
        )
        SubmitButton(
            label =
                stringResource(
                    if (state.isEditingEntry) {
                        Res.string.session_detail_save_button
                    } else {
                        Res.string.session_detail_submit_button
                    },
                ),
            enabled = state.draft.isNotBlank(),
            onClick = { state.eventSink(SessionDetailEvent.SubmitEntryClicked) },
        )
    }
}

// goes flat gray while the draft is empty, since submitting it does nothing
@Composable
private fun SubmitButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(if (enabled) AppPalette.Gold else AppPalette.SurfaceElevated)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = AppFontSize.Medium),
            color = if (enabled) AppPalette.Background else AppPalette.TextTertiary,
        )
    }
}

@Composable
private fun EditingEntryBanner(
    timeLabel: String,
    eventSink: (SessionDetailEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(start = 4.dp, end = 2.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(AppIcons.Edit, contentDescription = null, tint = AppPalette.GoldBright, modifier = Modifier.size(14.dp))
        Text(
            stringResource(Res.string.session_detail_editing_label_format, timeLabel),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.14.em),
            color = AppPalette.GoldBright,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .height(32.dp)
                    .appCard(
                        shape = AppShapes.Small,
                        background = Color.Transparent,
                        border = AppPalette.BorderHover,
                        onClick = { eventSink(SessionDetailEvent.CancelEditEntryClicked) },
                    ).padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(Res.string.action_cancel),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = AppPalette.TextMuted,
            )
        }
    }
}

// fixed 32x32 rounded-rect, not a text button - matches the "@" square next to the composer in the design
@Composable
private fun MentionTriggerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconBadge(
        modifier = modifier,
        size = 32.dp,
        shape = AppShapes.Tile,
        background = AppPalette.SurfaceElevated,
        onClick = onClick,
    ) {
        Text(
            "@",
            style = MaterialTheme.typography.titleSmall.copy(fontSize = AppFontSize.Large),
            color = AppPalette.GoldBright,
        )
    }
}

/** floats directly above the composer's top edge instead of growing the composer and shoving the feed up */
private class AbovePopupPositionProvider(
    private val gapPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(anchorBounds.left, anchorBounds.top - popupContentSize.height - gapPx)
}

/** the shared popup chrome of the mention and phrase-completion popovers, as wide as the composer */
@Composable
private fun ComposerPopover(
    composerWidthPx: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // a popup is a window of its own, so it ignores the zero size of a hidden tab and would hang at the screen top
    if (!LocalTabSelected.current) return
    val density = LocalDensity.current
    val popupWidth = with(density) { composerWidthPx.toDp() }
    val shape = AppShapes.Card
    val gapPx = with(density) { POPOVER_OFFSET_ABOVE_CONTENT.roundToPx() }
    Popup(popupPositionProvider = AbovePopupPositionProvider(gapPx)) {
        Column(
            modifier =
                modifier
                    .width(popupWidth)
                    .shadow(elevation = 16.dp, shape = shape)
                    .appCard(shape = shape, background = AppPalette.SurfacePopover, border = AppPalette.BorderPopover)
                    .heightIn(max = POPOVER_MAX_HEIGHT)
                    .verticalScroll(rememberScrollState())
                    .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun PhraseCompletionsPopover(
    state: SessionDetailState,
    composerWidthPx: Int,
) {
    ComposerPopover(composerWidthPx) {
        PhraseCompletionsContent(
            items = state.phraseCompletions,
            onPick = { state.eventSink(SessionDetailEvent.PhraseCompletionPicked(it)) },
        )
    }
}

@Composable
private fun MentionSuggestionsPopover(
    state: SessionDetailState,
    composerWidthPx: Int,
) {
    ComposerPopover(composerWidthPx) { MentionDropdownContent(state.mentionSuggestions, state.eventSink) }
}

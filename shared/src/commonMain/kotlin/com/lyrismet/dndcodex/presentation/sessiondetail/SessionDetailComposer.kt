package com.lyrismet.dndcodex.presentation.sessiondetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_composer_placeholder
import dndplayerscodex.shared.generated.resources.session_detail_mention_suggestions_header
import dndplayerscodex.shared.generated.resources.session_detail_submit_button
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SessionComposer(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    // gating suggestions on focus too (not just draft text) is what makes an outside tap dismiss the popover
    var isDraftFocused by remember { mutableStateOf(false) }
    val draftFocusRequester = remember { FocusRequester() }

    Column(modifier = modifier.fillMaxWidth().background(AppPalette.SurfaceSunken)) {
        // the design's footer sits below a hairline that separates it from the feed above
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppPalette.BorderSubtle))
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isDraftFocused && state.mentionSuggestions.isNotEmpty()) {
                MentionSuggestionsPopover(state)
            }
            ComposerInputRow(
                state = state,
                onDraftFocusChanged = { isDraftFocused = it },
                draftFocusRequester = draftFocusRequester,
            )
        }
    }
}

@Composable
private fun ComposerInputRow(
    state: SessionDetailState,
    onDraftFocusChanged: (Boolean) -> Unit,
    draftFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppPalette.Surface)
                .border(1.dp, AppPalette.Border, RoundedCornerShape(14.dp))
                .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextField(
            value = TextFieldValue(state.draft, state.draftSelection),
            onValueChange = { state.eventSink(SessionDetailEvent.DraftChanged(it.text, it.selection)) },
            placeholder = {
                Text(
                    stringResource(Res.string.session_detail_composer_placeholder),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPalette.TextTertiary,
                )
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = AppPalette.TextPrimary),
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppPalette.Gold,
                ),
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(draftFocusRequester)
                    .onFocusChanged { onDraftFocusChanged(it.isFocused) },
        )
        MentionTriggerButton(
            onClick = {
                draftFocusRequester.requestFocus()
                state.eventSink(SessionDetailEvent.InsertMentionTriggerClicked)
            },
        )
        Button(
            onClick = { state.eventSink(SessionDetailEvent.SubmitEntryClicked) },
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.height(38.dp),
        ) {
            Text(stringResource(Res.string.session_detail_submit_button), style = MaterialTheme.typography.labelLarge)
        }
    }
}

// fixed 32x32 rounded-rect, not a text button - matches the "@" square next to the composer in the design
@Composable
private fun MentionTriggerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(AppPalette.SurfaceElevated)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text("@", style = MaterialTheme.typography.titleMedium, color = AppPalette.GoldBright)
    }
}

@Composable
private fun MentionSuggestionsPopover(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppPalette.SurfaceElevated)
                .border(1.dp, AppPalette.BorderHover, RoundedCornerShape(14.dp))
                .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            stringResource(Res.string.session_detail_mention_suggestions_header),
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.TextTertiary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
        state.mentionSuggestions.forEach { suggestion ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable {
                            state.eventSink(SessionDetailEvent.MentionSuggestionPicked(suggestion.candidateKey))
                        }.padding(horizontal = 8.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.width(14.dp), contentAlignment = Alignment.Center) {
                    Text(suggestion.glyph.symbol, fontSize = 9.sp, color = suggestion.tint)
                }
                Text(
                    suggestion.name,
                    fontSize = 14.sp,
                    color = AppPalette.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                Text(suggestion.typeLabel, fontSize = 11.sp, color = AppPalette.TextSecondary)
            }
        }
    }
}

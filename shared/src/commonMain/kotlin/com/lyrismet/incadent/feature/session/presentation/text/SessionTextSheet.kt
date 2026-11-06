package com.lyrismet.incadent.feature.session.presentation.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.incadent.core.designsystem.AppFontSize
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.AppShapes
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppSheetColumn
import com.lyrismet.incadent.core.designsystem.component.AppSheetHeader
import com.lyrismet.incadent.core.designsystem.component.AppToggleCard
import com.lyrismet.incadent.core.designsystem.component.FormPrimaryButton
import com.lyrismet.incadent.core.designsystem.component.OverlineStyle
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.file.rememberTextSharing
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_text_actors_hint
import dndplayerscodex.shared.generated.resources.session_text_actors_toggle
import dndplayerscodex.shared.generated.resources.session_text_intro_hint
import dndplayerscodex.shared.generated.resources.session_text_intro_hint_named
import dndplayerscodex.shared.generated.resources.session_text_intro_toggle
import dndplayerscodex.shared.generated.resources.session_text_preview
import dndplayerscodex.shared.generated.resources.session_text_send
import dndplayerscodex.shared.generated.resources.session_text_title
import org.jetbrains.compose.resources.stringResource

private val PREVIEW_MAX_HEIGHT = 240.dp

/** what the "send as text" sheet draws, [shareNow] is the text waiting to be handed to the system share sheet */
@Immutable
data class SessionTextSheetState(
    val overline: String,
    val withIntro: Boolean,
    val withActors: Boolean,
    // the intro joke names the character marked "я", the hint says so
    val hasCharacter: Boolean,
    val preview: String,
    val shareNow: String? = null,
)

sealed interface SessionTextEvent {
    data object IntroToggled : SessionTextEvent

    data object ActorsToggled : SessionTextEvent

    data object SendClicked : SessionTextEvent

    /** the system share sheet has been shown for the text */
    data object Shared : SessionTextEvent

    data object Dismissed : SessionTextEvent
}

/** the sheet before a session goes out as text, two switches and a look at what will be sent */
@Composable
fun SessionTextSheet(
    state: SessionTextSheetState,
    onEvent: (SessionTextEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val share = rememberTextSharing()
    // the presenter has put the text together, only the view can open the system share sheet for it
    LaunchedEffect(state.shareNow) {
        val text = state.shareNow ?: return@LaunchedEffect
        share(text)
        onEvent(SessionTextEvent.Shared)
    }
    AppBottomSheet(onDismissRequest = { onEvent(SessionTextEvent.Dismissed) }) {
        AppSheetColumn(modifier = modifier, spacing = 12.dp, scroll = true, header = {
            AppSheetHeader(
                overline = state.overline,
                title = stringResource(Res.string.session_text_title),
                onClose = { onEvent(SessionTextEvent.Dismissed) },
            )
        }) {
            AppToggleCard(
                title = stringResource(Res.string.session_text_intro_toggle),
                hint =
                    stringResource(
                        if (state.hasCharacter) {
                            Res.string.session_text_intro_hint_named
                        } else {
                            Res.string.session_text_intro_hint
                        },
                    ),
                checked = state.withIntro,
                onToggle = { onEvent(SessionTextEvent.IntroToggled) },
            )
            AppToggleCard(
                title = stringResource(Res.string.session_text_actors_toggle),
                hint = stringResource(Res.string.session_text_actors_hint),
                checked = state.withActors,
                onToggle = { onEvent(SessionTextEvent.ActorsToggled) },
            )
            SectionOverline(stringResource(Res.string.session_text_preview), style = OverlineStyle.Page)
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .appCard(AppShapes.Card, AppPalette.Background, AppPalette.BorderSubtle)
                        .heightIn(max = PREVIEW_MAX_HEIGHT)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
            ) {
                Text(
                    text = state.preview,
                    color = AppPalette.TextPrimary,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = AppFontSize.Medium),
                )
            }
            FormPrimaryButton(
                text = stringResource(Res.string.session_text_send),
                onClick = { onEvent(SessionTextEvent.SendClicked) },
            )
        }
    }
}

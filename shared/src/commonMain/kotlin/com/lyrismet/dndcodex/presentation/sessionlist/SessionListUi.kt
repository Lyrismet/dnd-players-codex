package com.lyrismet.dndcodex.presentation.sessionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.ConfirmationDialog
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.component.HeaderActionButton
import com.lyrismet.dndcodex.core.designsystem.component.ScreenHeader
import com.lyrismet.dndcodex.core.designsystem.component.SectionOverline
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_list_archive_section
import dndplayerscodex.shared.generated.resources.session_list_delete_content_description
import dndplayerscodex.shared.generated.resources.session_list_delete_dialog_text
import dndplayerscodex.shared.generated.resources.session_list_delete_dialog_title
import dndplayerscodex.shared.generated.resources.session_list_empty
import dndplayerscodex.shared.generated.resources.session_list_new_session_button
import dndplayerscodex.shared.generated.resources.session_list_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionListUi(
    state: SessionListState,
    modifier: Modifier = Modifier,
) {
    // pending confirmation before a session is actually deleted - purely a transient ui flag
    var pendingDeleteSession by remember { mutableStateOf<SessionListItem?>(null) }

    Scaffold(modifier = modifier) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            ScreenHeader(
                overline = state.campaignName,
                title = stringResource(Res.string.session_list_title),
                overlineTrailingContent = {
                    HeaderActionButton(
                        text = stringResource(Res.string.session_list_new_session_button),
                        onClick = { state.eventSink(SessionListEvent.NewSessionClicked) },
                    )
                },
            )
            if (state.sessions.isEmpty()) {
                EmptyStatePlaceholder(text = stringResource(Res.string.session_list_empty))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        SectionOverline(
                            text = stringResource(Res.string.session_list_archive_section),
                            color = AppPalette.TextSecondary,
                            trailingContent = {
                                Text(
                                    state.sessions.size.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AppPalette.TextSecondary,
                                )
                            },
                        )
                    }
                    items(state.sessions, key = { it.id }) { session ->
                        SessionArchiveRow(
                            session = session,
                            onClick = { state.eventSink(SessionListEvent.SessionClicked(session.id)) },
                            onDeleteClick = { pendingDeleteSession = session },
                        )
                    }
                }
            }
        }
    }

    pendingDeleteSession?.let { session ->
        ConfirmationDialog(
            title = stringResource(Res.string.session_list_delete_dialog_title),
            text = stringResource(Res.string.session_list_delete_dialog_text, session.title),
            onConfirm = {
                state.eventSink(SessionListEvent.DeleteSessionClicked(session.id))
                pendingDeleteSession = null
            },
            onDismiss = { pendingDeleteSession = null },
        )
    }
}

@Composable
private fun SessionArchiveRow(
    session: SessionListItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(AppPalette.SurfaceVariant)
                .clickable(onClick = onClick)
                .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = session.numberLabel,
            style = MaterialTheme.typography.titleMedium,
            color = AppPalette.GoldDim,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(52.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(session.title, style = MaterialTheme.typography.titleMedium, color = AppPalette.TextHeading)
            Text(session.dateLabel, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
        }
        IconButton(onClick = onDeleteClick) {
            // neutral color deliberately - the destructive emphasis belongs on the confirm
            // dialog's button, not on an always-visible row icon
            Icon(
                Icons.Outlined.Delete,
                contentDescription = stringResource(Res.string.session_list_delete_content_description),
                tint = AppPalette.TextTertiary,
            )
        }
    }
}

package com.lyrismet.dndcodex.presentation.sessionlist

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.lyrismet.dndcodex.core.designsystem.component.MentionChip
import com.lyrismet.dndcodex.core.designsystem.component.ScreenHeader
import com.lyrismet.dndcodex.core.designsystem.component.SectionOverline
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_list_archive_section
import dndplayerscodex.shared.generated.resources.session_list_delete_content_description
import dndplayerscodex.shared.generated.resources.session_list_delete_dialog_text
import dndplayerscodex.shared.generated.resources.session_list_delete_dialog_title
import dndplayerscodex.shared.generated.resources.session_list_empty
import dndplayerscodex.shared.generated.resources.session_list_live_continue
import dndplayerscodex.shared.generated.resources.session_list_live_label
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
            if (state.liveSession == null && state.sessions.isEmpty()) {
                EmptyStatePlaceholder(text = stringResource(Res.string.session_list_empty))
            } else {
                SessionListContent(state, onDeleteRequest = { pendingDeleteSession = it })
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
private fun SessionListContent(
    state: SessionListState,
    onDeleteRequest: (SessionListItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.liveSession?.let { live ->
            item {
                LiveSessionCard(
                    session = live,
                    onClick = { state.eventSink(SessionListEvent.SessionClicked(live.id)) },
                    onMentionClick = { npcId -> state.eventSink(SessionListEvent.MentionChipClicked(npcId)) },
                )
            }
        }
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
                onDeleteClick = { onDeleteRequest(session) },
            )
        }
    }
}

@Composable
private fun LiveSessionCard(
    session: LiveSessionItem,
    onClick: () -> Unit,
    onMentionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(AppPalette.Surface)
                .border(1.dp, AppPalette.Gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(AppPalette.EmeraldBright))
            Text(
                stringResource(Res.string.session_list_live_label),
                style = MaterialTheme.typography.labelSmall,
                color = AppPalette.GoldBright,
                modifier = Modifier.weight(1f),
            )
            Text(session.dateLabel, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(session.numberLabel, style = MaterialTheme.typography.displayLarge, color = AppPalette.Gold)
            Column {
                Text(session.overline, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
                Text(session.title, style = MaterialTheme.typography.headlineMedium, color = AppPalette.TextHeading)
            }
        }
        if (session.mentions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                session.mentions.forEach { chip ->
                    MentionChip(item = chip, onClick = chip.npcId?.let { npcId -> { onMentionClick(npcId) } })
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(AppPalette.Border))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(session.notesSummary, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextSecondary)
            Text(
                stringResource(Res.string.session_list_live_continue),
                style = MaterialTheme.typography.labelLarge,
                color = AppPalette.GoldBright,
            )
        }
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

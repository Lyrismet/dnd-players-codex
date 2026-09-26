package com.lyrismet.dndcodex.presentation.sessiondetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.component.ConfirmationDialog
import com.lyrismet.dndcodex.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.dndcodex.core.designsystem.component.HeaderActionButton
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_delete
import dndplayerscodex.shared.generated.resources.session_detail_back
import dndplayerscodex.shared.generated.resources.session_detail_composer_placeholder
import dndplayerscodex.shared.generated.resources.session_detail_delete_entry_title
import dndplayerscodex.shared.generated.resources.session_detail_empty_feed
import dndplayerscodex.shared.generated.resources.session_detail_end_button
import dndplayerscodex.shared.generated.resources.session_detail_ended_badge
import dndplayerscodex.shared.generated.resources.session_detail_live_badge
import dndplayerscodex.shared.generated.resources.session_detail_resume_button
import dndplayerscodex.shared.generated.resources.session_detail_submit_button
import org.jetbrains.compose.resources.stringResource

@Composable
fun SessionDetailUi(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    // pending confirmation before an entry is actually deleted - purely a transient ui flag
    var pendingDeleteEntry by remember { mutableStateOf<SessionEntryItem?>(null) }

    Scaffold(modifier = modifier) { contentPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            SessionDetailHeader(state)

            if (state.entries.isEmpty()) {
                EmptyStatePlaceholder(
                    text = stringResource(Res.string.session_detail_empty_feed),
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(state.entries, key = { it.id }) { entry ->
                        SessionEntryRow(entry, onDeleteClick = { pendingDeleteEntry = entry })
                    }
                }
            }

            if (state.isLive) {
                SessionComposer(state)
            }
        }
    }

    pendingDeleteEntry?.let { entry ->
        ConfirmationDialog(
            title = stringResource(Res.string.session_detail_delete_entry_title),
            text = entry.body,
            onConfirm = {
                state.eventSink(SessionDetailEvent.DeleteEntryClicked(entry.id))
                pendingDeleteEntry = null
            },
            onDismiss = { pendingDeleteEntry = null },
        )
    }
}

@Composable
private fun SessionDetailHeader(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = { state.eventSink(SessionDetailEvent.BackClicked) }) {
                Text(
                    stringResource(Res.string.session_detail_back),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPalette.GoldBright,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
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
        Text(
            state.overline,
            style = MaterialTheme.typography.labelSmall,
            color = AppPalette.Gold,
            modifier = Modifier.padding(top = 4.dp),
        )
        TextField(
            value = state.title,
            onValueChange = { state.eventSink(SessionDetailEvent.TitleChanged(it)) },
            textStyle = MaterialTheme.typography.headlineLarge.copy(color = AppPalette.TextHeading),
            singleLine = true,
            colors =
                TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = AppPalette.Gold,
                ),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(state.dateLabel, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextSecondary)
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
                    .clip(RoundedCornerShape(7.dp))
                    .background(AppPalette.Emerald.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(AppPalette.EmeraldBright))
            Text(
                stringResource(Res.string.session_detail_live_badge),
                style = MaterialTheme.typography.labelMedium,
                color = AppPalette.EmeraldBright,
            )
        }
    } else {
        Text(
            stringResource(Res.string.session_detail_ended_badge),
            style = MaterialTheme.typography.labelMedium,
            color = AppPalette.TextTertiary,
            modifier = modifier,
        )
    }
}

// long-press to reveal actions, Telegram-style, instead of a permanently visible delete icon
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessionEntryRow(
    entry: SessionEntryItem,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = {}, onLongClick = { showMenu = true }),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                entry.timeLabel,
                style = MaterialTheme.typography.bodySmall,
                color = AppPalette.TextTertiary,
                modifier = Modifier.width(38.dp),
            )
            Text(
                entry.body,
                style = MaterialTheme.typography.bodyLarge,
                color = AppPalette.TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.action_delete), color = AppPalette.MaroonBright) },
                leadingIcon = {
                    Icon(Icons.Outlined.Delete, contentDescription = null, tint = AppPalette.MaroonBright)
                },
                onClick = {
                    showMenu = false
                    onDeleteClick()
                },
            )
        }
    }
}

@Composable
private fun SessionComposer(
    state: SessionDetailState,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(AppPalette.SurfaceSunken)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        TextField(
            value = state.draft,
            onValueChange = { state.eventSink(SessionDetailEvent.DraftChanged(it)) },
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
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { state.eventSink(SessionDetailEvent.SubmitEntryClicked) }) {
            Text(stringResource(Res.string.session_detail_submit_button))
        }
    }
}

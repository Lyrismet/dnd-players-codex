package com.lyrismet.incadent.presentation.sessionlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.EmptyStatePlaceholder
import com.lyrismet.incadent.core.designsystem.component.EntitySummarySheetContent
import com.lyrismet.incadent.core.designsystem.component.GlowingDot
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.MentionChip
import com.lyrismet.incadent.core.designsystem.component.NumberLabel
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SwipeToDeleteRow
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySummarySheetActions
import com.lyrismet.incadent.core.format.NumberSizeLadder
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_list_archive_section
import dndplayerscodex.shared.generated.resources.session_list_delete_content_description
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
                SessionListContent(state)
            }
        }
    }

    state.selectedEntity?.let { entity ->
        AppBottomSheet(onDismissRequest = { state.eventSink(SessionListEvent.SheetDismissed) }) {
            EntitySummarySheetContent(
                item = entity,
                actions =
                    EntitySummarySheetActions(
                        onEntityRefClicked = { ref -> state.eventSink(SessionListEvent.MentionChipClicked(ref)) },
                        onNpcStatusSelected = { id, status ->
                            state.eventSink(SessionListEvent.NpcStatusSelected(id, status))
                        },
                        onNpcLifeSelected = { id, life ->
                            state.eventSink(SessionListEvent.NpcLifeSelected(id, life))
                        },
                        onQuestStatusSelected = { id, status ->
                            state.eventSink(SessionListEvent.QuestStatusSelected(id, status))
                        },
                        onRelatedNoteClicked = { id -> state.eventSink(SessionListEvent.RelatedNoteClicked(id)) },
                    ),
                onClose = { state.eventSink(SessionListEvent.SheetDismissed) },
            )
        }
    }
}

@Composable
private fun SessionListContent(
    state: SessionListState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 2.dp, end = 16.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        state.liveSession?.let { live ->
            item {
                SwipeToDeleteRow(
                    onDeleteRequested = { state.eventSink(SessionListEvent.DeleteSessionClicked(live.id)) },
                    deleteContentDescription = stringResource(Res.string.session_list_delete_content_description),
                ) {
                    LiveSessionCard(
                        session = live,
                        onClick = { state.eventSink(SessionListEvent.SessionClicked(live.id)) },
                        onMentionClick = { ref -> state.eventSink(SessionListEvent.MentionChipClicked(ref)) },
                    )
                }
            }
        }
        item {
            SectionOverline(
                text = stringResource(Res.string.session_list_archive_section),
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 2.dp),
                color = AppPalette.TextSecondary,
                letterSpacing = 0.16.em,
                trailingContent = {
                    Text(
                        state.sessions.size.toString(),
                        style =
                            MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.em,
                            ),
                        color = AppPalette.TextSecondary,
                    )
                },
            )
        }
        items(state.sessions, key = { it.id }) { session ->
            SessionArchiveRow(
                session = session,
                onClick = { state.eventSink(SessionListEvent.SessionClicked(session.id)) },
                onDeleteClick = { state.eventSink(SessionListEvent.DeleteSessionClicked(session.id)) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun LiveSessionCard(
    session: LiveSessionItem,
    onClick: () -> Unit,
    onMentionClick: (EntityRef) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .appCard(
                    shape = RoundedCornerShape(16.dp),
                    border = AppPalette.Gold.copy(alpha = 0.45f),
                    onClick = onClick,
                ).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        LiveCardStatusRow(session.dateLabel)
        LiveCardTitleRow(session)
        if (session.mentions.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                session.mentions.forEach { chip ->
                    MentionChip(item = chip, onClick = chip.entityRef?.let { ref -> { onMentionClick(ref) } })
                }
            }
        }
        LiveCardFooter(session.notesSummary)
    }
}

@Composable
private fun LiveCardStatusRow(
    dateLabel: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlowingDot(dotSize = 7.dp)
        Text(
            stringResource(Res.string.session_list_live_label),
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.14.em),
            color = AppPalette.GoldBright,
            modifier = Modifier.weight(1f),
        )
        Text(
            dateLabel,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = AppPalette.TextSecondary,
        )
    }
}

// number and title share a bottom edge, the number's line height is tightened so it doesn't float above the title
@Composable
private fun LiveCardTitleRow(
    session: LiveSessionItem,
    modifier: Modifier = Modifier,
) {
    val numberSize = NumberSizeLadder.LIVE_SESSION.sizeFor(session.numberLabel).sp
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            session.numberLabel,
            style =
                MaterialTheme.typography.displayLarge.copy(
                    fontSize = numberSize,
                    lineHeight = numberSize * 0.9f,
                ),
            color = AppPalette.Gold,
            maxLines = 1,
            softWrap = false,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(session.overline, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
            Text(session.title, style = MaterialTheme.typography.headlineMedium, color = AppPalette.TextHeading)
        }
    }
}

@Composable
private fun LiveCardFooter(
    notesSummary: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AppDivider(color = AppPalette.Border)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(notesSummary, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextSecondary)
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
    SwipeToDeleteRow(
        onDeleteRequested = onDeleteClick,
        modifier = modifier,
        deleteContentDescription = stringResource(Res.string.session_list_delete_content_description),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .appCard(
                        shape = RoundedCornerShape(14.dp),
                        background = AppPalette.SurfaceVariant,
                        border = AppPalette.BorderSubtle,
                        onClick = onClick,
                    ).padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            NumberLabel(text = session.numberLabel, width = 52.dp, color = AppPalette.GoldDim)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(session.title, style = MaterialTheme.typography.titleMedium, color = AppPalette.TextHeading)
                Text(session.dateLabel, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
            }
        }
    }
}

package com.lyrismet.incadent.presentation.sessionlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.lyrismet.incadent.core.campaign.RenameSheetState
import com.lyrismet.incadent.core.designsystem.AppPalette
import com.lyrismet.incadent.core.designsystem.component.AppBottomSheet
import com.lyrismet.incadent.core.designsystem.component.AppDivider
import com.lyrismet.incadent.core.designsystem.component.AppToast
import com.lyrismet.incadent.core.designsystem.component.CampaignRenameSheet
import com.lyrismet.incadent.core.designsystem.component.EmptyStateAction
import com.lyrismet.incadent.core.designsystem.component.EmptyStateActions
import com.lyrismet.incadent.core.designsystem.component.EntitySummarySheetContent
import com.lyrismet.incadent.core.designsystem.component.GlowingDot
import com.lyrismet.incadent.core.designsystem.component.HeaderActionButton
import com.lyrismet.incadent.core.designsystem.component.MentionChip
import com.lyrismet.incadent.core.designsystem.component.NumberLabel
import com.lyrismet.incadent.core.designsystem.component.ScreenHeader
import com.lyrismet.incadent.core.designsystem.component.SectionOverline
import com.lyrismet.incadent.core.designsystem.component.SwipeHintBanner
import com.lyrismet.incadent.core.designsystem.component.SwipeToDeleteRow
import com.lyrismet.incadent.core.designsystem.component.TagChip
import com.lyrismet.incadent.core.designsystem.component.appCard
import com.lyrismet.incadent.core.designsystem.component.rememberSwipeHintPlayback
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.EntitySummarySheetActions
import com.lyrismet.incadent.core.format.NumberSizeLadder
import com.lyrismet.incadent.core.swipehint.SwipeHintDirection
import com.lyrismet.incadent.core.tags.sessionTagLabel
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_list_archive_section
import dndplayerscodex.shared.generated.resources.session_list_delete_content_description
import dndplayerscodex.shared.generated.resources.session_list_empty
import dndplayerscodex.shared.generated.resources.session_list_empty_button
import dndplayerscodex.shared.generated.resources.session_list_empty_title
import dndplayerscodex.shared.generated.resources.session_list_live_continue
import dndplayerscodex.shared.generated.resources.session_list_live_label
import dndplayerscodex.shared.generated.resources.session_list_mention_count_format
import dndplayerscodex.shared.generated.resources.session_list_new_session_button
import dndplayerscodex.shared.generated.resources.session_list_title
import dndplayerscodex.shared.generated.resources.swipe_hint_delete_only_title
import dndplayerscodex.shared.generated.resources.swipe_hint_dismiss
import dndplayerscodex.shared.generated.resources.swipe_hint_sessions_subtitle
import org.jetbrains.compose.resources.stringResource

// matches the bordered tag chip's own rendered height (16sp text + 3dp top/bottom padding) so an unbordered
// chip in the same row - the "@N" mention badge - lines up with it instead of sizing a touch shorter
private val SessionCardTagChipHeight = 22.dp

@Composable
fun SessionListUi(
    state: SessionListState,
    modifier: Modifier = Modifier,
) {
    val swipeHint = rememberSwipeHintPlayback(state.swipeHintTarget, SwipeHintDirection.DELETE_ONLY)
    Scaffold(modifier = modifier) { contentPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            SessionListBody(state, swipeHint.peekOffsetPx)
            AppToast(
                text = state.toast?.text,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
            )
            SwipeHintBanner(
                // the campaign-rename toast shares this anchor, so the hint yields to it instead of stacking
                visible = swipeHint.bannerVisible && state.toast == null,
                direction = SwipeHintDirection.DELETE_ONLY,
                title = stringResource(Res.string.swipe_hint_delete_only_title),
                subtitle = stringResource(Res.string.swipe_hint_sessions_subtitle),
                dismissLabel = stringResource(Res.string.swipe_hint_dismiss),
                onDismiss = swipeHint::dismiss,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }

    RenameSheetHost(state)

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
                        onRelatedNoteClicked = { id, entryId ->
                            state.eventSink(SessionListEvent.RelatedNoteClicked(id, entryId))
                        },
                    ),
                onClose = { state.eventSink(SessionListEvent.SheetDismissed) },
            )
        }
    }
}

@Composable
private fun SessionListBody(
    state: SessionListState,
    swipeHintPeekOffsetPx: Float,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            overline = state.campaignName,
            title = stringResource(Res.string.session_list_title),
            onOverlineClick = { state.eventSink(SessionListEvent.RenameOpened) },
            overlineTrailingContent = {
                HeaderActionButton(
                    text = stringResource(Res.string.session_list_new_session_button),
                    onClick = { state.eventSink(SessionListEvent.NewSessionClicked) },
                )
            },
        )
        if (state.liveSession == null && state.sessions.isEmpty()) {
            EmptyStateActions(
                title = stringResource(Res.string.session_list_empty_title),
                text = stringResource(Res.string.session_list_empty),
                primaryAction =
                    EmptyStateAction(label = stringResource(Res.string.session_list_empty_button)) {
                        state.eventSink(SessionListEvent.NewSessionClicked)
                    },
            )
        } else {
            SessionListContent(state, swipeHintPeekOffsetPx)
        }
    }
}

@Composable
private fun RenameSheetHost(state: SessionListState) {
    when (val sheet = state.renameSheet) {
        RenameSheetState.Hidden -> Unit
        is RenameSheetState.Editing ->
            CampaignRenameSheet(
                draft = sheet.draft,
                onDraftChanged = { value -> state.eventSink(SessionListEvent.RenameDraftChanged(value)) },
                onSave = { state.eventSink(SessionListEvent.RenameSaved) },
                onDismiss = { state.eventSink(SessionListEvent.RenameDismissed) },
            )
    }
}

@Composable
private fun SessionListContent(
    state: SessionListState,
    swipeHintPeekOffsetPx: Float,
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
                // only the row the swipe hint targets actually peeks - every other row stays at rest
                peekOffsetPx = if (session.id == state.swipeHintTarget?.itemId) swipeHintPeekOffsetPx else 0f,
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
    peekOffsetPx: Float,
    modifier: Modifier = Modifier,
) {
    SwipeToDeleteRow(
        onDeleteRequested = onDeleteClick,
        modifier = modifier,
        deleteContentDescription = stringResource(Res.string.session_list_delete_content_description),
        peekOffsetPx = peekOffsetPx,
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
                Text(session.archiveMeta, style = MaterialTheme.typography.bodySmall, color = AppPalette.TextSecondary)
                if (session.tags.isNotEmpty() || session.mentionCount > 0) {
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        session.tags.forEach { tag ->
                            TagChip(
                                text = sessionTagLabel(tag),
                                foreground = AppPalette.TextDescription,
                                background = AppPalette.SurfacePopover,
                                border = AppPalette.Border,
                                height = SessionCardTagChipHeight,
                            )
                        }
                        if (session.mentionCount > 0) {
                            val mentionLabel =
                                stringResource(Res.string.session_list_mention_count_format, session.mentionCount)
                            TagChip(
                                text = mentionLabel,
                                foreground = AppPalette.Gold,
                                background = AppPalette.Gold.copy(alpha = 0.08f),
                                height = SessionCardTagChipHeight,
                            )
                        }
                    }
                }
            }
        }
    }
}

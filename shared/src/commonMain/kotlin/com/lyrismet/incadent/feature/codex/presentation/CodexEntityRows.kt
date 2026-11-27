package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.component.NoPeek
import com.lyrismet.incadent.core.designsystem.component.SwipeToDeleteRow
import com.lyrismet.incadent.feature.codex.presentation.summary.EntityRef
import com.lyrismet.incadent.feature.codex.presentation.summary.EntitySheetEvent
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.codex_delete_content_description
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CodexDeletableRow(
    ref: EntityRef,
    eventSink: (CodexEvent) -> Unit,
    modifier: Modifier = Modifier,
    peekOffsetPx: () -> Float = NoPeek,
    content: @Composable () -> Unit,
) {
    SwipeToDeleteRow(
        onDeleteRequested = { eventSink(CodexEvent.EntityDeleteRequested(ref)) },
        modifier = modifier,
        deleteContentDescription = stringResource(Res.string.codex_delete_content_description),
        peekOffsetPx = peekOffsetPx,
        content = content,
    )
}

// one row per entity type, shared by its own tab list and the ALL tab so the wiring lives in one place
@Composable
internal fun PartyRow(
    member: PartyCodexItem,
    eventSink: (CodexEvent) -> Unit,
    peekOffsetPx: () -> Float = NoPeek,
) {
    CodexDeletableRow(
        ref = EntityRef.Party(member.id),
        eventSink = eventSink,
        peekOffsetPx = peekOffsetPx,
    ) {
        PartyCodexCard(item = member, onClick = {
            eventSink(CodexEvent.EntitySheet(EntitySheetEvent.EntityClicked(EntityRef.Party(member.id))))
        })
    }
}

@Composable
internal fun NpcRow(
    npc: NpcCodexItem,
    eventSink: (CodexEvent) -> Unit,
    peekOffsetPx: () -> Float = NoPeek,
) {
    CodexDeletableRow(
        ref = EntityRef.Npc(npc.id),
        eventSink = eventSink,
        peekOffsetPx = peekOffsetPx,
    ) {
        NpcCodexCard(item = npc, onClick = {
            eventSink(CodexEvent.EntitySheet(EntitySheetEvent.EntityClicked(EntityRef.Npc(npc.id))))
        })
    }
}

@Composable
internal fun QuestRow(
    quest: QuestCodexItem,
    eventSink: (CodexEvent) -> Unit,
    peekOffsetPx: () -> Float = NoPeek,
) {
    CodexDeletableRow(
        ref = EntityRef.Quest(quest.id),
        eventSink = eventSink,
        peekOffsetPx = peekOffsetPx,
    ) {
        QuestCodexCard(
            item = quest,
            onClick = { eventSink(CodexEvent.EntitySheet(EntitySheetEvent.EntityClicked(EntityRef.Quest(quest.id)))) },
            onGiverClick = { eventSink(CodexEvent.EntitySheet(EntitySheetEvent.EntityClicked(EntityRef.Npc(it)))) },
        )
    }
}

@Composable
internal fun LocationRow(
    location: LocationCodexItem,
    eventSink: (CodexEvent) -> Unit,
    peekOffsetPx: () -> Float = NoPeek,
) {
    CodexDeletableRow(
        ref = EntityRef.Location(location.id),
        eventSink = eventSink,
        peekOffsetPx = peekOffsetPx,
    ) {
        LocationCodexCard(
            item = location,
            onClick = {
                eventSink(
                    CodexEvent.EntitySheet(EntitySheetEvent.EntityClicked(EntityRef.Location(location.id))),
                )
            },
        )
    }
}

// the four rows as the lists build them - the swipe state and the peek of the hinted row come from the screen state
@Composable
internal fun CodexPartyEntry(
    state: CodexState,
    member: PartyCodexItem,
    hintPeekOffsetPx: () -> Float,
) = PartyRow(
    member = member,
    eventSink = state.eventSink,
    peekOffsetPx = state.peekOffsetPxFor(EntityRef.Party(member.id), hintPeekOffsetPx),
)

@Composable
internal fun CodexNpcEntry(
    state: CodexState,
    npc: NpcCodexItem,
    hintPeekOffsetPx: () -> Float,
) = NpcRow(
    npc = npc,
    eventSink = state.eventSink,
    peekOffsetPx = state.peekOffsetPxFor(EntityRef.Npc(npc.id), hintPeekOffsetPx),
)

@Composable
internal fun CodexQuestEntry(
    state: CodexState,
    quest: QuestCodexItem,
    hintPeekOffsetPx: () -> Float,
) = QuestRow(
    quest = quest,
    eventSink = state.eventSink,
    peekOffsetPx = state.peekOffsetPxFor(EntityRef.Quest(quest.id), hintPeekOffsetPx),
)

@Composable
internal fun CodexLocationEntry(
    state: CodexState,
    location: LocationCodexItem,
    hintPeekOffsetPx: () -> Float,
) = LocationRow(
    location = location,
    eventSink = state.eventSink,
    peekOffsetPx = state.peekOffsetPxFor(EntityRef.Location(location.id), hintPeekOffsetPx),
)

// only the row the swipe hint targets actually peeks - every other row stays at rest
internal fun CodexState.peekOffsetPxFor(
    ref: EntityRef,
    hintPeekOffsetPx: () -> Float,
): () -> Float = if (swipeHintTarget?.itemId == ref) hintPeekOffsetPx else NoPeek

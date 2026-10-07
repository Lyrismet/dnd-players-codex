package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.component.SwipeEditAction
import com.lyrismet.incadent.core.designsystem.component.SwipeToDeleteRow
import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.domain.model.EntityEditMode
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.action_edit
import dndplayerscodex.shared.generated.resources.codex_delete_content_description
import dndplayerscodex.shared.generated.resources.swipe_release_label
import org.jetbrains.compose.resources.stringResource

// a right swipe opens the form, so it is only offered in form mode - quick mode keeps rows delete-only
internal val CodexState.swipeEditEnabled: Boolean
    get() = editMode == EntityEditMode.FORM

@Composable
internal fun CodexDeletableRow(
    ref: EntityRef,
    eventSink: (CodexEvent) -> Unit,
    swipeEditEnabled: Boolean,
    modifier: Modifier = Modifier,
    peekOffsetPx: Float = 0f,
    content: @Composable () -> Unit,
) {
    SwipeToDeleteRow(
        onDeleteRequested = { eventSink(CodexEvent.EntityDeleteRequested(ref)) },
        modifier = modifier,
        deleteContentDescription = stringResource(Res.string.codex_delete_content_description),
        editAction = if (swipeEditEnabled) swipeEditAction(ref, eventSink) else null,
        peekOffsetPx = peekOffsetPx,
        content = content,
    )
}

// one row per entity type, shared by its own tab list and the ALL tab so the wiring lives in one place
@Composable
internal fun PartyRow(
    member: PartyCodexItem,
    eventSink: (CodexEvent) -> Unit,
    swipeEditEnabled: Boolean,
    peekOffsetPx: Float = 0f,
) {
    CodexDeletableRow(
        ref = EntityRef.Party(member.id),
        eventSink = eventSink,
        swipeEditEnabled = swipeEditEnabled,
        peekOffsetPx = peekOffsetPx,
    ) {
        PartyCodexCard(item = member, onClick = { eventSink(CodexEvent.EntityClicked(EntityRef.Party(member.id))) })
    }
}

@Composable
internal fun NpcRow(
    npc: NpcCodexItem,
    eventSink: (CodexEvent) -> Unit,
    swipeEditEnabled: Boolean,
    peekOffsetPx: Float = 0f,
) {
    CodexDeletableRow(
        ref = EntityRef.Npc(npc.id),
        eventSink = eventSink,
        swipeEditEnabled = swipeEditEnabled,
        peekOffsetPx = peekOffsetPx,
    ) {
        NpcCodexCard(item = npc, onClick = { eventSink(CodexEvent.EntityClicked(EntityRef.Npc(npc.id))) })
    }
}

@Composable
internal fun QuestRow(
    quest: QuestCodexItem,
    eventSink: (CodexEvent) -> Unit,
    swipeEditEnabled: Boolean,
    peekOffsetPx: Float = 0f,
) {
    CodexDeletableRow(
        ref = EntityRef.Quest(quest.id),
        eventSink = eventSink,
        swipeEditEnabled = swipeEditEnabled,
        peekOffsetPx = peekOffsetPx,
    ) {
        QuestCodexCard(
            item = quest,
            onClick = { eventSink(CodexEvent.EntityClicked(EntityRef.Quest(quest.id))) },
            onGiverClick = { eventSink(CodexEvent.EntityClicked(EntityRef.Npc(it))) },
        )
    }
}

@Composable
internal fun LocationRow(
    location: LocationCodexItem,
    eventSink: (CodexEvent) -> Unit,
    swipeEditEnabled: Boolean,
    peekOffsetPx: Float = 0f,
) {
    CodexDeletableRow(
        ref = EntityRef.Location(location.id),
        eventSink = eventSink,
        swipeEditEnabled = swipeEditEnabled,
        peekOffsetPx = peekOffsetPx,
    ) {
        LocationCodexCard(
            item = location,
            onClick = { eventSink(CodexEvent.EntityClicked(EntityRef.Location(location.id))) },
        )
    }
}

// the swipe-right "Изменить" of a row - only offered in form mode, see [swipeEditEnabled]
@Composable
private fun swipeEditAction(
    ref: EntityRef,
    eventSink: (CodexEvent) -> Unit,
): SwipeEditAction =
    SwipeEditAction(
        label = stringResource(Res.string.action_edit),
        releaseLabel = stringResource(Res.string.swipe_release_label),
        onEdit = { eventSink(CodexEvent.EditEntryRequested(ref)) },
    )

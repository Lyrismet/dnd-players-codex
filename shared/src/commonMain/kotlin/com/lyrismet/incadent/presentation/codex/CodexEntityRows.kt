package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lyrismet.incadent.core.designsystem.component.SwipeToDeleteRow
import com.lyrismet.incadent.core.entitysummary.EntityRef
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_delete_content_description
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CodexDeletableRow(
    ref: EntityRef,
    eventSink: (CodexEvent) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwipeToDeleteRow(
        onDeleteRequested = { eventSink(CodexEvent.EntityDeleteRequested(ref)) },
        modifier = modifier,
        deleteContentDescription = stringResource(Res.string.codex_delete_content_description),
        content = content,
    )
}

// one row per entity type, shared by its own tab list and the ALL tab so the wiring lives in one place
@Composable
internal fun PartyRow(
    member: PartyCodexItem,
    eventSink: (CodexEvent) -> Unit,
) {
    CodexDeletableRow(ref = EntityRef.Party(member.id), eventSink = eventSink) {
        PartyCodexCard(item = member, onClick = { eventSink(CodexEvent.EntityClicked(EntityRef.Party(member.id))) })
    }
}

@Composable
internal fun NpcRow(
    npc: NpcCodexItem,
    eventSink: (CodexEvent) -> Unit,
) {
    CodexDeletableRow(ref = EntityRef.Npc(npc.id), eventSink = eventSink) {
        NpcCodexCard(item = npc, onClick = { eventSink(CodexEvent.EntityClicked(EntityRef.Npc(npc.id))) })
    }
}

@Composable
internal fun QuestRow(
    quest: QuestCodexItem,
    eventSink: (CodexEvent) -> Unit,
) {
    CodexDeletableRow(ref = EntityRef.Quest(quest.id), eventSink = eventSink) {
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
) {
    CodexDeletableRow(ref = EntityRef.Location(location.id), eventSink = eventSink) {
        LocationCodexCard(
            item = location,
            onClick = { eventSink(CodexEvent.EntityClicked(EntityRef.Location(location.id))) },
        )
    }
}

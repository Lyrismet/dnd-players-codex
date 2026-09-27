package com.lyrismet.dndcodex.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lyrismet.dndcodex.core.designsystem.AppPalette
import com.lyrismet.dndcodex.core.designsystem.StatusColor
import com.lyrismet.dndcodex.core.designsystem.toStatusColor
import com.lyrismet.dndcodex.domain.model.Location
import com.lyrismet.dndcodex.domain.model.Npc
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.Quest
import com.lyrismet.dndcodex.domain.model.QuestStatus
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_npc_status_dead
import dndplayerscodex.shared.generated.resources.codex_npc_status_enemy
import dndplayerscodex.shared.generated.resources.codex_npc_status_friend
import dndplayerscodex.shared.generated.resources.codex_npc_status_neutral
import dndplayerscodex.shared.generated.resources.codex_quest_given_by_label
import dndplayerscodex.shared.generated.resources.codex_quest_reward_label
import dndplayerscodex.shared.generated.resources.codex_quest_status_active
import dndplayerscodex.shared.generated.resources.codex_quest_status_completed
import dndplayerscodex.shared.generated.resources.codex_quest_status_failed
import org.jetbrains.compose.resources.stringResource

/** the one codex entity a chip, a mention or a codex list row can point at - never more than one kind at a time */
sealed interface EntityRef {
    data class Npc(
        val id: Long,
    ) : EntityRef

    data class Location(
        val id: Long,
    ) : EntityRef

    data class Quest(
        val id: Long,
    ) : EntityRef
}

/** pre-formatted for direct rendering - shown inside an [AppBottomSheet], one shape per codex entity kind */
sealed interface EntitySummaryItem {
    data class NpcSummary(
        val name: String,
        val statusLabel: String,
        val statusColor: StatusColor,
        val description: String,
    ) : EntitySummaryItem

    data class LocationSummary(
        val name: String,
        val typeLabel: String,
        val description: String,
    ) : EntitySummaryItem

    data class QuestSummary(
        val title: String,
        val statusLabel: String,
        val statusColor: StatusColor,
        val giverName: String?,
        val reward: String,
    ) : EntitySummaryItem
}

/** everything [buildEntitySummary] needs to resolve any [EntityRef] - one instance covers a whole screen */
data class EntityLookup(
    val npcs: List<Npc>,
    val locations: List<Location>,
    val quests: List<Quest>,
    val npcStatusLabels: Map<NpcStatus, String>,
    val questStatusLabels: Map<QuestStatus, String>,
)

/** resolves [ref] against [lookup] - the one place every "tap a tag" entry point goes through */
fun buildEntitySummary(
    ref: EntityRef,
    lookup: EntityLookup,
): EntitySummaryItem? =
    when (ref) {
        is EntityRef.Npc ->
            lookup.npcs.find { it.id == ref.id }?.let { npc ->
                EntitySummaryItem.NpcSummary(
                    name = npc.name,
                    statusLabel = lookup.npcStatusLabels.getValue(npc.status),
                    statusColor = npc.status.toStatusColor(),
                    description = npc.description,
                )
            }

        is EntityRef.Location ->
            lookup.locations.find { it.id == ref.id }?.let { location ->
                EntitySummaryItem.LocationSummary(
                    name = location.name,
                    typeLabel = location.type,
                    description = location.description,
                )
            }

        is EntityRef.Quest ->
            lookup.quests.find { it.id == ref.id }?.let { quest ->
                EntitySummaryItem.QuestSummary(
                    title = quest.title,
                    statusLabel = lookup.questStatusLabels.getValue(quest.status),
                    statusColor = quest.status.toStatusColor(),
                    giverName = quest.givenByNpcId?.let { id -> lookup.npcs.find { it.id == id }?.name },
                    reward = quest.reward,
                )
            }
    }

@Composable
fun npcStatusLabels(): Map<NpcStatus, String> =
    mapOf(
        NpcStatus.FRIEND to stringResource(Res.string.codex_npc_status_friend),
        NpcStatus.ENEMY to stringResource(Res.string.codex_npc_status_enemy),
        NpcStatus.NEUTRAL to stringResource(Res.string.codex_npc_status_neutral),
        NpcStatus.DEAD to stringResource(Res.string.codex_npc_status_dead),
    )

@Composable
fun questStatusLabels(): Map<QuestStatus, String> =
    mapOf(
        QuestStatus.ACTIVE to stringResource(Res.string.codex_quest_status_active),
        QuestStatus.COMPLETED to stringResource(Res.string.codex_quest_status_completed),
        QuestStatus.FAILED to stringResource(Res.string.codex_quest_status_failed),
    )

/** the shared "resolve whatever's tapped" used by every screen that owns a nullable [EntityRef] selection */
@Composable
fun selectedEntitySummary(
    ref: EntityRef?,
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
): EntitySummaryItem? {
    if (ref == null) return null
    return buildEntitySummary(ref, EntityLookup(npcs, locations, quests, npcStatusLabels(), questStatusLabels()))
}

@Composable
fun EntitySummarySheetContent(
    item: EntitySummaryItem,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 44.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (item) {
            is EntitySummaryItem.NpcSummary -> NpcSummaryBody(item)
            is EntitySummaryItem.LocationSummary -> LocationSummaryBody(item)
            is EntitySummaryItem.QuestSummary -> QuestSummaryBody(item)
        }
    }
}

@Composable
private fun SheetTitleRow(
    title: String,
    statusLabel: String,
    statusColor: StatusColor,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = AppPalette.TextHeading,
            modifier = Modifier.weight(1f),
        )
        StatusBadge(text = statusLabel, color = statusColor)
    }
}

@Composable
private fun NpcSummaryBody(npc: EntitySummaryItem.NpcSummary) {
    SheetTitleRow(npc.name, npc.statusLabel, npc.statusColor)
    Text(npc.description, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
}

@Composable
private fun LocationSummaryBody(location: EntitySummaryItem.LocationSummary) {
    Text(location.name, style = MaterialTheme.typography.headlineLarge, color = AppPalette.TextHeading)
    Text(location.typeLabel, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextSecondary)
    Text(location.description, style = MaterialTheme.typography.bodyLarge, color = AppPalette.TextPrimary)
}

@Composable
private fun QuestSummaryBody(quest: EntitySummaryItem.QuestSummary) {
    SheetTitleRow(quest.title, quest.statusLabel, quest.statusColor)
    quest.giverName?.let { giverName ->
        SheetFactRow(stringResource(Res.string.codex_quest_given_by_label), giverName)
    }
    SheetFactRow(stringResource(Res.string.codex_quest_reward_label), quest.reward)
}

@Composable
private fun SheetFactRow(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextTertiary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = AppPalette.TextPrimary)
    }
}

package com.lyrismet.dndcodex.core.entitysummary

import androidx.compose.runtime.Composable
import com.lyrismet.dndcodex.core.designsystem.LocationMentionColor
import com.lyrismet.dndcodex.core.designsystem.StatusColor
import com.lyrismet.dndcodex.core.designsystem.component.MentionChipItem
import com.lyrismet.dndcodex.core.designsystem.component.toMentionChip
import com.lyrismet.dndcodex.core.designsystem.toStatusColor
import com.lyrismet.dndcodex.core.format.chronologicalNumberLabels
import com.lyrismet.dndcodex.core.format.toDisplayDate
import com.lyrismet.dndcodex.core.mention.MentionCandidate
import com.lyrismet.dndcodex.core.mention.MentionSegment
import com.lyrismet.dndcodex.core.mention.dedupeKey
import com.lyrismet.dndcodex.core.mention.parseMentions
import com.lyrismet.dndcodex.domain.model.Location
import com.lyrismet.dndcodex.domain.model.Npc
import com.lyrismet.dndcodex.domain.model.NpcStatus
import com.lyrismet.dndcodex.domain.model.Quest
import com.lyrismet.dndcodex.domain.model.QuestStatus
import com.lyrismet.dndcodex.domain.model.SessionEntry
import com.lyrismet.dndcodex.domain.model.SessionNote
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
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_npcs
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_quests
import dndplayerscodex.shared.generated.resources.entity_sheet_group_npc_quests_given
import dndplayerscodex.shared.generated.resources.entity_sheet_location_region_label
import dndplayerscodex.shared.generated.resources.entity_sheet_location_type_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_faction_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_location_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_race_label
import dndplayerscodex.shared.generated.resources.entity_sheet_quest_location_label
import org.jetbrains.compose.resources.StringResource
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

enum class EntityEmblemShape { CIRCLE, ROUNDED }

/** self-contained visual identity for the sheet header - initial letter (circle) or a glyph (rounded) */
data class EntityEmblem(
    val text: String,
    val shape: EntityEmblemShape,
    val color: StatusColor,
)

/** one row's value in the facts grid - plain text or a chip that reopens the sheet on another entity */
sealed interface FactValue {
    data class Text(
        val text: String,
    ) : FactValue

    data class Link(
        val chip: MentionChipItem,
    ) : FactValue
}

data class FactRow(
    val label: StringResource,
    val value: FactValue,
)

/** "выдал квесты" / "кто здесь" / "квесты" - a titled row of chips, each reopening the sheet in place */
data class RelationGroup(
    val title: StringResource,
    val items: List<MentionChipItem>,
)

data class RelatedNoteItem(
    val sessionNoteId: Long,
    val numberLabel: String,
    val title: String,
    val dateLabel: String,
    val snippet: String,
    val matchCount: Int,
)

data class StatusOption<T>(
    val value: T,
    val label: String,
    val color: StatusColor,
    val isSelected: Boolean,
)

/** pre-formatted for direct rendering - shown inside an [AppBottomSheet], one shape per codex entity kind */
sealed interface EntitySummaryItem {
    data class NpcSummary(
        val ref: EntityRef.Npc,
        val name: String,
        val statusLabel: String,
        val isDead: Boolean,
        val description: String,
        val emblem: EntityEmblem,
        val subtitle: String,
        val statusOptions: List<StatusOption<NpcStatus>>,
        val facts: List<FactRow>,
        val groups: List<RelationGroup>,
        val relatedNotes: List<RelatedNoteItem>,
    ) : EntitySummaryItem

    data class LocationSummary(
        val ref: EntityRef.Location,
        val name: String,
        val typeLabel: String,
        val description: String,
        val emblem: EntityEmblem,
        val subtitle: String,
        val facts: List<FactRow>,
        val groups: List<RelationGroup>,
        val relatedNotes: List<RelatedNoteItem>,
    ) : EntitySummaryItem

    data class QuestSummary(
        val ref: EntityRef.Quest,
        val title: String,
        val statusLabel: String,
        val reward: String,
        val emblem: EntityEmblem,
        val subtitle: String,
        val statusOptions: List<StatusOption<QuestStatus>>,
        val facts: List<FactRow>,
        val groups: List<RelationGroup>,
        val relatedNotes: List<RelatedNoteItem>,
    ) : EntitySummaryItem
}

/** everything [buildEntitySummary] needs to resolve any [EntityRef] - one instance covers a whole screen */
data class EntityLookup(
    val npcs: List<Npc>,
    val locations: List<Location>,
    val quests: List<Quest>,
    val npcStatusLabels: Map<NpcStatus, String>,
    val questStatusLabels: Map<QuestStatus, String>,
    val sessionNotes: List<SessionNote>,
    val sessionEntries: List<SessionEntry>,
    val mentionCandidates: List<MentionCandidate>,
)

/** resolves [ref] against [lookup] - the one place every "tap a tag" entry point goes through */
fun buildEntitySummary(
    ref: EntityRef,
    lookup: EntityLookup,
): EntitySummaryItem? =
    when (ref) {
        is EntityRef.Npc -> lookup.npcs.find { it.id == ref.id }?.let { buildNpcSummary(it, lookup) }
        is EntityRef.Location -> lookup.locations.find { it.id == ref.id }?.let { buildLocationSummary(it, lookup) }
        is EntityRef.Quest -> lookup.quests.find { it.id == ref.id }?.let { buildQuestSummary(it, lookup) }
    }

private fun buildNpcSummary(
    npc: Npc,
    lookup: EntityLookup,
): EntitySummaryItem.NpcSummary {
    val location = npc.locationId?.let { id -> lookup.locations.find { it.id == id } }
    val givenQuests = lookup.quests.filter { it.givenByNpcId == npc.id }
    return EntitySummaryItem.NpcSummary(
        ref = EntityRef.Npc(npc.id),
        name = npc.name,
        statusLabel = lookup.npcStatusLabels.getValue(npc.status),
        isDead = npc.status == NpcStatus.DEAD,
        description = npc.description,
        emblem = EntityEmblem(npc.name.take(1).uppercase(), EntityEmblemShape.CIRCLE, npc.status.toStatusColor()),
        subtitle = "${npc.race} · ${npc.faction}",
        statusOptions =
            statusOptions(NpcStatus.entries, npc.status, lookup.npcStatusLabels) { status -> status.toStatusColor() },
        facts =
            listOfNotNull(
                FactRow(Res.string.entity_sheet_npc_race_label, FactValue.Text(npc.race)),
                FactRow(Res.string.entity_sheet_npc_faction_label, FactValue.Text(npc.faction)),
                location?.let {
                    FactRow(Res.string.entity_sheet_npc_location_label, FactValue.Link(it.toMentionChip()))
                },
            ),
        groups =
            listOfNotNull(
                givenQuests.takeIf { it.isNotEmpty() }?.let {
                    RelationGroup(Res.string.entity_sheet_group_npc_quests_given, it.map { q -> q.toMentionChip() })
                },
            ),
        relatedNotes = buildRelatedNotes(EntityRef.Npc(npc.id), lookup),
    )
}

private fun buildQuestSummary(
    quest: Quest,
    lookup: EntityLookup,
): EntitySummaryItem.QuestSummary {
    val giver = quest.givenByNpcId?.let { id -> lookup.npcs.find { it.id == id } }
    val location = quest.locationId?.let { id -> lookup.locations.find { it.id == id } }
    return EntitySummaryItem.QuestSummary(
        ref = EntityRef.Quest(quest.id),
        title = quest.title,
        statusLabel = lookup.questStatusLabels.getValue(quest.status),
        reward = quest.reward,
        emblem = EntityEmblem("◆", EntityEmblemShape.ROUNDED, quest.status.toStatusColor()),
        subtitle = location?.name.orEmpty(),
        statusOptions =
            statusOptions(QuestStatus.entries, quest.status, lookup.questStatusLabels) { status ->
                status.toStatusColor()
            },
        facts =
            listOfNotNull(
                giver?.let { FactRow(Res.string.codex_quest_given_by_label, FactValue.Link(it.toMentionChip())) },
                FactRow(Res.string.codex_quest_reward_label, FactValue.Text(quest.reward)),
                location?.let {
                    FactRow(Res.string.entity_sheet_quest_location_label, FactValue.Link(it.toMentionChip()))
                },
            ),
        groups = emptyList(),
        relatedNotes = buildRelatedNotes(EntityRef.Quest(quest.id), lookup),
    )
}

private fun buildLocationSummary(
    location: Location,
    lookup: EntityLookup,
): EntitySummaryItem.LocationSummary {
    val npcsHere = lookup.npcs.filter { it.locationId == location.id }
    val questsHere = lookup.quests.filter { it.locationId == location.id }
    return EntitySummaryItem.LocationSummary(
        ref = EntityRef.Location(location.id),
        name = location.name,
        typeLabel = location.type,
        description = location.description,
        emblem = EntityEmblem("▲", EntityEmblemShape.ROUNDED, LocationMentionColor),
        subtitle = location.region,
        facts =
            listOf(
                FactRow(Res.string.entity_sheet_location_type_label, FactValue.Text(location.type)),
                FactRow(Res.string.entity_sheet_location_region_label, FactValue.Text(location.region)),
            ),
        groups =
            listOfNotNull(
                npcsHere.takeIf { it.isNotEmpty() }?.let {
                    RelationGroup(Res.string.entity_sheet_group_location_npcs, it.map { n -> n.toMentionChip() })
                },
                questsHere.takeIf { it.isNotEmpty() }?.let {
                    RelationGroup(Res.string.entity_sheet_group_location_quests, it.map { q -> q.toMentionChip() })
                },
            ),
        relatedNotes = buildRelatedNotes(EntityRef.Location(location.id), lookup),
    )
}

private fun <T> statusOptions(
    values: List<T>,
    current: T,
    labels: Map<T, String>,
    colorOf: (T) -> StatusColor,
): List<StatusOption<T>> =
    values.map { value -> StatusOption(value, labels.getValue(value), colorOf(value), value == current) }

private const val SNIPPET_MAX_LENGTH = 110

private fun EntityRef.dedupeKey(): String =
    when (this) {
        is EntityRef.Npc -> "npc:$id"
        is EntityRef.Location -> "location:$id"
        is EntityRef.Quest -> "quest:$id"
    }

/** every session with at least one note that `@mentions` [ref], newest first, with a snippet and hit count */
private fun buildRelatedNotes(
    ref: EntityRef,
    lookup: EntityLookup,
): List<RelatedNoteItem> {
    val entriesBySession = lookup.sessionEntries.groupBy { it.sessionNoteId }
    val numberLabels = lookup.sessionNotes.chronologicalNumberLabels()
    return lookup.sessionNotes
        .sortedByDescending { it.sessionDate }
        .mapNotNull { note ->
            val hits =
                entriesBySession[note.id].orEmpty().filter { entry ->
                    parseMentions(entry.body, lookup.mentionCandidates)
                        .filterIsInstance<MentionSegment.Mention>()
                        .any { it.entity.dedupeKey() == ref.dedupeKey() }
                }
            if (hits.isEmpty()) {
                null
            } else {
                RelatedNoteItem(
                    sessionNoteId = note.id,
                    numberLabel = numberLabels.getValue(note.id),
                    title = note.title,
                    dateLabel = note.sessionDate.toDisplayDate(),
                    snippet = plainTextSnippet(hits.first().body, lookup.mentionCandidates),
                    matchCount = hits.size,
                )
            }
        }
}

private fun plainTextSnippet(
    body: String,
    candidates: List<MentionCandidate>,
): String {
    val plain =
        parseMentions(body, candidates).joinToString("") { segment ->
            when (segment) {
                is MentionSegment.Text -> segment.text
                is MentionSegment.Mention -> segment.entity.name
            }
        }
    return if (plain.length > SNIPPET_MAX_LENGTH) plain.take(SNIPPET_MAX_LENGTH - 1) + "…" else plain
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
    sessionNotes: List<SessionNote>,
    sessionEntries: List<SessionEntry>,
    mentionCandidates: List<MentionCandidate>,
): EntitySummaryItem? {
    if (ref == null) return null
    return buildEntitySummary(
        ref,
        EntityLookup(
            npcs = npcs,
            locations = locations,
            quests = quests,
            npcStatusLabels = npcStatusLabels(),
            questStatusLabels = questStatusLabels(),
            sessionNotes = sessionNotes,
            sessionEntries = sessionEntries,
            mentionCandidates = mentionCandidates,
        ),
    )
}

/** actions the sheet's interactive pieces dispatch - one bundle instead of one lambda param per feature */
data class EntitySummarySheetActions(
    val onEntityRefClicked: (EntityRef) -> Unit,
    val onNpcStatusSelected: (npcId: Long, status: NpcStatus) -> Unit,
    val onQuestStatusSelected: (questId: Long, status: QuestStatus) -> Unit,
    val onRelatedNoteClicked: (sessionNoteId: Long) -> Unit,
)

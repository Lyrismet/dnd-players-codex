package com.lyrismet.incadent.core.entitysummary

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.core.designsystem.LocationMentionColor
import com.lyrismet.incadent.core.designsystem.PartyMemberColor
import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.designsystem.component.MentionChipItem
import com.lyrismet.incadent.core.designsystem.component.MentionGlyph
import com.lyrismet.incadent.core.designsystem.component.toMentionChip
import com.lyrismet.incadent.core.designsystem.toStatusColor
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.core.mention.MentionCandidate
import com.lyrismet.incadent.core.quickedit.InlineEdit
import com.lyrismet.incadent.core.quickedit.QuickEditField
import com.lyrismet.incadent.core.quickedit.QuickEditUiEvent
import com.lyrismet.incadent.domain.model.Location
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.PartyMember
import com.lyrismet.incadent.domain.model.PartyPresence
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.model.SessionEntry
import com.lyrismet.incadent.domain.model.SessionNote
import com.lyrismet.incadent.domain.model.SessionNumbering
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_initiative_bonus
import dndplayerscodex.shared.generated.resources.codex_quest_given_by_label
import dndplayerscodex.shared.generated.resources.codex_quest_reward_label
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_npcs
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_quests
import dndplayerscodex.shared.generated.resources.entity_sheet_group_npc_quests_given
import dndplayerscodex.shared.generated.resources.entity_sheet_location_region_label
import dndplayerscodex.shared.generated.resources.entity_sheet_location_type_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_faction_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_location_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_race_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_ac_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_class_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_hp_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_level_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_player_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_you_label
import dndplayerscodex.shared.generated.resources.entity_sheet_quest_location_label
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** the one codex entity a chip, a mention or a codex list row can point at - never more than one kind at a time */
sealed interface EntityRef {
    val id: Long

    data class Npc(
        override val id: Long,
    ) : EntityRef

    data class Location(
        override val id: Long,
    ) : EntityRef

    data class Quest(
        override val id: Long,
    ) : EntityRef

    data class Party(
        override val id: Long,
    ) : EntityRef
}

enum class EntityEmblemShape { CIRCLE, ROUNDED }

/** self-contained visual identity for the sheet header - initial letter (circle) or a glyph (rounded) */
data class EntityEmblem(
    val text: String,
    val shape: EntityEmblemShape,
    val color: StatusColor,
    // only an npc or a party member can carry a portrait - see Players Codex v6.dc.html's `por` flag
    val portraitBase64: String? = null,
    val isMonochrome: Boolean = false,
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
    // null keeps the fact read-only even in quick-edit mode
    val edit: QuickEditField? = null,
    // the entities a link fact can be re-pointed at - only read when [edit] is a link field
    val linkOptions: List<MentionChipItem> = emptyList(),
)

/** "выдал квесты" / "кто здесь" / "квесты" - a titled row of chips, each reopening the sheet in place */
data class RelationGroup(
    val title: StringResource,
    val items: List<MentionChipItem>,
)

data class RelatedNoteItem(
    val sessionNoteId: Long,
    // the first matching entry, so opening the note can scroll to and highlight it
    val entryId: Long,
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
        val lifeOptions: List<StatusOption<NpcLifeState>>,
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
        val description: String,
        val emblem: EntityEmblem,
        val subtitle: String,
        val statusOptions: List<StatusOption<QuestStatus>>,
        val facts: List<FactRow>,
        val groups: List<RelationGroup>,
        val relatedNotes: List<RelatedNoteItem>,
    ) : EntitySummaryItem

    data class PartySummary(
        val ref: EntityRef.Party,
        val name: String,
        val overlineValue: String,
        val subtitle: String,
        val description: String,
        val emblem: EntityEmblem,
        val presenceOptions: List<StatusOption<PartyPresence>>,
        val facts: List<FactRow>,
    ) : EntitySummaryItem
}

/** everything [buildEntitySummary] needs to resolve any [EntityRef] - one instance covers a whole screen */
data class EntityLookup(
    val npcs: List<Npc>,
    val locations: List<Location>,
    val quests: List<Quest>,
    val npcStatusLabels: Map<NpcStatus, String>,
    val npcLifeLabels: Map<NpcLifeState, String>,
    val questStatusLabels: Map<QuestStatus, String>,
    val parties: List<PartyMember>,
    val partyPresenceLabels: Map<PartyPresence, String>,
    val partyYouLabel: String,
    val sessionNotes: List<SessionNote>,
    val sessionEntries: List<SessionEntry>,
    val mentionCandidates: List<MentionCandidate>,
    val sessionNumbering: SessionNumbering,
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
        is EntityRef.Party -> lookup.parties.find { it.id == ref.id }?.let { buildPartySummary(it, lookup) }
    }

private fun buildNpcSummary(
    npc: Npc,
    lookup: EntityLookup,
): EntitySummaryItem.NpcSummary {
    val location = npc.locationId?.let { id -> lookup.locations.find { it.id == id } }
    val givenQuests = lookup.quests.filter { it.givenByNpcId == npc.id }
    val isDead = npc.lifeState == NpcLifeState.DEAD
    val relationLabel = lookup.npcStatusLabels.getValue(npc.status)
    return EntitySummaryItem.NpcSummary(
        ref = EntityRef.Npc(npc.id),
        name = npc.name,
        statusLabel =
            if (isDead) joinWithDot(relationLabel, lookup.npcLifeLabels.getValue(npc.lifeState)) else relationLabel,
        isDead = isDead,
        description = npc.description,
        emblem =
            EntityEmblem(
                text = npc.name.take(1).uppercase(),
                shape = EntityEmblemShape.CIRCLE,
                color = npc.status.toStatusColor(),
                portraitBase64 = npc.portraitBase64,
                isMonochrome = isDead,
            ),
        subtitle = "${npc.race} · ${npc.faction}",
        statusOptions =
            statusOptions(NpcStatus.entries, npc.status, lookup.npcStatusLabels) { status -> status.toStatusColor() },
        lifeOptions =
            statusOptions(NpcLifeState.entries, npc.lifeState, lookup.npcLifeLabels) { life -> life.toStatusColor() },
        facts =
            listOf(
                FactRow(Res.string.entity_sheet_npc_race_label, FactValue.Text(npc.race), QuickEditField.RACE),
                FactRow(Res.string.entity_sheet_npc_faction_label, FactValue.Text(npc.faction), QuickEditField.FACTION),
                linkFact(
                    Res.string.entity_sheet_npc_location_label,
                    location?.toMentionChip(),
                    QuickEditField.PLACE,
                    lookup.locations.map { it.toMentionChip() },
                ),
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

private fun buildPartySummary(
    member: PartyMember,
    lookup: EntityLookup,
): EntitySummaryItem.PartySummary {
    val ownerLabel = if (member.isPlayerCharacter) lookup.partyYouLabel else member.playerName
    return EntitySummaryItem.PartySummary(
        ref = EntityRef.Party(member.id),
        name = member.name,
        overlineValue = joinWithDot(member.characterClass, if (member.isPlayerCharacter) lookup.partyYouLabel else ""),
        subtitle = member.race,
        description = member.description,
        emblem =
            EntityEmblem(
                text = member.name.take(1).uppercase(),
                shape = EntityEmblemShape.CIRCLE,
                color = PartyMemberColor,
                portraitBase64 = member.portraitBase64,
            ),
        presenceOptions =
            statusOptions(PartyPresence.entries, member.presence, lookup.partyPresenceLabels) { presence ->
                presence.toStatusColor()
            },
        facts =
            listOf(
                FactRow(
                    Res.string.entity_sheet_party_player_label,
                    FactValue.Text(ownerLabel),
                    edit = if (member.isPlayerCharacter) null else QuickEditField.PLAYER_NAME,
                ),
                FactRow(
                    Res.string.entity_sheet_party_class_label,
                    FactValue.Text(member.characterClass),
                    QuickEditField.CHARACTER_CLASS,
                ),
                FactRow(Res.string.entity_sheet_npc_race_label, FactValue.Text(member.race), QuickEditField.RACE),
                FactRow(
                    Res.string.entity_sheet_party_level_label,
                    FactValue.Text(member.level.toString()),
                    QuickEditField.LEVEL,
                ),
                FactRow(
                    Res.string.entity_sheet_party_ac_label,
                    FactValue.Text(member.armorClass.toString()),
                    QuickEditField.ARMOR_CLASS,
                ),
                FactRow(
                    Res.string.entity_sheet_party_hp_label,
                    FactValue.Text(member.hpMax.toString()),
                    QuickEditField.HP_MAX,
                ),
                FactRow(
                    Res.string.codex_entry_label_initiative_bonus,
                    FactValue.Text(signedNumber(member.initiativeBonus)),
                    QuickEditField.INITIATIVE_BONUS,
                ),
            ),
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
        description = quest.description,
        emblem = EntityEmblem(MentionGlyph.QUEST.symbol, EntityEmblemShape.ROUNDED, quest.status.toStatusColor()),
        subtitle = location?.name.orEmpty(),
        statusOptions =
            statusOptions(QuestStatus.entries, quest.status, lookup.questStatusLabels) { status ->
                status.toStatusColor()
            },
        facts =
            listOf(
                linkFact(
                    Res.string.codex_quest_given_by_label,
                    giver?.toMentionChip(),
                    QuickEditField.GIVER,
                    lookup.npcs.map { it.toMentionChip() },
                ),
                FactRow(Res.string.codex_quest_reward_label, FactValue.Text(quest.reward), QuickEditField.REWARD),
                linkFact(
                    Res.string.entity_sheet_quest_location_label,
                    location?.toMentionChip(),
                    QuickEditField.PLACE,
                    lookup.locations.map { it.toMentionChip() },
                ),
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
        emblem = EntityEmblem(MentionGlyph.LOCATION.symbol, EntityEmblemShape.ROUNDED, LocationMentionColor),
        subtitle = location.region,
        facts =
            listOf(
                FactRow(
                    Res.string.entity_sheet_location_type_label,
                    FactValue.Text(location.type),
                    QuickEditField.LOCATION_TYPE,
                ),
                FactRow(
                    Res.string.entity_sheet_location_region_label,
                    FactValue.Text(location.region),
                    QuickEditField.LOCATION_REGION,
                ),
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

/** a link fact always has its row, so an empty link stays editable - it shows as a dash until something is picked */
private fun linkFact(
    label: StringResource,
    chip: MentionChipItem?,
    field: QuickEditField,
    options: List<MentionChipItem>,
): FactRow = FactRow(label, chip?.let { FactValue.Link(it) } ?: FactValue.Text(""), field, options)

private fun signedNumber(value: Int): String = if (value > 0) "+$value" else value.toString()

private fun <T> statusOptions(
    values: List<T>,
    current: T,
    labels: Map<T, String>,
    colorOf: (T) -> StatusColor,
): List<StatusOption<T>> =
    values.map { value -> StatusOption(value, labels.getValue(value), colorOf(value), value == current) }

/** the records plus resolved labels a summary is built from - composable only because the labels come from resources */
@Composable
fun entityLookupOf(
    npcs: List<Npc>,
    locations: List<Location>,
    quests: List<Quest>,
    sessionNotes: List<SessionNote>,
    sessionEntries: List<SessionEntry>,
    mentionCandidates: List<MentionCandidate>,
    sessionNumbering: SessionNumbering,
    parties: List<PartyMember>,
): EntityLookup =
    EntityLookup(
        npcs = npcs,
        locations = locations,
        quests = quests,
        npcStatusLabels = npcStatusLabels(),
        npcLifeLabels = npcLifeLabels(),
        questStatusLabels = questStatusLabels(),
        parties = parties,
        partyPresenceLabels = partyPresenceLabels(),
        partyYouLabel = stringResource(Res.string.entity_sheet_party_you_label),
        sessionNotes = sessionNotes,
        sessionEntries = sessionEntries,
        mentionCandidates = mentionCandidates,
        sessionNumbering = sessionNumbering,
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
    sessionNumbering: SessionNumbering,
    parties: List<PartyMember>,
): EntitySummaryItem? {
    if (ref == null) return null
    val lookup =
        entityLookupOf(
            npcs,
            locations,
            quests,
            sessionNotes,
            sessionEntries,
            mentionCandidates,
            sessionNumbering,
            parties,
        )
    return buildEntitySummary(ref, lookup)
}

/** actions the sheet's interactive pieces dispatch - one bundle instead of one lambda param per feature */
data class EntitySummarySheetActions(
    val onEntityRefClicked: (EntityRef) -> Unit,
    val onNpcStatusSelected: (npcId: Long, status: NpcStatus) -> Unit,
    val onNpcLifeSelected: (npcId: Long, lifeState: NpcLifeState) -> Unit,
    val onQuestStatusSelected: (questId: Long, status: QuestStatus) -> Unit,
    val onRelatedNoteClicked: (sessionNoteId: Long, entryId: Long) -> Unit,
    val onEditClicked: ((EntityRef) -> Unit)? = null,
    // null hides the presence picker - only the codex manages party membership
    val onPartyPresenceSelected: ((partyId: Long, presence: PartyPresence) -> Unit)? = null,
    // true shows the status pickers as plain badges - the form-mode card is read-only
    val statusesReadOnly: Boolean = false,
    // null keeps the card read-only - in-place editing only exists in the codex
    val quickEdit: QuickEditSheet? = null,
)

/** the in-place editing state a card renders - the presenter owns it, the card only shows it and forwards events */
data class QuickEditSheet(
    val inlineEdit: InlineEdit?,
    val holdTipVisible: Boolean,
    val onEvent: (QuickEditUiEvent) -> Unit,
)

package com.lyrismet.incadent.feature.codex.presentation.summary

import com.lyrismet.incadent.core.designsystem.StatusColor
import com.lyrismet.incadent.core.format.joinWithDot
import com.lyrismet.incadent.core.format.orDash
import com.lyrismet.incadent.feature.codex.domain.grouping.PlaceTree
import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.domain.model.joinNicknames
import com.lyrismet.incadent.feature.codex.presentation.quickedit.QuickEditField
import com.lyrismet.incadent.feature.codex.presentation.style.DEFAULT_STATUS_GLYPH
import com.lyrismet.incadent.feature.codex.presentation.style.LocationMentionColor
import com.lyrismet.incadent.feature.codex.presentation.style.PartyMemberColor
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusColor
import com.lyrismet.incadent.feature.codex.presentation.style.toStatusGlyph
import com.lyrismet.incadent.feature.mention.presentation.MentionChipItem
import com.lyrismet.incadent.feature.mention.presentation.MentionGlyph
import com.lyrismet.incadent.feature.mention.presentation.toMentionChip
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_quest_given_by_label
import dndplayerscodex.shared.generated.resources.codex_quest_reward_label
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_inside
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_inside_empty
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_npcs
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_npcs_empty
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_quests
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_quests_empty
import dndplayerscodex.shared.generated.resources.entity_sheet_group_npc_quests_given
import dndplayerscodex.shared.generated.resources.entity_sheet_group_npc_quests_given_empty
import dndplayerscodex.shared.generated.resources.entity_sheet_location_parent_label
import dndplayerscodex.shared.generated.resources.entity_sheet_nicknames_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_location_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_race_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_class_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_hp_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_level_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_player_label
import dndplayerscodex.shared.generated.resources.entity_sheet_quest_location_label
import org.jetbrains.compose.resources.StringResource

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
    val places = npc.locationIds.mapNotNull { id -> lookup.locations.find { it.id == id } }
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
                portrait = npc.portrait,
                isMonochrome = isDead,
                hasPortraitSlot = true,
            ),
        subtitle = npc.race,
        statusOptions =
            statusOptions(NpcStatus.entries, npc.status, lookup.npcStatusLabels, NpcStatus::toStatusGlyph) { status ->
                status.toStatusColor()
            },
        lifeOptions =
            statusOptions(
                NpcLifeState.entries,
                npc.lifeState,
                lookup.npcLifeLabels,
                NpcLifeState::toStatusGlyph,
            ) { life ->
                life.toStatusColor()
            },
        facts =
            listOf(
                FactRow(Res.string.entity_sheet_npc_race_label, FactValue.Text(npc.race), QuickEditField.RACE),
                nicknamesFact(npc.nicknames),
                FactRow(
                    Res.string.entity_sheet_npc_location_label,
                    FactValue.Links(places.map { it.toMentionChip() }),
                    QuickEditField.PLACE,
                ),
            ),
        groups =
            listOf(
                RelationGroup(
                    title = Res.string.entity_sheet_group_npc_quests_given,
                    items = givenQuests.map { q -> q.toMentionChip() },
                    edit = QuickEditField.GIVEN_QUESTS,
                    options = lookup.quests.map { RelationOption(it.toMentionChip(), it.givenByNpcId == npc.id) },
                    emptyText = Res.string.entity_sheet_group_npc_quests_given_empty,
                ),
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
                portrait = member.portrait,
                hasPortraitSlot = true,
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
                    FactValue.Text(member.level.orDash()),
                    QuickEditField.LEVEL,
                ),
                FactRow(
                    Res.string.entity_sheet_party_hp_label,
                    FactValue.Text(member.hpMax.orDash()),
                    QuickEditField.HP_MAX,
                ),
            ),
        relatedNotes = buildRelatedNotes(EntityRef.Party(member.id), lookup),
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
            statusOptions(
                QuestStatus.entries,
                quest.status,
                lookup.questStatusLabels,
                QuestStatus::toStatusGlyph,
            ) { status ->
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
    val tree = PlaceTree(lookup.locations)
    // an npc met in a place inside this one counts as met here too
    val insideIds = tree.descendantIds(location.id) + location.id
    val npcsHere = lookup.npcs.filter { npc -> npc.locationIds.any { it in insideIds } }
    val questsHere = lookup.quests.filter { it.locationId == location.id }
    val placesInside = lookup.locations.filter { it.parentId == location.id }
    val parent = tree.find(location.parentId)
    val hasPlaceToBeIn = lookup.locations.any { other -> other.id != location.id }
    return EntitySummaryItem.LocationSummary(
        ref = EntityRef.Location(location.id),
        name = location.name,
        description = location.description,
        cover = location.cover,
        emblem = EntityEmblem(MentionGlyph.LOCATION.symbol, EntityEmblemShape.ROUNDED, LocationMentionColor),
        subtitle = tree.ancestorsLabel(location),
        // with no other place in the codex there is nothing to be inside of, so the row would be a dead end
        facts =
            listOfNotNull(
                linkFact(
                    Res.string.entity_sheet_location_parent_label,
                    parent?.toMentionChip(),
                    QuickEditField.PARENT,
                ).takeIf { hasPlaceToBeIn || parent != null },
            ),
        groups =
            listOf(
                RelationGroup(
                    title = Res.string.entity_sheet_group_location_inside,
                    items = placesInside.map { p -> p.toMentionChip() },
                    edit = QuickEditField.INSIDE.takeIf { hasPlaceToBeIn },
                    emptyText = Res.string.entity_sheet_group_location_inside_empty,
                ),
                RelationGroup(
                    title = Res.string.entity_sheet_group_location_npcs,
                    items = npcsHere.map { n -> n.toMentionChip() },
                    edit = QuickEditField.RESIDENTS,
                    // only the npcs met right here are ticked, the ones of a place inside are shown but not unticked
                    options = lookup.npcs.map { RelationOption(it.toMentionChip(), location.id in it.locationIds) },
                    emptyText = Res.string.entity_sheet_group_location_npcs_empty,
                ),
                RelationGroup(
                    title = Res.string.entity_sheet_group_location_quests,
                    items = questsHere.map { q -> q.toMentionChip() },
                    edit = QuickEditField.PLACE_QUESTS,
                    options = lookup.quests.map { RelationOption(it.toMentionChip(), it.locationId == location.id) },
                    emptyText = Res.string.entity_sheet_group_location_quests_empty,
                ),
            ),
        relatedNotes = buildRelatedNotes(EntityRef.Location(location.id), lookup),
        canMove = hasPlaceToBeIn,
    )
}

/** a link fact always has its row, so an empty link stays editable - it shows as a dash until something is picked */
private fun linkFact(
    label: StringResource,
    chip: MentionChipItem?,
    field: QuickEditField,
    options: List<MentionChipItem> = emptyList(),
): FactRow = FactRow(label, chip?.let { FactValue.Link(it) } ?: FactValue.Text(""), field, options)

private fun <T> statusOptions(
    values: List<T>,
    current: T,
    labels: Map<T, String>,
    glyphOf: (T) -> String = { DEFAULT_STATUS_GLYPH },
    colorOf: (T) -> StatusColor,
): List<StatusOption<T>> =
    values.map { value ->
        StatusOption(value, labels.getValue(value), colorOf(value), value == current, glyphOf(value))
    }

private fun nicknamesFact(nicknames: List<String>) =
    FactRow(
        Res.string.entity_sheet_nicknames_label,
        FactValue.Text(nicknames.joinNicknames()),
        QuickEditField.NICKNAMES,
    )

package com.lyrismet.incadent.feature.codex.presentation

import androidx.compose.runtime.Composable
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.grouping.CodexGroupLabels
import com.lyrismet.incadent.feature.codex.domain.grouping.LocationGroupBy
import com.lyrismet.incadent.feature.codex.domain.grouping.NpcGroupBy
import com.lyrismet.incadent.feature.codex.domain.grouping.QuestGroupBy
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.codex.presentation.summary.npcLifeLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.npcStatusLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.partyPresenceLabels
import com.lyrismet.incadent.feature.codex.presentation.summary.questStatusLabels
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_type_location
import dndplayerscodex.shared.generated.resources.codex_entry_type_npc
import dndplayerscodex.shared.generated.resources.codex_entry_type_party
import dndplayerscodex.shared.generated.resources.codex_entry_type_quest
import dndplayerscodex.shared.generated.resources.codex_entry_type_species
import dndplayerscodex.shared.generated.resources.codex_group_giver
import dndplayerscodex.shared.generated.resources.codex_group_giver_prefix
import dndplayerscodex.shared.generated.resources.codex_group_no_giver
import dndplayerscodex.shared.generated.resources.codex_group_no_place
import dndplayerscodex.shared.generated.resources.codex_group_no_region_quest
import dndplayerscodex.shared.generated.resources.codex_group_none
import dndplayerscodex.shared.generated.resources.codex_group_region
import dndplayerscodex.shared.generated.resources.codex_group_relation
import dndplayerscodex.shared.generated.resources.codex_group_status
import dndplayerscodex.shared.generated.resources.codex_group_top_level
import dndplayerscodex.shared.generated.resources.codex_group_tree
import dndplayerscodex.shared.generated.resources.codex_party_card_hp
import dndplayerscodex.shared.generated.resources.codex_party_card_player_prefix
import dndplayerscodex.shared.generated.resources.codex_party_card_you
import dndplayerscodex.shared.generated.resources.codex_party_fallen_word
import dndplayerscodex.shared.generated.resources.codex_undo_deleted_title
import org.jetbrains.compose.resources.stringResource

/** the labels the codex screen resolves from resources, collected once per composition */
internal class CodexLabels(
    val partyCard: PartyCardLabels,
    val entityType: Map<EntityKind, String>,
    val undoDeletedTitle: String,
    val grouped: CodexGroupedLabels,
)

@Composable
internal fun codexLabels(): CodexLabels {
    val partyPresence = partyPresenceLabels()
    return CodexLabels(
        partyCard =
            PartyCardLabels(
                presence = partyPresence,
                you = stringResource(Res.string.codex_party_card_you),
                playerPrefix = stringResource(Res.string.codex_party_card_player_prefix),
                hpShort = stringResource(Res.string.codex_party_card_hp),
                fallen = stringResource(Res.string.codex_party_fallen_word),
            ),
        entityType =
            mapOf(
                EntityKind.PARTY to stringResource(Res.string.codex_entry_type_party),
                EntityKind.NPC to stringResource(Res.string.codex_entry_type_npc),
                EntityKind.QUEST to stringResource(Res.string.codex_entry_type_quest),
                EntityKind.LOCATION to stringResource(Res.string.codex_entry_type_location),
                EntityKind.SPECIES to stringResource(Res.string.codex_entry_type_species),
            ),
        undoDeletedTitle = stringResource(Res.string.codex_undo_deleted_title),
        grouped = groupedLabels(npcStatusLabels(), questStatusLabels()),
    )
}

@Composable
private fun groupedLabels(
    npcStatus: Map<NpcStatus, String>,
    questStatus: Map<QuestStatus, String>,
): CodexGroupedLabels =
    CodexGroupedLabels(
        titles =
            CodexGroupLabels(
                noRegionForQuest = stringResource(Res.string.codex_group_no_region_quest),
                noPlace = stringResource(Res.string.codex_group_no_place),
                noGiver = stringResource(Res.string.codex_group_no_giver),
                giverPrefix = stringResource(Res.string.codex_group_giver_prefix),
                topLevel = stringResource(Res.string.codex_group_top_level),
                npcStatus = npcStatus,
                questStatus = questStatus,
            ),
        options =
            CodexGroupOptionLabels(
                npc =
                    mapOf(
                        NpcGroupBy.REGION to stringResource(Res.string.codex_group_region),
                        NpcGroupBy.RELATION to stringResource(Res.string.codex_group_relation),
                        NpcGroupBy.NONE to stringResource(Res.string.codex_group_none),
                    ),
                quest =
                    mapOf(
                        QuestGroupBy.REGION to stringResource(Res.string.codex_group_region),
                        QuestGroupBy.STATUS to stringResource(Res.string.codex_group_status),
                        QuestGroupBy.GIVER to stringResource(Res.string.codex_group_giver),
                        QuestGroupBy.NONE to stringResource(Res.string.codex_group_none),
                    ),
                place =
                    mapOf(
                        LocationGroupBy.REGION to stringResource(Res.string.codex_group_tree),
                        LocationGroupBy.NONE to stringResource(Res.string.codex_group_none),
                    ),
            ),
        npcLife = npcLifeLabels(),
    )

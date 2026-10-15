package com.lyrismet.incadent.feature.codex.presentation.quickedit

import androidx.compose.runtime.Composable
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_entry_label_cover
import dndplayerscodex.shared.generated.resources.codex_entry_label_portrait
import dndplayerscodex.shared.generated.resources.codex_quest_given_by_label
import dndplayerscodex.shared.generated.resources.codex_quest_reward_label
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_npcs
import dndplayerscodex.shared.generated.resources.entity_sheet_group_location_quests
import dndplayerscodex.shared.generated.resources.entity_sheet_group_npc_quests_given
import dndplayerscodex.shared.generated.resources.entity_sheet_life_label
import dndplayerscodex.shared.generated.resources.entity_sheet_location_parent_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_faction_label
import dndplayerscodex.shared.generated.resources.entity_sheet_npc_race_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_class_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_hp_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_level_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_player_label
import dndplayerscodex.shared.generated.resources.entity_sheet_party_presence_label
import dndplayerscodex.shared.generated.resources.entity_sheet_quest_location_label
import dndplayerscodex.shared.generated.resources.entity_sheet_relation_label
import dndplayerscodex.shared.generated.resources.entity_sheet_status_label
import dndplayerscodex.shared.generated.resources.quick_edit_label_description
import dndplayerscodex.shared.generated.resources.quick_edit_label_name
import org.jetbrains.compose.resources.stringResource

/** the human name of every editable field - used for undo toasts, so it covers statuses too */
@Composable
fun rememberQuickEditFieldTitles(): Map<QuickEditField, String> =
    mapOf(
        QuickEditField.NAME to stringResource(Res.string.quick_edit_label_name),
        QuickEditField.DESCRIPTION to stringResource(Res.string.quick_edit_label_description),
        QuickEditField.RACE to stringResource(Res.string.entity_sheet_npc_race_label),
        QuickEditField.FACTION to stringResource(Res.string.entity_sheet_npc_faction_label),
        QuickEditField.CHARACTER_CLASS to stringResource(Res.string.entity_sheet_party_class_label),
        QuickEditField.PLAYER_NAME to stringResource(Res.string.entity_sheet_party_player_label),
        QuickEditField.REWARD to stringResource(Res.string.codex_quest_reward_label),
        QuickEditField.LEVEL to stringResource(Res.string.entity_sheet_party_level_label),
        QuickEditField.HP_MAX to stringResource(Res.string.entity_sheet_party_hp_label),
        QuickEditField.GIVER to stringResource(Res.string.codex_quest_given_by_label),
        QuickEditField.PLACE to stringResource(Res.string.entity_sheet_quest_location_label),
        QuickEditField.PARENT to stringResource(Res.string.entity_sheet_location_parent_label),
        QuickEditField.RELATION to stringResource(Res.string.entity_sheet_relation_label),
        QuickEditField.LIFE to stringResource(Res.string.entity_sheet_life_label),
        QuickEditField.QUEST_STATUS to stringResource(Res.string.entity_sheet_status_label),
        QuickEditField.PRESENCE to stringResource(Res.string.entity_sheet_party_presence_label),
        QuickEditField.PORTRAIT to stringResource(Res.string.codex_entry_label_portrait),
        QuickEditField.COVER to stringResource(Res.string.codex_entry_label_cover),
        QuickEditField.GIVEN_QUESTS to stringResource(Res.string.entity_sheet_group_npc_quests_given),
        QuickEditField.RESIDENTS to stringResource(Res.string.entity_sheet_group_location_npcs),
        QuickEditField.PLACE_QUESTS to stringResource(Res.string.entity_sheet_group_location_quests),
    )

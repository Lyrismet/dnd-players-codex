package com.lyrismet.incadent.feature.me.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.lyrismet.incadent.core.undo.UndoController
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.codex.domain.model.CharacterSheet
import com.lyrismet.incadent.feature.codex.domain.model.CodexRecords
import com.lyrismet.incadent.feature.codex.domain.model.ItemOwner
import com.lyrismet.incadent.feature.codex.domain.model.PartyMember
import com.lyrismet.incadent.feature.codex.domain.repository.CodexRepositories
import com.lyrismet.incadent.feature.codex.presentation.form.rememberMentionSource
import com.lyrismet.incadent.feature.codex.presentation.item.ItemsController
import com.lyrismet.incadent.feature.codex.presentation.item.rememberItemsState
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import com.lyrismet.incadent.feature.mention.presentation.MentionField
import incadent.shared.generated.resources.Res
import incadent.shared.generated.resources.codex_entry_type_location
import incadent.shared.generated.resources.codex_entry_type_npc
import incadent.shared.generated.resources.codex_entry_type_party
import org.jetbrains.compose.resources.stringResource

/** the codex records the item sheet and the "@" of the notes work with */
@Composable
internal fun rememberMeRecords(
    repositories: CodexRepositories,
    parties: List<PartyMember>,
): CodexRecords {
    val npcs by remember { repositories.npcRepository.observeAll() }.collectAsState(initial = emptyList())
    val quests by remember { repositories.questRepository.observeAll() }.collectAsState(initial = emptyList())
    val locations by remember { repositories.locationRepository.observeAll() }.collectAsState(initial = emptyList())
    val species by remember { repositories.speciesRepository.observeAll() }.collectAsState(initial = emptyList())
    val items by remember { repositories.itemRepository.observeAll() }.collectAsState(initial = emptyList())
    val index =
        remember(npcs, locations, quests, parties, species, items) {
            MentionIndex(mentionEntitiesFrom(npcs, locations, quests, parties, species, items))
        }
    return remember(parties, npcs, quests, locations, index, species, items) {
        CodexRecords(
            parties = parties,
            npcs = npcs,
            quests = quests,
            locations = locations,
            mentionIndex = index,
            species = species,
            items = items,
        )
    }
}

/** the things tab of [member]: the items they carry, the purse, the notes and the item sheet when it is open */
@Composable
internal fun rememberMeItems(
    member: PartyMember,
    sheet: CharacterSheet,
    ui: MeUiState,
    records: CodexRecords,
    controller: ItemsController,
    repositories: CodexRepositories,
    undoController: UndoController,
): MeItems {
    val mention = rememberMentionSource(records, repositories, undoController)
    val kinds =
        mapOf(
            EntityKind.PARTY to stringResource(Res.string.codex_entry_type_party),
            EntityKind.NPC to stringResource(Res.string.codex_entry_type_npc),
            EntityKind.LOCATION to stringResource(Res.string.codex_entry_type_location),
        )
    val shown = rememberItemsState(controller, records, "", kinds, mention)
    val owner = ItemOwner.Of(EntityKind.PARTY, member.id)
    val rows =
        remember(records.items, owner) {
            records.items.filter { it.owner == owner }.map { MeItemRow(it.id, it.name, it.quantity, it.price, it.coin) }
        }
    return MeItems(
        rows = rows,
        purse = sheet.purse,
        notes = ui.textDrafts[MeText.NOTES] ?: sheet.notes,
        sheet = shown.sheet,
        mention = MentionField(mention, MentionRef(EntityKind.PARTY, member.id)),
    )
}

package com.lyrismet.incadent.presentation.codex

import androidx.compose.runtime.Composable
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.codex_empty_all_button
import dndplayerscodex.shared.generated.resources.codex_empty_all_text
import dndplayerscodex.shared.generated.resources.codex_empty_all_title
import dndplayerscodex.shared.generated.resources.codex_empty_location_button
import dndplayerscodex.shared.generated.resources.codex_empty_location_text
import dndplayerscodex.shared.generated.resources.codex_empty_location_title
import dndplayerscodex.shared.generated.resources.codex_empty_npc_button
import dndplayerscodex.shared.generated.resources.codex_empty_npc_text
import dndplayerscodex.shared.generated.resources.codex_empty_npc_title
import dndplayerscodex.shared.generated.resources.codex_empty_party_button
import dndplayerscodex.shared.generated.resources.codex_empty_party_text
import dndplayerscodex.shared.generated.resources.codex_empty_party_title
import dndplayerscodex.shared.generated.resources.codex_empty_quest_button
import dndplayerscodex.shared.generated.resources.codex_empty_quest_text
import dndplayerscodex.shared.generated.resources.codex_empty_quest_title
import dndplayerscodex.shared.generated.resources.codex_search_create_location
import dndplayerscodex.shared.generated.resources.codex_search_create_npc
import dndplayerscodex.shared.generated.resources.codex_search_create_party
import dndplayerscodex.shared.generated.resources.codex_search_create_quest
import dndplayerscodex.shared.generated.resources.codex_search_empty_reset
import dndplayerscodex.shared.generated.resources.codex_search_empty_text
import dndplayerscodex.shared.generated.resources.codex_search_empty_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** resolves the pure empty kind of the active tab into the text and buttons the list area renders */
@Composable
internal fun codexEmptyState(kind: CodexEmptyKind): CodexEmptyState =
    when (kind) {
        CodexEmptyKind.None -> CodexEmptyState.Hidden
        is CodexEmptyKind.Blank -> blankEmptyState(kind.tab)
        is CodexEmptyKind.NoMatch -> noMatchEmptyState(kind)
    }

@Composable
private fun blankEmptyState(tab: CodexTab): CodexEmptyState.Blank =
    CodexEmptyState.Blank(
        title = stringResource(tab.blankTitle()),
        text = stringResource(tab.blankText()),
        action = CodexEmptyAction(label = stringResource(tab.blankButton()), entryType = tab.toEntryType()),
    )

@Composable
private fun noMatchEmptyState(kind: CodexEmptyKind.NoMatch): CodexEmptyState.NoMatch {
    val create = kind.create
    return CodexEmptyState.NoMatch(
        title = stringResource(Res.string.codex_search_empty_title),
        text = stringResource(Res.string.codex_search_empty_text, kind.query),
        resetLabel = stringResource(Res.string.codex_search_empty_reset),
        create =
            create?.let {
                CodexEmptyAction(
                    label = stringResource(it.type.searchCreateButton(), it.name),
                    entryType = it.type,
                    name = it.name,
                )
            },
    )
}

private fun CodexTab.blankTitle(): StringResource =
    when (this) {
        CodexTab.ALL -> Res.string.codex_empty_all_title
        CodexTab.PARTY -> Res.string.codex_empty_party_title
        CodexTab.NPC -> Res.string.codex_empty_npc_title
        CodexTab.QUEST -> Res.string.codex_empty_quest_title
        CodexTab.LOCATION -> Res.string.codex_empty_location_title
    }

private fun CodexTab.blankText(): StringResource =
    when (this) {
        CodexTab.ALL -> Res.string.codex_empty_all_text
        CodexTab.PARTY -> Res.string.codex_empty_party_text
        CodexTab.NPC -> Res.string.codex_empty_npc_text
        CodexTab.QUEST -> Res.string.codex_empty_quest_text
        CodexTab.LOCATION -> Res.string.codex_empty_location_text
    }

private fun CodexTab.blankButton(): StringResource =
    when (this) {
        CodexTab.ALL -> Res.string.codex_empty_all_button
        CodexTab.PARTY -> Res.string.codex_empty_party_button
        CodexTab.NPC -> Res.string.codex_empty_npc_button
        CodexTab.QUEST -> Res.string.codex_empty_quest_button
        CodexTab.LOCATION -> Res.string.codex_empty_location_button
    }

private fun CodexEntryType.searchCreateButton(): StringResource =
    when (this) {
        CodexEntryType.PARTY -> Res.string.codex_search_create_party
        CodexEntryType.NPC -> Res.string.codex_search_create_npc
        CodexEntryType.QUEST -> Res.string.codex_search_create_quest
        CodexEntryType.LOCATION -> Res.string.codex_search_create_location
    }

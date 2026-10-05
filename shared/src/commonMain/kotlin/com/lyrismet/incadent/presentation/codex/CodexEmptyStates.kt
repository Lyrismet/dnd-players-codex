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
private fun blankEmptyState(tab: CodexTab): CodexEmptyState.Blank {
    val strings = tab.blankStrings()
    return CodexEmptyState.Blank(
        title = stringResource(strings.title),
        text = stringResource(strings.text),
        action = CodexEmptyAction(label = stringResource(strings.button), entryType = tab.toEntryType()),
    )
}

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

// the three strings of a tab's blank state, resolved once per tab instead of one switch per field
private class BlankStrings(
    val title: StringResource,
    val text: StringResource,
    val button: StringResource,
)

private fun CodexTab.blankStrings(): BlankStrings =
    when (this) {
        CodexTab.ALL ->
            BlankStrings(
                Res.string.codex_empty_all_title,
                Res.string.codex_empty_all_text,
                Res.string.codex_empty_all_button,
            )
        CodexTab.PARTY ->
            BlankStrings(
                Res.string.codex_empty_party_title,
                Res.string.codex_empty_party_text,
                Res.string.codex_empty_party_button,
            )
        CodexTab.NPC ->
            BlankStrings(
                Res.string.codex_empty_npc_title,
                Res.string.codex_empty_npc_text,
                Res.string.codex_empty_npc_button,
            )
        CodexTab.QUEST ->
            BlankStrings(
                Res.string.codex_empty_quest_title,
                Res.string.codex_empty_quest_text,
                Res.string.codex_empty_quest_button,
            )
        CodexTab.LOCATION ->
            BlankStrings(
                Res.string.codex_empty_location_title,
                Res.string.codex_empty_location_text,
                Res.string.codex_empty_location_button,
            )
    }

private fun CodexEntryType.searchCreateButton(): StringResource =
    when (this) {
        CodexEntryType.PARTY -> Res.string.codex_search_create_party
        CodexEntryType.NPC -> Res.string.codex_search_create_npc
        CodexEntryType.QUEST -> Res.string.codex_search_create_quest
        CodexEntryType.LOCATION -> Res.string.codex_search_create_location
    }

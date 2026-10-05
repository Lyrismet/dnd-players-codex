package com.lyrismet.incadent.presentation.codex

// a shorter trimmed query is more likely a typo than a name worth creating an entry for
internal const val CREATE_FROM_SEARCH_MIN_LENGTH = 3

/** a name typed into an empty search, with the entry type of the tab the search ran on */
internal data class CodexSearchCreate(
    val type: CodexEntryType,
    val name: String,
)

/** what the list area shows in place of the entries - [None] while the tab has something to list */
internal sealed interface CodexEmptyKind {
    data object None : CodexEmptyKind

    data class Blank(
        val tab: CodexTab,
    ) : CodexEmptyKind

    data class NoMatch(
        val query: String,
        val create: CodexSearchCreate?,
    ) : CodexEmptyKind
}

/** the create offer of an empty search - null when the trimmed query is too short to be a name */
internal fun codexSearchCreate(
    tab: CodexTab,
    query: String,
): CodexSearchCreate? {
    val name = query.trim()
    return if (name.length >= CREATE_FROM_SEARCH_MIN_LENGTH) CodexSearchCreate(tab.toEntryType(), name) else null
}

/** the empty state of the active tab - a search that found nothing is its own case, not a blank tab */
internal fun codexEmptyKind(
    tab: CodexTab,
    query: String,
    hasEntries: Boolean,
): CodexEmptyKind {
    val trimmed = query.trim()
    return when {
        hasEntries -> CodexEmptyKind.None
        trimmed.isEmpty() -> CodexEmptyKind.Blank(tab)
        else -> CodexEmptyKind.NoMatch(trimmed, codexSearchCreate(tab, trimmed))
    }
}

// mirrors the mockup's openNew() - "Все" falls back to NPC, every other tab keeps its own type
internal fun CodexTab.toEntryType(): CodexEntryType =
    when (this) {
        CodexTab.PARTY -> CodexEntryType.PARTY
        CodexTab.QUEST -> CodexEntryType.QUEST
        CodexTab.LOCATION -> CodexEntryType.LOCATION
        CodexTab.ALL, CodexTab.NPC -> CodexEntryType.NPC
    }

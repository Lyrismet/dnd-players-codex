package com.lyrismet.incadent.presentation.codex

/** true for the events that write campaign data, an archived campaign drops these in the presenter too */
internal fun CodexEvent.writesCampaignData(): Boolean =
    when (this) {
        is CodexEvent.TabSelected,
        is CodexEvent.SearchQueryChanged,
        is CodexEvent.GroupBySelected,
        is CodexEvent.EntityClicked,
        is CodexEvent.RelatedNoteClicked,
        is CodexEvent.PlaceSectionToggled,
        CodexEvent.SheetDismissed,
        CodexEvent.UndoClicked,
        CodexEvent.EntityDeleteCancelled,
        -> false
        is CodexEvent.NpcStatusSelected,
        is CodexEvent.NpcLifeSelected,
        is CodexEvent.PartyPresenceSelected,
        is CodexEvent.QuestStatusSelected,
        is CodexEvent.QuickEdit,
        is CodexEvent.QuickPlaceCreated,
        CodexEvent.AddEntryClicked,
        is CodexEvent.EmptyActionClicked,
        is CodexEvent.EditEntryRequested,
        is CodexEvent.EntityDeleteRequested,
        CodexEvent.EntityDeleteConfirmed,
        is CodexEntryFormEvent,
        -> true
    }

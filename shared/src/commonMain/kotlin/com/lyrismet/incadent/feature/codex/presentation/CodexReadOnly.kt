package com.lyrismet.incadent.feature.codex.presentation

import com.lyrismet.incadent.feature.codex.presentation.summary.writesCampaignData

/** true for the events that write campaign data, an archived campaign drops these in the presenter too */
internal fun CodexEvent.writesCampaignData(): Boolean =
    when (this) {
        is CodexEvent.EntitySheet -> event.writesCampaignData()
        is CodexEvent.TabSelected,
        is CodexEvent.SearchQueryChanged,
        is CodexEvent.GroupBySelected,
        is CodexEvent.PlaceSectionToggled,
        CodexEvent.UndoClicked,
        CodexEvent.EntityDeleteCancelled,
        -> false
        is CodexEvent.QuickEdit,
        is CodexEvent.QuickPlaceCreated,
        CodexEvent.AddEntryClicked,
        is CodexEvent.EmptyActionClicked,
        is CodexEvent.EditEntryRequested,
        is CodexEvent.EntityDeleteRequested,
        CodexEvent.EntityDeleteConfirmed,
        is CodexEvent.EntryForm,
        -> true
    }

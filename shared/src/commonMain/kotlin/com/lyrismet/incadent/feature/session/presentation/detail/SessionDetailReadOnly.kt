package com.lyrismet.incadent.feature.session.presentation.detail

import com.lyrismet.incadent.feature.codex.presentation.summary.writesCampaignData

/** true for the events that write campaign data, an archived campaign drops these in the presenter too */
internal fun SessionDetailEvent.writesCampaignData(): Boolean =
    when (this) {
        is SessionDetailEvent.EntitySheet -> event.writesCampaignData()
        SessionDetailEvent.BackClicked,
        is SessionDetailEvent.DraftChanged,
        SessionDetailEvent.InsertMentionTriggerClicked,
        is SessionDetailEvent.MentionSuggestionPicked,
        is SessionDetailEvent.MentionCreateOpened,
        is SessionDetailEvent.MentionCreateNameChanged,
        is SessionDetailEvent.MentionCreateTypeChanged,
        SessionDetailEvent.MentionCreateDismissed,
        is SessionDetailEvent.EntryClicked,
        SessionDetailEvent.CancelEditEntryClicked,
        SessionDetailEvent.TagButtonClicked,
        SessionDetailEvent.TagSheetDismissed,
        is SessionDetailEvent.TagDraftChanged,
        is SessionDetailEvent.PhraseStripPicked,
        is SessionDetailEvent.PhraseCompletionPicked,
        SessionDetailEvent.PhraseSheetOpened,
        is SessionDetailEvent.DeletedMentionClicked,
        is SessionDetailEvent.SpeciesChipClicked,
        SessionDetailEvent.DeletedMentionSheetDismissed,
        is SessionDetailEvent.DeletedMentionPickingChanged,
        is SessionDetailEvent.DeletedMentionQueryChanged,
        SessionDetailEvent.TitleEditFinished,
        SessionDetailEvent.OutsideTapped,
        -> false
        is SessionDetailEvent.TitleChanged,
        SessionDetailEvent.TitleEditStarted,
        SessionDetailEvent.SubmitEntryClicked,
        is SessionDetailEvent.EditEntryClicked,
        is SessionDetailEvent.DeleteEntryClicked,
        SessionDetailEvent.EndSessionClicked,
        SessionDetailEvent.ResumeSessionClicked,
        is SessionDetailEvent.TagToggled,
        is SessionDetailEvent.TagSuggestionAdded,
        SessionDetailEvent.TagAddAllSuggestionsClicked,
        SessionDetailEvent.TagDraftSubmitted,
        is SessionDetailEvent.PhraseSheet,
        is SessionDetailEvent.SaveEntryAsPhraseClicked,
        SessionDetailEvent.MentionCreateConfirmed,
        SessionDetailEvent.DeletedMentionCreateClicked,
        is SessionDetailEvent.DeletedMentionLinked,
        SessionDetailEvent.DeletedMentionKeptAsText,
        is SessionDetailEvent.AutoMentionsLinkClicked,
        is SessionDetailEvent.AutoMentionsDismissed,
        is SessionDetailEvent.AutoMentionWordClicked,
        is SessionDetailEvent.EntryForm,
        -> true
    }

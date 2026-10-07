package com.lyrismet.incadent.core.campaign

/** the trimmed campaign name, or null when it is blank - a blank name is never saved */
fun savableCampaignName(draft: String): String? = draft.trim().takeIf { it.isNotEmpty() }

/** the campaign rename sheet's state, shared by Settings and the session list header */
sealed interface RenameSheetState {
    data object Hidden : RenameSheetState

    data class Editing(
        val draft: String,
    ) : RenameSheetState
}

/** the sheet's trimmed draft if it is savable, or null when blank - the sheet then stays open as-is */
fun RenameSheetState.savedCampaignNameOrNull(): String? =
    (this as? RenameSheetState.Editing)?.draft?.let { savableCampaignName(it) }

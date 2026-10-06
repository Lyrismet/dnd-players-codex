package com.lyrismet.incadent.core.campaign

/** the trimmed campaign name, or null when it is blank - a blank name is never saved */
fun savableCampaignName(draft: String): String? = draft.trim().takeIf { it.isNotEmpty() }

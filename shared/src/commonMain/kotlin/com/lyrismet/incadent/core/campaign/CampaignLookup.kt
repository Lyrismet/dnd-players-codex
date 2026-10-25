package com.lyrismet.incadent.core.campaign

/** reads which campaign should be open on launch, implemented where the campaigns are stored */
interface CampaignLookup {
    /** the saved id if that campaign still exists, otherwise the latest live one, null when there is none */
    fun resolveInitialId(savedId: String?): String?
}

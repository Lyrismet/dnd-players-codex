package com.lyrismet.incadent.feature.campaign.data

import com.lyrismet.incadent.core.campaign.CampaignLookup
import com.lyrismet.incadent.db.AppDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CampaignLookupImpl(
    database: AppDatabase,
) : CampaignLookup {
    private val queries = database.campaignQueries

    override fun resolveInitialId(savedId: String?): String? {
        if (savedId != null && queries.exists(savedId).executeAsOne() > 0L) return savedId
        return queries.selectFirstLiveId().executeAsOneOrNull()
    }
}

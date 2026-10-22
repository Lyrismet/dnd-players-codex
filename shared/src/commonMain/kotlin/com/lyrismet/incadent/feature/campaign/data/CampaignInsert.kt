package com.lyrismet.incadent.feature.campaign.data

import com.lyrismet.incadent.db.CampaignQueries
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** adds an open campaign with a fresh id and returns the id */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
internal fun CampaignQueries.insertLive(
    name: String,
    id: String = Uuid.random().toString(),
): String {
    val now = Clock.System.now().toEpochMilliseconds()
    insert(id, name, CampaignStatus.LIVE, now, now)
    return id
}

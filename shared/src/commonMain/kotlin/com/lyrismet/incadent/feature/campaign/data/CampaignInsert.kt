package com.lyrismet.incadent.feature.campaign.data

import com.lyrismet.incadent.core.identity.newOriginId
import com.lyrismet.incadent.db.CampaignQueries
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** adds an open campaign with a fresh id and returns the id, the origin id is the one a file brought or a new one */
@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
internal fun CampaignQueries.insertLive(
    name: String,
    id: String = Uuid.random().toString(),
    originId: String = newOriginId(),
): String {
    val now = Clock.System.now().toEpochMilliseconds()
    insert(id, name, CampaignStatus.LIVE, now, now, originId)
    return id
}

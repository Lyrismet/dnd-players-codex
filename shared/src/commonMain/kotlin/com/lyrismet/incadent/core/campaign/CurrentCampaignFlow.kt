package com.lyrismet.incadent.core.campaign

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest

/** re-runs [query] for each campaign that gets opened, so a repository flow follows the campaign switch */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T> CurrentCampaign.observe(query: (campaignId: String) -> Flow<T>): Flow<T> =
    id.filterNotNull().flatMapLatest(query)

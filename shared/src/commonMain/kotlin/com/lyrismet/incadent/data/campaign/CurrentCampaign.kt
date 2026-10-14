package com.lyrismet.incadent.data.campaign

import com.lyrismet.incadent.db.AppDatabase
import com.russhwolf.settings.ObservableSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val KEY_CURRENT_CAMPAIGN_ID = "current_campaign_id"

/**
 * the campaign every data repository reads and writes in. It is remembered across launches, null means no campaign is
 * open and the library or onboarding is shown instead.
 */
@Inject
@SingleIn(AppScope::class)
class CurrentCampaign(
    private val settings: ObservableSettings,
    database: AppDatabase,
) {
    private val queries = database.campaignQueries
    private val state = MutableStateFlow(resolveInitialId())

    val id: StateFlow<String?> = state.asStateFlow()

    fun requireId(): String = checkNotNull(state.value) { "no campaign is open" }

    fun open(id: String) {
        settings.putString(KEY_CURRENT_CAMPAIGN_ID, id)
        state.value = id
    }

    fun close() {
        settings.remove(KEY_CURRENT_CAMPAIGN_ID)
        state.value = null
    }

    // the saved campaign if it still exists, otherwise the latest live one so a wiped row never strands the app
    private fun resolveInitialId(): String? {
        val saved = settings.getStringOrNull(KEY_CURRENT_CAMPAIGN_ID)
        if (saved != null && queries.exists(saved).executeAsOne() > 0L) return saved
        return queries.selectFirstLiveId().executeAsOneOrNull()?.also { open(it) }
    }
}

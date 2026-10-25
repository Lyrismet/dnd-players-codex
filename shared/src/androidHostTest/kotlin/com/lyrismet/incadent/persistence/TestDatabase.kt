package com.lyrismet.incadent.persistence

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.data.CampaignLookupImpl
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.persistence.createAppDatabase
import com.russhwolf.settings.MapSettings

/** a fresh in-memory database built from the real schema and adapters, with one live campaign open */
internal class TestDatabase(
    campaignId: String = FIRST_CAMPAIGN,
) {
    val database: AppDatabase =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            .also { AppDatabase.Schema.create(it) }
            .let(::createAppDatabase)

    init {
        addCampaign(campaignId)
    }

    val current = CurrentCampaign(MapSettings(), CampaignLookupImpl(database))

    init {
        current.open(campaignId)
    }

    fun addCampaign(id: String) {
        database.campaignQueries.insert(id, "Campaign $id", CampaignStatus.LIVE, 0L, 0L)
    }

    companion object {
        const val FIRST_CAMPAIGN = "campaign-1"
        const val SECOND_CAMPAIGN = "campaign-2"
    }
}

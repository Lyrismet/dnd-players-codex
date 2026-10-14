package com.lyrismet.incadent.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.lyrismet.incadent.data.campaign.CurrentCampaign
import com.lyrismet.incadent.data.db.createAppDatabase
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.CampaignStatus
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

    val current = CurrentCampaign(MapSettings(), database)

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

package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.codex.domain.grouping.decodeClasses
import com.lyrismet.incadent.feature.codex.domain.grouping.encodeClasses
import com.lyrismet.incadent.feature.codex.domain.model.PartyClass
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class PartyClassesExchangeTest {
    private val db = TestDatabase().also { it.addCampaign(TestDatabase.SECOND_CAMPAIGN) }
    private val database: AppDatabase = db.database
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }
    private val classes = listOf(PartyClass("Жрец", "Домен Света", 3), PartyClass("Воин", "", 2))

    private fun seed(list: String) {
        database.partyMemberQueries.insert(
            "Лира",
            "Жрец",
            "Домен Света",
            "",
            5L,
            "",
            false,
            PartyPresence.IN,
            null,
            null,
            "",
            null,
            null,
            TestDatabase.FIRST_CAMPAIGN,
            "lira",
            "",
            null,
            list,
        )
    }

    private fun roundTrip(): String {
        val options = CampaignExporter(database).catalog(TestDatabase.FIRST_CAMPAIGN).options
        val selection = RecordSelection(party = options.party.map { it.id }.toSet(), mode = ExportMode.FRIEND)
        val file = MemoryFile().also { CampaignExporter(database).write(TestDatabase.FIRST_CAMPAIGN, selection, it) }
        val target =
            importer
                .import(origin(file), ImportRequest(ImportTarget.NewCampaign("Fallback"), ConflictMode.SKIP))
                .campaignId
        return database.partyMemberQueries
            .selectAll(target)
            .executeAsList()
            .single()
            .classes
    }

    @Test
    fun `a multiclass character keeps its classes through an export and an import`() {
        seed(classes.encodeClasses())
        assertEquals(classes, decodeClasses(roundTrip()))
    }

    @Test
    fun `a character with one class comes back with no list`() {
        seed("")
        assertEquals("", roundTrip())
    }
}

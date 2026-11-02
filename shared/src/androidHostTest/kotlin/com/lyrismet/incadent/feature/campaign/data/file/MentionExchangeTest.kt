package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.file.FileSource
import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.FieldChoice
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.RecordField
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.persistence.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// the mentions in the text of a record point at ids, and an id means a record only inside its own campaign
class MentionExchangeTest {
    private val db = TestDatabase().also { it.addCampaign(MINE) }
    private val database: AppDatabase = db.database
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }

    private fun origin(file: MemoryFile) =
        object : FileOrigin {
            override val name = "test.incadent.json"

            override fun open(): FileSource = MemoryFile(file.text.lines())
        }

    private fun place(
        campaignId: String,
        name: String,
        originId: String,
    ): Long {
        database.locationQueries.insert(name, "", null, null, null, campaignId, originId)
        return database.locationQueries.lastInsertRowId().executeAsOne()
    }

    private fun npc(
        campaignId: String,
        name: String,
        originId: String,
        description: String,
    ): Long {
        database.npcQueries.insert(
            name,
            NpcStatus.FRIEND,
            description,
            "",
            NpcLifeState.ALIVE,
            "",
            null,
            "",
            null,
            null,
            campaignId,
            originId,
        )
        return database.npcQueries.lastInsertRowId().executeAsOne()
    }

    private fun export(): MemoryFile {
        val options = CampaignExporter(database).catalog(THEIRS).options
        val all =
            RecordSelection(
                places = options.places.map { it.id }.toSet(),
                npcs = options.npcs.map { it.id }.toSet(),
                mode = ExportMode.FRIEND,
            )
        return MemoryFile().also { CampaignExporter(database).write(THEIRS, all, it) }
    }

    private fun descriptionOf(
        campaignId: String,
        name: String,
    ) = database.npcQueries
        .selectAll(campaignId)
        .executeAsList()
        .first { it.name == name }
        .description

    private fun placeId(
        campaignId: String,
        name: String,
    ) = database.locationQueries
        .selectAll(campaignId)
        .executeAsList()
        .first { it.name == name }
        .id

    @Test
    fun `a mention in a description follows the place to its new id`() {
        // the ids of the two campaigns differ, so a token left as it was would point at the wrong place
        place(MINE, "Other", "other")
        val inn = place(THEIRS, "Inn", "inn")
        npc(THEIRS, "Bob", "bob", "met @[Inn|l:$inn]")

        importer.import(
            origin(export()),
            ImportRequest(ImportTarget.Existing(MINE), ConflictMode.SKIP),
        )

        val newInn = placeId(MINE, "Inn")
        assertEquals("met @[Inn|l:$newInn]", descriptionOf(MINE, "Bob"))
    }

    @Test
    fun `a mention of a record the file does not hold becomes a gone record`() {
        val inn = place(THEIRS, "Inn", "inn")
        npc(THEIRS, "Bob", "bob", "met @[Inn|l:$inn]")
        val options = CampaignExporter(database).catalog(THEIRS).options
        val onlyNpcs = RecordSelection(npcs = options.npcs.map { it.id }.toSet(), mode = ExportMode.FRIEND)
        val file = MemoryFile().also { CampaignExporter(database).write(THEIRS, onlyNpcs, it) }

        importer.import(origin(file), ImportRequest(ImportTarget.Existing(MINE), ConflictMode.SKIP))

        assertTrue(descriptionOf(MINE, "Bob").contains("|l:0]"), descriptionOf(MINE, "Bob"))
    }

    @Test
    fun `taking both texts moves only the mentions of the file text`() {
        val mineInn = place(MINE, "Inn", "inn")
        val mineBob = npc(MINE, "Bob", "bob", "mine @[Inn|l:$mineInn]")
        val theirInn = place(THEIRS, "Inn", "inn")
        npc(THEIRS, "Bob", "bob", "theirs @[Inn|l:$theirInn]")
        val ref = ExchangeRef(ExchangeKind.NPC, 1)

        importer.import(
            origin(export()),
            ImportRequest(
                ImportTarget.Existing(MINE),
                ConflictMode.SKIP,
                fields = mapOf(ref to mapOf(RecordField.DESCRIPTION to FieldChoice.BOTH)),
                fromFileLabel = "from file",
            ),
        )

        val text =
            database.npcQueries
                .selectById(mineBob)
                .executeAsOne()
                .description
        assertEquals("mine @[Inn|l:$mineInn]\n\nfrom file: theirs @[Inn|l:$mineInn]", text)
    }

    @Test
    fun `the same words with other ids are not a difference`() {
        val mineInn = place(MINE, "Inn", "inn")
        npc(MINE, "Bob", "bob", "met @[Inn|l:$mineInn]")
        val theirInn = place(THEIRS, "Inn", "inn")
        npc(THEIRS, "Bob", "bob", "met @[Inn|l:$theirInn]")

        val plan = ImportPlanner(database).plan(origin(export()), MINE)

        assertTrue(plan.nothingNew, plan.toString())
    }

    private companion object {
        const val MINE = TestDatabase.SECOND_CAMPAIGN
        const val THEIRS = TestDatabase.FIRST_CAMPAIGN
    }
}

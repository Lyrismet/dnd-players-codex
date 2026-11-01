package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.file.FileSource
import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class OriginIdExchangeTest {
    private val db = TestDatabase()
    private val database: AppDatabase = db.database
    private val source = TestDatabase.FIRST_CAMPAIGN
    private val date = LocalDateTime.parse("2026-03-04T18:00")
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }

    private fun origin(file: MemoryFile) =
        object : FileOrigin {
            override val name = "test.incadent.json"

            override fun open(): FileSource = MemoryFile(file.text.lines())
        }

    private fun seed() {
        database.speciesQueries.insert(
            "Wolf",
            CreatureType.BEAST,
            "",
            "",
            null,
            null,
            null,
            source,
            "wolf-origin",
            null,
        )
        database.locationQueries.insert("Town", "", null, null, null, source, "town-origin")
        database.npcQueries.insert(
            "Bob",
            NpcStatus.FRIEND,
            "",
            "",
            NpcLifeState.ALIVE,
            "",
            null,
            "",
            null,
            null,
            source,
            "bob-origin",
        )
        database.questQueries.insert("Find", QuestStatus.ACTIVE, "", null, null, "", source, "quest-origin")
        database.partyMemberQueries.insert(
            "Hero",
            "",
            "",
            null,
            "",
            false,
            PartyPresence.IN,
            null,
            null,
            "",
            null,
            null,
            source,
            "hero-origin",
        )
        database.sessionNoteQueries.insert("Day one", date, date, 5L, source, "session-origin")
        val session = database.sessionNoteQueries.lastInsertRowId().executeAsOne()
        database.sessionEntryQueries.insert(session, date, "note", false, false, "entry-origin")
        database.combatQueries.insertFinished(
            session,
            date,
            1L,
            0L,
            CombatOutcome.VICTORY,
            date,
            source,
            "combat-origin",
        )
        database.campaignQueries.rename("World", source)
    }

    private fun export(): MemoryFile {
        val options = CampaignExporter(database).catalog(source).options
        val all =
            RecordSelection(
                sessions = options.sessions.map { it.id }.toSet(),
                places = options.places.map { it.id }.toSet(),
                npcs = options.npcs.map { it.id }.toSet(),
                quests = options.quests.map { it.id }.toSet(),
                species = options.species.map { it.id }.toSet(),
                party = options.party.map { it.id }.toSet(),
            )
        return MemoryFile().also { CampaignExporter(database).write(source, all, it) }
    }

    private fun importNew(file: MemoryFile) =
        importer
            .import(
                origin(file),
                ImportRequest(ImportTarget.NewCampaign("Fallback"), ConflictMode.SKIP, null),
            ).campaignId

    private fun originIdsOf(campaignId: String): Map<String, List<String>> {
        val sessions = database.sessionNoteQueries.selectAll(campaignId).executeAsList()
        return mapOf(
            "species" to
                database.speciesQueries
                    .selectAll(campaignId)
                    .executeAsList()
                    .map { it.origin_id },
            "place" to
                database.locationQueries
                    .selectAll(campaignId)
                    .executeAsList()
                    .map { it.origin_id },
            "npc" to
                database.npcQueries
                    .selectAll(campaignId)
                    .executeAsList()
                    .map { it.origin_id },
            "quest" to
                database.questQueries
                    .selectAll(campaignId)
                    .executeAsList()
                    .map { it.origin_id },
            "party" to
                database.partyMemberQueries
                    .selectAll(campaignId)
                    .executeAsList()
                    .map { it.origin_id },
            "session" to sessions.map { it.origin_id },
            "entry" to
                sessions
                    .flatMap {
                        database.sessionEntryQueries
                            .selectForSession(
                                it.id,
                            ).executeAsList()
                    }.map { it.origin_id },
            "combat" to
                database.combatQueries
                    .selectAll(campaignId)
                    .executeAsList()
                    .map { it.origin_id },
        )
    }

    @Test
    fun `origin ids travel in the file and come back in a new campaign`() {
        seed()
        val target = importNew(export())

        assertEquals(originIdsOf(source), originIdsOf(target))
        assertEquals(
            database.campaignQueries.selectOriginId(source).executeAsOne(),
            database.campaignQueries.selectOriginId(target).executeAsOne(),
        )
    }

    @Test
    fun `the same file imported twice as new campaigns keeps its ids both times`() {
        seed()
        val file = export()
        val first = importNew(file)
        val second = importNew(file)

        assertEquals(originIdsOf(first), originIdsOf(second))
    }

    @Test
    fun `a copy into the same campaign gets fresh ids and the originals keep theirs`() {
        seed()
        val before = originIdsOf(source)
        importer.import(origin(export()), ImportRequest(ImportTarget.Existing(source), ConflictMode.COPY))

        val after = originIdsOf(source)
        before.forEach { (kind, ids) ->
            assertTrue(after.getValue(kind).containsAll(ids), kind)
        }
        assertEquals(2, after.getValue("npc").size)
        assertEquals(2, after.getValue("npc").toSet().size)
        assertEquals(2, after.getValue("species").toSet().size)
        assertEquals(2, after.getValue("combat").toSet().size)
        assertEquals(2, after.getValue("session").toSet().size)
    }

    @Test
    fun `a record matched by name keeps the origin id it has`() {
        seed()
        importer.import(origin(export()), ImportRequest(ImportTarget.Existing(source), ConflictMode.SKIP))

        assertEquals(listOf("bob-origin"), originIdsOf(source).getValue("npc"))
    }

    @Test
    fun `a file without origin ids imports and every row gets its own`() {
        val lines =
            listOf(
                """{"format":"incadent","version":2,"name":"Old","records":2,"sessions":1}""",
                """{"t":"species","id":1,"name":"Wolf","creatureType":"BEAST"}""",
                """{"t":"species","id":2,"name":"Bear","creatureType":"BEAST"}""",
                """{"t":"session","id":1,"title":"S","date":"2026-03-04T18:00"}""",
                """{"t":"entry","sessionId":1,"createdAt":"2026-03-04T18:01","body":"a"}""",
                """{"t":"entry","sessionId":1,"createdAt":"2026-03-04T18:02","body":"b"}""",
            )
        val target = importNew(MemoryFile(lines))

        val ids = originIdsOf(target)
        assertEquals(2, ids.getValue("species").toSet().size)
        assertEquals(2, ids.getValue("entry").toSet().size)
        assertTrue(ids.values.flatten().none { it.isBlank() })
        assertNotEquals("", database.campaignQueries.selectOriginId(target).executeAsOne())
    }
}

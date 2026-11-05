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
import com.lyrismet.incadent.feature.campaign.domain.model.RecordStatus
import com.lyrismet.incadent.feature.campaign.domain.model.Resolution
import com.lyrismet.incadent.feature.campaign.domain.model.SessionStatus
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MergeExchangeTest {
    private val db = TestDatabase().also { it.addCampaign(MINE) }
    private val database: AppDatabase = db.database
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }
    private val planner = ImportPlanner(database)
    private val rollback = ImportRollback(database) { _, _ -> byteArrayOf(7) }
    private val day = LocalDateTime.parse("2026-10-24T18:00")

    private fun origin(file: MemoryFile) =
        object : FileOrigin {
            override val name = "test.incadent.json"

            override fun open(): FileSource = MemoryFile(file.text.lines())
        }

    private fun everything(campaignId: String): RecordSelection {
        val options = CampaignExporter(database).catalog(campaignId).options
        return RecordSelection(
            sessions = options.sessions.map { it.id }.toSet(),
            places = options.places.map { it.id }.toSet(),
            npcs = options.npcs.map { it.id }.toSet(),
            quests = options.quests.map { it.id }.toSet(),
            species = options.species.map { it.id }.toSet(),
            party = options.party.map { it.id }.toSet(),
        )
    }

    private fun export(
        campaignId: String = THEIRS,
        mode: ExportMode = ExportMode.FRIEND,
        partyNotes: Boolean = false,
    ): MemoryFile =
        MemoryFile().also {
            CampaignExporter(database).write(
                campaignId,
                everything(campaignId).copy(mode = mode, partyNotes = partyNotes),
                it,
            )
        }

    private fun import(
        file: MemoryFile,
        target: String = MINE,
        request: (ImportTarget) -> ImportRequest = { ImportRequest(it, ConflictMode.SKIP) },
    ) = importer.import(origin(file), request(ImportTarget.Existing(target)))

    private fun npc(
        campaignId: String,
        name: String,
        originId: String,
        description: String = "",
        status: NpcStatus = NpcStatus.FRIEND,
    ): Long {
        database.npcQueries.insert(
            name,
            status,
            description,
            "",
            "",
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

    private fun hero(
        campaignId: String,
        originId: String,
        mine: Boolean,
        hp: Long?,
        notes: String,
    ): Long {
        database.partyMemberQueries.insert(
            "Hero",
            "Rogue",
            "",
            "human",
            3L,
            "Ana",
            mine,
            PartyPresence.IN,
            hp,
            hp,
            notes,
            null,
            null,
            campaignId,
            originId,
        )
        return database.partyMemberQueries.lastInsertRowId().executeAsOne()
    }

    private fun session(
        campaignId: String,
        originId: String,
        notes: List<Pair<String, String>>,
        title: String = "Огни на холме",
    ): Long {
        database.sessionNoteQueries.insert(title, day, day, 1L, campaignId, originId)
        val id = database.sessionNoteQueries.lastInsertRowId().executeAsOne()
        notes.forEach { (origin, body) -> database.sessionEntryQueries.insert(id, day, body, false, false, origin) }
        return id
    }

    private fun entries(sessionId: Long) = database.sessionEntryQueries.selectForSession(sessionId).executeAsList()

    private fun npcNames(campaignId: String) =
        database.npcQueries
            .selectAll(campaignId)
            .executeAsList()
            .map { it.name }

    @Test
    fun `a friend copy carries no vitals notes or mark`() {
        hero(THEIRS, "hero", mine = true, hp = 20L, notes = "secret")

        val party =
            import(export(mode = ExportMode.FRIEND), target = MINE).let {
                database.partyMemberQueries
                    .selectAll(MINE)
                    .executeAsList()
                    .single()
            }

        assertNull(party.hp_max)
        assertNull(party.hp_current)
        assertEquals("", party.description)
        assertFalse(party.is_player_character)
        assertEquals("Ana", party.player_name)
    }

    @Test
    fun `a file without vitals leaves the local hit points and the mark alone`() {
        val local = hero(MINE, "hero", mine = true, hp = 30L, notes = "mine")
        hero(THEIRS, "hero", mine = false, hp = 20L, notes = "theirs")

        import(export(mode = ExportMode.FRIEND)) {
            ImportRequest(it, ConflictMode.SKIP, replace = setOf(ExchangeRef(ExchangeKind.PARTY, 1)))
        }

        val row = database.partyMemberQueries.selectById(local).executeAsOne()
        assertEquals(30L, row.hp_max)
        assertEquals("mine", row.description)
        assertTrue(row.is_player_character)
        assertEquals(
            1,
            database.partyMemberQueries
                .selectAll(MINE)
                .executeAsList()
                .size,
        )
    }

    @Test
    fun `a personal copy of someone else brings the mark and vitals only when asked`() {
        hero(THEIRS, "hero", mine = true, hp = 20L, notes = "theirs")
        val file = export(mode = ExportMode.SELF)

        import(file) { ImportRequest(it, ConflictMode.SKIP) }
        val plain =
            database.partyMemberQueries
                .selectAll(MINE)
                .executeAsList()
                .single()
        assertFalse(plain.is_player_character)
        assertNull(plain.hp_max)

        val other = "campaign-3".also { db.addCampaign(it) }
        import(file, target = other) { ImportRequest(it, ConflictMode.SKIP, takeAsMine = true, takeVitals = true) }
        val taken =
            database.partyMemberQueries
                .selectAll(other)
                .executeAsList()
                .single()
        assertTrue(taken.is_player_character)
        assertEquals(20L, taken.hp_max)
        assertEquals("theirs", taken.description)
    }

    @Test
    fun `importing the same file again finds nothing new and adds nothing`() {
        npc(THEIRS, "Bob", "bob")
        session(THEIRS, "s-1", listOf("e1" to "one", "e2" to "two"))
        val file = export()

        import(file)
        val again = planner.plan(origin(file), MINE)

        assertTrue(again.nothingNew, again.toString())
        import(file)
        assertEquals(listOf("Bob"), npcNames(MINE))
        assertEquals(
            1,
            database.sessionNoteQueries
                .selectAll(MINE)
                .executeAsList()
                .size,
        )
        assertEquals(
            2,
            entries(
                database.sessionNoteQueries
                    .selectAll(MINE)
                    .executeAsList()
                    .single()
                    .id,
            ).size,
        )
    }

    @Test
    fun `a record is told apart by origin name and likeness`() {
        npc(THEIRS, "Bob", "same", description = "x")
        npc(THEIRS, "Ann", "ann", description = "file")
        npc(THEIRS, "Kahuri", "kahuri")
        npc(THEIRS, "Eve", "eve")
        npc(MINE, "Bob", "same", description = "x")
        npc(MINE, "Ann", "ann", description = "mine")
        npc(MINE, "Village Kahuri", "local-kahuri")

        val plan = planner.plan(origin(export()), MINE)

        val byName = plan.records.associate { it.name to it.status }
        assertEquals(RecordStatus.SAME, byName["Bob"])
        assertEquals(RecordStatus.CHANGED, byName["Ann"])
        assertEquals(RecordStatus.SIMILAR, byName["Kahuri"])
        assertEquals(RecordStatus.NEW, byName["Eve"])
        assertEquals(
            1,
            plan.records
                .single { it.name == "Ann" }
                .changes.size,
        )
    }

    @Test
    fun `saying a similar record is the same merges it and remembers the old id`() {
        npc(THEIRS, "Kahuri", "theirs-kahuri")
        val local = npc(MINE, "Village Kahuri", "local-kahuri")
        val file = export()
        val ref = ExchangeRef(ExchangeKind.NPC, 1)

        import(file) { ImportRequest(it, ConflictMode.SKIP, resolutions = mapOf(ref to Resolution.SAME)) }

        assertEquals(listOf("Village Kahuri"), npcNames(MINE))
        assertEquals(
            "local-kahuri",
            database.originAliasQueries.selectOriginId(MINE, ExchangeKind.NPC, "theirs-kahuri").executeAsOne(),
        )
        assertEquals(
            RecordStatus.SAME,
            planner
                .plan(origin(file), MINE)
                .records
                .single()
                .status,
        )
        assertEquals(
            local,
            database.npcQueries
                .selectAll(MINE)
                .executeAsList()
                .single()
                .id,
        )
    }

    @Test
    fun `saying they are different keeps both and is not asked again`() {
        npc(THEIRS, "Kahuri", "theirs-kahuri")
        npc(MINE, "Village Kahuri", "local-kahuri")
        val file = export()
        val ref = ExchangeRef(ExchangeKind.NPC, 1)

        import(file) { ImportRequest(it, ConflictMode.SKIP, resolutions = mapOf(ref to Resolution.APART)) }

        assertEquals(setOf("Kahuri", "Village Kahuri"), npcNames(MINE).toSet())
        assertEquals(
            RecordStatus.SAME,
            planner
                .plan(origin(file), MINE)
                .records
                .single()
                .status,
        )
    }

    @Test
    fun `leaving a similar record for later takes nothing`() {
        npc(THEIRS, "Kahuri", "theirs-kahuri")
        npc(MINE, "Village Kahuri", "local-kahuri")

        import(export())

        assertEquals(listOf("Village Kahuri"), npcNames(MINE))
    }

    @Test
    fun `six notes and four with two shared merge into eight`() {
        val shared = listOf("c1" to "a", "c2" to "b")
        session(THEIRS, "theirs", shared + listOf("f1" to "c", "f2" to "d"))
        val mine = session(MINE, "mine", shared + (1..4).map { "m$it" to "n$it" })
        val file = export()

        val plan = planner.plan(origin(file), MINE)
        val planned = plan.sessions.single()
        assertEquals(SessionStatus.MERGE, planned.status)
        assertEquals(6, planned.overlap.mine)
        assertEquals(4, planned.overlap.theirs)
        assertEquals(2, planned.overlap.common)
        assertEquals(8, planned.overlap.merged)

        import(file)

        assertEquals(8, entries(mine).size)
        assertEquals(
            1,
            database.sessionNoteQueries
                .selectAll(MINE)
                .executeAsList()
                .size,
        )
    }

    @Test
    fun `keeping a session separate adds a second one with the mark`() {
        session(THEIRS, "theirs", listOf("f1" to "c"))
        session(MINE, "mine", listOf("m1" to "x"))

        import(
            export(),
        ) { ImportRequest(it, ConflictMode.SKIP, separateSessions = setOf(1L), fromFileLabel = "из файла") }

        val titles =
            database.sessionNoteQueries
                .selectAll(MINE)
                .executeAsList()
                .map { it.title }
        assertEquals(setOf("Огни на холме", "Огни на холме · из файла"), titles.toSet())
    }

    @Test
    fun `a deleted record does not come back until it is brought back`() {
        npc(THEIRS, "Bob", "bob")
        val file = export()
        import(file)
        val bob =
            database.npcQueries
                .selectAll(MINE)
                .executeAsList()
                .single()
        database.npcQueries.deleteById(bob.id)

        val plan = planner.plan(origin(file), MINE)
        assertEquals(RecordStatus.DELETED_BEFORE, plan.records.single().status)
        assertEquals(1, plan.deletedBefore.size)
        import(file)
        assertTrue(npcNames(MINE).isEmpty())

        import(file) { ImportRequest(it, ConflictMode.SKIP, restore = setOf(ExchangeRef(ExchangeKind.NPC, 1))) }
        assertEquals(listOf("Bob"), npcNames(MINE))
        assertTrue(
            database.deletedOriginQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .isEmpty(),
        )
    }

    @Test
    fun `putting a deleted record back with its old id clears it from the list`() {
        val id = npc(MINE, "Bob", "bob")
        database.npcQueries.deleteById(id)
        assertEquals(
            1,
            database.deletedOriginQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .size,
        )

        npc(MINE, "Bob", "bob")

        assertTrue(
            database.deletedOriginQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .isEmpty(),
        )
    }

    @Test
    fun `deleting a whole campaign leaves nothing in the list of deleted records`() {
        npc(MINE, "Bob", "bob")
        database.campaignQueries.deleteById(MINE)

        assertTrue(
            database.deletedOriginQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .isEmpty(),
        )
    }

    @Test
    fun `taking the last import back removes what it added and restores what it changed`() {
        npc(THEIRS, "Ann", "ann", description = "file")
        npc(THEIRS, "Eve", "eve")
        session(THEIRS, "theirs", listOf("f1" to "c"))
        val ann = npc(MINE, "Ann", "ann", description = "mine")
        val mineSession = session(MINE, "mine", listOf("m1" to "x"))
        val refAnn = ExchangeRef(ExchangeKind.NPC, 1)

        import(export()) {
            ImportRequest(
                it,
                ConflictMode.SKIP,
                fields =
                    mapOf(
                        refAnn to mapOf(RecordField.DESCRIPTION to FieldChoice.FILE),
                    ),
            )
        }
        assertEquals(
            "file",
            database.npcQueries
                .selectById(ann)
                .executeAsOne()
                .description,
        )
        assertEquals(setOf("Ann", "Eve"), npcNames(MINE).toSet())
        assertEquals(2, entries(mineSession).size)
        assertTrue(rollback.hasJournal(MINE))

        rollback.undo(MINE)

        assertEquals(
            "mine",
            database.npcQueries
                .selectById(ann)
                .executeAsOne()
                .description,
        )
        assertEquals(listOf("Ann"), npcNames(MINE))
        assertEquals(1, entries(mineSession).size)
        assertTrue(
            database.deletedOriginQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .isEmpty(),
        )
        assertFalse(rollback.hasJournal(MINE))
    }

    @Test
    fun `a merged away id of a deleted record is still told as deleted`() {
        npc(THEIRS, "Kahuri", "theirs-kahuri")
        val local = npc(MINE, "Village Kahuri", "local-kahuri")
        val file = export()
        val ref = ExchangeRef(ExchangeKind.NPC, 1)
        import(file) { ImportRequest(it, ConflictMode.SKIP, resolutions = mapOf(ref to Resolution.SAME)) }
        database.npcQueries.deleteById(local)

        val plan = planner.plan(origin(file), MINE)

        assertEquals(RecordStatus.DELETED_BEFORE, plan.records.single().status)
    }

    @Test
    fun `taking the last import back unmarks a matched member it marked as mine`() {
        val local = hero(MINE, "hero", mine = false, hp = null, notes = "")
        hero(THEIRS, "hero", mine = true, hp = 20L, notes = "")

        import(export(mode = ExportMode.SELF)) { ImportRequest(it, ConflictMode.SKIP, takeAsMine = true) }
        assertTrue(
            database.partyMemberQueries
                .selectById(local)
                .executeAsOne()
                .is_player_character,
        )

        rollback.undo(MINE)

        assertFalse(
            database.partyMemberQueries
                .selectById(local)
                .executeAsOne()
                .is_player_character,
        )
    }

    @Test
    fun `an import that changes nothing keeps the journal of the one before`() {
        npc(THEIRS, "Eve", "eve")
        val file = export()
        import(file)
        assertTrue(rollback.hasJournal(MINE))

        import(file)

        assertTrue(rollback.hasJournal(MINE))
    }

    private fun fileNpc(
        name: String,
        description: String,
        race: String = "",
        originId: String = "file-origin",
    ) = npc(THEIRS, name, originId, description).also {
        database.npcQueries.update(
            name,
            NpcStatus.FRIEND,
            description,
            race,
            "",
            "",
            NpcLifeState.ALIVE,
            "",
            null,
            "",
            it,
        )
    }

    @Test
    fun `a field the campaign has empty is filled from the file without asking`() {
        val local = npc(MINE, "Bob", "bob", description = "mine")
        fileNpc("Bob", "mine", race = "elf", originId = "bob")

        val plan = planner.plan(origin(export()), MINE)
        assertTrue(
            plan.records
                .single()
                .changes
                .single()
                .isFill,
        )
        import(export())

        assertEquals(
            "elf",
            database.npcQueries
                .selectById(local)
                .executeAsOne()
                .race,
        )
    }

    @Test
    fun `a field the file leaves empty never clears the one in the campaign`() {
        val local = npc(MINE, "Bob", "bob", description = "mine")
        database.npcQueries.update(
            "Bob",
            NpcStatus.FRIEND,
            "mine",
            "dwarf",
            "",
            "",
            NpcLifeState.ALIVE,
            "",
            null,
            "",
            local,
        )
        fileNpc("Bob", "file text", race = "", originId = "bob")

        import(export()) { ImportRequest(it, ConflictMode.SKIP, replace = setOf(ExchangeRef(ExchangeKind.NPC, 1))) }

        val row = database.npcQueries.selectById(local).executeAsOne()
        assertEquals("file text", row.description)
        assertEquals("dwarf", row.race)
    }

    @Test
    fun `a conflict stays as it is unless the person takes the file`() {
        val local = npc(MINE, "Bob", "bob", description = "mine")
        fileNpc("Bob", "theirs", originId = "bob")

        import(export())

        assertEquals(
            "mine",
            database.npcQueries
                .selectById(local)
                .executeAsOne()
                .description,
        )
    }

    @Test
    fun `keeping both texts puts the file text below the mine with the mark`() {
        val local = npc(MINE, "Bob", "bob", description = "mine")
        fileNpc("Bob", "theirs", originId = "bob")
        val ref = ExchangeRef(ExchangeKind.NPC, 1)

        import(export()) {
            ImportRequest(
                it,
                ConflictMode.SKIP,
                fields = mapOf(ref to mapOf(RecordField.DESCRIPTION to FieldChoice.BOTH)),
                fromFileLabel = "from file",
            )
        }

        assertEquals(
            "mine\n\nfrom file: theirs",
            database.npcQueries
                .selectById(local)
                .executeAsOne()
                .description,
        )
    }

    @Test
    fun `a field is taken one by one and the others stay`() {
        val local = npc(MINE, "Bob", "bob", description = "mine", status = NpcStatus.ENEMY)
        fileNpc("Bob", "theirs", originId = "bob")
        val ref = ExchangeRef(ExchangeKind.NPC, 1)

        import(export()) {
            ImportRequest(
                it,
                ConflictMode.SKIP,
                fields =
                    mapOf(
                        ref to mapOf(RecordField.DESCRIPTION to FieldChoice.FILE),
                    ),
            )
        }

        val row = database.npcQueries.selectById(local).executeAsOne()
        assertEquals("theirs", row.description)
        assertEquals(NpcStatus.ENEMY, row.status)
    }

    @Test
    fun `names that match come in as copies when the person does not take them for one record`() {
        npc(MINE, "Bob", "mine-bob")
        npc(THEIRS, "Bob", "their-bob")

        import(export()) { ImportRequest(it, ConflictMode.SKIP, mergeNameTwins = false) }

        assertEquals(setOf("Bob", "Bob (2)"), npcNames(MINE).toSet())
    }

    private companion object {
        const val MINE = TestDatabase.SECOND_CAMPAIGN
        const val THEIRS = TestDatabase.FIRST_CAMPAIGN
    }
}

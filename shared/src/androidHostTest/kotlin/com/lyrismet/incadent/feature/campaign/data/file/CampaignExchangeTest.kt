package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.core.file.FileSource
import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ImportError
import com.lyrismet.incadent.feature.campaign.domain.model.ImportFailedException
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.codex.domain.model.CreatureType
import com.lyrismet.incadent.feature.codex.domain.model.DamageType
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.combat.domain.damage.HitScale
import com.lyrismet.incadent.feature.combat.domain.model.CombatCondition
import com.lyrismet.incadent.feature.combat.domain.model.CombatEventKind
import com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.HitEffect
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.datetime.LocalDateTime
import kotlin.io.encoding.Base64
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CampaignExchangeTest {
    private val db = TestDatabase()
    private val database: AppDatabase = db.database
    private val source = TestDatabase.FIRST_CAMPAIGN
    private val date = LocalDateTime.parse("2026-03-04T18:00")
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }

    // a bare jpeg header that reads as 16 by 32 pixels
    private val jpeg =
        byteArrayOf(-1, -40, -1, -64, 0, 17, 8, 0, 32, 0, 16, 3, 1, 34, 0, 2, 17, 1, 3, 17, 1, -1, -39)

    private fun origin(file: MemoryFile) =
        object : FileOrigin {
            override val name = "test.incadent.json"

            override fun open(): FileSource = MemoryFile(file.text.lines())
        }

    private var npcId: Long = 0L

    private fun seed() {
        database.speciesQueries.insert("Wolf", CreatureType.BEAST, "bite", "", "", 25L, jpeg, byteArrayOf(1), source)
        val speciesId = database.speciesQueries.lastInsertRowId().executeAsOne()
        database.locationQueries.insert("Town", "big", null, jpeg, byteArrayOf(1), source)
        val town = database.locationQueries.lastInsertRowId().executeAsOne()
        database.locationQueries.insert("Inn", "", town, null, null, source)
        val inn = database.locationQueries.lastInsertRowId().executeAsOne()
        database.npcQueries.insert(
            "Bob",
            NpcStatus.FRIEND,
            "d",
            "elf",
            NpcLifeState.ALIVE,
            "bobby",
            speciesId,
            "",
            jpeg,
            byteArrayOf(1),
            source,
        )
        npcId = database.npcQueries.lastInsertRowId().executeAsOne()
        database.npcLocationQueries.insert(npcId, inn)
        database.questQueries.insert("Find", QuestStatus.ACTIVE, "gold", npcId, inn, "", source)
        database.partyMemberQueries.insert(
            "Hero",
            "Rogue",
            "human",
            3L,
            "Ana",
            true,
            PartyPresence.IN,
            20L,
            15L,
            "",
            null,
            null,
            source,
        )
        database.tagQueries.insert("fight", source)
        database.phraseSnippetQueries.insert("Roll for it", source)
        database.sessionNoteQueries.insert("Day one", date, date, 5L, source)
        val session = database.sessionNoteQueries.lastInsertRowId().executeAsOne()
        val tag = database.tagQueries.selectIdByName("fight", source).executeAsOne()
        database.sessionTagQueries.insert(session, tag)
        database.sessionEntryQueries.insert(session, date, "met @[Bob|n:$npcId] at @[Inn|l:$inn]", false, true)
    }

    private fun export(selection: RecordSelection): MemoryFile =
        MemoryFile().also {
            database.campaignQueries.rename("World", source)
            CampaignExporter(database).write(source, selection, it)
        }

    private fun allOf(): RecordSelection {
        val options = CampaignExporter(database).catalog(source).options
        return RecordSelection(
            sessions = options.sessions.map { it.id }.toSet(),
            places = options.places.map { it.id }.toSet(),
            npcs = options.npcs.map { it.id }.toSet(),
            quests = options.quests.map { it.id }.toSet(),
            species = options.species.map { it.id }.toSet(),
            party = options.party.map { it.id }.toSet(),
            tags = true,
            phrases = true,
        )
    }

    private fun importNew(
        file: MemoryFile,
        selection: RecordSelection? = null,
    ) = importer.import(origin(file), ImportRequest(ImportTarget.NewCampaign("Fallback"), ConflictMode.SKIP, selection))

    @Test
    fun `an export imported as a new campaign brings every record and link`() {
        seed()
        val result = importNew(export(allOf()))

        val target = result.campaignId
        assertEquals(6, result.records)
        assertEquals(1, result.sessions)
        val npc =
            database.npcQueries
                .selectAll(target)
                .executeAsList()
                .single()
        assertEquals("Bob", npc.name)
        assertEquals(NpcStatus.FRIEND, npc.status)
        assertNotNull(npc.species_id)
        val wolf = database.speciesQueries.selectById(npc.species_id!!).executeAsOne()
        assertEquals("Wolf", wolf.name)
        assertEquals(25L, wolf.hit_points)
        val places =
            database.locationQueries
                .selectAll(target)
                .executeAsList()
                .associateBy { it.name }
        assertEquals(places.getValue("Town").id, places.getValue("Inn").parent_id)
        assertEquals(listOf(places.getValue("Inn").id), database.npcLocationQueries.selectByNpc(npc.id).executeAsList())
        val quest =
            database.questQueries
                .selectAll(target)
                .executeAsList()
                .single()
        assertEquals(npc.id, quest.given_by_npc_id)
        assertEquals(places.getValue("Inn").id, quest.location_id)
        assertEquals(
            listOf("fight"),
            database.tagQueries
                .selectAll(target)
                .executeAsList()
                .map { it.name },
        )
        assertEquals(
            listOf("Roll for it"),
            database.phraseSnippetQueries
                .selectAll(target)
                .executeAsList()
                .map { it.text },
        )
        assertTrue(
            database.partyMemberQueries
                .selectAll(target)
                .executeAsList()
                .single()
                .is_player_character,
        )
    }

    @Test
    fun `note tokens follow the new ids and never point at the old campaign`() {
        seed()
        val target = importNew(export(allOf())).campaignId
        val session =
            database.sessionNoteQueries
                .selectAll(target)
                .executeAsList()
                .single()
        val entry =
            database.sessionEntryQueries
                .selectForSession(session.id)
                .executeAsList()
                .single()
        val newNpc =
            database.npcQueries
                .selectAll(target)
                .executeAsList()
                .single()
                .id
        val newInn =
            database.locationQueries
                .selectAll(target)
                .executeAsList()
                .first { it.name == "Inn" }
                .id
        assertEquals("met @[Bob|n:$newNpc] at @[Inn|l:$newInn]", entry.body)
        assertEquals(listOf("fight"), database.sessionTagQueries.selectTagNamesForSession(session.id).executeAsList())
    }

    @Test
    fun `the source campaign is not touched by an export and an import`() {
        seed()
        importNew(export(allOf()))
        assertEquals(
            1,
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .size,
        )
        assertEquals(
            2,
            database.locationQueries
                .selectAll(source)
                .executeAsList()
                .size,
        )
    }

    @Test
    fun `photos come back and the thumbnail is made again`() {
        seed()
        val target = importNew(export(allOf())).campaignId
        val npc =
            database.npcQueries
                .selectAll(target)
                .executeAsList()
                .single()
        val full = database.npcQueries.selectById(npc.id).executeAsOne()
        assertContentEquals(jpeg, full.portrait)
        assertContentEquals(byteArrayOf(7), full.portrait_thumb)
    }

    @Test
    fun `an export without photos writes none`() {
        seed()
        val file = export(allOf().copy(photos = false))
        assertTrue(file.text.lines().none { it.contains("\"t\":\"photo\"") })
        val target = importNew(file).campaignId
        assertEquals(
            null,
            database.npcQueries
                .selectById(
                    database.npcQueries
                        .selectAll(target)
                        .executeAsList()
                        .single()
                        .id,
                ).executeAsOne()
                .portrait,
        )
    }

    @Test
    fun `a ticked session brings what it mentions and nothing else`() {
        seed()
        database.npcQueries.insert(
            "Other",
            NpcStatus.NEUTRAL,
            "",
            "",
            NpcLifeState.ALIVE,
            "",
            null,
            "",
            null,
            null,
            source,
        )
        val sessionId =
            database.sessionNoteQueries
                .selectAll(source)
                .executeAsList()
                .single()
                .id
        val target = importNew(export(RecordSelection(sessions = setOf(sessionId)))).campaignId
        assertEquals(
            listOf("Bob"),
            database.npcQueries
                .selectAll(target)
                .executeAsList()
                .map { it.name },
        )
        assertEquals(
            0,
            database.partyMemberQueries
                .selectAll(target)
                .executeAsList()
                .size,
        )
    }

    @Test
    fun `a match is skipped and the notes point at the existing record`() {
        seed()
        val file = export(allOf())
        val before =
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .single()
                .id
        val result =
            importer.import(origin(file), ImportRequest(ImportTarget.Existing(source), ConflictMode.SKIP))
        assertEquals(
            1,
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .size,
        )
        assertEquals(0, result.records)
        val bodies =
            database.sessionEntryQueries
                .selectAll(source)
                .executeAsList()
                .map { it.body }
        assertTrue(bodies.all { "@[Bob|n:$before]" in it })
    }

    @Test
    fun `a ticked match takes the version of the file and an unticked one keeps its own`() {
        seed()
        val file = export(allOf())
        val bob =
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .single()
        database.npcQueries.update("Bob", NpcStatus.ENEMY, "edited", "orc", NpcLifeState.DEAD, "", null, "", bob.id)
        val quest =
            database.questQueries
                .selectAll(source)
                .executeAsList()
                .single()
        database.questQueries.update("Find", QuestStatus.COMPLETED, "edited", null, null, "", quest.id)
        val fileBob = readImportContents(origin(file)).options.npcs.single { it.name == "Bob" }

        val result =
            importer.import(
                origin(file),
                ImportRequest(
                    ImportTarget.Existing(source),
                    ConflictMode.SKIP,
                    replace = setOf(ExchangeRef(ExchangeKind.NPC, fileBob.id)),
                ),
            )

        val replaced =
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .single()
        assertEquals(bob.id, replaced.id)
        assertEquals(NpcStatus.FRIEND, replaced.status)
        assertEquals("d", replaced.description)
        assertEquals(NpcLifeState.ALIVE, replaced.life_state)
        assertEquals(
            "edited",
            database.questQueries
                .selectAll(source)
                .executeAsList()
                .single()
                .reward,
        )
        assertEquals(1, result.records)
    }

    @Test
    fun `a replaced species keeps its hit points when the file has none and takes new ones`() {
        seed()
        val ref = setOf(ExchangeRef(ExchangeKind.SPECIES, 1))

        fun replaceWith(hitPoints: Int?): Long? {
            val file = MemoryFile()
            CampaignFileWriter(file).run {
                writeHeader(FileHeader(FILE_FORMAT, FILE_VERSION, "World", false, 1, 0))
                write(FileRecord.Species(1, "Wolf", CreatureType.BEAST, hitPoints = hitPoints))
            }
            importer.import(
                origin(file),
                ImportRequest(ImportTarget.Existing(source), ConflictMode.SKIP, replace = ref),
            )
            return database.speciesQueries
                .selectAll(source)
                .executeAsList()
                .single()
                .hit_points
        }

        assertEquals(25L, replaceWith(null))
        assertEquals(40L, replaceWith(40))
    }

    @Test
    fun `a replaced npc loses the places and the nickname the file does not have`() {
        seed()
        val bob =
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .single()
        val bare = MemoryFile()
        CampaignFileWriter(bare).run {
            writeHeader(FileHeader(FILE_FORMAT, FILE_VERSION, "World", false, 1, 0))
            write(FileRecord.Npc(id = 1, name = "Bob", status = NpcStatus.FRIEND))
        }

        importer.import(
            origin(bare),
            ImportRequest(
                ImportTarget.Existing(source),
                ConflictMode.SKIP,
                replace = setOf(ExchangeRef(ExchangeKind.NPC, 1)),
            ),
        )

        assertTrue(
            database.npcLocationQueries
                .selectByNpc(bob.id)
                .executeAsList()
                .isEmpty(),
        )
        assertEquals(
            "",
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .single()
                .nicknames,
        )
    }

    @Test
    fun `a replaced npc keeps its places when the file places did not come along`() {
        seed()
        val bob =
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .single()
        val before = database.npcLocationQueries.selectByNpc(bob.id).executeAsList()
        val file = MemoryFile()
        CampaignFileWriter(file).run {
            writeHeader(FileHeader(FILE_FORMAT, FILE_VERSION, "World", false, 2, 0))
            write(FileRecord.Place(id = 1, name = "Elsewhere", description = "", parentId = null))
            write(FileRecord.Npc(id = 1, name = "Bob", status = NpcStatus.FRIEND, placeIds = listOf(1)))
        }

        importer.import(
            origin(file),
            ImportRequest(
                ImportTarget.Existing(source),
                ConflictMode.SKIP,
                scope = RecordScope(emptySet(), emptySet(), setOf(1L), emptySet(), emptySet(), emptySet()),
                replace = setOf(ExchangeRef(ExchangeKind.NPC, 1)),
            ),
        )

        assertEquals(before, database.npcLocationQueries.selectByNpc(bob.id).executeAsList())
    }

    @Test
    fun `a replaced place never lands inside its own child`() {
        seed()
        val file = export(allOf())
        val town =
            database.locationQueries
                .selectAll(source)
                .executeAsList()
                .single { it.name == "Town" }
        val inn =
            database.locationQueries
                .selectAll(source)
                .executeAsList()
                .single { it.name == "Inn" }
        // the campaign now keeps the town inside the inn, the file has it the other way round
        database.locationQueries.setParent(null, inn.id)
        database.locationQueries.setParent(inn.id, town.id)
        val fileInn = readImportContents(origin(file)).options.places.single { it.name == "Inn" }

        importer.import(
            origin(file),
            ImportRequest(
                ImportTarget.Existing(source),
                ConflictMode.SKIP,
                replace = setOf(ExchangeRef(ExchangeKind.PLACE, fileInn.id)),
            ),
        )

        val parents =
            database.locationQueries
                .selectAll(source)
                .executeAsList()
                .associate { it.name to it.parent_id }
        assertTrue(parents["Town"] == inn.id && parents["Inn"] == null)
    }

    @Test
    fun `a match is copied under a second name`() {
        seed()
        val file = export(allOf())
        importer.import(origin(file), ImportRequest(ImportTarget.Existing(source), ConflictMode.COPY))
        val names =
            database.npcQueries
                .selectAll(source)
                .executeAsList()
                .map { it.name }
                .sorted()
        assertEquals(listOf("Bob", "Bob (2)"), names)
        assertEquals(
            2,
            database.questQueries
                .selectAll(source)
                .executeAsList()
                .size,
        )
    }

    @Test
    fun `a second player character is not made when one exists`() {
        seed()
        importer.import(origin(export(allOf())), ImportRequest(ImportTarget.Existing(source), ConflictMode.COPY))
        assertEquals(
            1,
            database.partyMemberQueries
                .selectAll(source)
                .executeAsList()
                .count { it.is_player_character },
        )
    }

    @Test
    fun `a broken file leaves the database as it was`() {
        seed()
        val lines = export(allOf()).text.lines().toMutableList()
        lines[lines.size - 2] = """{"t":"entry","sessionId":99,"createdAt":"2026-03-04T18:00","body":"x"}"""
        val campaigns =
            database.campaignQueries
                .selectSummaries { id, _, _, _, _, _, _, _ -> id }
                .executeAsList()
                .size
        assertFailsWith<ImportFailedException> {
            importNew(MemoryFile(lines))
        }.also { assertEquals(ImportError.Malformed, it.error) }
        assertEquals(
            campaigns,
            database.campaignQueries
                .selectSummaries {
                    id,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    _,
                    ->
                    id
                }.executeAsList()
                .size,
        )
    }

    @Test
    fun `a photo that is not a jpeg is dropped and the record stays`() {
        seed()
        val lines =
            export(allOf()).text.lines().map {
                if (it.contains(
                        "\"t\":\"photo\"",
                    )
                ) {
                    it.replace(
                        Regex("\"data\":\"[^\"]*\""),
                        "\"data\":\"${Base64.encode(byteArrayOf(1, 2, 3))}\"",
                    )
                } else {
                    it
                }
            }
        val target = importNew(MemoryFile(lines)).campaignId
        assertEquals(
            1,
            database.npcQueries
                .selectAll(target)
                .executeAsList()
                .size,
        )
        assertEquals(
            null,
            database.npcQueries
                .selectById(
                    database.npcQueries
                        .selectAll(target)
                        .executeAsList()
                        .single()
                        .id,
                ).executeAsOne()
                .portrait,
        )
    }

    @Test
    fun `the preview lists the file without writing`() {
        seed()
        val contents = readImportContents(origin(export(allOf())))
        assertEquals("World", contents.campaignName)
        assertEquals(6, contents.options.recordCount)
        assertEquals(1, contents.options.sessionCount)
        assertTrue(contents.withPhotos)
        assertEquals(
            1,
            database.campaignQueries
                .selectSummaries { id, _, _, _, _, _, _, _ -> id }
                .executeAsList()
                .size,
        )
    }

    private fun seedFight(): Long {
        val session =
            database.sessionNoteQueries
                .selectAll(source)
                .executeAsList()
                .single()
                .id
        val species =
            database.speciesQueries
                .selectAll(source)
                .executeAsList()
                .single()
                .id
        database.combatQueries.insertFinished(session, date, 3L, 1L, CombatOutcome.VICTORY, date, source)
        val combat = database.combatQueries.lastInsertRowId().executeAsOne()

        fun fighter(
            name: String,
            side: CombatSide,
            speciesId: Long?,
        ): Long {
            database.combatantQueries.insert(
                combat,
                name,
                side,
                null,
                speciesId,
                null,
                1L,
                CombatState.DOWN,
                1L,
                0L,
                0L,
                0L,
                null,
                CombatStance.ENEMY,
                0L,
                0L,
                setOf(CombatCondition.PRONE),
                "limps",
                1L,
                null,
                null,
            )
            return database.combatantQueries.lastInsertRowId().executeAsOne()
        }
        val hero = fighter("Hero", CombatSide.PARTY, null)
        val wolf = fighter("Wolf", CombatSide.ENEMY, species)
        database.combatEventQueries.insert(
            combat,
            1L,
            CombatEventKind.DAMAGE,
            hero,
            wolf,
            9L,
            DamageType.SLASHING,
            HitEffect.HALF,
            null,
            null,
            2L,
            7L,
            18L,
            HitScale.ONE,
        )
        database.speciesHitObservationQueries.insert(species, combat, 9L, 1L)
        return combat
    }

    @Test
    fun `a saved fight travels with its session and its links`() {
        seed()
        seedFight()
        val target = importNew(export(allOf())).campaignId

        val combat =
            database.combatQueries
                .selectAll(target)
                .executeAsList()
                .single()
        assertEquals(CombatPhase.FINISHED, combat.phase)
        assertEquals(CombatOutcome.VICTORY, combat.outcome)
        assertEquals(3L, combat.round)
        val session =
            database.sessionNoteQueries
                .selectAll(target)
                .executeAsList()
                .single()
        assertEquals(session.id, combat.session_note_id)
        val fighters =
            database.combatantQueries
                .selectByCombat(combat.id)
                .executeAsList()
                .associateBy { it.name }
        val newSpecies =
            database.speciesQueries
                .selectAll(target)
                .executeAsList()
                .single()
                .id
        assertEquals(newSpecies, fighters.getValue("Wolf").species_id)
        assertEquals(setOf(CombatCondition.PRONE), fighters.getValue("Hero").conditions)
        val blow =
            database.combatEventQueries
                .selectByCombat(combat.id)
                .executeAsList()
                .single()
        assertEquals(fighters.getValue("Hero").id, blow.source_id)
        assertEquals(fighters.getValue("Wolf").id, blow.target_id)
        assertEquals(HitEffect.HALF, blow.effect)
        assertEquals(18L, blow.raw)
        val seen =
            database.speciesHitObservationQueries
                .selectByCampaign(target)
                .executeAsList()
                .single()
        assertEquals(newSpecies, seen.species_id)
        assertEquals(combat.id, seen.combat_id)
    }

    @Test
    fun `a fight of a session that was not picked stays behind`() {
        seed()
        seedFight()
        val selection = allOf().copy(sessions = emptySet())
        val target = importNew(export(selection)).campaignId
        assertTrue(
            database.combatQueries
                .selectAll(target)
                .executeAsList()
                .isEmpty(),
        )
    }

    @Test
    fun `a file of the first version without fights is read as before`() {
        val lines =
            listOf(
                """{"format":"incadent","version":1,"name":"Old","records":1,"sessions":0}""",
                """{"t":"species","id":1,"name":"Wolf","creatureType":"BEAST"}""",
            )
        val file = MemoryFile(lines)
        val target = importNew(file).campaignId
        assertEquals(
            listOf("Wolf"),
            database.speciesQueries
                .selectAll(target)
                .executeAsList()
                .map { it.name },
        )
    }

    @Test
    fun `a fallen character stays fallen through an export and an import`() {
        seed()
        val hero =
            database.partyMemberQueries
                .selectAll(source)
                .executeAsList()
                .single()
                .id
        database.partyMemberQueries.setFallen(true, null, hero)
        val target = importNew(export(allOf())).campaignId
        assertTrue(
            database.partyMemberQueries
                .selectAll(target)
                .executeAsList()
                .single()
                .fallen,
        )
    }
}

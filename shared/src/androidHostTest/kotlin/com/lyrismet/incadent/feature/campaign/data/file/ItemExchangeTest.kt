package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.item.SelectAll
import com.lyrismet.incadent.domain.model.EntityKind
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
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItemExchangeTest {
    private val db = TestDatabase().also { it.addCampaign(MINE) }
    private val database: AppDatabase = db.database
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }
    private val planner = ImportPlanner(database)
    private val rollback = ImportRollback(database) { _, _ -> byteArrayOf(7) }

    private fun export(mode: ExportMode = ExportMode.FRIEND): MemoryFile {
        val options = CampaignExporter(database).catalog(THEIRS).options
        val all =
            RecordSelection(
                sessions = options.sessions.map { it.id }.toSet(),
                places = options.places.map { it.id }.toSet(),
                npcs = options.npcs.map { it.id }.toSet(),
                quests = options.quests.map { it.id }.toSet(),
                species = options.species.map { it.id }.toSet(),
                party = options.party.map { it.id }.toSet(),
                items = options.items.map { it.id }.toSet(),
                mode = mode,
            )
        return MemoryFile().also { CampaignExporter(database).write(THEIRS, all, it) }
    }

    private fun import(
        file: MemoryFile,
        target: ImportTarget = ImportTarget.Existing(MINE),
        request: (ImportTarget) -> ImportRequest = { ImportRequest(it, ConflictMode.SKIP) },
    ) = importer.import(origin(file), request(target))

    private fun npc(
        campaignId: String,
        name: String,
        originId: String,
    ): Long {
        database.npcQueries.insert(
            name,
            NpcStatus.FRIEND,
            "",
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

    private fun hero(campaignId: String): Long {
        database.partyMemberQueries.insert(
            "Hero",
            "",
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
            campaignId,
            "hero",
        )
        return database.partyMemberQueries.lastInsertRowId().executeAsOne()
    }

    private fun item(
        campaignId: String,
        name: String,
        originId: String,
        quantity: Long = 1,
        price: Long? = null,
        owner: Pair<EntityKind, Long>? = null,
        notes: String = "",
    ): Long {
        database.itemQueries.insert(
            name,
            quantity,
            price,
            Coin.GOLD,
            owner?.first,
            owner?.second ?: 0L,
            notes,
            null,
            null,
            campaignId,
            originId,
        )
        return database.itemQueries.lastInsertRowId().executeAsOne()
    }

    private fun itemsOf(campaignId: String): List<SelectAll> =
        database.itemQueries.selectAll(campaignId).executeAsList()

    // a campaign with a character, an npc and the things they carry, two stacks share a name on purpose
    private fun seed() {
        val bob = npc(THEIRS, "Bob", "bob")
        val hero = hero(THEIRS)
        item(
            THEIRS,
            "Potion",
            "potion-bob",
            quantity = 2,
            price = 50,
            owner = EntityKind.NPC to bob,
            notes = "from @[Bob|n:$bob]",
        )
        item(THEIRS, "Potion", "potion-shared")
        item(THEIRS, "Dagger", "dagger", owner = EntityKind.PARTY to hero)
    }

    @Test
    fun `a friend copy carries the things of the world and leaves out what a character carries`() {
        seed()
        val target = import(export(), ImportTarget.NewCampaign("Fallback")).campaignId

        val names = itemsOf(target).map { it.name }
        assertEquals(listOf("Potion", "Potion"), names.sorted())
        // the character itself travels in a friend copy, only what it carries stays behind
        assertEquals(
            listOf("Hero"),
            database.partyMemberQueries
                .selectAll(target)
                .executeAsList()
                .map { it.name },
        )
    }

    @Test
    fun `a personal copy brings the things of a character with their owner`() {
        seed()
        val target =
            import(export(ExportMode.SELF), ImportTarget.NewCampaign("Fallback")) {
                ImportRequest(it, ConflictMode.SKIP, takeAsMine = true)
            }.campaignId

        val hero =
            database.partyMemberQueries
                .selectAll(target)
                .executeAsList()
                .single()
        val dagger = itemsOf(target).single { it.name == "Dagger" }
        assertEquals(EntityKind.PARTY, dagger.owner_kind)
        assertEquals(hero.id, dagger.owner_id)
    }

    @Test
    fun `an item keeps its amount price coin owner and notes with the mentions moved to the new ids`() {
        seed()
        val target = import(export(), ImportTarget.NewCampaign("Fallback")).campaignId

        val bob =
            database.npcQueries
                .selectAll(target)
                .executeAsList()
                .single()
        val potion = itemsOf(target).single { it.owner_kind == EntityKind.NPC }
        assertEquals(2L, potion.quantity)
        assertEquals(50L, potion.price)
        assertEquals(Coin.GOLD, potion.coin)
        assertEquals(bob.id, potion.owner_id)
        assertEquals("from @[Bob|n:${bob.id}]", potion.notes)
        assertTrue(potion.origin_id.isNotBlank())
    }

    @Test
    fun `two stacks with the same name stay two items`() {
        seed()
        val file = export()
        val target = import(file, ImportTarget.NewCampaign("Fallback")).campaignId
        import(file, ImportTarget.Existing(target))

        assertEquals(2, itemsOf(target).size)
    }

    @Test
    fun `an item already there by its origin id is not made again`() {
        seed()
        item(MINE, "Potion", "potion-bob", quantity = 2, price = 50, notes = "from Bob")
        item(MINE, "Potion", "potion-shared")

        val plan = planner.plan(origin(export()), MINE)

        assertEquals(
            listOf(RecordStatus.SAME, RecordStatus.SAME),
            plan.records
                .filter {
                    it.ref.kind ==
                        ExchangeKind.ITEM
                }.map { it.status },
        )
    }

    @Test
    fun `an item of the file with an other amount is a change the person decides`() {
        seed()
        val mine = item(MINE, "Potion", "potion-bob", quantity = 5, price = 50, notes = "from Bob")
        val file = export()

        val record =
            planner.plan(origin(file), MINE).records.single {
                it.ref.kind == ExchangeKind.ITEM &&
                    it.changes.isNotEmpty()
            }
        assertEquals(RecordStatus.CHANGED, record.status)
        assertEquals(listOf(RecordField.QUANTITY), record.changes.map { it.field })

        import(file)
        assertEquals(
            5L,
            database.itemQueries
                .selectById(mine)
                .executeAsOne()
                .quantity,
        )

        val ref = ExchangeRef(ExchangeKind.ITEM, record.ref.id)
        import(file) {
            ImportRequest(
                it,
                ConflictMode.SKIP,
                fields =
                    mapOf(
                        ref to mapOf(RecordField.QUANTITY to FieldChoice.FILE),
                    ),
            )
        }
        assertEquals(
            2L,
            database.itemQueries
                .selectById(mine)
                .executeAsOne()
                .quantity,
        )
    }

    @Test
    fun `taking the import back removes the added items and restores the changed one`() {
        seed()
        val mine = item(MINE, "Potion", "potion-bob", quantity = 5, price = 50, notes = "from Bob")
        val file = export()
        val ref =
            ExchangeRef(
                ExchangeKind.ITEM,
                planner
                    .plan(origin(file), MINE)
                    .records
                    .first { it.changes.isNotEmpty() }
                    .ref.id,
            )

        import(file) {
            ImportRequest(
                it,
                ConflictMode.SKIP,
                fields =
                    mapOf(
                        ref to mapOf(RecordField.QUANTITY to FieldChoice.FILE),
                    ),
            )
        }
        assertEquals(2, itemsOf(MINE).size)

        rollback.undo(MINE)

        assertEquals(listOf(mine), itemsOf(MINE).map { it.id })
        assertEquals(
            5L,
            database.itemQueries
                .selectById(mine)
                .executeAsOne()
                .quantity,
        )
        assertTrue(
            database.deletedOriginQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .isEmpty(),
        )
    }

    @Test
    fun `an item the person deleted is not brought back by the next file`() {
        seed()
        val file = export()
        import(file)
        val gone = itemsOf(MINE).single { it.origin_id == "potion-shared" }
        database.itemQueries.deleteById(gone.id)

        val plan = planner.plan(origin(file), MINE)
        assertEquals(1, plan.deletedBefore.count { it.ref.kind == ExchangeKind.ITEM })
        import(file)

        assertNull(itemsOf(MINE).firstOrNull { it.origin_id == "potion-shared" })
    }

    @Test
    fun `a ticked session brings the item it mentions and the one who carries it`() {
        val bob = npc(THEIRS, "Bob", "bob")
        val rope = item(THEIRS, "Rope", "rope", owner = EntityKind.NPC to bob)
        database.sessionNoteQueries.insert(
            "Day",
            kotlinx.datetime.LocalDateTime.parse("2026-03-04T18:00"),
            null,
            1L,
            THEIRS,
            "day",
        )
        val session = database.sessionNoteQueries.lastInsertRowId().executeAsOne()
        database.sessionEntryQueries.insert(
            session,
            kotlinx.datetime.LocalDateTime.parse("2026-03-04T18:01"),
            "took @[Rope|i:$rope]",
            false,
            false,
            "e1",
        )
        val options = CampaignExporter(database).catalog(THEIRS).options
        val picked = RecordSelection(sessions = options.sessions.map { it.id }.toSet())
        val file = MemoryFile().also { CampaignExporter(database).write(THEIRS, picked, it) }

        val target = import(file, ImportTarget.NewCampaign("Fallback")).campaignId

        val newRope = itemsOf(target).single()
        assertEquals("Rope", newRope.name)
        assertNotNull(
            database.npcQueries
                .selectAll(target)
                .executeAsList()
                .singleOrNull { it.id == newRope.owner_id },
        )
        val entry =
            database.sessionEntryQueries
                .selectForSession(
                    database.sessionNoteQueries
                        .selectAll(target)
                        .executeAsList()
                        .single()
                        .id,
                ).executeAsList()
                .single()
        assertEquals("took @[Rope|i:${newRope.id}]", entry.body)
    }

    private companion object {
        const val MINE = TestDatabase.SECOND_CAMPAIGN
        const val THEIRS = TestDatabase.FIRST_CAMPAIGN
    }
}

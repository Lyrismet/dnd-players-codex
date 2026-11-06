package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.codex.domain.model.Ability
import com.lyrismet.incadent.feature.codex.domain.model.Coin
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.persistence.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonalCopyExchangeTest {
    private val db = TestDatabase().also { it.addCampaign(MINE) }
    private val database: AppDatabase = db.database
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }
    private val rollback = ImportRollback(database) { _, _ -> byteArrayOf(7) }

    private fun member(
        campaignId: String,
        name: String,
        originId: String,
        isMe: Boolean,
    ): Long {
        database.partyMemberQueries.insert(
            name,
            "Wizard",
            "",
            "elf",
            5L,
            if (isMe) "Ana" else "",
            isMe,
            PartyPresence.IN,
            28L,
            22L,
            "",
            null,
            null,
            campaignId,
            originId,
        )
        return database.partyMemberQueries.lastInsertRowId().executeAsOne()
    }

    private fun sheetOf(
        campaignId: String,
        memberId: Long,
        gold: Int,
        resource: String,
    ) {
        writeSheetRecord(
            database,
            campaignId,
            memberId,
            FileRecord.Sheet(
                partyId = 0,
                abilities = mapOf(Ability.STRENGTH to 8, Ability.INTELLIGENCE to 18),
                saves = setOf(Ability.INTELLIGENCE),
                background = "Sage",
                gold = gold,
                copper = 25,
            ),
        )
        writeSheetRecord(database, campaignId, memberId, FileRecord.Resource(0, resource, 3, 2, null, 0))
    }

    // a character with a sheet, a counter, vitals and a dagger, and a companion with a sheet of their own
    private fun seed() {
        val lira = member(THEIRS, "Lira", "lira", isMe = true)
        sheetOf(THEIRS, lira, gold = 34, resource = "Arcane Recovery")
        database.partyMemberQueries.setVitals(22L, 3L, 12L, "mage armor", 1L, 2L, lira)
        database.itemQueries.insert(
            "Dagger",
            1L,
            null,
            Coin.GOLD,
            EntityKind.PARTY,
            lira,
            "",
            null,
            null,
            THEIRS,
            "dagger",
        )
        val torvin = member(THEIRS, "Torvin", "torvin", isMe = false)
        database.itemQueries.insert("Axe", 1L, null, Coin.GOLD, EntityKind.PARTY, torvin, "", null, null, THEIRS, "axe")
    }

    private fun export(mode: ExportMode): MemoryFile {
        val options = CampaignExporter(database).catalog(THEIRS).options
        val all =
            RecordSelection(
                party = options.party.map { it.id }.toSet(),
                items = options.items.map { it.id }.toSet(),
                mode = mode,
            )
        return MemoryFile().also { CampaignExporter(database).write(THEIRS, all, it) }
    }

    private fun import(
        file: MemoryFile,
        target: ImportTarget,
        takeAsMine: Boolean = false,
        takeVitals: Boolean = false,
    ) = importer.import(
        origin(file),
        ImportRequest(target, ConflictMode.SKIP, takeAsMine = takeAsMine, takeVitals = takeVitals),
    )

    private fun partyOf(campaignId: String) = database.partyMemberQueries.selectAll(campaignId).executeAsList()

    private fun itemsOf(campaignId: String) = database.itemQueries.selectAll(campaignId).executeAsList()

    @Test
    fun `a friend copy leaves out the sheet the counters the vitals and what a character carries`() {
        seed()
        val file = export(ExportMode.FRIEND)

        val target =
            import(file, ImportTarget.NewCampaign("Fallback"), takeAsMine = true, takeVitals = true).campaignId

        val lira = partyOf(target).single { it.name == "Lira" }
        assertNull(database.characterSheetQueries.selectByMember(lira.id).executeAsOneOrNull())
        assertTrue(
            database.characterResourceQueries
                .selectByMember(lira.id)
                .executeAsList()
                .isEmpty(),
        )
        val vitals = database.partyMemberQueries.selectVitals(lira.id).executeAsOne()
        assertNull(vitals.armor_class)
        assertEquals(0L, vitals.temp_hp)
        assertTrue(itemsOf(target).isEmpty())
        assertFalse(lira.is_player_character)
        assertFalse(file.text.contains("Arcane Recovery"))
        assertFalse(file.text.contains("Sage"))
    }

    @Test
    fun `a personal copy restored as a campaign brings the sheet the wallet the counters and the vitals`() {
        seed()

        val target =
            import(
                export(ExportMode.SELF),
                ImportTarget.NewCampaign("Fallback"),
                takeAsMine = true,
                takeVitals = true,
            ).campaignId

        val lira = partyOf(target).single { it.name == "Lira" }
        assertTrue(lira.is_player_character)
        val sheet = database.characterSheetQueries.selectByMember(lira.id).executeAsOne()
        assertEquals(18L, sheet.ability_int)
        assertNull(sheet.ability_dex)
        assertEquals(34L, sheet.coin_gold)
        assertEquals(25L, sheet.coin_copper)
        assertEquals("Sage", sheet.background)
        val counter =
            database.characterResourceQueries
                .selectByMember(lira.id)
                .executeAsList()
                .single()
        assertEquals("Arcane Recovery", counter.name)
        assertEquals(2L, counter.current_value)
        val vitals = database.partyMemberQueries.selectVitals(lira.id).executeAsOne()
        assertEquals(12L, vitals.armor_class)
        assertEquals("mage armor", vitals.armor_class_from)
        assertEquals(3L, vitals.temp_hp)
        assertEquals(1L, vitals.death_saves_success)
        assertEquals(2L, vitals.death_saves_failure)
        assertEquals(listOf("Axe", "Dagger"), itemsOf(target).map { it.name }.sorted())
    }

    @Test
    fun `a personal copy of someone else brings no sheet no items and no mark until asked`() {
        seed()

        import(export(ExportMode.SELF), ImportTarget.Existing(MINE))

        val lira = partyOf(MINE).single { it.name == "Lira" }
        assertFalse(lira.is_player_character)
        assertNull(database.characterSheetQueries.selectByMember(lira.id).executeAsOneOrNull())
        assertTrue(itemsOf(MINE).isEmpty())
        assertNull(
            database.partyMemberQueries
                .selectVitals(lira.id)
                .executeAsOne()
                .armor_class,
        )
        assertNull(partyOf(MINE).first { it.name == "Lira" }.hp_current)
    }

    @Test
    fun `taking the sheet from the file replaces the one there and taking the import back returns it`() {
        seed()
        val mine = member(MINE, "Lira", "lira", isMe = true)
        sheetOf(MINE, mine, gold = 5, resource = "Old counter")
        val file = export(ExportMode.SELF)

        import(file, ImportTarget.Existing(MINE), takeAsMine = true)

        assertEquals(
            34L,
            database.characterSheetQueries
                .selectByMember(mine)
                .executeAsOne()
                .coin_gold,
        )
        assertEquals(
            listOf("Arcane Recovery"),
            database.characterResourceQueries
                .selectByMember(mine)
                .executeAsList()
                .map { it.name },
        )

        rollback.undo(MINE)

        assertEquals(
            5L,
            database.characterSheetQueries
                .selectByMember(mine)
                .executeAsOne()
                .coin_gold,
        )
        assertEquals(
            listOf("Old counter"),
            database.characterResourceQueries
                .selectByMember(mine)
                .executeAsList()
                .map { it.name },
        )
    }

    @Test
    fun `keeping the device leaves the sheet and the wallet of the character as they are`() {
        seed()
        val mine = member(MINE, "Lira", "lira", isMe = true)
        sheetOf(MINE, mine, gold = 5, resource = "Old counter")

        import(export(ExportMode.SELF), ImportTarget.Existing(MINE), takeAsMine = false, takeVitals = true)

        assertEquals(
            5L,
            database.characterSheetQueries
                .selectByMember(mine)
                .executeAsOne()
                .coin_gold,
        )
        assertNotNull(
            database.characterResourceQueries
                .selectByMember(mine)
                .executeAsList()
                .singleOrNull(),
        )
        assertTrue(itemsOf(MINE).isEmpty())
    }

    @Test
    fun `a file made before the sheet existed does not touch the sheet or the vitals there`() {
        val mine = member(MINE, "Lira", "lira", isMe = true)
        sheetOf(MINE, mine, gold = 5, resource = "Old counter")
        database.partyMemberQueries.setVitals(22L, 3L, 12L, "", 0L, 0L, mine)
        val old =
            MemoryFile().also {
                val writer = CampaignFileWriter(it)
                writer.writeHeader(FileHeader(FILE_FORMAT, 5, "World", false, 1, 0, mode = ExportMode.SELF))
                writer.write(
                    FileRecord.Party(
                        1,
                        "Lira",
                        "Wizard",
                        "elf",
                        5,
                        "Ana",
                        true,
                        hpMax = 28,
                        hpCurrent = 20,
                        originId = "lira",
                    ),
                )
                it.close()
            }

        import(old, ImportTarget.Existing(MINE), takeAsMine = true, takeVitals = true)

        assertEquals(
            5L,
            database.characterSheetQueries
                .selectByMember(mine)
                .executeAsOne()
                .coin_gold,
        )
        val vitals = database.partyMemberQueries.selectVitals(mine).executeAsOne()
        assertEquals(12L, vitals.armor_class)
        assertEquals(3L, vitals.temp_hp)
    }

    private companion object {
        const val MINE = TestDatabase.SECOND_CAMPAIGN
        const val THEIRS = TestDatabase.FIRST_CAMPAIGN
    }
}

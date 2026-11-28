package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.feature.combat.domain.model.MarkKind
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DowntimeMarksExchangeTest {
    private val db = TestDatabase().also { it.addCampaign(MINE) }
    private val database: AppDatabase = db.database
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }
    private val rollback = ImportRollback(database) { _, _ -> byteArrayOf(7) }
    private val day = LocalDateTime(2026, 11, 6, 19, 0)

    // one session with a downtime after it, and a fight in it with a marked wolf that knows its weak spot
    private fun seed() {
        database.sessionNoteQueries.insert("Night", day, day, 0L, THEIRS, "night")
        val session = database.sessionNoteQueries.lastInsertRowId().executeAsOne()
        database.downtimeQueries.insert(
            THEIRS,
            session,
            "short",
            "long",
            "gained",
            50L,
            """[{"name":"Rope","quantity":2}]""",
            0L,
            0L,
            0L,
            "dt-1",
        )
        database.customConditionQueries.insert(THEIRS, "Cursed")
        database.combatQueries.insertFinished(
            session,
            day,
            2L,
            0L,
            com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome.VICTORY,
            day,
            THEIRS,
            "fight-1",
        )
        val combat = database.combatQueries.lastInsertRowId().executeAsOne()
        database.combatantQueries.insert(
            combat,
            "Wolf",
            CombatSide.ENEMY,
            null,
            null,
            null,
            1L,
            CombatState.WHOLE,
            1L,
            0L,
            0L,
            0L,
            null,
            CombatStance.ENEMY,
            0L,
            0L,
            emptySet(),
            "",
            1L,
            null,
            null,
            "",
            0L,
        )
        val wolf = database.combatantQueries.lastInsertRowId().executeAsOne()
        database.combatantQueries.updateVulnerabilities("fire", wolf)
        database.combatantQueries.updateFacts("limps", wolf)
        database.combatantMarkQueries.insert(wolf, MarkKind.CUSTOM, "Cursed", 3L, 2L)
    }

    private fun export(): MemoryFile {
        val options = CampaignExporter(database).catalog(THEIRS).options
        val all = RecordSelection(sessions = options.sessions.map { it.id }.toSet(), mode = ExportMode.FRIEND)
        return MemoryFile().also { CampaignExporter(database).write(THEIRS, all, it) }
    }

    private fun import(file: MemoryFile) =
        importer.import(origin(file), ImportRequest(ImportTarget.Existing(MINE), ConflictMode.SKIP))

    @Test
    fun `downtime conditions marks and fighter notes travel with the file`() {
        seed()

        import(export())

        val downtime = database.downtimeQueries.selectAll(MINE).executeAsOne()
        assertEquals(50, downtime.gold_delta)
        assertEquals("long", downtime.long_text)
        assertTrue(downtime.items.contains("Rope"))
        assertEquals(
            listOf("Cursed"),
            database.customConditionQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .map { it.name },
        )
        val wolf = database.combatantQueries.selectByCampaign(MINE).executeAsOne()
        assertEquals("fire", wolf.vulnerabilities)
        assertEquals("limps", wolf.facts)
        val mark = database.combatantMarkQueries.selectByCampaign(MINE).executeAsOne()
        assertEquals(MarkKind.CUSTOM, mark.kind)
        assertEquals(3L, mark.rounds_left)
        assertEquals(2L, mark.started_round)
    }

    @Test
    fun `the same file twice adds nothing and an undo takes the downtime away`() {
        seed()
        val file = export()

        import(file)
        import(file)

        assertEquals(
            1,
            database.downtimeQueries
                .selectAll(MINE)
                .executeAsList()
                .size,
        )
        assertEquals(
            1,
            database.customConditionQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .size,
        )
        assertEquals(
            1,
            database.combatantMarkQueries
                .selectByCampaign(MINE)
                .executeAsList()
                .size,
        )
        rollback.undo(MINE)
        assertTrue(
            database.downtimeQueries
                .selectAll(MINE)
                .executeAsList()
                .isEmpty(),
        )
    }

    @Test
    fun `the level up hit point numbers travel in a personal copy and stay home from a friend`() {
        database.partyMemberQueries.insert(
            "Lira",
            "Wizard",
            "",
            "elf",
            5L,
            "Ana",
            true,
            PartyPresence.IN,
            28L,
            22L,
            "",
            null,
            null,
            THEIRS,
            "lira",
            "",
            null,
            "",
        )
        val lira = database.partyMemberQueries.lastInsertRowId().executeAsOne()
        database.partyMemberQueries.setHpMaxApprox(true, lira)
        database.partyMemberQueries.setCharacterExtras(2L, 4L, lira)
        val options = CampaignExporter(database).catalog(THEIRS).options

        fun copy(mode: ExportMode) =
            MemoryFile().also {
                val all = RecordSelection(party = options.party.map { p -> p.id }.toSet(), mode = mode)
                CampaignExporter(database).write(THEIRS, all, it)
            }
        importer.import(
            origin(copy(ExportMode.SELF)),
            ImportRequest(ImportTarget.Existing(MINE), ConflictMode.SKIP, takeVitals = true),
        )
        val mine = database.partyMemberQueries.selectAll(MINE).executeAsOne()
        assertTrue(mine.hp_max_approx)
        assertEquals(2L, mine.hp_bonus_per_level)
        assertEquals(4L, mine.last_level_gain)

        rollback.undo(MINE)
        importer.import(origin(copy(ExportMode.FRIEND)), ImportRequest(ImportTarget.Existing(MINE), ConflictMode.SKIP))
        val friend = database.partyMemberQueries.selectAll(MINE).executeAsOne()
        assertEquals(0L, friend.hp_bonus_per_level)
        assertEquals(false, friend.hp_max_approx)
    }

    private companion object {
        const val MINE = TestDatabase.SECOND_CAMPAIGN
        const val THEIRS = TestDatabase.FIRST_CAMPAIGN
    }
}

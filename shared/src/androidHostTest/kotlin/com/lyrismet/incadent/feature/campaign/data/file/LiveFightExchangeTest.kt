package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.MemoryFile
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.ConflictMode
import com.lyrismet.incadent.feature.campaign.domain.model.ExportMode
import com.lyrismet.incadent.feature.campaign.domain.model.ImportRequest
import com.lyrismet.incadent.feature.campaign.domain.model.ImportTarget
import com.lyrismet.incadent.feature.campaign.domain.model.LiveFightStatus
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection
import com.lyrismet.incadent.feature.codex.domain.model.PartyPresence
import com.lyrismet.incadent.feature.combat.domain.model.CombatPhase
import com.lyrismet.incadent.feature.combat.domain.model.CombatSide
import com.lyrismet.incadent.feature.combat.domain.model.CombatStance
import com.lyrismet.incadent.feature.combat.domain.model.CombatState
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LiveFightExchangeTest {
    private val db = TestDatabase().also { it.addCampaign(MINE) }
    private val database: AppDatabase = db.database
    private val importer = CampaignImporter(database) { _, _ -> byteArrayOf(7) }
    private val rollback = ImportRollback(database) { _, _ -> byteArrayOf(7) }
    private val startedAt = LocalDateTime(2026, 11, 6, 19, 0)

    // a hero of the party and a wolf, the fight is in round 3 and it is the wolf's turn
    private fun seedLiveFight(
        campaignId: String = THEIRS,
        fightOrigin: String = "live-fight",
    ) {
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
            campaignId,
            "lira",
        )
        val lira = database.partyMemberQueries.lastInsertRowId().executeAsOne()
        database.combatQueries.insert(null, startedAt, campaignId, fightOrigin)
        val combat = database.combatQueries.lastInsertRowId().executeAsOne()
        database.combatQueries.start(startedAt, combat)
        database.combatQueries.updateTurn(3L, 1L, combat)

        fun fighter(
            name: String,
            side: CombatSide,
            partyId: Long?,
            order: Long,
            reaction: Long,
        ) = database.combatantQueries.insert(
            combat,
            name,
            side,
            null,
            null,
            partyId,
            1L,
            CombatState.WHOLE,
            1L,
            0L,
            0L,
            0L,
            order,
            CombatStance.ENEMY,
            0L,
            0L,
            emptySet(),
            "",
            reaction,
            if (partyId != null) 17L else null,
            if (partyId != null) 28L else null,
            "",
            0L,
        )
        fighter("Lira", CombatSide.PARTY, lira, 1L, 0L)
        fighter("Wolf", CombatSide.ENEMY, null, 0L, 1L)
    }

    private fun export(
        mode: ExportMode,
        liveFight: Boolean = true,
    ): MemoryFile {
        val options = CampaignExporter(database).catalog(THEIRS).options
        val all = RecordSelection(party = options.party.map { it.id }.toSet(), mode = mode, liveFight = liveFight)
        return MemoryFile().also { CampaignExporter(database).write(THEIRS, all, it) }
    }

    private fun import(
        file: MemoryFile,
        target: ImportTarget,
    ) = importer.import(origin(file), ImportRequest(target, ConflictMode.SKIP))

    private fun combatsOf(campaignId: String) = database.combatQueries.selectAll(campaignId).executeAsList()

    @Test
    fun `the catalog tells about the fight under way`() {
        seedLiveFight()

        val live = CampaignExporter(database).catalog(THEIRS).liveFight

        assertEquals(3, live?.round)
        assertEquals(2, live?.fighters)
        assertTrue(live?.ongoing == true)
    }

    @Test
    fun `a friend copy carries the fight under way whole`() {
        seedLiveFight()

        val target = import(export(ExportMode.FRIEND), ImportTarget.NewCampaign("Fallback")).campaignId

        val combat = combatsOf(target).single()
        assertEquals(CombatPhase.ONGOING, combat.phase)
        assertEquals(3L, combat.round)
        assertEquals(1L, combat.turn)
        assertNull(combat.outcome)
        val fighters =
            database.combatantQueries
                .selectByCampaign(target)
                .executeAsList()
                .sortedBy { it.id }
        assertEquals(listOf("Lira", "Wolf"), fighters.map { it.name })
        assertEquals(listOf(1L, 0L), fighters.map { it.turn_order })
        assertEquals(listOf(0L, 1L), fighters.map { it.reaction_available })
        assertEquals(17L, fighters.first().hp_current)
        // the friend gets the party member, the mark of who is "me" stays behind
        val lira =
            database.partyMemberQueries
                .selectAll(target)
                .executeAsList()
                .single()
        assertFalse(lira.is_player_character)
        assertEquals(lira.id, fighters.first().party_member_id)
    }

    @Test
    fun `the fight stays home when its switch is off`() {
        seedLiveFight()

        val target =
            import(
                export(ExportMode.FRIEND, liveFight = false),
                ImportTarget.NewCampaign("Fallback"),
            ).campaignId

        assertTrue(combatsOf(target).isEmpty())
    }

    @Test
    fun `a personal copy carries the fight under way too`() {
        seedLiveFight()

        val target = import(export(ExportMode.SELF), ImportTarget.NewCampaign("Fallback")).campaignId

        assertEquals(CombatPhase.ONGOING, combatsOf(target).single().phase)
    }

    @Test
    fun `a device with a fight under way leaves the incoming one out`() {
        seedLiveFight()
        seedLiveFight(MINE, fightOrigin = "other-fight")
        val file = export(ExportMode.FRIEND)
        val before = combatsOf(MINE).single().id

        val plan = ImportPlanner(database).plan(origin(file), MINE)
        import(file, ImportTarget.Existing(MINE))

        assertEquals(LiveFightStatus.BUSY, plan.liveFight?.status)
        assertEquals(listOf(before), combatsOf(MINE).map { it.id })
    }

    @Test
    fun `the same fight imported twice is written once`() {
        seedLiveFight()
        val file = export(ExportMode.FRIEND)

        import(file, ImportTarget.Existing(MINE))
        val plan = ImportPlanner(database).plan(origin(file), MINE)
        import(file, ImportTarget.Existing(MINE))

        assertEquals(1, combatsOf(MINE).size)
        assertEquals(LiveFightStatus.SAME, plan.liveFight?.status)
    }

    @Test
    fun `undoing the import takes the fight away again`() {
        seedLiveFight()

        import(export(ExportMode.FRIEND), ImportTarget.Existing(MINE))
        assertEquals(1, combatsOf(MINE).size)
        rollback.undo(MINE)

        assertTrue(combatsOf(MINE).isEmpty())
    }

    @Test
    fun `a fight that is over does not count as one under way`() {
        seedLiveFight()
        database.combatQueries.updateOutcome(
            com.lyrismet.incadent.feature.combat.domain.model.CombatOutcome.VICTORY,
            startedAt,
            combatsOf(THEIRS).single().id,
        )

        assertNull(CampaignExporter(database).catalog(THEIRS).liveFight)
    }

    private companion object {
        const val MINE = TestDatabase.SECOND_CAMPAIGN
        const val THEIRS = TestDatabase.FIRST_CAMPAIGN
    }
}

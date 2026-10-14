package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.FakeLocationRepository
import com.lyrismet.incadent.core.entitysummary.FakeNpcRepository
import com.lyrismet.incadent.core.entitysummary.FakePartyRepository
import com.lyrismet.incadent.core.entitysummary.FakeQuestRepository
import com.lyrismet.incadent.core.mention.MentionEntity
import com.lyrismet.incadent.core.mention.MentionIndex
import com.lyrismet.incadent.core.mention.token
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.SessionEntry
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val gromm = Npc(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE, "", emptyList(), "", "")
private val grommMention = MentionEntity.NpcMention(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE)
private val labels = CodexEntryType.entries.associateWith { it.name }

private fun entry(
    id: Long,
    session: Long,
    body: String,
) = SessionEntry(id, session, LocalDateTime(2026, 1, 1, 12, 0), body)

class CodexDeletionPlannerTest {
    private val npcs = FakeNpcRepository(gromm)
    private val planner =
        CodexDeletionPlanner(npcs, FakePartyRepository(), FakeQuestRepository(), FakeLocationRepository())

    private fun records(vararg bodies: Pair<Long, String>) =
        CodexRecords(
            parties = emptyList(),
            npcs = listOf(gromm),
            quests = emptyList(),
            locations = emptyList(),
            entries = bodies.mapIndexed { index, (session, body) -> entry(index + 1L, session, body) },
            mentionIndex = MentionIndex(listOf(grommMention)),
        )

    @Test
    fun `a record the notes mention carries the hit counts`() =
        runTest {
            val token = grommMention.token()
            val plan =
                planner.planFor(
                    EntityRef.Npc(1),
                    records(1L to "$token тут", 1L to "и $token там", 2L to "ничего"),
                    labels,
                )!!

            assertEquals(2, plan.mentionHits.notes)
            assertEquals(1, plan.mentionHits.sessions)
            assertEquals("Громм", plan.name)
        }

    @Test
    fun `a record nothing mentions has no hits`() =
        runTest {
            val plan = planner.planFor(EntityRef.Npc(1), records(1L to "просто текст"), labels)!!

            assertEquals(0, plan.mentionHits.notes)
        }

    @Test
    fun `deleting removes the record`() =
        runTest {
            val plan = planner.planFor(EntityRef.Npc(1), records(), labels)!!

            plan.delete()

            assertNull(npcs.stored(1))
        }

    @Test
    fun `undo brings the record back under its own id so the tokens in notes find it`() =
        runTest {
            val plan = planner.planFor(EntityRef.Npc(1), records(1L to grommMention.token()), labels)!!

            plan.delete()
            plan.restore()

            assertEquals(listOf(1L), npcs.all.map { it.id })
        }
}

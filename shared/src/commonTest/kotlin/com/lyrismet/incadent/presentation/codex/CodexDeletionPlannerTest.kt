package com.lyrismet.incadent.presentation.codex

import com.lyrismet.incadent.core.entitysummary.EntityRef
import com.lyrismet.incadent.core.entitysummary.FakeDeletedMentionRepository
import com.lyrismet.incadent.core.entitysummary.FakeLocationRepository
import com.lyrismet.incadent.core.entitysummary.FakeNpcRepository
import com.lyrismet.incadent.core.entitysummary.FakePartyRepository
import com.lyrismet.incadent.core.entitysummary.FakeQuestRepository
import com.lyrismet.incadent.core.mention.MentionEntity
import com.lyrismet.incadent.core.mention.mentionCandidates
import com.lyrismet.incadent.domain.model.DeletedMention
import com.lyrismet.incadent.domain.model.DeletedMentionType
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.SessionEntry
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val gromm = Npc(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE, "", emptyList(), "", "")
private val labels = CodexEntryType.entries.associateWith { it.name }

private fun entry(
    id: Long,
    session: Long,
    body: String,
) = SessionEntry(id, session, LocalDateTime(2026, 1, 1, 12, 0), body)

class CodexDeletionPlannerTest {
    private val npcs = FakeNpcRepository(gromm)
    private val deleted = FakeDeletedMentionRepository()
    private val planner =
        CodexDeletionPlanner(npcs, FakePartyRepository(), FakeQuestRepository(), FakeLocationRepository(), deleted)

    private fun records(vararg bodies: Pair<Long, String>): CodexRecords {
        val entities = listOf<MentionEntity>(MentionEntity.NpcMention(1, "Громм", NpcStatus.FRIEND, NpcLifeState.ALIVE))
        return CodexRecords(
            parties = emptyList(),
            npcs = listOf(gromm),
            quests = emptyList(),
            locations = emptyList(),
            entries = bodies.mapIndexed { index, (session, body) -> entry(index + 1L, session, body) },
            mentionCandidates = mentionCandidates(entities, "Квест: "),
        )
    }

    @Test
    fun `a record the notes mention carries the hit counts and is remembered when deleted`() =
        runTest {
            val plan =
                planner.planFor(
                    EntityRef.Npc(1),
                    records(1L to "@Громм тут", 1L to "и @Громм там", 2L to "ничего"),
                    labels,
                )!!

            assertEquals(2, plan.mentionHits.notes)
            assertEquals(1, plan.mentionHits.sessions)

            plan.delete()
            assertEquals(DeletedMention("Громм", DeletedMentionType.NPC), deleted.stored["Громм"])
            assertNull(npcs.stored(1))
        }

    @Test
    fun `a record nothing mentions leaves no snapshot behind`() =
        runTest {
            val plan = planner.planFor(EntityRef.Npc(1), records(1L to "просто текст"), labels)!!

            assertEquals(0, plan.mentionHits.notes)
            plan.delete()
            assertTrue(deleted.stored.isEmpty())
        }

    @Test
    fun `undo brings the record back and drops its snapshot`() =
        runTest {
            val plan = planner.planFor(EntityRef.Npc(1), records(1L to "@Громм"), labels)!!

            plan.delete()
            plan.restore()

            assertTrue(deleted.stored.isEmpty())
        }
}

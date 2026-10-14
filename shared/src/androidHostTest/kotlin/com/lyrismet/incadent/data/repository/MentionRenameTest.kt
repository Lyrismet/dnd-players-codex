package com.lyrismet.incadent.data.repository

import com.lyrismet.incadent.data.TestDatabase
import com.lyrismet.incadent.domain.model.Npc
import com.lyrismet.incadent.domain.model.NpcLifeState
import com.lyrismet.incadent.domain.model.NpcStatus
import com.lyrismet.incadent.domain.model.Quest
import com.lyrismet.incadent.domain.model.QuestStatus
import com.lyrismet.incadent.domain.model.SessionNote
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

private val moment = LocalDateTime(2026, 1, 1, 12, 0)

class MentionRenameTest {
    private val db = TestDatabase()
    private val renamer = MentionRenamer(db.database, db.current)
    private val npcs = NpcRepositoryImpl(db.database, renamer, db.current)
    private val quests = QuestRepositoryImpl(db.database, renamer, db.current)
    private val notes = SessionNoteRepositoryImpl(db.database, db.current)
    private val entries = SessionEntryRepositoryImpl(db.database, db.current)

    private fun npc(
        id: Long,
        name: String,
    ) = Npc(id, name, NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), "", "")

    private suspend fun bodies(): List<String> = entries.observeAll().first().map { it.body }

    @Test
    fun `renaming an npc rewrites its mentions in every note and leaves a longer name alone`() =
        runTest {
            val session = notes.upsert(SessionNote(0, "С", moment, null))
            val gromm = npcs.upsert(npc(0, "Громм"))
            npcs.upsert(npc(0, "Громм Железный"))
            entries.add(session, "встретили @Громм у моста")
            entries.add(session, "@Громм Железный кивнул")

            npcs.upsert(npc(gromm, "Грумм"))

            assertEquals(listOf("встретили @Грумм у моста", "@Громм Железный кивнул"), bodies())
        }

    @Test
    fun `renaming a quest rewrites its mentions by title`() =
        runTest {
            val session = notes.upsert(SessionNote(0, "С", moment, null))
            val quest = quests.upsert(Quest(0, "Ключ", QuestStatus.ACTIVE, "", null, null, ""))
            entries.add(session, "ищем @Ключ")

            quests.upsert(Quest(quest, "Печать", QuestStatus.ACTIVE, "", null, null, ""))

            assertEquals(listOf("ищем @Печать"), bodies())
        }
}

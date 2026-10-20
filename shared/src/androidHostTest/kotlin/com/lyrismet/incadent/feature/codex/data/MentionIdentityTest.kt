package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcLifeState
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus
import com.lyrismet.incadent.feature.mention.domain.MentionEntity
import com.lyrismet.incadent.feature.mention.domain.MentionIndex
import com.lyrismet.incadent.feature.mention.domain.MentionSegment
import com.lyrismet.incadent.feature.mention.domain.mentionEntitiesFrom
import com.lyrismet.incadent.feature.mention.domain.parseMentions
import com.lyrismet.incadent.feature.mention.domain.token
import com.lyrismet.incadent.feature.session.data.SessionEntryRepositoryImpl
import com.lyrismet.incadent.feature.session.data.SessionNoteRepositoryImpl
import com.lyrismet.incadent.feature.session.domain.model.SessionNote
import com.lyrismet.incadent.persistence.TestDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

private val moment = LocalDateTime(2026, 1, 1, 12, 0)

class MentionIdentityTest {
    private val db = TestDatabase()
    private val npcs = NpcRepositoryImpl(db.database, db.current)
    private val quests = QuestRepositoryImpl(db.database, db.current)
    private val locations = LocationRepositoryImpl(db.database, db.current)
    private val notes = SessionNoteRepositoryImpl(db.database, db.current)
    private val entries = SessionEntryRepositoryImpl(db.database, db.current)

    private fun npc(
        id: Long,
        name: String,
    ) = Npc(id, name, NpcStatus.NEUTRAL, NpcLifeState.ALIVE, "", emptyList(), "")

    private suspend fun index(): MentionIndex =
        MentionIndex(
            mentionEntitiesFrom(
                npcs.observeAll().first(),
                locations.observeAll().first(),
                quests.observeAll().first(),
            ),
        )

    private suspend fun mentioned(body: String) =
        parseMentions(body, index()).filterIsInstance<MentionSegment.Mention>().map { it.entity }

    @Test
    fun `an npc a quest and a place with one name each resolve to their own record`() =
        runTest {
            val npcId = npcs.upsert(npc(0, "Рагнар"))
            val questId = quests.upsert(Quest(0, "Рагнар", QuestStatus.ACTIVE, "", null, null, ""))
            val placeId = locations.upsert(Location(0, "Рагнар", ""))
            val live = index()
            // every table counts its own ids, so the three records may even share one
            val body =
                listOf<Class<*>>(
                    MentionEntity.QuestMention::class.java,
                    MentionEntity.LocationMention::class.java,
                    MentionEntity.NpcMention::class.java,
                ).joinToString(" ") { kind -> live.entities.first { kind.isInstance(it) }.token() }

            assertEquals(3, live.entities.size)
            assertEquals(listOf(npcId, questId, placeId).size, 3)
            assertEquals(
                listOf<Class<*>>(
                    MentionEntity.QuestMention::class.java,
                    MentionEntity.LocationMention::class.java,
                    MentionEntity.NpcMention::class.java,
                ),
                mentioned(body).map { it::class.java },
            )
        }

    @Test
    fun `renaming a record leaves the note text alone and the tag shows the new name`() =
        runTest {
            val session = notes.upsert(SessionNote(0, "С", moment, null))
            val id = npcs.upsert(npc(0, "Громм"))
            val token = index().entities.single().token()
            entries.add(session, "встретили $token у моста")

            npcs.upsert(npc(id, "Грумм"))

            assertEquals(listOf("встретили $token у моста"), entries.observeAll().first().map { it.body })
            assertEquals(listOf("Грумм"), mentioned(token).map { it.name })
        }

    @Test
    fun `a deleted record put back under its id is found by the tokens already in the notes`() =
        runTest {
            val id = npcs.upsert(npc(0, "Громм"))
            val token = index().entities.single().token()
            npcs.delete(id)
            assertEquals(listOf(MentionEntity.DeletedMention::class.java), mentioned(token).map { it::class.java })

            val restored = npcs.upsert(npc(id, "Громм"))

            assertEquals(id, restored)
            assertEquals(listOf(MentionEntity.NpcMention::class.java), mentioned(token).map { it::class.java })
        }
}

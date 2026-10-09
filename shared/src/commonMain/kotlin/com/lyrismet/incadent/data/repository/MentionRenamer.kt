package com.lyrismet.incadent.data.repository

import com.lyrismet.incadent.core.mention.renameMentionIn
import com.lyrismet.incadent.db.AppDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import org.jetbrains.compose.resources.getString

/** keeps "@mention" text in session entries in step with the codex when an npc, location or quest is renamed */
@Inject
@SingleIn(AppScope::class)
class MentionRenamer(
    private val database: AppDatabase,
) {
    suspend fun npcRenamed(
        id: Long,
        oldName: String,
        newName: String,
    ) = rename(oldName, newName) { keysExcluding(npcId = id) }

    suspend fun locationRenamed(
        id: Long,
        oldName: String,
        newName: String,
    ) = rename(oldName, newName) { keysExcluding(locationId = id) }

    suspend fun questRenamed(
        id: Long,
        oldTitle: String,
        newTitle: String,
    ) {
        val prefix = getString(Res.string.session_detail_quest_mention_prefix)
        rename(prefix + oldTitle, prefix + newTitle) { keysExcluding(questId = id) }
    }

    private suspend fun rename(
        oldKey: String,
        newKey: String,
        otherKeys: suspend () -> List<String>,
    ) {
        if (oldKey == newKey) return
        val others = otherKeys()
        val queries = database.sessionEntryQueries
        queries.transaction {
            queries.selectAll().executeAsList().forEach { entry ->
                val renamed = renameMentionIn(entry.body, oldKey, newKey, others)
                if (renamed != entry.body) queries.renameBody(renamed, entry.id)
            }
        }
    }

    private suspend fun keysExcluding(
        npcId: Long? = null,
        locationId: Long? = null,
        questId: Long? = null,
    ): List<String> {
        val prefix = getString(Res.string.session_detail_quest_mention_prefix)
        return database.npcQueries
            .selectAll()
            .executeAsList()
            .filter { it.id != npcId }
            .map { it.name } +
            database.locationQueries
                .selectAll()
                .executeAsList()
                .filter { it.id != locationId }
                .map { it.name } +
            database.questQueries
                .selectAll()
                .executeAsList()
                .filter { it.id != questId }
                .map { prefix + it.title }
    }
}

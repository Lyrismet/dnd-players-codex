package com.lyrismet.incadent.data.repository

import com.lyrismet.incadent.core.mention.renameMentionIn
import com.lyrismet.incadent.db.AppDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dndplayerscodex.shared.generated.resources.Res
import dndplayerscodex.shared.generated.resources.session_detail_quest_mention_prefix
import org.jetbrains.compose.resources.getString

/**
 * runs a rename of an npc, location or quest and rewrites the "@mention" text in session entries in one transaction.
 * The quest prefix follows the current app language, same as the mention parser.
 */
@Inject
@SingleIn(AppScope::class)
class MentionRenamer(
    private val database: AppDatabase,
) {
    suspend fun npcRenamed(
        id: Long,
        oldName: String,
        newName: String,
        update: () -> Unit,
    ) {
        val prefix = questPrefix()
        rename(oldName, newName, update) { keysExcluding(prefix, npcId = id) }
    }

    suspend fun locationRenamed(
        id: Long,
        oldName: String,
        newName: String,
        update: () -> Unit,
    ) {
        val prefix = questPrefix()
        rename(oldName, newName, update) { keysExcluding(prefix, locationId = id) }
    }

    suspend fun questRenamed(
        id: Long,
        oldTitle: String,
        newTitle: String,
        update: () -> Unit,
    ) {
        val prefix = questPrefix()
        rename(prefix + oldTitle, prefix + newTitle, update) { keysExcluding(prefix, questId = id) }
    }

    private suspend fun questPrefix(): String = getString(Res.string.session_detail_quest_mention_prefix)

    private fun rename(
        oldKey: String,
        newKey: String,
        update: () -> Unit,
        otherKeys: () -> List<String>,
    ) {
        database.transaction {
            update()
            if (oldKey != newKey) rewriteEntries(oldKey, newKey, otherKeys())
        }
    }

    private fun rewriteEntries(
        oldKey: String,
        newKey: String,
        otherKeys: List<String>,
    ) {
        val queries = database.sessionEntryQueries
        queries.selectAll().executeAsList().forEach { entry ->
            val renamed = renameMentionIn(entry.body, oldKey, newKey, otherKeys)
            if (renamed != entry.body) queries.renameBody(renamed, entry.id)
        }
    }

    private fun keysExcluding(
        questPrefix: String,
        npcId: Long? = null,
        locationId: Long? = null,
        questId: Long? = null,
    ): List<String> =
        database.npcQueries
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
                .map { questPrefix + it.title }
}

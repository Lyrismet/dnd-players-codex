package com.lyrismet.incadent.data.repository

import com.lyrismet.incadent.core.mention.renameMentionIn
import com.lyrismet.incadent.data.campaign.CurrentCampaign
import com.lyrismet.incadent.db.AppDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * runs a rename of an npc, location or quest and rewrites the "@mention" text in session entries in one transaction.
 */
@Inject
@SingleIn(AppScope::class)
class MentionRenamer(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) {
    fun npcRenamed(
        id: Long,
        oldName: String,
        newName: String,
        update: () -> Unit,
    ) {
        rename(oldName, newName, update) { keysExcluding(npcId = id) }
    }

    fun partyRenamed(
        id: Long,
        oldName: String,
        newName: String,
        update: () -> Unit,
    ) {
        rename(oldName, newName, update) { keysExcluding(partyId = id) }
    }

    fun locationRenamed(
        id: Long,
        oldName: String,
        newName: String,
        update: () -> Unit,
    ) {
        rename(oldName, newName, update) { keysExcluding(locationId = id) }
    }

    fun questRenamed(
        id: Long,
        oldTitle: String,
        newTitle: String,
        update: () -> Unit,
    ) {
        rename(oldTitle, newTitle, update) { keysExcluding(questId = id) }
    }

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
        queries.selectAll(current.requireId()).executeAsList().forEach { entry ->
            val renamed = renameMentionIn(entry.body, oldKey, newKey, otherKeys)
            if (renamed != entry.body) queries.renameBody(renamed, entry.id)
        }
    }

    private fun keysExcluding(
        npcId: Long? = null,
        locationId: Long? = null,
        questId: Long? = null,
        partyId: Long? = null,
    ): List<String> =
        database.npcQueries
            .selectAll(current.requireId())
            .executeAsList()
            .filter { it.id != npcId }
            .map { it.name } +
            database.partyMemberQueries
                .selectAll(current.requireId())
                .executeAsList()
                .filter { it.id != partyId }
                .map { it.name } +
            database.locationQueries
                .selectAll(current.requireId())
                .executeAsList()
                .filter { it.id != locationId }
                .map { it.name } +
            database.questQueries
                .selectAll(current.requireId())
                .executeAsList()
                .filter { it.id != questId }
                .map { it.title } +
            // a "?" tag keeps its own key, so a rename must not eat the tag of a longer deleted name
            database.deletedMentionQueries
                .selectAll(current.requireId())
                .executeAsList()
                .map { it.name }
}

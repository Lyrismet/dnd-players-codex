package com.lyrismet.incadent.feature.codex.data

import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.codex.domain.grouping.filledFrom
import com.lyrismet.incadent.feature.codex.domain.model.MentionHost
import com.lyrismet.incadent.feature.codex.domain.model.MergedText
import com.lyrismet.incadent.feature.codex.domain.model.NpcMergeTrace
import com.lyrismet.incadent.feature.codex.domain.repository.NpcMergeRepository
import com.lyrismet.incadent.feature.mention.domain.remapMentionTokens
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.withContext

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class NpcMergeRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
    private val npcs: NpcRepositoryImpl,
) : NpcMergeRepository {
    private val queries = database.npcMergeQueries

    override suspend fun merge(
        survivorId: Long,
        loserId: Long,
    ): NpcMergeTrace? {
        val survivor = npcs.getById(survivorId).takeIf { survivorId != loserId }
        val loser = npcs.getById(loserId)
        if (survivor == null || loser == null) return null
        // the text that is taken over still carries the loser's own tokens
        val taken = loser.copy(description = moveMentions(loser.description, survivorId, loserId))
        val moved =
            withContext(DatabaseDispatcher) {
                database.transactionWithResult {
                    val links = moveLinks(survivor.originId, loser.originId, survivorId, loserId)
                    npcs.write(survivor.filledFrom(taken))
                    npcs.remove(loserId)
                    links
                }
            }
        return NpcMergeTrace(survivor, loser, moved.combatants, moved.quests, moved.texts, moved.aliases)
    }

    override suspend fun undo(trace: NpcMergeTrace) {
        npcs.upsert(trace.loser)
        npcs.upsert(trace.survivorBefore)
        withContext(DatabaseDispatcher) {
            database.transaction {
                trace.combatantIds.forEach { queries.setCombatantNpc(trace.loser.id, it) }
                trace.questIds.forEach { queries.setQuestGiver(trace.loser.id, it) }
                trace.texts.forEach { host(it.host).write(it.id, it.before) }
                val campaignId = current.requireId()
                database.originAliasQueries.delete(campaignId, ExchangeKind.NPC, trace.loser.originId)
                trace.repointedAliases.forEach {
                    database.originAliasQueries.insert(campaignId, ExchangeKind.NPC, it, trace.loser.originId)
                }
            }
        }
    }

    private class Moved(
        val combatants: List<Long>,
        val quests: List<Long>,
        val texts: List<MergedText>,
        val aliases: List<String>,
    )

    private fun moveLinks(
        survivorOrigin: String,
        loserOrigin: String,
        survivorId: Long,
        loserId: Long,
    ): Moved {
        val combatants = queries.combatantIdsOfNpc(loserId).executeAsList()
        queries.moveCombatants(survivorId, loserId)
        val quests = queries.questIdsGivenBy(loserId).executeAsList()
        queries.moveQuestGiven(survivorId, loserId)
        val texts =
            MentionHost.entries.flatMap { host ->
                val target = host(host)
                target
                    .find(loserId)
                    .filterNot { host == MentionHost.NPC && it.first == loserId }
                    .map { (id, body) ->
                        target.write(id, moveMentions(body, survivorId, loserId))
                        MergedText(host, id, body)
                    }
            }
        return Moved(combatants, quests, texts, moveAliases(survivorOrigin, loserOrigin))
    }

    private fun moveAliases(
        survivorOrigin: String,
        loserOrigin: String,
    ): List<String> {
        if (loserOrigin.isEmpty() || survivorOrigin.isEmpty()) return emptyList()
        val campaignId = current.requireId()
        val aliases = database.originAliasQueries
        val leading = aliases.selectAliasesOf(campaignId, ExchangeKind.NPC, loserOrigin).executeAsList()
        aliases.repoint(survivorOrigin, campaignId, ExchangeKind.NPC, loserOrigin)
        aliases.insert(campaignId, ExchangeKind.NPC, loserOrigin, survivorOrigin)
        return leading
    }

    private fun moveMentions(
        body: String,
        survivorId: Long,
        loserId: Long,
    ): String =
        remapMentionTokens(body) { ref ->
            if (ref.type == EntityKind.NPC && ref.id == loserId) survivorId else ref.id
        }

    private class Host(
        val find: (Long) -> List<Pair<Long, String>>,
        val write: (Long, String) -> Unit,
    )

    private fun host(kind: MentionHost): Host =
        when (kind) {
            MentionHost.ENTRY ->
                Host(
                    { id -> queries.entriesMentioning(id.toString()).executeAsList().map { it.id to it.body } },
                    { id, text -> queries.setEntryBody(text, id) },
                )
            MentionHost.SPECIES ->
                Host(
                    { id -> queries.speciesMentioning(id.toString()).executeAsList().map { it.id to it.notes } },
                    { id, text -> database.speciesQueries.setNotes(text, id) },
                )
            MentionHost.PLACE ->
                Host(
                    { id -> queries.placesMentioning(id.toString()).executeAsList().map { it.id to it.description } },
                    { id, text -> database.locationQueries.setDescription(text, id) },
                )
            MentionHost.NPC ->
                Host(
                    { id -> queries.npcsMentioning(id.toString()).executeAsList().map { it.id to it.description } },
                    { id, text -> database.npcQueries.setDescription(text, id) },
                )
            MentionHost.QUEST ->
                Host(
                    { id -> queries.questsMentioning(id.toString()).executeAsList().map { it.id to it.description } },
                    { id, text -> database.questQueries.setDescription(text, id) },
                )
            MentionHost.PARTY ->
                Host(
                    { id -> queries.partyMentioning(id.toString()).executeAsList().map { it.id to it.description } },
                    { id, text -> database.partyMemberQueries.setDescription(text, id) },
                )
            MentionHost.ITEM ->
                Host(
                    { id -> queries.itemsMentioning(id.toString()).executeAsList().map { it.id to it.notes } },
                    { id, text -> database.itemQueries.setNotes(text, id) },
                )
        }
}

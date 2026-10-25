package com.lyrismet.incadent.feature.campaign.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.lyrismet.incadent.core.campaign.CurrentCampaign
import com.lyrismet.incadent.core.coroutines.DatabaseDispatcher
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.Campaign
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignPlayer
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignStatus
import com.lyrismet.incadent.feature.campaign.domain.model.CampaignTransfer
import com.lyrismet.incadent.feature.campaign.domain.model.TransferCandidate
import com.lyrismet.incadent.feature.campaign.domain.model.TransferCandidates
import com.lyrismet.incadent.feature.campaign.domain.repository.CampaignRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CampaignRepositoryImpl(
    private val database: AppDatabase,
    private val current: CurrentCampaign,
) : CampaignRepository {
    private val queries = database.campaignQueries
    private val locationQueries = database.locationQueries
    private val npcQueries = database.npcQueries
    private val speciesQueries = database.speciesQueries
    private val npcLocationQueries = database.npcLocationQueries
    private val phraseQueries = database.phraseSnippetQueries

    override fun observeAll(): Flow<List<Campaign>> =
        queries.selectSummaries(::toDomain).asFlow().mapToList(DatabaseDispatcher)

    override fun observeCurrent(): Flow<Campaign?> =
        combine(observeAll(), current.id) { all, id -> all.firstOrNull { it.id == id } }

    override suspend fun open(id: String) {
        withContext(DatabaseDispatcher) {
            queries.touch(now(), id)
            current.open(id)
        }
    }

    override suspend fun create(
        name: String,
        transfer: CampaignTransfer?,
        starterPhrases: List<String>,
    ): Int =
        withContext(DatabaseDispatcher) {
            val id = Uuid.random().toString()
            val copied =
                database.transactionWithResult {
                    queries.insertLive(name.trim(), id)
                    starterPhrases.forEach { phraseQueries.insert(it, id) }
                    if (transfer == null) 0 else copyWorld(transfer, id)
                }
            current.open(id)
            copied
        }

    override suspend fun rename(
        id: String,
        name: String,
    ) {
        withContext(DatabaseDispatcher) { queries.rename(name.trim(), id) }
    }

    override suspend fun archive(id: String) {
        withContext(DatabaseDispatcher) {
            queries.setStatus(CampaignStatus.ARCHIVED, id)
            if (current.id.value == id) openFirstLiveOrClose()
        }
    }

    override suspend fun resume(id: String) {
        withContext(DatabaseDispatcher) { queries.setStatus(CampaignStatus.LIVE, id) }
    }

    override suspend fun deleteWithData(id: String) {
        withContext(DatabaseDispatcher) {
            queries.deleteById(id)
            if (current.id.value == id) openFirstLiveOrClose()
        }
    }

    // the last live campaign leaves the app on the library instead of an archive
    private fun openFirstLiveOrClose() {
        queries.selectFirstLiveId().executeAsOneOrNull()?.let { current.open(it) } ?: current.close()
    }

    override fun observeTransferCandidates(sourceId: String): Flow<TransferCandidates> =
        combine(
            locationQueries.selectAll(sourceId).asFlow().mapToList(DatabaseDispatcher),
            npcQueries.selectAllWithLocations(sourceId).asFlow().mapToList(DatabaseDispatcher),
        ) { places, npcs ->
            val byId = places.associateBy { it.id }

            fun path(parentId: Long?): String =
                generateSequence(parentId?.let(byId::get)) { it.parent_id?.let(byId::get) }
                    .toList()
                    .reversed()
                    .joinToString(" › ") { it.name }
            TransferCandidates(
                places = places.map { TransferCandidate(it.id, it.name, path(it.parent_id)) },
                npcs =
                    npcs.map { npc ->
                        val where =
                            npc.location_ids
                                .split(',')
                                .mapNotNull { it.toLongOrNull()?.let(byId::get)?.name }
                                .joinToString(", ")
                        TransferCandidate(npc.id, npc.name, where)
                    },
            )
        }

    // copies are independent rows, notes, quests and the party stay behind
    private fun copyWorld(
        transfer: CampaignTransfer,
        targetId: String,
    ): Int {
        val source = transfer.sourceId
        val parents =
            locationQueries
                .selectAll(source) { id, _, _, parentId, _ -> id to parentId }
                .executeAsList()
                .toMap()
        val npcPlaces =
            transfer.npcIds.associateWith { npcLocationQueries.selectByNpc(it).executeAsList() }

        // a place brings its parents along so the copied tree keeps its shape
        val wanted = linkedSetOf<Long>()
        (transfer.placeIds + npcPlaces.values.flatten()).forEach { id ->
            var next: Long? = id
            while (next != null && next in parents && wanted.add(next)) next = parents[next]
        }
        val placeMap = mutableMapOf<Long, Long>()
        // parents first, so a child always finds its copied parent
        wanted.sortedBy { depth(it, parents) }.forEach { oldId ->
            val full = locationQueries.selectById(oldId).executeAsOne()
            locationQueries.insert(
                full.name,
                full.description,
                full.parent_id?.let(placeMap::get),
                full.cover,
                full.cover_thumb,
                targetId,
            )
            placeMap[oldId] = locationQueries.lastInsertRowId().executeAsOne()
        }
        // one copy per species, shared by every copied npc that had it
        val speciesMap = mutableMapOf<Long, Long>()
        var copiedNpcs = 0
        transfer.npcIds.forEach { oldId ->
            val npc = npcQueries.selectById(oldId).executeAsOneOrNull() ?: return@forEach
            npcQueries.insert(
                npc.name,
                npc.status,
                npc.description,
                npc.race,
                npc.life_state,
                npc.nicknames,
                npc.species_id?.let { copySpecies(it, targetId, speciesMap) },
                npc.vulnerabilities,
                npc.portrait,
                npc.portrait_thumb,
                targetId,
            )
            val newId = npcQueries.lastInsertRowId().executeAsOne()
            npcPlaces[oldId].orEmpty().mapNotNull(placeMap::get).forEach { npcLocationQueries.insert(newId, it) }
            copiedNpcs++
        }
        // the whole bestiary comes along, not only the species a copied npc points at
        speciesQueries.selectAll(source) { id, _, _, _, _, _, _ -> id }.executeAsList().forEach {
            copySpecies(it, targetId, speciesMap)
        }
        if (transfer.copyTags) queries.copyTags(targetId, source)
        if (transfer.copyPhrases) queries.copyPhrases(targetId, source)
        return placeMap.size + copiedNpcs
    }

    private fun copySpecies(
        oldId: Long,
        targetId: String,
        copied: MutableMap<Long, Long>,
    ): Long? =
        copied[oldId] ?: speciesQueries.selectById(oldId).executeAsOneOrNull()?.let { full ->
            speciesQueries.insert(
                full.name,
                full.creature_type,
                full.vulnerabilities,
                full.notes,
                full.hit_points,
                full.photo,
                full.photo_thumb,
                targetId,
            )
            speciesQueries.lastInsertRowId().executeAsOne().also { copied[oldId] = it }
        }

    private fun depth(
        id: Long,
        parents: Map<Long, Long?>,
    ): Int = generateSequence(parents[id]) { parents[it] }.count()

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    // one param per summary column - dictated by SQLDelight's generated query mapper shape
    @Suppress("LongParameterList")
    private fun toDomain(
        id: String,
        name: String,
        status: CampaignStatus,
        sessionCount: Long,
        lastSessionDate: LocalDateTime?,
        playerName: String?,
        playerClass: String?,
        playerLevel: Long?,
    ) = Campaign(
        id = id,
        name = name,
        status = status,
        sessionCount = sessionCount.toInt(),
        lastSessionDate = lastSessionDate,
        player = playerName?.let { CampaignPlayer(it, playerClass.orEmpty(), playerLevel?.toInt()) },
    )
}

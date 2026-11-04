package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.Session_entry
import com.lyrismet.incadent.feature.campaign.domain.exchange.LocalRecord
import com.lyrismet.incadent.feature.campaign.domain.exchange.RecordMatch
import com.lyrismet.incadent.feature.campaign.domain.exchange.matchRecord
import com.lyrismet.incadent.feature.campaign.domain.exchange.sessionKey
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import kotlinx.datetime.LocalDate
import com.lyrismet.incadent.db.sessionNote.SelectAll as SessionRow

/** everything an import has to know about the campaign it goes into, read once */
internal class CampaignLocals(
    database: AppDatabase,
    val campaignId: String,
) {
    val species = database.speciesQueries.selectAll(campaignId).executeAsList()
    val places = database.locationQueries.selectAll(campaignId).executeAsList()
    val npcs = database.npcQueries.selectAllWithLocations(campaignId).executeAsList()
    val quests = database.questQueries.selectAll(campaignId).executeAsList()
    val party = database.partyMemberQueries.selectAll(campaignId).executeAsList()
    val items = database.itemQueries.selectAll(campaignId).executeAsList()
    val sessions = database.sessionNoteQueries.selectAll(campaignId).executeAsList()
    val entries: Map<Long, List<Session_entry>> =
        database.sessionEntryQueries
            .selectAll(campaignId)
            .executeAsList()
            .groupBy { it.session_note_id }

    private val aliases: Map<ExchangeKind, Map<String, String>> =
        database.originAliasQueries
            .selectByCampaign(campaignId)
            .executeAsList()
            .groupBy({ it.kind }, { it.alias_origin_id to it.origin_id })
            .mapValues { it.value.toMap() }

    private val apart: Map<ExchangeKind, Set<Pair<String, String>>> =
        database.keptApartQueries
            .selectByCampaign(campaignId)
            .executeAsList()
            .groupBy({ it.kind }, { it.file_origin_id to it.local_origin_id })
            .mapValues { it.value.toSet() }

    private val deleted: Map<ExchangeKind, Set<String>> =
        database.deletedOriginQueries
            .selectByCampaign(campaignId)
            .executeAsList()
            .groupBy({ it.kind }, { it.origin_id })
            .mapValues { it.value.toSet() }

    private val records: Map<ExchangeKind, List<LocalRecord>> =
        mapOf(
            ExchangeKind.SPECIES to species.map { LocalRecord(it.id, it.name, it.origin_id) },
            ExchangeKind.PLACE to places.map { LocalRecord(it.id, it.name, it.origin_id) },
            ExchangeKind.NPC to npcs.map { LocalRecord(it.id, it.name, it.origin_id) },
            ExchangeKind.QUEST to quests.map { LocalRecord(it.id, it.title, it.origin_id) },
            ExchangeKind.PARTY to party.map { LocalRecord(it.id, it.name, it.origin_id) },
            ExchangeKind.ITEM to items.map { LocalRecord(it.id, it.name, it.origin_id) },
            ExchangeKind.SESSION to sessions.map { LocalRecord(it.id, it.title, it.origin_id) },
        )

    fun originOf(
        kind: ExchangeKind,
        id: Long,
    ): String? = records[kind]?.firstOrNull { it.id == id }?.originId

    fun match(
        kind: ExchangeKind,
        fileOriginId: String?,
        name: String,
    ): RecordMatch =
        matchRecord(
            fileOriginId,
            name,
            records.getValue(kind),
            apart[kind].orEmpty(),
            // stacks of the same name are common, so an item is only ever the one with its origin id
            byName = kind != ExchangeKind.ITEM,
        ) { aliases[kind]?.get(it) ?: it }

    /** the origin id a file's id stands for here, an id merged away leads to its survivor */
    fun resolveOrigin(
        kind: ExchangeKind,
        fileOriginId: String,
    ): String = aliases[kind]?.get(fileOriginId) ?: fileOriginId

    /** the session of the campaign a session of a file is: the same origin id, or the same day and title */
    fun sessionTwin(
        fileOriginId: String?,
        title: String,
        day: LocalDate,
    ): SessionRow? {
        val wanted = fileOriginId?.takeIf { it.isNotBlank() }?.let { resolveOrigin(ExchangeKind.SESSION, it) }
        return sessions.firstOrNull { wanted != null && it.origin_id == wanted }
            ?: sessions.firstOrNull { sessionKey(it.session_date.date, it.title) == sessionKey(day, title) }
    }

    fun isDeleted(
        kind: ExchangeKind,
        fileOriginId: String?,
    ): Boolean = !fileOriginId.isNullOrBlank() && resolveOrigin(kind, fileOriginId) in deleted[kind].orEmpty()
}

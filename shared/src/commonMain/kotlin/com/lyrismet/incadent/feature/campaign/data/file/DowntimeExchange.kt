package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

// the shape of the items column of a downtime row, the same json the session data layer writes
@Serializable
private class StoredLoot(
    val name: String,
    val quantity: Int = 1,
    val itemId: Long = 0,
)

private val StoredLootList = ListSerializer(StoredLoot.serializer())

/** the downtimes of the sessions in [scope], the links to a sheet only when [personal] and the owner came along */
internal fun readDowntimes(
    database: AppDatabase,
    campaignId: String,
    scope: RecordScope,
    ids: FileIds,
    personal: Boolean,
): List<FileRecord.Downtime> =
    database.downtimeQueries
        .selectAll(campaignId)
        .executeAsList()
        .filter { it.after_session_id in scope.sessions }
        .sortedBy { it.after_session_id }
        .map { row ->
            val member = row.member_id.takeIf { personal && row.applied != 0L && it in scope.party }
            val stored =
                runCatching { FileJson.decodeFromString(StoredLootList, row.items.ifEmpty { "[]" }) }
                    .getOrDefault(emptyList())
            FileRecord.Downtime(
                sessionId = ids.session(row.after_session_id),
                shortText = row.short_text,
                longText = row.long_text,
                gainedText = row.gained_text,
                goldDelta = row.gold_delta.toInt(),
                loot =
                    stored.map {
                        val item = it.itemId.takeIf { id -> member != null && id in scope.items }
                        FileRecord.Downtime.Loot(it.name, it.quantity, item?.let(ids::item))
                    },
                appliedPartyId = member?.let(ids::party),
                appliedGold = if (member != null) row.applied_gold.toInt() else 0,
                originId = row.origin_id,
            )
        }

/** writes the downtimes of a file, one per session, a session that already has one keeps its own */
@Suppress("LongParameterList")
internal class DowntimeImport(
    private val database: AppDatabase,
    private val campaignId: String,
    private val sessionMap: Map<Long, Long>,
    private val partyMap: Map<Long, Long>,
    private val itemMap: Map<Long, Long>,
    private val sheetTaken: Set<Long>,
    private val journal: ImportJournal,
) {
    private val held = database.downtimeQueries.selectAll(campaignId).executeAsList()
    private val sessionsWith = held.map { it.after_session_id }.toMutableSet()
    private val origins = OriginSet(held.map { it.origin_id })

    fun add(record: FileRecord.Downtime) {
        val sessionId = sessionMap[record.sessionId] ?: return
        if (!sessionsWith.add(sessionId)) return
        // the gold and things are in a sheet only when the import brought that sheet
        val member = record.appliedPartyId?.let(partyMap::get)?.takeIf { it in sheetTaken }
        val loot =
            record.loot.map {
                StoredLoot(it.name, it.quantity, if (member != null) it.itemId?.let(itemMap::get) ?: 0L else 0L)
            }
        database.downtimeQueries.insert(
            campaignId,
            sessionId,
            record.shortText,
            record.longText,
            record.gainedText,
            record.goldDelta.toLong(),
            if (loot.isEmpty()) "" else FileJson.encodeToString(StoredLootList, loot),
            if (member != null) 1L else 0L,
            member ?: 0L,
            if (member != null) record.appliedGold.toLong() else 0L,
            origins.claim(record.originId),
        )
        journal.downtime(database.downtimeQueries.lastInsertRowId().executeAsOne())
    }
}

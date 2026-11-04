package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.portrait.PhotoSlot
import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.db.importJournal.SelectByCampaign
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.persistence.creatureTypeIdIn
import kotlin.io.encoding.Base64

/** puts a campaign back as it was before its last import, every row the journal names is added or restored */
internal class ImportRollback(
    private val database: AppDatabase,
    private val makeThumb: (ByteArray, PhotoSlot) -> ByteArray,
) {
    fun hasJournal(campaignId: String): Boolean = database.importJournalQueries.hasAny(campaignId).executeAsOne() > 0

    fun undo(campaignId: String) {
        database.transaction {
            val queries = database.importJournalQueries
            queries.selectByCampaign(campaignId).executeAsList().forEach { apply(campaignId, it) }
            queries.clear(campaignId)
        }
    }

    private fun apply(
        campaignId: String,
        row: SelectByCampaign,
    ) {
        when (row.action) {
            JournalAction.ADDED -> removeAdded(campaignId, row)
            JournalAction.ENTRY -> database.sessionEntryQueries.deleteById(row.row_id)
            JournalAction.COMBAT -> database.combatQueries.deleteById(row.row_id, campaignId)
            JournalAction.ALIAS ->
                database.originAliasQueries.delete(campaignId, ExchangeKind.valueOf(row.kind), row.ref)
            JournalAction.LABEL -> restoreLabel(campaignId, row.row_id)
            JournalAction.PHOTO -> restorePhoto(row)
            JournalAction.CHANGED ->
                restoreRecord(
                    campaignId,
                    FileJson.decodeFromString(FileRecord.serializer(), row.snapshot),
                )
        }
    }

    // deleting the row writes it to the list of what the person deleted, which an import must not leave behind
    private fun removeAdded(
        campaignId: String,
        row: SelectByCampaign,
    ) {
        val kind = ExchangeKind.valueOf(row.kind)
        when (kind) {
            ExchangeKind.SPECIES -> database.speciesQueries.deleteById(row.row_id)
            ExchangeKind.PLACE -> database.locationQueries.deleteById(row.row_id)
            ExchangeKind.NPC -> database.npcQueries.deleteById(row.row_id)
            ExchangeKind.QUEST -> database.questQueries.deleteById(row.row_id)
            ExchangeKind.PARTY -> database.partyMemberQueries.deleteById(row.row_id)
            ExchangeKind.ITEM -> database.itemQueries.deleteById(row.row_id)
            ExchangeKind.SESSION -> database.sessionNoteQueries.deleteById(row.row_id)
        }
        if (row.flag == 0L) database.deletedOriginQueries.forget(campaignId, kind, row.ref)
    }

    private fun restoreLabel(
        campaignId: String,
        previousId: Long,
    ) {
        val queries = database.partyMemberQueries
        val row = queries.selectById(previousId).executeAsOneOrNull() ?: return
        queries.clearOtherPlayerCharacters(previousId, campaignId)
        queries.update(
            row.name,
            row.character_class,
            row.race,
            row.level,
            row.player_name,
            true,
            row.presence,
            row.hp_max,
            row.hp_current,
            row.description,
            previousId,
        )
    }

    private fun restorePhoto(row: SelectByCampaign) {
        val bytes = if (row.flag == 1L) Base64.decode(row.snapshot) else null
        val id = row.row_id
        when (PhotoOwner.valueOf(row.kind)) {
            PhotoOwner.SPECIES ->
                database.speciesQueries.setPhoto(
                    bytes,
                    bytes?.let { makeThumb(it, PhotoSlot.PORTRAIT) },
                    id,
                )
            PhotoOwner.PLACE ->
                database.locationQueries.setCover(
                    bytes,
                    bytes?.let { makeThumb(it, PhotoSlot.COVER) },
                    id,
                )
            PhotoOwner.NPC ->
                database.npcQueries.setPortrait(
                    bytes,
                    bytes?.let { makeThumb(it, PhotoSlot.PORTRAIT) },
                    id,
                )
            PhotoOwner.PARTY ->
                database.partyMemberQueries.setPortrait(
                    bytes,
                    bytes?.let { makeThumb(it, PhotoSlot.PORTRAIT) },
                    id,
                )
            PhotoOwner.ITEM ->
                database.itemQueries.setPhoto(
                    bytes,
                    bytes?.let { makeThumb(it, PhotoSlot.PORTRAIT) },
                    id,
                )
        }
    }

    // the snapshot holds the ids this database had, so nothing is mapped
    @Suppress("LongMethod")
    private fun restoreRecord(
        campaignId: String,
        record: FileRecord,
    ) {
        when (record) {
            is FileRecord.Species ->
                database.speciesQueries.updateImported(
                    record.name,
                    record.creatureType,
                    database.creatureTypeIdIn(campaignId, record.customType),
                    record.vulnerabilities,
                    record.notes,
                    record.hitPoints?.toLong(),
                    record.id,
                )
            is FileRecord.Place ->
                database.locationQueries.update(
                    record.name,
                    record.description,
                    record.parentId,
                    record.id,
                )
            is FileRecord.Npc -> {
                database.npcQueries.update(
                    record.name,
                    record.status,
                    record.description,
                    record.race,
                    record.lifeState,
                    record.nicknames,
                    record.speciesId,
                    record.vulnerabilities,
                    record.id,
                )
                database.npcLocationQueries.deleteByNpc(record.id)
                record.placeIds.forEach { database.npcLocationQueries.insert(record.id, it) }
            }
            is FileRecord.Quest ->
                database.questQueries.update(
                    record.title,
                    record.status,
                    record.reward,
                    record.giverNpcId,
                    record.placeId,
                    record.description,
                    record.id,
                )
            is FileRecord.Party ->
                database.partyMemberQueries.update(
                    record.name,
                    record.characterClass,
                    record.race,
                    record.level?.toLong(),
                    record.playerName,
                    record.isPlayerCharacter,
                    record.presence,
                    record.hpMax?.toLong(),
                    record.hpCurrent?.toLong(),
                    record.description,
                    record.id,
                )
            is FileRecord.Item ->
                database.itemQueries.update(
                    record.name,
                    record.quantity.toLong(),
                    record.price?.toLong(),
                    record.coin,
                    record.ownerKind?.toOwnerKind(),
                    record.ownerId ?: 0L,
                    record.notes,
                    record.id,
                )
            else -> Unit
        }
    }
}

// the kinds of record that can carry an item, in the kinds the item table keeps
private fun ExchangeKind.toOwnerKind(): EntityKind? =
    when (this) {
        ExchangeKind.NPC -> EntityKind.NPC
        ExchangeKind.PLACE -> EntityKind.LOCATION
        ExchangeKind.PARTY -> EntityKind.PARTY
        else -> null
    }

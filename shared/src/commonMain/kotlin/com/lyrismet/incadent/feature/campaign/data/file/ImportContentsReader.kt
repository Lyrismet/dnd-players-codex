package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.core.file.FileOrigin
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.exchange.withKnownMentionsOnly
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.ImportContents
import com.lyrismet.incadent.feature.mention.domain.mentionRefsIn

/** reads a whole file once and lists what it holds, nothing is written anywhere */
internal fun readImportContents(origin: FileOrigin): ImportContents {
    val source = origin.open()
    try {
        val reader = CampaignFileReader.open(source)
        val collector = ContentsCollector()
        while (true) collector.add(reader.nextRecord(skipPhotos = true) ?: break)
        val rows = collector.rows()
        val options = rows.options()
        return ImportContents(
            fileName = origin.name,
            campaignName = reader.header.name,
            bytes = source.sizeOrNull ?: 0L,
            withPhotos = reader.header.withPhotos,
            options = options,
            graph = rows.graph().withKnownMentionsOnly(options),
        )
    } finally {
        source.close()
    }
}

/** keeps what the picker lists and the links between records while a file is read, the notes' text itself is dropped */
private class ContentsCollector {
    private val species = mutableListOf<FileRecord.Species>()
    private val places = mutableListOf<FileRecord.Place>()
    private val npcs = mutableListOf<FileRecord.Npc>()
    private val quests = mutableListOf<FileRecord.Quest>()
    private val party = mutableListOf<FileRecord.Party>()
    private val sessions = mutableListOf<FileRecord.Session>()
    private val mentions = mutableMapOf<Long, MutableSet<ExchangeRef>>()

    fun add(record: FileRecord) {
        when (record) {
            is FileRecord.Species -> species += record
            is FileRecord.Place -> places += record
            is FileRecord.Npc -> npcs += record
            is FileRecord.Quest -> quests += record
            is FileRecord.Party -> party += record
            is FileRecord.Session -> sessions += record
            is FileRecord.Tag, is FileRecord.Phrase -> Unit
            is FileRecord.Entry -> addMentions(record)
            is FightRecord -> Unit
            is FileRecord.Photo -> Unit
        }
    }

    private fun addMentions(entry: FileRecord.Entry) {
        val found = mentions.getOrPut(entry.sessionId) { mutableSetOf() }
        mentionRefsIn(entry.body).forEach { found.add(ExchangeRef(it.type.toExchangeKind(), it.id)) }
    }

    fun rows() =
        ExchangeRows(
            sessions = sessions.map { SessionRow(it.id, it.title, it.date.date.toString()) },
            places = places.map { PlaceRow(it.id, it.name, it.parentId) },
            npcs = npcs.map { NpcRow(it.id, it.name, it.speciesId, it.placeIds) },
            quests = quests.map { QuestRow(it.id, it.title, it.giverNpcId, it.placeId) },
            species = species.map { ExchangeItem(it.id, it.name) },
            party = party.map { ExchangeItem(it.id, it.name) },
            mentions = mentions,
        )
}

internal fun EntityKind.toExchangeKind(): ExchangeKind =
    when (this) {
        EntityKind.NPC -> ExchangeKind.NPC
        EntityKind.PARTY -> ExchangeKind.PARTY
        EntityKind.LOCATION -> ExchangeKind.PLACE
        EntityKind.QUEST -> ExchangeKind.QUEST
        EntityKind.SPECIES -> ExchangeKind.SPECIES
    }

package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.db.AppDatabase
import com.lyrismet.incadent.domain.model.EntityKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.mention.domain.remapMentionTokens

/** a text of the file carries mentions by the file's numbers, they move to the new ids once every record is in */
internal class ImportMentions(
    private val database: AppDatabase,
    private val idsByKind: Map<EntityKind, Map<Long, Long>>,
) {
    private class Pending(
        val kind: ExchangeKind,
        val id: Long,
        val raw: String,
    )

    private val pending = mutableListOf<Pending>()

    fun later(
        kind: ExchangeKind,
        id: Long,
        raw: String,
    ) {
        if (raw.contains("@[")) pending += Pending(kind, id, raw)
    }

    // a text that now holds the file's words is one whose mentions are still the file's
    fun took(
        kind: ExchangeKind,
        id: Long,
        before: String,
        after: String,
        raw: String,
    ) {
        if (after != before) later(kind, id, raw)
    }

    fun finish() = pending.forEach(::point)

    // only the part that came from the file is moved, what was there before keeps its own ids
    private fun point(text: Pending) {
        val fixed = remapMentionTokens(text.raw) { idsByKind[it.type]?.get(it.id) }
        val current = read(text)
        val at = current.lastIndexOf(text.raw)
        if (fixed != text.raw && at >= 0) write(text, current.replaceRange(at, at + text.raw.length, fixed))
    }

    private fun read(text: Pending): String =
        when (text.kind) {
            ExchangeKind.SPECIES -> database.speciesQueries.selectNotes(text.id).executeAsOne()
            ExchangeKind.PLACE -> database.locationQueries.selectDescription(text.id).executeAsOne()
            ExchangeKind.NPC -> database.npcQueries.selectDescription(text.id).executeAsOne()
            ExchangeKind.QUEST -> database.questQueries.selectDescription(text.id).executeAsOne()
            ExchangeKind.PARTY -> database.partyMemberQueries.selectDescription(text.id).executeAsOne()
            ExchangeKind.ITEM -> database.itemQueries.selectNotes(text.id).executeAsOne()
            ExchangeKind.SESSION -> ""
        }

    private fun write(
        text: Pending,
        value: String,
    ) {
        when (text.kind) {
            ExchangeKind.SPECIES -> database.speciesQueries.setNotes(value, text.id)
            ExchangeKind.PLACE -> database.locationQueries.setDescription(value, text.id)
            ExchangeKind.NPC -> database.npcQueries.setDescription(value, text.id)
            ExchangeKind.QUEST -> database.questQueries.setDescription(value, text.id)
            ExchangeKind.PARTY -> database.partyMemberQueries.setDescription(value, text.id)
            ExchangeKind.ITEM -> database.itemQueries.setNotes(value, text.id)
            ExchangeKind.SESSION -> Unit
        }
    }
}

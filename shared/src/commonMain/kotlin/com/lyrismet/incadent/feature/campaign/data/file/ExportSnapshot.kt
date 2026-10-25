package com.lyrismet.incadent.feature.campaign.data.file

import com.lyrismet.incadent.db.SelectAllWithLocations
import com.lyrismet.incadent.db.Session_entry
import com.lyrismet.incadent.feature.campaign.domain.exchange.withKnownMentionsOnly
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeCatalog
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.mention.domain.MentionRef
import com.lyrismet.incadent.feature.mention.domain.mentionRefsIn
import com.lyrismet.incadent.db.location.SelectAll as PlaceSql
import com.lyrismet.incadent.db.partyMember.SelectAll as PartySql
import com.lyrismet.incadent.db.quest.SelectAll as QuestSql
import com.lyrismet.incadent.db.sessionNote.SelectAll as SessionSql
import com.lyrismet.incadent.db.species.SelectAll as SpeciesSql

internal fun String.toIdList(): List<Long> = split(',').mapNotNull { it.toLongOrNull() }

private fun MentionRef.toExchangeRef(): ExchangeRef = ExchangeRef(type.toExchangeKind(), id)

/** the rows of one campaign as they were read, [catalog] is what the picker offers from them */
@Suppress("LongParameterList")
internal class Snapshot(
    val campaignId: String,
    val name: String,
    val places: List<PlaceSql>,
    val npcs: List<SelectAllWithLocations>,
    val quests: List<QuestSql>,
    val sessions: List<SessionSql>,
    val species: List<SpeciesSql>,
    val party: List<PartySql>,
    val entries: Map<Long, List<Session_entry>>,
    val tagNames: Map<Long, List<String>>,
) {
    val catalog: ExchangeCatalog by lazy {
        val rows =
            ExchangeRows(
                sessions = sessions.map { SessionRow(it.id, it.title, it.session_date.date.toString()) },
                places = places.map { PlaceRow(it.id, it.name, it.parent_id) },
                npcs = npcs.map { NpcRow(it.id, it.name, it.species_id, it.location_ids.toIdList()) },
                quests = quests.map { QuestRow(it.id, it.title, it.given_by_npc_id, it.location_id) },
                species = species.map { ExchangeItem(it.id, it.name) },
                party = party.map { ExchangeItem(it.id, it.name) },
                mentions =
                    entries.mapValues { (_, list) ->
                        list.flatMap { mentionRefsIn(it.body) }.map { it.toExchangeRef() }.toSet()
                    },
            )
        val options = rows.options()
        ExchangeCatalog(name, options, rows.graph().withKnownMentionsOnly(options))
    }
}

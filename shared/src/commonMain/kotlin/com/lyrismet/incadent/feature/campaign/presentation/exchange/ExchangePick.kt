package com.lyrismet.incadent.feature.campaign.presentation.exchange

import com.lyrismet.incadent.core.format.foldForSearch
import com.lyrismet.incadent.feature.campaign.domain.exchange.nameKey
import com.lyrismet.incadent.feature.campaign.domain.exchange.resolve
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeGraph
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeItem
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeKind
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeOptions
import com.lyrismet.incadent.feature.campaign.domain.model.ExchangeRef
import com.lyrismet.incadent.feature.campaign.domain.model.RecordScope
import com.lyrismet.incadent.feature.campaign.domain.model.RecordSelection

enum class ExchangeTab { PLACES, NPCS, QUESTS, SPECIES, SESSIONS }

/** [Pulled] is a record nobody ticked that comes along because a ticked one needs it */
enum class PickMark { Off, On, Pulled }

enum class PullKind { NPC_PLACE, NPC_SPECIES, QUEST, SESSION, PLACE_PARENT }

/** why a record comes along: what needs it and the name of that record */
data class PullReason(
    val kind: PullKind,
    val source: String,
)

data class PickRow(
    val id: Long,
    val name: String,
    val subtitle: String,
    val mark: PickMark,
    val tab: ExchangeTab,
    val reason: PullReason? = null,
)

/** how many records of a tab are in the file, ticked or pulled in, out of all the tab has */
data class PickTabCount(
    val tab: ExchangeTab,
    val inFile: Int,
    val total: Int,
)

/** the "what to take" pick of an export or an import - what is on offer, what is ticked and which tab is looked at */
data class ExchangePick(
    val options: ExchangeOptions,
    val graph: ExchangeGraph,
    val selection: RecordSelection,
    val tab: ExchangeTab = ExchangeTab.PLACES,
    val query: String = "",
) {
    val scope: RecordScope = selection.resolve(graph)

    val recordCount: Int =
        scope.places.size + scope.npcs.size + scope.quests.size + scope.species.size + scope.party.size
    val sessionCount: Int = scope.sessions.size

    fun tabCounts(): List<PickTabCount> =
        ExchangeTab.entries.map { PickTabCount(it, scope.idsOf(it).size, options.itemsOf(it).size) }

    fun rows(): List<PickRow> {
        val explicit = selection.idsOf(tab)
        val all = scope.idsOf(tab)
        val needle = query.nameKey()
        return options
            .itemsOf(tab)
            .filter { needle.isEmpty() || it.name.foldForSearch().contains(needle) }
            .map {
                val mark = markOf(it.id, explicit, all)
                PickRow(it.id, it.name, it.subtitle, mark, tab, if (mark == PickMark.Pulled) reasonFor(it.id) else null)
            }
    }

    // the first record in the file that needs [id], looked up in the order the resolver pulls them in
    private fun reasonFor(id: Long): PullReason? =
        when (tab) {
            ExchangeTab.SPECIES -> speciesReason(id)
            ExchangeTab.PLACES -> placeReason(id)
            ExchangeTab.NPCS -> npcReason(id)
            ExchangeTab.QUESTS -> sessionReason(ExchangeKind.QUEST, id)
            ExchangeTab.SESSIONS -> null
        }

    private fun speciesReason(id: Long): PullReason? =
        namedIn(options.npcs, scope.npcs) { graph.npcSpecies[it] == id }?.let { PullReason(PullKind.NPC_SPECIES, it) }

    private fun placeReason(id: Long): PullReason? =
        namedIn(
            options.npcs,
            scope.npcs,
        ) { id in graph.npcPlaces[it].orEmpty() }?.let { PullReason(PullKind.NPC_PLACE, it) }
            ?: namedIn(
                options.quests,
                scope.quests,
            ) { graph.questPlace[it] == id }?.let { PullReason(PullKind.QUEST, it) }
            ?: sessionReason(ExchangeKind.PLACE, id)
            ?: namedIn(options.places, scope.places) { graph.placeParent[it] == id }
                ?.let { PullReason(PullKind.PLACE_PARENT, it) }

    private fun npcReason(id: Long): PullReason? =
        namedIn(options.quests, scope.quests) { graph.questGiver[it] == id }?.let { PullReason(PullKind.QUEST, it) }
            ?: sessionReason(ExchangeKind.NPC, id)

    private fun sessionReason(
        kind: ExchangeKind,
        id: Long,
    ): PullReason? =
        namedIn(options.sessions, scope.sessions) { ExchangeRef(kind, id) in graph.sessionMentions[it].orEmpty() }
            ?.let { PullReason(PullKind.SESSION, it) }

    private fun namedIn(
        items: List<ExchangeItem>,
        ids: Set<Long>,
        matches: (Long) -> Boolean,
    ): String? = items.firstOrNull { it.id in ids && matches(it.id) }?.name

    fun allShownTicked(): Boolean = rows().let { rows -> rows.isNotEmpty() && rows.all { it.mark == PickMark.On } }

    /** a tap on a ticked or unticked row, a pulled row is not the person's to untick */
    fun toggled(id: Long): ExchangePick {
        val ticked = selection.idsOf(tab)
        return when {
            id in ticked -> copy(selection = selection.withIds(tab, ticked - id))
            id in scope.idsOf(tab) -> this
            else -> copy(selection = selection.withIds(tab, ticked + id))
        }
    }

    /** ticks every row the search shows, or unticks them when all of them were ticked */
    fun allToggled(): ExchangePick {
        val shown = rows().map { it.id }.toSet()
        val ticked = allShownTicked()
        val current = selection.idsOf(tab)
        return copy(selection = selection.withIds(tab, if (ticked) current - shown else current + shown))
    }

    fun withTab(next: ExchangeTab): ExchangePick = copy(tab = next, query = "")

    fun withQuery(next: String): ExchangePick = copy(query = next)

    private fun markOf(
        id: Long,
        explicit: Set<Long>,
        all: Set<Long>,
    ): PickMark =
        when {
            id in explicit -> PickMark.On
            id in all -> PickMark.Pulled
            else -> PickMark.Off
        }

    companion object {
        /** everything ticked, the party too - what "the whole campaign" takes */
        fun everything(
            options: ExchangeOptions,
            graph: ExchangeGraph,
            photos: Boolean = true,
        ): ExchangePick =
            ExchangePick(
                options,
                graph,
                RecordSelection(
                    sessions = options.sessions.ids(),
                    places = options.places.ids(),
                    npcs = options.npcs.ids(),
                    quests = options.quests.ids(),
                    species = options.species.ids(),
                    party = options.party.ids(),
                    tags = true,
                    phrases = true,
                    photos = photos,
                ),
            )

        /** the start of a hand pick, everything ticked but the party, which only comes when a note mentions it */
        fun handPick(
            options: ExchangeOptions,
            graph: ExchangeGraph,
            photos: Boolean = true,
        ): ExchangePick =
            everything(options, graph, photos).let { it.copy(selection = it.selection.copy(party = emptySet())) }
    }
}

private fun List<ExchangeItem>.ids(): Set<Long> = map { it.id }.toSet()

private fun ExchangeOptions.itemsOf(tab: ExchangeTab): List<ExchangeItem> =
    when (tab) {
        ExchangeTab.PLACES -> places
        ExchangeTab.NPCS -> npcs
        ExchangeTab.QUESTS -> quests
        ExchangeTab.SPECIES -> species
        ExchangeTab.SESSIONS -> sessions
    }

private fun RecordSelection.idsOf(tab: ExchangeTab): Set<Long> =
    when (tab) {
        ExchangeTab.PLACES -> places
        ExchangeTab.NPCS -> npcs
        ExchangeTab.QUESTS -> quests
        ExchangeTab.SPECIES -> species
        ExchangeTab.SESSIONS -> sessions
    }

private fun RecordScope.idsOf(tab: ExchangeTab): Set<Long> =
    when (tab) {
        ExchangeTab.PLACES -> places
        ExchangeTab.NPCS -> npcs
        ExchangeTab.QUESTS -> quests
        ExchangeTab.SPECIES -> species
        ExchangeTab.SESSIONS -> sessions
    }

private fun RecordSelection.withIds(
    tab: ExchangeTab,
    ids: Set<Long>,
): RecordSelection =
    when (tab) {
        ExchangeTab.PLACES -> copy(places = ids)
        ExchangeTab.NPCS -> copy(npcs = ids)
        ExchangeTab.QUESTS -> copy(quests = ids)
        ExchangeTab.SPECIES -> copy(species = ids)
        ExchangeTab.SESSIONS -> copy(sessions = ids)
    }

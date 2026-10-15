package com.lyrismet.incadent.feature.codex.domain.grouping

import com.lyrismet.incadent.feature.codex.domain.model.Location
import com.lyrismet.incadent.feature.codex.domain.model.Npc
import com.lyrismet.incadent.feature.codex.domain.model.NpcStatus
import com.lyrismet.incadent.feature.codex.domain.model.Quest
import com.lyrismet.incadent.feature.codex.domain.model.QuestStatus

/** one titled section of a list - a null [title] is the ungrouped "as a list" view */
data class CodexGroup<T>(
    val title: String?,
    val items: List<T>,
    // a tree section also carries the path above its place, and the place's id when its header folds the section
    val path: String? = null,
    val toggleId: Long? = null,
    val collapsed: Boolean = false,
    // the number shown on the header - a folded section keeps it while its items are hidden
    val count: Int = items.size,
)

/** an entity inside a group, with the subtitle the grouping swaps in - null keeps the entity's default subtitle */
data class CodexGroupItem<T>(
    val entity: T,
    val subtitle: String?,
)

fun <T, R> CodexGroup<T>.map(transform: (T) -> R): CodexGroup<R> =
    CodexGroup(title, items.map(transform), path, toggleId, collapsed, count)

/** the words the section titles are built from, resolved from resources by the presenter */
data class CodexGroupLabels(
    val noRegionForQuest: String,
    val noPlace: String,
    val noFaction: String,
    val noGiver: String,
    val giverPrefix: String,
    val topLevel: String,
    val npcStatus: Map<NpcStatus, String>,
    val questStatus: Map<QuestStatus, String>,
)

fun groupNpcs(
    npcs: List<Npc>,
    locations: List<Location>,
    by: NpcGroupBy,
    labels: CodexGroupLabels,
    regions: CodexRegions = CodexRegions(npcs, locations),
): List<CodexGroup<CodexGroupItem<Npc>>> {
    val rows =
        sortNpcs(npcs).map { npc ->
            CodexGroupItem(npc, subtitle = if (by == NpcGroupBy.REGION) placeLine(npc, locations) else null)
        }
    return when (by) {
        NpcGroupBy.NONE -> rows.asUngrouped()
        // an npc met at several places shows up under each of their regions
        NpcGroupBy.REGION ->
            rows.toSectionsMulti { row ->
                regions
                    .regionsOfNpc(row.entity)
                    .map { it.name.toSectionKey(labels.noPlace) }
                    .ifEmpty { listOf(null.toSectionKey(labels.noPlace)) }
            }
        NpcGroupBy.RELATION -> rows.toSections { it.entity.status.toSectionKey(labels.npcStatus) }
        NpcGroupBy.FACTION -> rows.toSections { it.entity.faction.toSectionKey(labels.noFaction) }
    }
}

@Suppress("LongParameterList")
fun groupQuests(
    quests: List<Quest>,
    npcs: List<Npc>,
    locations: List<Location>,
    by: QuestGroupBy,
    labels: CodexGroupLabels,
    regions: CodexRegions = CodexRegions(npcs, locations),
): List<CodexGroup<Quest>> {
    val rows = sortQuests(quests)
    return when (by) {
        QuestGroupBy.NONE -> rows.asUngrouped()
        QuestGroupBy.REGION ->
            rows.toSections { regions.regionOfQuest(it)?.name.toSectionKey(labels.noRegionForQuest) }
        QuestGroupBy.STATUS -> rows.toSections { it.status.toSectionKey(labels.questStatus) }
        QuestGroupBy.GIVER -> rows.toSections { giverKey(it, npcs, labels) }
    }
}

@Suppress("LongParameterList")
fun groupLocations(
    locations: List<Location>,
    by: LocationGroupBy,
    allLocations: List<Location>,
    labels: CodexGroupLabels,
    regions: CodexRegions = CodexRegions(emptyList(), allLocations),
    collapsedIds: Set<Long> = emptySet(),
): List<CodexGroup<CodexGroupItem<Location>>> =
    when (by) {
        LocationGroupBy.NONE ->
            sortLocations(locations)
                .map { place ->
                    val path = regions.places.ancestorsLabel(place)
                    CodexGroupItem(place, subtitle = path.ifBlank { labels.topLevel })
                }.asUngrouped()
        LocationGroupBy.REGION -> groupLocationTree(locations, regions.places, collapsedIds)
    }

// the top level places first as plain rows, then one section per place that holds places, titled with its name and
// the path above it - a section lists only what lies directly in it, so no section swallows its descendants
private fun groupLocationTree(
    locations: List<Location>,
    tree: PlaceTree,
    collapsedIds: Set<Long>,
): List<CodexGroup<CodexGroupItem<Location>>> {
    val shownIds = locations.map { it.id }.toSet()
    val visited = HashSet<Long>()
    val sections = ArrayList<CodexGroup<CodexGroupItem<Location>>>()

    fun rows(places: List<Location>) = sortLocations(places).map { CodexGroupItem(it, subtitle = null) }

    fun walk(place: Location) {
        if (!visited.add(place.id)) return
        val children = tree.childrenOf(place.id)
        val own = children.filter { it.id in shownIds }
        if (own.isNotEmpty()) {
            val folded = place.id in collapsedIds
            sections +=
                CodexGroup(
                    title = place.name.uppercase(),
                    items = if (folded) emptyList() else rows(own),
                    path = tree.shortAncestorsLabel(place).ifBlank { null },
                    toggleId = place.id,
                    collapsed = folded,
                    count = own.size,
                )
        }
        sortLocations(children).forEach(::walk)
    }
    sortLocations(tree.roots).forEach(::walk)
    // places on a parent loop are reachable from no top level place, they still belong in the list
    val unreached = locations.filter { it.id !in visited && it.parentId != null && it.id in shownIds }
    val top = rows(tree.roots.filter { it.id in shownIds } + unreached)
    return (if (top.isEmpty()) emptyList() else listOf(CodexGroup(null, top))) + sections
}

private fun <T> List<T>.asUngrouped(): List<CodexGroup<T>> =
    if (isEmpty()) emptyList() else listOf(CodexGroup(null, this))

private fun <T> List<T>.toSections(keyOf: (T) -> SectionKey): List<CodexGroup<T>> =
    toSectionsMulti {
        listOf(keyOf(it))
    }

// keeps the incoming order inside each section, orders the sections by key with the "no value" one last
private fun <T> List<T>.toSectionsMulti(keysOf: (T) -> List<SectionKey>): List<CodexGroup<T>> {
    val sections = LinkedHashMap<SectionKey, MutableList<T>>()
    forEach { item -> keysOf(item).distinct().forEach { key -> sections.getOrPut(key) { mutableListOf() }.add(item) } }
    val ordered =
        sections.toList().sortedWith(
            compareBy<Pair<SectionKey, List<T>>>(
                { it.first.order == null },
                { it.first.order },
                { it.first.title },
            ),
        )
    return ordered.map { (key, items) -> CodexGroup(key.title, items) }
}
